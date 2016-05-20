/*
 * (C) 2006 SAP XI 7.1 Adapter Framework Resource Adapter Skeleton
 */
package com.equalize.xpi.adapter.ra;

import java.io.File;
import java.util.LinkedList;
import java.util.Locale;

import javax.resource.ResourceException;

import com.sap.aii.af.service.administration.api.AdapterCallback;
import com.sap.aii.af.service.administration.api.AdapterCapability;
import com.sap.aii.af.service.administration.api.AdapterRegistry;
import com.sap.aii.af.service.administration.api.AdapterRegistryFactory;
import com.sap.aii.af.service.administration.api.cpa.CPAFactory;
import com.sap.aii.af.service.administration.api.cpa.CPALookupManager;
import com.sap.aii.af.service.administration.api.cpa.ChannelLifecycleCallback;
import com.sap.aii.af.service.administration.api.i18n.LocalizationCallback;
import com.sap.aii.af.service.administration.api.i18n.LocalizationNotPossibleException;
import com.sap.aii.af.service.administration.api.monitoring.ChannelState;
import com.sap.aii.af.service.administration.api.monitoring.ChannelStatus;
import com.sap.aii.af.service.administration.api.monitoring.ChannelStatusCallback;
import com.sap.aii.af.service.administration.api.monitoring.ChannelStatusFactory;
import com.sap.aii.af.service.administration.api.monitoring.ChannelUnknownException;
import com.sap.aii.af.service.cpa.Channel;
import com.sap.aii.af.service.cpa.Direction;

/**
 * Type: XIConfiguration
 * 
 * Semantic: XIConfiguration manages all the XI channel information.
 * During the start up phase all channels for this adapter are read by using the AF AAM and CPA API.
 * During the run time this information is kept up-to-date by registering a call back method at the XI AAM service.
 * Please note that this class was changed heavily from 3.0 SP16 to SP17: The CPA callback mechanism was replaced by the AAM machanism
 * and the CPA managers are used from the AAM directly.
 * (ra implementation specific) 
 * 
 * @version: $Id: //tc/xpi.external/NW07_07_REL/src/_sample_rar_module/rar/src/com/sap/aii/af/sample/adapter/ra/XIConfiguration.java#1 $
 **/
public class XIConfiguration implements ChannelLifecycleCallback, ChannelStatusCallback, LocalizationCallback  {

	private static final XITrace TRACE = new XITrace(XIConfiguration.class.getName());

	// Adapter type and namespace might be set to other values depending on the mcf settings 
	private static String ADAPTER_TYPE = AdapterConstants.adapterType;
	private static String ADAPTER_NAMESPACE = AdapterConstants.adapterNamespace;  
	private String adapterType;
	private String adapterNamespace;  

	// Lists with all channels for this adapter type
	private LinkedList outboundChannels = null; //Note that other collections might show a better performance
	private LinkedList inboundChannels = null;

	// AAM access classes
	private CPALookupManager lookupManager = null;
	private AdapterRegistry adapterRegistry = null;
	private LocalizationCallback localizer = null;

	// Store reference to calling mcf to propagte callback information
	private SPIManagedConnectionFactory mcf = null;
	
	/**
	 * Creates a XI configuration object. Therefore the XI AAM LookupManager is instanciated.
	 * Use default adapter values
	 */
	public XIConfiguration() {
		this(ADAPTER_TYPE, ADAPTER_NAMESPACE);
	}

	/**
	 * Creates a XI configuration object. Therefore the XI AAM LookupManager is instanciated.
	 * 
	 * @param adapterType Adapter type as configured in the mcf property settings.
	 * @param adapterNamespace Namespace for this adapter type as configured in the mcf property settings.
	 */
	public XIConfiguration(String adapterType, String adapterNamespace) {
		final String SIGNATURE = "XIConfiguration(String adapterType, String adapterNamespace)";
		TRACE.entering(SIGNATURE, new Object[] {adapterType, adapterNamespace});
		try {
			CPAFactory cf = CPAFactory.getInstance();
			lookupManager = cf.getLookupManager();
		} catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
			TRACE.errorT(SIGNATURE, XIAdapterCategories.CONFIG, "CPALookupManager cannot be instantiated due to {0}", new Object[] {e.getMessage()});
			TRACE.errorT(SIGNATURE, XIAdapterCategories.CONFIG, "No channel configuration can be read, no message exchange possible!");
		}
		this.adapterType = adapterType;
		this.adapterNamespace = adapterNamespace;
		TRACE.exiting(SIGNATURE);
	}

	/**
	 * @see com.sap.aii.af.service.administration.api.cpa#channelAdded(com.sap.aii.af.service.cpa.Channel)
	 */
	// CS_CPACB START
	public void channelAdded(Channel channel) {
		final String SIGNATURE = "channelAdded(Channel channel)";
		TRACE.entering(SIGNATURE, new Object[] {channel});

		String dir = null;
		String name = null;
		
		// Store channel in local lists. Please note: Synchronization could be optimized by synchronization on the lists
		synchronized (this) {
			if (channel.getDirection() == Direction.INBOUND) {
				inboundChannels.add(channel);
				try {
					dir = channel.getValueAsString("fileInDir");
					name = channel.getValueAsString("fileInName");
				}
				catch (Exception e) {
					TRACE.catching(SIGNATURE, e);
					TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Channel configuration value cannot be read due to {0}", new Object[] {e.getMessage()});
				}	
			}
			else if (channel.getDirection() == Direction.OUTBOUND) {
				outboundChannels.add(channel);
				try {
					//dir = channel.getValueAsString("fileOutDir");
					//name = channel.getValueAsString("fileOutPrefix");
					dir = channel.getValueAsString("xpathToFile");
					name = "";
				}
				catch (Exception e) {
					TRACE.catching(SIGNATURE, e);
					TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Channel configuration value cannot be read due to {0}", new Object[] {e.getMessage()});
				}	
			}
		}
		
		// Trace the new channel; avoid throwing exceptions here, channel errors should be reported in the monitoring
		TRACE.infoT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Channel with ID {0} for party {1} and service {2} added (direction is {3}, directory: {4}, name: {5}).", 
			new Object[] {channel.getObjectId(), channel.getParty(), channel.getService(), channel.getDirection().toString(), dir, name}); 
		TRACE.exiting(SIGNATURE);
	}

	/**
	 * @see com.sap.aii.af.service.administration.api.cpa#channelUpdated(com.sap.aii.af.service.cpa.Channel)
	 */
	public void channelUpdated(Channel channel) {
		final String SIGNATURE = "channelUpdated(Channel channel)";
		TRACE.entering(SIGNATURE);
		//Performance optimization are possible here but since the number of channels is usually low it has no big impact
		channelRemoved(channel);
		channelAdded(channel);
		TRACE.exiting(SIGNATURE);
	}

	/**
	 * @see com.sap.aii.af.service.administration.api.cpa#channelRemoved(com.sap.aii.af.service.cpa.Channel)
	 */
	public void channelRemoved(Channel channel) {
		final String SIGNATURE = "channelRemoved(Channel channel)";
		TRACE.entering(SIGNATURE, new Object[] {channel});
		LinkedList channels = null;

		TRACE.infoT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Channel with ID {0} for party {1} and service {2} will be removed now. (direction is {3}).", 
				new Object[] {channel.getObjectId(), channel.getParty(), channel.getService(), channel.getDirection().toString()}); 

		String channelID = channel.getObjectId();
		
		if (channel.getDirection() == Direction.INBOUND) 
			channels = inboundChannels;
		else
			channels = outboundChannels;
		
		// Remove the channel form the local list and inform the mcf in case of outbound (=CCIConnection) channels
		// This allows the mcf to delete the related CCIConnection as well
		synchronized (this) {
			for (int i = 0; i < channels.size(); i++) {
				Channel storedChannel = (Channel) channels.get(i);
				if (storedChannel.getObjectId().equalsIgnoreCase(channelID)) {
					channels.remove(i);
					if (channel.getDirection() == Direction.OUTBOUND) {
						try {
							mcf.destroyManagedConnection(channelID);
						} catch (Exception e) {
							TRACE.catching(SIGNATURE, e);
							TRACE.warningT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "The ManagedConnection for channel {0} cannot be destroyed. Configuration update might not work.", new Object[] {channelID}); 
						}
					}
					break;
				}
			}
		}		
		TRACE.exiting(SIGNATURE);
	}
	// CS_CPACB END
	
	/**
	 * Initializes the inbound and outbound channel lists and takes care of
	 * the XI CPA callback registration.
	 * @param mcf MCF that has to be informed in case of channel updates
	 * @throws ResourceException if the CPA lookup or registration fails
	 */
	public void init(SPIManagedConnectionFactory mcf) throws ResourceException {
		final String SIGNATURE = "init(mcf)";
		TRACE.entering(SIGNATURE);

		String dir = null;
		String name = null;
		this.mcf = mcf;
	
		// Register this adapter at XI AAM
		//CS_CPAREG START
		//CS_ADMONR START
		try {
			localizer = XILocalizationUtilities.getLocalizationCallback();
			AdapterRegistryFactory arf = AdapterRegistryFactory.getInstance();
			adapterRegistry = arf.getAdapterRegistry();
			adapterRegistry.registerAdapter(adapterNamespace, adapterType, new AdapterCapability[] { AdapterCapability.PUSH_PROCESS_STATUS }, new AdapterCallback[] {this});
		} catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
			ResourceException re = new ResourceException("XI AAM registration failed due to: " + e.getMessage());
			TRACE.throwing(SIGNATURE, re);
			throw re;
		}
		//CS_CPAREG END
		//CS_ADMONR END

		// Get all channels
		synchronized(this) {
			inboundChannels = new LinkedList();
			outboundChannels = new LinkedList();
	
			// First get all channels for this adapter. By using the AAM service it will receive the "started" channels only.
			//CS_CHINLU START
			try {
				LinkedList allChannels = lookupManager.getChannelsByAdapterType(adapterType, adapterNamespace);
				TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "The XI AAM service returned {0} channels for adapter type {1} with namespace {2}", 
					new Object[] {new Integer(allChannels.size()), adapterType, adapterNamespace}); 

				for (int i = 0; i < allChannels.size(); i++) {
					Channel channel = (Channel) allChannels.get(i);
					if (channel.getDirection() == Direction.INBOUND) {
						inboundChannels.add(channel);
						dir = channel.getValueAsString("fileInDir");
						name = channel.getValueAsString("fileInName");
					}
					else if (channel.getDirection() == Direction.OUTBOUND) {
						outboundChannels.add(channel);
						//dir = channel.getValueAsString("fileOutDir");
						//name = channel.getValueAsString("fileOutPrefix");
						dir = channel.getValueAsString("xpathToFile");
						name = "";
					}
					else
						continue;  //Ignore unknown direction channels
					TRACE.infoT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Channel with ID {0} for party {1} and service {2} added (direction is {3}, directory: {4}, name: {5}).", 
						new Object[] {channel.getObjectId(), channel.getParty(), channel.getService(), channel.getDirection().toString(), dir, name}); 
				}
			} catch (Exception e) {
				TRACE.catching(SIGNATURE, e);
				ResourceException re = new ResourceException("XI CPA lookup failed due to: " + e.getMessage());
				TRACE.throwing(SIGNATURE, re);
				throw re;
			}
			//CS_CHINLU END
		}
		TRACE.exiting(SIGNATURE);
	}

	/**
	 * Stops the automatic configuration update via CPA callback
	 */
	public void stop() throws ResourceException {
		final String SIGNATURE = "stop()";
		TRACE.entering(SIGNATURE);
		// CS_ADMOND START
		try {
			adapterRegistry.unregisterAdapter(adapterNamespace, adapterType);
		} catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
			ResourceException re = new ResourceException("XI AAM unregistration failed due to: " + e.getMessage());
			TRACE.throwing(SIGNATURE, re);
			throw re;
		}
		// CS_ADMOND END
		TRACE.exiting(SIGNATURE);
	}

	/**
	 * Get a copy of one of the channel lists for internal processings
	 * @param direction Determines whether inbound (sender) channels or outbound (receiver) channels will be returned
	 * @throws ResourceException if the direction is invalid
	 */
	public LinkedList getCopy(Direction direction) throws ResourceException {
		final String SIGNATURE = "getCopy(Direction direction)";
		LinkedList out = null;
		if ((inboundChannels == null) || (outboundChannels == null))
			init(mcf);
		synchronized(this) {
			if (direction == Direction.INBOUND)
				out = (LinkedList) inboundChannels.clone();
			else if (direction == Direction.OUTBOUND)
				out = (LinkedList) outboundChannels.clone();
			else {
				ResourceException re = new ResourceException("Direction invalid");
				TRACE.throwing(SIGNATURE, re);
				throw re;
			}
		}	
		return out;
	}

	/**
	 * The <code>getChannelStatus()</code> is called by the XI AF administration GUI's to visualize the
	 * channel status of adapters. The adapter might also propagate processing status information with
	 * the AAM push method (using <code>com.sap.aii.af.service.administration.api.monitoring.MonitoringManager</code>). 
	 * Channel status can also be reported actively (via the push mechansim) but <code>getChannelStatus()</code> and
	 * <code>reportChannelStatus()</code> are mutually exclusive. This sample here uses pull for channel status and
	 * push for process status.  
	 * Starting with XI 3.0 SP17 the monitoring part was changed significantly since the previous 
	 * <code>com.sap.aii.af.service.monitor.api.AdapterMonitor</code> did allow to collect unstructured status such as
	 * the status of the MCF, other sub-components and channels. With AAM, the channel and process status can be
	 * reported in a well-defined way that allows to provide general additional service on top, like alerting or localized
	 * status reports.
	 * @param channel: The channel for which the status is reported
	 * @param locale: Localization of human readable strings
	 * @return ChannelStatus that describes the status of the channel
	 * (XI specific)
	**/
	// CS_ADMON START
	public ChannelStatus getChannelStatus(Channel channel, Locale locale) throws ChannelUnknownException { 
		final String SIGNATURE = "getChannelStatus(Channel channel, Locale locale)";
		TRACE.entering(SIGNATURE, new Object[] {channel, locale});
		
		// Prepare lookup
		boolean channelFound = false;
		Channel storedChannel = null;
		String channelID = "<unknown>";
		Exception cause = null;
		ChannelStatus cs = null;
		
		// Lookup internal channel
		try {
			channelID = channel.getObjectId();
			LinkedList channels = null;
			if (channel.getDirection() == Direction.INBOUND) 
				channels = inboundChannels;
			else
				channels = outboundChannels;
			
			// Read the channel from locale list
			synchronized (this) {
				for (int i = 0; i < channels.size(); i++) {
					storedChannel = (Channel) channels.get(i);
					if (storedChannel.getObjectId().equalsIgnoreCase(channelID)) {
						channelFound = true;
						break;
					}
				}
			}		
		} catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
			cause = e;
			TRACE.errorT(SIGNATURE, XIAdapterCategories.CONFIG, "Channel lookup failed due to {0}.", new Object[] {e.getMessage()});
		}
		
		if (!channelFound) {
			ChannelUnknownException cue = new ChannelUnknownException("Channel with ID " + channelID + " is not known.", cause);
			TRACE.errorT(SIGNATURE, XIAdapterCategories.CONFIG, "Channel {0} is not known.", new Object[] {channelID});
			TRACE.throwing(SIGNATURE, cue);
			throw cue;
		}

		// Create factory for ChannelStatus
		ChannelStatusFactory csf = ChannelStatusFactory.getInstance();
		if (csf == null) {
			ChannelUnknownException cue = new ChannelUnknownException("Internal error: Unable to get instance of ChannelStatusFactory.", cause);
			TRACE.errorT(SIGNATURE, XIAdapterCategories.CONFIG, "Unable to get instance of ChannelStatusFactory.");
			TRACE.throwing(SIGNATURE, cue);
			throw cue;
		}

		// Determine channel status and report it
		try {
			// Check directory adapter status of channel is not done anymore since AAM start/stop with channelAdded/Deleted was introduced 

			if (storedChannel.getDirection() == Direction.INBOUND) {
				String directory = channel.getValueAsString("fileInDir");
				if ((directory == null) || (directory.length() == 0)) {
					TRACE.warningT(SIGNATURE, XIAdapterCategories.CONFIG, "Unable to determine input file directory. Take default: " + SPIManagedConnectionFactory.IN_DIR);
					directory = SPIManagedConnectionFactory.IN_DIR;
				}	
				String name = channel.getValueAsString("fileInName");
				if ((name == null) || (name.length() == 0)) {
					TRACE.warningT(SIGNATURE, XIAdapterCategories.CONFIG, "Unable to determine input file prefix. Take default: " + SPIManagedConnectionFactory.IN_NAME);
					name = SPIManagedConnectionFactory.IN_NAME;
				}	
				// Only check the dir here. No file is not an error situation!
				File dir = new File (directory);
				if (!dir.exists()) {
					cs = csf.createChannelStatus(channel, ChannelState.ERROR, "Input file directory " + directory + " does not exists.");
					TRACE.exiting(SIGNATURE, new Object[] {cs});
					return cs;
				}
			
				// Check whether the mcf in principle is up and running (i.e. thread is running).
				if (!mcf.isRunning()) {
					cs = csf.createChannelStatus(channel, ChannelState.ERROR, "The JCA adapter inbound thread is not working correctly. No inbound messages possible!");
					TRACE.exiting(SIGNATURE, new Object[] {cs});
					return cs;
				}
			} else {
				String xpath = channel.getValueAsString("xpathToFile");
				if ((xpath == null) || (xpath.length() == 0)) {
					cs = csf.createChannelStatus(channel, ChannelState.ERROR, "XPath expression is not set.");
					TRACE.exiting(SIGNATURE, new Object[] {cs});
					return cs;
				}	
/*				String directory = channel.getValueAsString("fileOutDir");
				if ((directory == null) || (directory.length() == 0)) {
					cs = csf.createChannelStatus(channel, ChannelState.ERROR, "Output file directory name is not set.");
					TRACE.exiting(SIGNATURE, new Object[] {cs});
					return cs;
				}	
				else {
					File dir = new File (directory);
					if (!dir.exists()) {
						cs = csf.createChannelStatus(channel, ChannelState.ERROR, "Output file directory " + directory + " does not exists.");
						TRACE.exiting(SIGNATURE, new Object[] {cs});
						return cs;
					}
				}*/
			}

			// Channel is OK
			cs = csf.createChannelStatus(channel, ChannelState.OK, localizer.localizeString("CHANNEL_OK", locale));

		} catch (Exception e) {
			// No special handling of LocalizationNotPossibleException here. This might be changed for a product implementation. 
			TRACE.catching(SIGNATURE, e);
			TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Cannot retrieve status for channel {0}. Received exception: {1}", new Object[] {channel.getChannelName(), e.getMessage()});
			cs = csf.createChannelStatus(channel, ChannelState.ERROR, "Cannot retrieve status for this channel due to: " + e.getMessage());
			TRACE.exiting(SIGNATURE, new Object[] {cs});
			return cs;
		}
		
		TRACE.exiting(SIGNATURE, new Object[] {cs});
		return cs;
	}
	// CS_ADMON END

	// CS_LOCAL START
	/** 
	 * The implementation of the LocalizationCallback allows the AAM to translate the channel and process status string at 
	 * a later point in time into the locale of the browser that the user currently uses. It is called for the 
	 * the push status mechanism (In case of status pull the locale is set in the request and the adapter can return
	 * the translated text directly). 
	 * @see com.sap.aii.af.service.administration.api.i18n.LocalizationCallback#localizeString(java.lang.String, java.util.Locale)
	 */
	public String localizeString(String str, Locale locale) throws LocalizationNotPossibleException {
		return localizer.localizeString(str, locale);
	}
	// CS_LOCAL END
}
