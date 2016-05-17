/*
 * (C) 2006 SAP XI 7.1 Adapter Framework Resource Adapter Skeleton
 */

package com.equalize.xpi.adapter.ra;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.ejb.NoSuchObjectLocalException;
import javax.naming.InitialContext;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
//import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.security.PasswordCredential;
import javax.security.auth.Subject;

import com.sap.transaction.TxException;
import com.sap.transaction.TxManager; 
import com.sap.transaction.TransactionTicket;
import com.sap.transaction.TxRollbackException;

import com.sap.guid.GUID;

import com.sap.engine.interfaces.connector.ManagedConnectionFactoryActivation; 

import com.sap.engine.interfaces.messaging.api.Message;
import com.sap.engine.interfaces.messaging.api.Payload;
import com.sap.engine.interfaces.messaging.api.PublicAPIAccessFactory;
import com.sap.engine.interfaces.messaging.api.TextPayload;
import com.sap.engine.interfaces.messaging.api.XMLPayload;
import com.sap.engine.interfaces.messaging.api.DeliverySemantics;
import com.sap.engine.interfaces.messaging.api.MessageDirection;
import com.sap.engine.interfaces.messaging.api.MessageKey;

import com.sap.engine.interfaces.messaging.api.auditlog.AuditLogStatus;
import com.sap.engine.interfaces.messaging.api.auditlog.AuditAccess;

import com.sap.aii.af.lib.mp.module.ModuleData;
import com.sap.aii.af.lib.mp.processor.ModuleProcessor;
import com.sap.aii.af.lib.mp.processor.ModuleProcessorFactory;

import com.sap.aii.af.lib.ra.cci.XIAdapterException;

import com.sap.aii.af.service.administration.api.cpa.CPAFactory;
import com.sap.aii.af.service.administration.api.cpa.CPAInboundRuntimeLookupManager;
import com.sap.aii.af.service.cpa.Binding;
import com.sap.aii.af.service.cpa.CPAObjectType;
import com.sap.aii.af.service.cpa.Channel;
import com.sap.aii.af.service.cpa.Direction;
import com.sap.aii.af.service.cpa.NormalizationManager;
import com.sap.aii.af.service.cpa.Party;
import com.sap.aii.af.service.cpa.Service;
import com.sap.aii.af.service.idmap.MessageIDMapper;
import com.sap.aii.af.service.resource.SAPAdapterResources;

import com.sap.aii.af.service.administration.api.monitoring.MonitoringManager;
import com.sap.aii.af.service.administration.api.monitoring.MonitoringManagerFactory;
import com.sap.aii.af.service.administration.api.monitoring.ProcessContext;
import com.sap.aii.af.service.administration.api.monitoring.ProcessContextFactory;
import com.sap.aii.af.service.administration.api.monitoring.ChannelDirection;
import com.sap.aii.af.service.administration.api.monitoring.ProcessState;

/**
 * An object of the class <code>SpiManagedConnectionFactory</code> (MCF) is a factory of both,
 * <code>ManagedConnection</code> and <code>CciConnectionFactory</code> instances.
 * This class supports connection pooling by defining methods for matching and creating connections.
 * <code>SpiManagedConnectionFactory</code> MUST be a Java Bean to be JCA compliant (see pg. 51)
 * A SAP J2EE server peculiarity is the implementation of the <code>ManagedConnectionFactoryActivation</code>
 * interface. This allows the MCF to be informed when the adapter is being started or stopped.
 * It represents a JCA 1.0 workaround for the missing JCA 1.5 activation feature in NetWeaver'04 (hence
 * it is deprecated since the next major NetWeaver version supports JCA 1.5).  
 * 
 * <b>Changes from 3.0/7.0 to 7.1:</b>
 * 1. Repackaging com.sap.engine.interfaces.messaging
 * 2. AuditLog simplified: MessageDirection, MessageKey instead of AuditDirection, AuditKey
 * 3. MessageIDMapper: 
 * 4. Use of SAP J2EE transaction manager instead of JTA. (com.sap.transaction.TxManager instead of javax.transaction.TransactionManager) 
 * 5. XIInteractionSpec: START, STOP removed, is offered by com.sap.aii.af.service.administration.api now
 * 6. XIMessageRecord does not extend com.sap.engine.interfaces.messaging.api.Message anymore, but wraps it now. Use get/setMessage() instead of direct calls.
 * 7. Introduce XITrace utility class and refer to J2EE logging directly
 * 8. Built against the new XI AF facades, see cross reference Excel for details.
 * 9. ModuleProcessor reference is not cached. (7.1 SP3 change)
 * 
 * @version: $Id: //tc/xpi.external/NW07_07_REL/src/_sample_rar_module/rar/src/com/sap/aii/af/sample/adapter/ra/SPIManagedConnectionFactory.java#1 $
 **/

public class SPIManagedConnectionFactory implements ManagedConnectionFactory, Serializable, Runnable, ManagedConnectionFactoryActivation {

	// See Serializable
	static final long serialVersionUID = -2387046407149571208L;

	// Trace and audit log
	private static final XITrace TRACE = new XITrace(SPIManagedConnectionFactory.class.getName());
	private AuditAccess audit = null;

	// For debug purposes: Use a GUID and a timer for "keep alive" trace messages
	private GUID mcfLocalGuid = null;         // GUID of this mcf
	private Timer controlTimer = new Timer();
	private static final int TIMER_FIRST_RUN = 120000;
	private static final int TIMER_PERIOD = 60000;

	// Some internal control parameters 
	private static int fileCounter = 0; 
	private static int waitTime = 5000; 

	// Adjust the JNDI name of the RA when this sample is copied and make it configurable!
    public static final String JNDI_NAME = "deployedAdapters/sample_ra/shareable/sample_ra";
    
    // The JCA logwriter stream (not used by the XI AF)
    transient PrintWriter logWriter;
    
    // An object for synchronization purposes
	private static Object synchronizer = new Object();
	
	// Use the XI AF SAPAdapterResources for thread and transaction management as replacement for missing JCA 1.5 functionality
	private SAPAdapterResources  msRes = null;

	// Thread status of the inbound processing thread
	private int threadStatus = TH_INIT;
	private static final int TH_INIT 	= 0;
	private static final int TH_STARTED = 1;
	private static final int TH_STOPPED = 2;


	// The initial context for JNDI lookups
	private InitialContext ctx = null;
	
	// A specific class that manages the XI configuration
	private XIConfiguration xIConfiguration = null;

	// A Map to manage all created ManagedConnections
	// Although the J2EE JCA container manages pools of ManagedConnections as well, it is reasonable
	// to maintain the own ManagedConnection objects because ManagedConnection in this sample	
	// has a 1:1 relationship to XI receiver channels. And those XI receiver channles might be removed, changed, etc.
	// by XI CPA cache updates which are not synchronized with the JCA container. 
	// => When the ManagedConnection is removed (by XI CPA) it must be invalidated here and the container must be informed (via event connection.closed)
	// The Map must be synchronized because it might be changed by a CPA update or the JCA container in parallel.
	private Map managedConnections = Collections.synchronizedMap(new HashMap());

	// XI AF Message ID mapper
	private transient MessageIDMapper messageIDMapper = null;
    private transient XIMessageFactoryImpl mf = null; 

	// Channel status
	static final String AS_ACTIVE = "active";
	static final String AS_INACTIVE = "inactive";
	
	// Address modes
	private static final String AM_CPA = "CPA";
	private static final String AM_MSG = "MSG";

	// Address schema used in this sample
	private static final String ADDR_AGENCY_EAN = "009"; //Values see XI IB
	private static final String ADDR_SCHEMA_GLN = "GLN"; //Values see XI IB

	// The next MCF properties are typical JCA MCF settings that can't be
	// set in the XI ID since the values must be known beforehand
	// Hence the adapter waits during the startup phase till all values are known
	private String addressMode = null;
	private String adapterType = null;
	private String adapterNamespace = null;
	private int propWaitNum = 10; 
	private int propWaitTime = 1000; 
	
	// Defaults if channel parameters cannot be read
	static final String OUT_DIR      = "c:/temp";
	static final String OUT_PREFIX   = "sample_ra_output";
	static final String IN_DIR       = "c:/temp";	static final String IN_NAME      = "sample_ra_input";

	// The process mode controls the handling of a sent file (sender channel)
	private static final String PM_TEST   = "test";     // Do not change the file, no duplicate check
	private static final String PM_RENAME = "rename";   // Rename the file, use duplicate check to guarantee EO

	// The file mode controls the handling of a received file (receiver channel)
	static final String FM_NEW     = "new";     // Create a new file for each message
	static final String FM_REPLACE = "replace"; // Use one file only, replace it with the new message if it exists

	// Message quality of service constants
	private static final String QOS_EO   = "EO";    
	private static final String QOS_EOIO = "EOIO";    
	private static final String QOS_BE   = "BE";    

	// Configurable error conditions for demo purposes
	private static final String ERR_NONE     = "none";    
	private static final String ERR_ROLLBACK = "rollback";    

	// Adapter specific message attributes (ASMA)
	static final String ASMA_NAME    = "JCAChannelID"; // see also Adaptermetadata of sample

	/**
	 * The JCA specification requires a default constructor since this class must be Java Bean compliant
	 * and is propagated from this class to them.
	 * XI specific is the access to the SAPAdapterResource typed objects that
	 * allow access to the SAP J2EE thread and transaction manager as substitute for the missing
	 * work and inbound transaction concepts of JCA 1.0.
	 * (SPI JCA 1.0)
	 * @throws ResourceException not thrown 
	 */
	public SPIManagedConnectionFactory() throws ResourceException {
		final String SIGNATURE = "SpiManagedConnectionFactory()";
		TRACE.entering(SIGNATURE);

		// Examine this method to see all possibilities the SAP J2EE logging API offers
		traceSample("XI AF Trace API Test");

		// Access the XI AF resources
		// CS_THREADMGR START
		try {
			ctx = new InitialContext();
			Object res = ctx.lookup("SAPAdapterResources");
			msRes = (SAPAdapterResources)res;
			// txMgr = msRes.getTransactionManager(); // Obsolete with 7.1
		}
		catch(Exception e) {
			TRACE.catching(SIGNATURE, e);
			TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Access to XI AF MS resource failed. Adapter cannot be started.");
		}
		// CS_THREADMGR END
	
		// Optional: Set mcf GUID for debugging purposes
		try {
			synchronized(synchronizer) {
				// CS_GUID START
				mcfLocalGuid = new GUID();
				TRACE.infoT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "This SPIManagedConnectionFactory has the GUID: " + mcfLocalGuid.toString());
				// CS_GUID END
			}
		}
		catch(Exception e) {
			TRACE.catching(SIGNATURE, e);
			TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Creation of MCF GUID failed. Thus no periodic status report possible! Reason: " + e.getMessage());
		}

		// Important: DO NOT start the inbound processing here! Instead start it when start() is being called!
		// The inbound processing must be stopped when stop() is called
		// If this rule is not considered the setting of the MCF properties via the J2EE visual admin will end
		// in two MCF instances running both the inbound processing. Usually conflicts with non-shareable ressources will happen then.

		TRACE.exiting(SIGNATURE);
    }

	/**
	 * The method <code>traceSample()</code> just demonstrates the XI AF API Trace possibilities
	 * Please read the trace file to understand the output format of the different trace statements
	 * (ra implementation specific)
	 * 
	 * @param someText String to test the entering() method 
	 */ 
	private void traceSample(String someText) {
		final String SIGNATURE = "traceSample()";
		// CS_TROUT START
		// Use the plain entering method if you do not have input values 
		TRACE.entering(SIGNATURE);
		// In case of input parameters trace them with the entering
		TRACE.entering(SIGNATURE, new Object[] {someText});

		TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Ignore this and the following trace messages in the method traceSample(). It is just a sample for using the trace API!");

		// Now following an example how to work with the XI AF trace API
		
		// Do not show fata messages here. Since they are always printed out even in productive installations it would create confusion. Fatal message were moved to CCCInteraction.send()
		// A fatal trace message with signature, category and text
		// TRACE.fatalT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "A fatal trace message with signature, category and text");
		// A fatal trace message with signature, category, text and parameters
		// TRACE.fatalT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "A fatal trace message with signature, category, text and {0}.", new Object[] {"parameters"});
		// A fatal trace message with signature and text only
		// TRACE.fatalT(SIGNATURE, "A fatal trace message with signature and text only");
		// A fatal trace message with signature, text and parameters
		// TRACE.fatalT(SIGNATURE, "A fatal trace message with signature, text and {0}", new Object[] {"parameters"});

		// A error trace message with signature, category and text
		TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "A error trace message with signature, category and text");
		// A error trace message with signature, category, text and parameters
		TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "A error trace message with signature, category, text and {0}.", new Object[] {"parameters"});
		// A error trace message with signature and text only
		TRACE.errorT(SIGNATURE, "A error trace message with signature and text only");
		// A error trace message with signature, text and parameters
		TRACE.errorT(SIGNATURE, "A error trace message with signature, text and {0}", new Object[] {"parameters"});

		// A warning trace message with signature, category and text
		TRACE.warningT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "A warning trace message with signature, category and text");
		// A warning trace message with signature, category, text and parameters
		TRACE.warningT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "A warning trace message with signature, category, text and {0}.", new Object[] {"parameters"});
		// A warning trace message with signature and text only
		TRACE.warningT(SIGNATURE, "A warning trace message with signature and text only");
		// A warning trace message with signature, text and parameters
		TRACE.warningT(SIGNATURE, "A warning trace message with signature, text and {0}", new Object[] {"parameters"});

		// A info trace message with signature, category and text
		TRACE.infoT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "A info trace message with signature, category and text");
		// A info trace message with signature, category, text and parameters
		TRACE.infoT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "A info trace message with signature, category, text and {0}.", new Object[] {"parameters"});
		// A info trace message with signature and text only
		TRACE.infoT(SIGNATURE, "A info trace message with signature and text only");
		// A info trace message with signature, text and parameters
		TRACE.infoT(SIGNATURE, "A info trace message with signature, text and {0}", new Object[] {"parameters"});

		// A debug trace message with signature, category and text
		TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "A debug trace message with signature, category and text");
		// A debug trace message with signature, category, text and parameters
		TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "A debug trace message with signature, category, text and {0}.", new Object[] {"parameters"});
		// A debug trace message with signature and text only
		TRACE.debugT(SIGNATURE, "A debug trace message with signature and text only");
		// A debug trace message with signature, text and parameters
		TRACE.debugT(SIGNATURE, "A debug trace message with signature, text and {0}. Infos about the Trace object: {1}", new Object[] {"parameters", TRACE.toString()});

		// Check the trace level being set
		if (TRACE.beLogged(XITrace.SEVERITY_FATAL) == true)
			TRACE.debugT(SIGNATURE, "Severity 'Fatal' is switched on");
		if (TRACE.beLogged(XITrace.SEVERITY_ERROR) == true)
			TRACE.debugT(SIGNATURE, "Severity 'Error' is switched on");
		if (TRACE.beLogged(XITrace.SEVERITY_WARNING) == true)
			TRACE.debugT(SIGNATURE, "Severity 'Warning' is switched on");
		if (TRACE.beLogged(XITrace.SEVERITY_INFO) == true)
			TRACE.debugT(SIGNATURE, "Severity 'Info' is switched on");
		if (TRACE.beLogged(XITrace.SEVERITY_DEBUG) == true)
			TRACE.debugT(SIGNATURE, "Severity 'Debug' is switched on");
		if (TRACE.beLogged(XITrace.SEVERITY_ALL) == true)
			TRACE.debugT(SIGNATURE, "Severity 'All' is switched on");
		if (TRACE.beLogged(XITrace.SEVERITY_MAX) == true)
			TRACE.debugT(SIGNATURE, "Severity 'Max' is switched on");
		if (TRACE.beLogged(XITrace.SEVERITY_MIN) == true)
			TRACE.debugT(SIGNATURE, "Severity 'Min' is switched on");
		if (TRACE.beLogged(XITrace.SEVERITY_GROUP) == true)
			TRACE.debugT(SIGNATURE, "Severity 'Group' is switched on");
		if (TRACE.beLogged(XITrace.SEVERITY_PATH) == true)
			TRACE.debugT(SIGNATURE, "Severity 'Path' is switched on");

		// Of course those if's can be implemented in an elegant way with assertions:
		TRACE.assertion(SIGNATURE, XIAdapterCategories.CONNECT_AF, (!TRACE.beLogged(XITrace.SEVERITY_NONE)), "Logging severity level is not set to 'None'");
		
		// Exception handling and trace
		try {
			// Usually trace exception are not instantiated by the trace API user
			XIAdapterException te = new XIAdapterException("Test exception only");
			// It is recommended to document thrown exceptions if triggered by the adapter 
			TRACE.throwing(SIGNATURE, te);
			throw te;
		} catch(Exception e) {
			// It is recommended to document catches in the trace:
			TRACE.catching(SIGNATURE, e);
			TRACE.debugT(SIGNATURE, "A test TraceException was catched and ignored!");
		}			
		
		// Use the plain exiting method if you do not have return values 
		TRACE.exiting(SIGNATURE);
		// In case of a return object use this exiting method
		TRACE.exiting(SIGNATURE, "some return value");
		// CS_TROUT END
	}

	/**
	 * The lookup of the XI AF module processor entry bean is a XI specific 
	 * procedure which substitues the missing inflow message contract of JCA 1.0
	 * It is important to note that the lookup itself should be made in a separate SAP J2EE
	 * application thread to avoid deadlock during the start-up phase.
	 * Thus this helper method must be called in run() instead of the constructor, which is
	 * executed in a J2EE system thread!
	 * (ra implementation specific)
	 * @param retryNum Number of lookup retries
	 * @return the module processor
	 * @throws ResourceException Thrown when the XI AF module processor entry bean cannot accessed.
	 */
	private ModuleProcessor lookUpModuleProcessor(int retryNum) throws ResourceException {
		final String SIGNATURE = "lookUpModuleProcessor()";
		TRACE.entering(SIGNATURE);
		ModuleProcessor mp = null;

		try {
			mp = ModuleProcessorFactory.getModuleProcessor(true, retryNum, propWaitTime);
		} catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
			TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Cannot get access to the XI AF module processor. Ejb might not have been started yet.");
			ResourceException re = new ResourceException("Cannot get access to the XI AF module processor. Ejb might not have been started yet.");
			throw re;
		}
		TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Lookup of XI AF MP entry ejb was succesfully.");
		TRACE.exiting(SIGNATURE);
		return mp;
	}

	/**
	 * This factory method is used in the context of XI AF: The J2EE server passed a <code>ConnectionManager</code> object
	 * via the <code>createConnectionFactory</code> call to the <code>ManagedConnectionFactory</code>, which in turn
	 * will the approriate constructor of the <code>CciConnectionFactory</code>.  
	 * (SPI JCA 1.0)
	 *
	 * @param cm <code>ConnectionManager</code> of the J2EE server
	 **/    
	public Object createConnectionFactory(ConnectionManager cm) throws ResourceException {
		final String SIGNATURE = "createConnectionFactory(ConnectionManager cxManager)";
		TRACE.entering(SIGNATURE, new Object[] {cm});
		CCIConnectionFactory factory = new CCIConnectionFactory(this, cm);
		TRACE.exiting(SIGNATURE);
        return factory;
    }

	/**
	 * This factory method is used for the two-tier, non J2EE container approach:
	 * The <code>ConnectionManager</code> implementation of the resource adapter
	 * is used instead of the J2EE <code>ConnectionManager</code>.
	 * (SPI JCA 1.0)
	 **/      
	public Object createConnectionFactory() throws ResourceException {
		final String SIGNATURE = "createConnectionFactory()";
		TRACE.entering(SIGNATURE);
		CCIConnectionFactory factory = new CCIConnectionFactory(this, null);
		TRACE.exiting(SIGNATURE);
		return factory;
    }

	/**
	 * This factory method is used called by the J2EE server to create a new managed connection.
	 * Note that it does not necessarily mean that a new physical connection is being created.
	 * However, in this sample a new file is opened.
	 * (SPI JCA 1.0)
	 *
	 * @param subject JAAS authentification data with logon credentials to open the physical connection
	 * @param info <code>ConnectionRequestInfo</code> with additional information to open the managed connection
	 * @return New created managed connection of class <code>SpiManagedConnection</code>
	 * @throws ResourceException Thrown if managed connection cannot be created
	 **/      
	public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo info) throws ResourceException {
		final String SIGNATURE = "createManagedConnection(Subject subject, ConnectionRequestInfo info)";
		TRACE.entering(SIGNATURE, new Object[] {subject, info});
		String channelID = null;
		Channel channel = null;
				
		// Check input
		SPIManagedConnection mc = null;
   
		if (!(info instanceof CCIConnectionRequestInfo)) {
			TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Received an unknown ConnectionRequestInfo. Cannot determine channelId!");
			ResourceException re = new ResourceException("Received an unknown ConnectionRequestInfo. Cannot determine channelId!");
			TRACE.throwing(SIGNATURE, re);
			throw re;
		}   
   
        // CS_CIDLKUP START
		// Determine channel configuration. In this sample it is just the output directory and file name
		try {
			channelID = ((CCIConnectionRequestInfo)info).getChannelId();
			channel = (Channel) CPAFactory.getInstance().getLookupManager().getCPAObject(CPAObjectType.CHANNEL, channelID);
		}
		catch(Exception e) {
			TRACE.catching(SIGNATURE, e);
			TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Cannot access the channel parameters of channel: " + channelID + ". Check whether the channel is stopped in the administrator console.");
			ResourceException re = new ResourceException("Cannot access the channel parameters of channel: " + channelID + ". Check whether the channel is stopped in the administrator console.");
			throw re;
		}
		// CS_CIDLKUP END

		PasswordCredential credential = XISecurityUtilities.getPasswordCredential(this, subject, info);
		mc = new SPIManagedConnection(this, credential, false, channelID, channel);

		// Store the new mc in the map
		if (mc != null) {
			managedConnections.put(channelID, mc);
			TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "For channelID {0} this managed connection is stored: {1}", new Object[] {channelID, mc});
		}
	
		TRACE.exiting(SIGNATURE);
		return mc;
    }

	/**
	 * 
	 * When the XI CPA Cache triggers a channel remove it makes sense to remove the associated 
	 * ManagedConnection as well. Advantages:
	 * - Early release of resources (physical connections, JCA connections, ...)
	 * - JCA pool cleanup
	 * (ra implementation specific)
	 *
	 * @param channelID XI channel ID that identifies the ManagedConnection that has to be removed
	 * @throws ResourceException Thrown if managed connection cannot be created
	 **/      
	void destroyManagedConnection(String channelID) throws ResourceException {
		final String SIGNATURE = "destroyManagedConnection(String channelID)";
		TRACE.entering(SIGNATURE, new Object[] {channelID});
		SPIManagedConnection mc = null;
		try {
			//Lookup the MC that is related to the XI channel
			mc = (SPIManagedConnection) managedConnections.get(channelID);
			if (mc != null) {
				//Inform all SPI event listeners (i.e. the JCA container that this mc is destroyed now)
				mc.sendEvent(ConnectionEvent.CONNECTION_CLOSED, null, mc);
				managedConnections.remove(channelID);
				mc.destroy(true);
				TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "ManagedConnection for channel ID {0} found and destroyed.", new Object[] {channelID});
			}
			else
				TRACE.warningT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "ManagedConnection for channel ID {0} not found.", new Object[] {channelID});
		} catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
			TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Received exception during ManagedConnection destroy: " + e.getMessage());
		}
		TRACE.exiting(SIGNATURE);
	}

	/**
	 * 
	 * If a ManagedConnection is destroyed by the JCA container it reports this to its 
	 * ManagedConnectionFactory. The MCF then removes the MC from the internal pool. 
	 * (ra implementation specific)
	 *
	 * @param channelID XI channel ID that identifies the ManagedConnection that has to be removed
	 **/      
	void removeManagedConnection(String channelID) {
		final String SIGNATURE = "removeManagedConnection(String channelID)";
		TRACE.entering(SIGNATURE, new Object[] {channelID});
		managedConnections.remove(channelID);
		TRACE.exiting(SIGNATURE);
	}
	
	/**
	 * This method is not a factory method but more a selection method: It selects the best-fitting 
	 * managed connection contained in the connection set based on the subject (user, password) and
	 * connection information (<code>ConnectionRequestInfo</code>).
	 * This sample simply checks the equality of the subject and that the managed connection
	 * was opened for a given XI channel.
	 * (SPI JCA 1.0)
	 *
	 * @param connectionSet containing <code>SpiManagedConnection</code> objects
	 * @param subject JAAS authentification data with logon credentials to open the physical connection
	 * @param info <code>ConnectionRequestInfo</code> with additional information to open the managed connection
	 * @return Existing managed connection of class <code>SpiManagedConnection</code>
	 * @throws ResourceException Thrown if managed connection cannot be created
	 **/      
    public ManagedConnection matchManagedConnections(Set connectionSet, Subject subject, ConnectionRequestInfo info) throws ResourceException {
		final String SIGNATURE = "matchManagedConnections(Set connectionSet, Subject subject, ConnectionRequestInfo info)";
		TRACE.entering(SIGNATURE, new Object[] {connectionSet, subject, info});

		SPIManagedConnection mcFound = null;
		CCIConnectionRequestInfo cciInfo = null;
        PasswordCredential pc =  XISecurityUtilities.getPasswordCredential(this, subject, info);
        
        if (info instanceof CCIConnectionRequestInfo)
        	cciInfo = (CCIConnectionRequestInfo) info;
        else {
			TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Unknown ConnectionRequestInfo parameter received. Cannot match connection");
			return null;
        }
        
        Iterator it = connectionSet.iterator();
        
        // A managed connection is returned if the channelID which is requested is the same
        while (it.hasNext() && (mcFound == null)) {
            Object obj = it.next();
            if (obj instanceof SPIManagedConnection) {
                SPIManagedConnection mc = (SPIManagedConnection) obj;
                if (!mc.isDestroyed()) {
	                ManagedConnectionFactory mcf = mc.getManagedConnectionFactory();
	                if ( (XISecurityUtilities.isPasswordCredentialEqual(mc.getPasswordCredential(), pc)) &&
	                     (mcf.equals(this)) &&
	                     (mc.getChannelID().equalsIgnoreCase(cciInfo.getChannelId())) ) {
	                    mcFound = mc;
						TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT, "Found existing ManagedConnection in container set for channel {0}.", new Object [] {mc.getChannelID()});
	                }
	                else
						TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT, "ManagedConnection in container set does not fit. Ignore.");
                }
                else
					TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT, "Destroyed sample ManagedConnection in container set. Ignore.");
            }
            else
				TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT, "This is not a sample ManagedConnection in container set. Ignore.");
        }
		TRACE.exiting(SIGNATURE);
        return mcFound;
    }

	/**
	 * Sets the JCA J2EE logwriter <code>PrintWriter</code> object. Although JCA specifies
	 * this mechansim it is not being used by XI AF. Instead the resource adapters should
	 * use the XI AF trace service classes as done here in this sample.
	 * 
	 * (SPI JCA 1.0)   
	 *
	 * @param out <code>PrintWriter</code> print writer for logging purposes
	 */    
	public void setLogWriter(PrintWriter out) throws ResourceException {
		final String SIGNATURE = "setLogWriter(PrintWriter out)";
		TRACE.entering(SIGNATURE, new Object[] {out});
		out.print("XI AF " + AdapterConstants.adapterName + " has received a J2EE container log writer.");
		out.print("XI AF " + AdapterConstants.adapterName + " will not use the J2EE container log writer. See the trace file for details.");
        logWriter = out;
		TRACE.exiting(SIGNATURE);
    }

	/**
	 * Gets the JCA J2EE logwriter <code>PrintWriter</code> object.
	 * (SPI JCA 1.0)   
	 *
	 * @return <code>PrintWriter</code> print writer for logging purposes
	 */    
	public PrintWriter getLogWriter() throws ResourceException {
        return logWriter;
    }

	/**
	 * Gets the XI AF audit log access <code>AuditAccess</code> object.
	 * (ra implementation specific)   
	 *
	 * @return <code>AuditAccess</code>  for logging purposes, might be null during the start up phase
	 */    
	AuditAccess getAuditAccess() {
        return audit;
    }

	/**
	 * Gets the XI AF message factory wrapper <code>XIMessageFactoryImpl</code> object.
	 * (ra implementation specific)   
	 *
	 * @return <code>XIMessageFactoryImpl</code> to create messages
	 */    
	XIMessageFactoryImpl getXIMessageFactoryImpl() {
        return mf;
    }
	
	/**
	 * Gets a new unique file name for a new physical connection
	 * (ra implementation specific)   
	 *
	 * @return New unique file name to create an output file
	 */      
	public String getOutFileName(String outFileNamePrefix) {
		final String SIGNATURE = "getFileName()";
		TRACE.entering(SIGNATURE);
		
		int cnt = 0;
		String fileName = null;
		
		synchronized(synchronizer) {
			cnt = fileCounter;
			fileCounter++;
		}
		
		fileName = new String(outFileNamePrefix + "." + Integer.toString(cnt) + ".txt");
		TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT, "Output file name =" + fileName);

        TRACE.exiting(SIGNATURE);
        return fileName;
    }

	/**
	 * A JCA 1.0 compliant <code>SpiManagedConnectionFactory</code> must implement
	 * the <code>equals()</code> to allow the J2EE container a sensible 
	 * connection pooling. The equality MUST be defined on the complete
	 * property set (see pg. 50) 
	 *
	 * (SPI JCA 1.0)
	 *
	 * @param obj	Object to compare
	 * @return True of obj is equal (but not necessarily identical) with this <code>SpiManagedConnectionFactory</code>, false otherwise 
	 */	    
	public boolean equals(Object obj) {
		final String SIGNATURE = "equals(Object obj)";
		TRACE.entering(SIGNATURE, new Object[] {obj});
		boolean equal = false;
		if (obj instanceof SPIManagedConnectionFactory) {
			SPIManagedConnectionFactory other = (SPIManagedConnectionFactory) obj;
			if ( (adapterNamespace.equals(other.getAdapterNamespace())) &&
				 (adapterType.equals(other.getAdapterType())) &&
				 (addressMode.equals(other.getAddressMode())))
				equal = true;
		}
		TRACE.exiting(SIGNATURE);
		return equal;
    }

	/**
	 * A JCA 1.0 compliant <code>SpiManagedConnectionFactory</code> must implement
	 * the <code>hashCode()</code> to allow the J2EE container a sensible 
	 * connection pooling. The equality MUST be defined on the complete
	 * property set (see pg. 50) 
	 *
	 * (SPI JCA 1.0)
	 *
	 * @return Integer value representing the hash code of this <code>SpiManagedConnectionFactory</code> 
	 */	
	public int hashCode() {
		final String SIGNATURE = "hashCode()";
		TRACE.entering(SIGNATURE);
		int hash = 0;
		String propset = adapterNamespace + adapterType + addressMode;
		hash = propset.hashCode();
		TRACE.exiting(SIGNATURE);
		return hash;
	}

	/**
	 * This class must be JavaBean compliant hence it offers at least getters for properties
	 * @return Current count value of the file name generator
	 */
	public static int getFileCounter() {
		return fileCounter;
	}

	/**
	 * This class must be JavaBean compliant hence it offers getters for properties
	 * @return The address determination mode
	 */
	public String getAddressMode() {
		final String SIGNATURE = "getAddressMode()";
		TRACE.entering(SIGNATURE);
		TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT, "Address determination mode =" + addressMode);
		TRACE.exiting(SIGNATURE);
		return addressMode;
	}

	/**
	 * This class must be JavaBean compliant hence it offers setters for properties
	 * @param addressMode The address determination mode
	 */
	public void setAddressMode(String addressMode) {
		this.addressMode = addressMode;
	}

	/**
	 * Starts the inbound processing
	 * (ra implementation specific)
	 * @throws ResourceException Thrown if thread cannot be started
	 */    
	public void startMCF() throws ResourceException {
		final String SIGNATURE = "startMCF()";
		TRACE.entering(SIGNATURE);

		// CS_THSTR START
		if (threadStatus != TH_STARTED) {

			/* XI AF ra's MUST NOT use Java native threads 
			Thread inboundSimulator = null;
			try {
				inboundSimulator = new Thread(null, this, "XI AF Sample Adapter Inbound");
			}
			catch(Exception e) {
				TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Cannot create inbound message thread");
				ResourceException re = new ResourceException(e.getMessage());
				TRACE.throwing(SIGNATURE, re);
				throw re;
			} */

			try {
				threadStatus = TH_STARTED;
				msRes.startRunnable(this);
				//inboundSimulator.start(); see above
			}
			catch(Exception e) {
				TRACE.catching(SIGNATURE, e);
				threadStatus = TH_STOPPED;
				TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Cannot start inbound message thread");
				ResourceException re = new ResourceException(e.getMessage());
				TRACE.throwing(SIGNATURE, re);
				throw re;
			}
			// CS_THSTR END

		}
		TRACE.exiting(SIGNATURE);
	}

	/**
	 * Stops the inbound processing
	 * (ra implementation specific)
	 */    
	public void stopMCF() throws ResourceException {
		final String SIGNATURE = "stopMCF()";
		TRACE.entering(SIGNATURE);
		// Inform run thread
		threadStatus = TH_STOPPED;
		// Wait waittime to be sure that thread has ended
		try {
			// Wait for completion
			synchronized(this) {
				// Wake up run() if it is waiting for the next poll interval
				((Object)this).notify();
				// $JL-WAIT$ The wait time is deterministic
				wait(waitTime+1000);
			}
			xIConfiguration.stop();
		}
		catch(Exception e) {
			TRACE.catching(SIGNATURE, e);
			TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Cannot stop inbound message thread. Reason: " + e.getMessage());
			ResourceException re = new ResourceException(e.getMessage());
			TRACE.throwing(SIGNATURE, re);
			throw re;
		}
		TRACE.exiting(SIGNATURE);
	}

	/**
	 * Starts the control timer
	 * For debugging purposes the MCF prints out the MCF GUID ID and its status periodically
	 * (ra implementation specific)
	 */    
	public void startTimer() {
		final String SIGNATURE = "startTimer()";
		TRACE.entering(SIGNATURE);
		if (mcfLocalGuid != null) {
			try {
				controlTimer.scheduleAtFixedRate(new XIManagedConnectionFactoryController(this, ctx), TIMER_FIRST_RUN, TIMER_PERIOD);
			}
			catch(Exception e) {
				TRACE.catching(SIGNATURE, e);
				TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Creation of MCF controller failed. No periodic MCF status reports available! Reason: " + e.getMessage());
			}
		}
		TRACE.exiting(SIGNATURE);
	}

	/**
	 * Stops the control timer
	 * (ra implementation specific)
	 */    
	public void stopTimer() {
		final String SIGNATURE = "stopTimer()";
		TRACE.entering(SIGNATURE);
		controlTimer.cancel();
		TRACE.exiting(SIGNATURE);
	}

	/**
	 * Simulates incoming messages in a separate thread.
	 * (ra implementation specific)
	 */    
	public void run() {
		final String SIGNATURE = "run()";
		TRACE.entering(SIGNATURE);

		// CS_MCFTNAMESET START
		String oldThreadName = Thread.currentThread().getName();
		String newThreadName = "XI AF Sample Adapter MCF " + mcfLocalGuid;
		try {
			Thread.currentThread().setName(newThreadName);
			TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Switched thread name to: {0}", new Object[] {newThreadName});
			// CS_MCFTNAMESET END

			// CS_MCFPROPS START
			// Wait now till the MCF properties are set that cannot be configured via XI ID
			boolean notSet = true;
			int numTry = 0;
			int pollTime = -1;
			
			while ((notSet) && (numTry < propWaitNum)) {
				if ( (addressMode != null) && (adapterType != null) && (adapterNamespace != null) )
					notSet = false;
				numTry++;
				TRACE.debugT(SIGNATURE,	XIAdapterCategories.CONNECT_AF,	"MCF waits for setter completion. Try: {0} of {1}.", new Object [] {Integer.toString(numTry), Integer.toString(propWaitNum)});
				try { Thread.sleep(propWaitTime); } catch (Exception e) {TRACE.catching(SIGNATURE, e);};
			}
			if (addressMode == null)
				addressMode = AM_CPA;
			if (adapterType == null)
				adapterType = AdapterConstants.adapterType;
			if (adapterNamespace == null)
				adapterNamespace = AdapterConstants.adapterNamespace;
			// CS_MCFPROPS END
	
			// Create the module processor local IF instance here 
			// Important: It MUST NOT be created in the JCA ra thread, it must be created
			// in a separate J2EE server application thread!
			// CS_MPLOOK START
			ModuleProcessor mp = null;
			try {
				mp = lookUpModuleProcessor(propWaitNum);
			}
			catch(Exception e) {
				TRACE.catching(SIGNATURE, e);
				TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Cannot instatiate the XI AF module processor bean. The inbound processing is stopped. Exception:" + e.toString());
				threadStatus = TH_STOPPED;
			}
			// CS_MPLOOK END
			
			// Create the helper object that manages the XI channel information
			if (xIConfiguration == null) {
				try {
					xIConfiguration = new XIConfiguration(adapterType, adapterNamespace);
					xIConfiguration.init(this);
				}
				catch(Exception e) {
					TRACE.catching(SIGNATURE, e);
					TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Cannot instatiate the XI CPA handler. The inbound processing is stopped. Exception:" + e.toString());
					threadStatus = TH_STOPPED;
				}
			}
			
			while (threadStatus == TH_STARTED) {
	
				try {
		            LinkedList channels = xIConfiguration.getCopy(Direction.INBOUND);
					//TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Got {0} inbound channels.", new Object[] {new Integer(channels.size())}); 
					 
					for (int i = 0; i < channels.size(); i++) {
						Channel channel = (Channel) channels.get(i);
						try {
							
							// The old "adapterStatus" check was removed with introduction of AAM which uses start/stop and channelAdded/Remove 
							// to start and stop a channel
							
							// Try to read the configuration parameter from CPA
							// If one parameter cannot be read, set a sensible default but try to read the rest to ensure backward compatibility
							String directory = null;
							String name = null;
							String processMode = null;
							String qos = null;
							String psec = null;
							String pmsec = null;
							String raiseError = null;
							String channelAddressMode = null;
							boolean set_asma = false;
							try {
								directory = channel.getValueAsString("fileInDir");
							} catch (Exception e) {
								TRACE.catching(SIGNATURE, e);
							}
							try {
								name = channel.getValueAsString("fileInName");
							} catch (Exception e) {
								TRACE.catching(SIGNATURE, e);
							}
							try {
								processMode = channel.getValueAsString("processMode");
							} catch (Exception e) {
								TRACE.catching(SIGNATURE, e);
							}
							try {
								qos = channel.getValueAsString("qos");
							} catch (Exception e) {
								TRACE.catching(SIGNATURE, e);
							}
							try {
								psec = channel.getValueAsString("filePollInterval");
							} catch (Exception e) {
								TRACE.catching(SIGNATURE, e);
							}
							try {
								pmsec = channel.getValueAsString("filePollIntervalMsecs");
							} catch (Exception e) {
								TRACE.catching(SIGNATURE, e);
							}
							try {
								raiseError = channel.getValueAsString("raiseError");
							} catch (Exception e) {
								TRACE.catching(SIGNATURE, e);
							}
							try {
								channelAddressMode = channel.getValueAsString("channelAddressMode");
							} catch (Exception e) {
								TRACE.catching(SIGNATURE, e);
							}
							try {
								set_asma = channel.getValueAsBoolean("enableDynConfigSender");
								if (set_asma == true) 
									set_asma = channel.getValueAsBoolean("dynConfigJCAChannelID");
							} catch (Exception e) {
								TRACE.catching(SIGNATURE, e);
							}
							// The smallest poll interval will be choosen for all channels (simplification for this sample only!)
							int ptime = 0;
							if ((psec != null) && (psec.length() > 0))
								ptime = Integer.valueOf(psec).intValue()*1000;
							if ((pmsec != null) && (pmsec.length() > 0))
								ptime = ptime + Integer.valueOf(pmsec).intValue();
							if ( (pollTime < 0) || (ptime < pollTime))
								pollTime = ptime;
							
							if ((directory == null) || (directory.length() == 0)) {
								TRACE.warningT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Unable to determine input file directory. Take default: " + IN_DIR);
								directory = IN_DIR;
							}	
	
							if ((name == null) || (name.length() == 0)) {
								TRACE.warningT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Unable to determine input file prefix. Take default: " + IN_NAME);
								name = IN_NAME;
							}	
	
							if ((processMode == null) || (processMode.length() == 0)) {
								TRACE.warningT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Unable to determine processing mode. Take default: " + PM_TEST);
								processMode = PM_TEST;
							}	
	
							if ((qos == null) || (qos.length() == 0)) {
								TRACE.warningT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Unable to determine QOS. Take default: " + QOS_EO);
								qos = QOS_EO;
							}	
	
							if ((raiseError == null) || (raiseError.length() == 0)) {
								TRACE.warningT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Unable to determine error raise condition. Take default: " + ERR_NONE);
								raiseError = ERR_NONE;
							}	
	
							if ((channelAddressMode == null) || (channelAddressMode.length() == 0)) {
								TRACE.warningT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Unable to determine address mode. Take default from JCA property: " + addressMode);
								channelAddressMode = addressMode;
							}	
	
							String completeName = directory + "/" + name;
							
							// TRACE.infoT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Channel " + channel.getChannelName() + " processes file " + completeName);
							sendMessageFromFile(completeName, channel, processMode, qos, raiseError, channelAddressMode, set_asma);
						} catch (Exception e) {
							TRACE.catching(SIGNATURE, e);
							TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Cannot send message to channel {0}. Received exception: {1}", 
							    new Object[] {channel.getObjectId(), e.getMessage()});
						}
					}			
				} catch (Exception e) {
					TRACE.catching(SIGNATURE, e);
					TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Cannot access inbound channel configuration. Received exception: " + e.getMessage());
				}
	
				try {
					synchronized(this) {
					   if (pollTime <= 0)    // No sender channel or huge poll interval is configured => take default poll interval 
					   		wait(waitTime);
					   else					 // At least one sender channel is configured => take smallest configured poll interval
					   		wait(pollTime);
					}
				} catch (InterruptedException e1) {
					TRACE.catching(SIGNATURE, e1);
					TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Inbound thread stopped. Received exception during wait period: " + e1.getMessage());
					threadStatus = TH_STOPPED;
				}
			}
			// CS_MCFTNAMERESET START
		} finally {
			Thread.currentThread().setName(oldThreadName);
			TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Switched thread name back to: {0}", new Object[] {oldThreadName});
			// CS_MCFTNAMERESET END
		}
	}

	/**
	 * <code>sendMessageFromFile</code> manages the file read process and creates
	 * afterwards the EO(IO) XI message. This message is then sent to the XI AF MP.
	 * It illustrates two different approaches to work with channels:
	 * If the addressMode is set to "CPA" (set via deployment descriptor or channel configuration) then
	 * the addresses are taken from the channel configuration, else from the
	 * message. In the latter case the channel is look-up'ed dynamically.
	 * (ra implementation specific)
	 * 
	 * @param inFileName File of file to read constructed from the channel data
	 * @param channel Predetermined channel
	 * @param processMode Predetermined process mode that specifies the handling if sent files
	 * @param qos Quality of service of this channel (to be set in the message)
	 * @param raiseError raise a error condition for demo purposes
	 * @param channelAddressMode Determines whether the address data should be read from the inbound message (B2B) or from the XI agreements (A2A)
	 * @param set_asma If true, then the XI adapter specific message attribute "JCAChannelID" is set as example
	 */
	private void sendMessageFromFile(String inFileName, Channel channel, String processMode, String qos, String raiseError, String channelAddressMode, boolean set_asma) {
		final String SIGNATURE = "sendMessageFromFile(String inFileName)";
		String msgText = new String();
		boolean fileRead;
		File inputFile = null;
		String channelId = null;
		String extMsgId = null; 
		String xiMsgId = null; 
		
		// The next lines just reads out the file and renames it afterwards
		// They do not show any particular XI or JCA handling
		
		fileRead = true;
		
		try {
			inputFile = new File (inFileName);
			if (inputFile.exists() == false) {
				// TRACE.warningT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Input file " + inFileName + " does not exist. Retry in " + Integer.toString(waitTime) + " milliseconds!");
				fileRead = false;
			}
		}
		catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
			TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Input file " + inFileName + " attributes cannot be read. Received exception: " + e.getMessage());
			fileRead = false;
		}	

		// Check whether this file was sent before already (duplicate check for EO, only if process mode is unequal "test")
		// CS_IDMAPCHECK START
		if ( (fileRead == true) && (0 != processMode.compareToIgnoreCase(PM_TEST))) {
			// Use a hash code (over file name + date) as "external message id"
			extMsgId = "JCASample" + String.valueOf((long)inputFile.hashCode() + inputFile.lastModified());

			if ( (qos.equalsIgnoreCase("EOIO")) ||
				 (qos.equalsIgnoreCase("EO")) ) {
				// Yes, it is a duplicate. Log it and return without further processing
				if ((xiMsgId = messageIDMapper.getMappedId(extMsgId)) != null) {
					TRACE.infoT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Duplicated and already processed file (message) with id {0} detected.  It will be ignored.", new Object[] {extMsgId});
					MessageKey amk = new MessageKey(xiMsgId, MessageDirection.OUTBOUND);
					audit.addAuditLogEntry(amk, AuditLogStatus.SUCCESS, "Duplicated and already processed file (message) with id {0} detected.  It will be ignored.", new Object[] {extMsgId});
					audit.flushAuditLogEntries(amk);
					//Just ignore the message (file) but process it as specified
					if (0 == processMode.compareToIgnoreCase(PM_RENAME)) {
						// Rename the file which marks it as "processed"
						try {
							renameFile(inFileName, inputFile);
						} catch (Exception e) {
							TRACE.catching(SIGNATURE, e);  // Can't do better here, rely on duplicate detection for error handling
						}							
					}
					TRACE.exiting(SIGNATURE);
					return;
				}
				else
					TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Duplicate check passed succesfully. New message, no duplicate (id {0})", new Object[] {extMsgId});
			}
		}
		// CS_IDMAPCHECK END

		if (fileRead == true) {
			try {
				BufferedReader in = new BufferedReader(new FileReader(inputFile));
				String line = null;
				while ((line = in.readLine()) != null) {
					msgText = msgText + line + "\n";
				}
				in.close();
				TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "File message text: " + msgText);
			}
			catch (Exception e) {
				TRACE.catching(SIGNATURE, e);
				TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Input file " + inFileName + " cannot be opened. Retry in " + Integer.toString(waitTime) + " milliseconds!" + " Received exception: " + e.getMessage());
				fileRead = false;
			}	
		}
		
		if (fileRead == true) {
			try {
				String fromParty = null;
				String toParty = null;
				String fromService = null;
				String toService = null;
				String action = null;
				String actionNS = null;
				
				// Determination of addresses and channels
				// CS_AMCPA START
				if (channelAddressMode.equalsIgnoreCase(AM_CPA)) {
					// Determination of addresses based on the binding (sender agreement) data
					channelId = channel.getObjectId();
					// Here it makes absolutely sense to use the getBindingByChannelId() method (in contrast to the receiver/outbound direction)
					// since it is the only chance to lookup the "from" address. In this case the channel must not be
					// assigned to different agreements! To ensure this, set AdapterTypeMetaData/@senderAgreementUnique in the adapter metadata
					// If the receiver ("to") address is not set the IS will take over the routing (see receiver determination)      
					Binding binding = CPAFactory.getInstance().getLookupManager().getBindingByChannelId(channelId);
					action = binding.getActionName();
					actionNS = binding.getActionNamespace(); 
					fromParty = binding.getFromParty();
					fromService = binding.getFromService();
					toParty = binding.getToParty();
					toService = binding.getToService();
				}
				// CS_AMCPA END
				// CS_AMMSG START
				else {			
					// Determination of addresses based on the (external) message data
					// This method assumes that one inbound channel received messages from different
					// sender parties or services. Thus a dynamical channel lookup is necessary to
					// determine the correct channel for the inbound processing
					
					TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Input file " + inFileName + " was read.");
			
					fromParty = findValue("FromParty:", msgText);
					toParty = findValue("ToParty:", msgText);
					fromService = findValue("FromService:", msgText);
					toService = findValue("ToService:", msgText);
					action = findValue("Action:", msgText);
					actionNS = findValue("ActionNS:", msgText);
					String areGLN = findValue("GLNMode:", msgText);

					// CS_HDNORM2 START
					// The flag "GLNMode" demonstrates how to handle non XI address identifers such as DUNS or GLN
					// In this case use the address normalization if you receive external party and service identfiers
					if ( (areGLN != null) && (areGLN.compareToIgnoreCase("true")==0) ) {
						TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT, "Access the normalization manager now.");
						NormalizationManager normalizer = NormalizationManager.getInstance();

						Service fromXIService = normalizer.getXIService(fromParty, ADDR_SCHEMA_GLN, fromService);
						if ( (fromXIService != null) && (fromXIService.getService() != null) && (fromXIService.getService().length() > 0)) {
							TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT, "Address normalization for service: {0} is: {1}", new Object[] { fromService, fromXIService.getService()});
							fromService = fromXIService.getService();
						}
						else	
							TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT, "Address normalization is not defined for service: {0}", new Object[] { fromService });

						Party fromXIParty = normalizer.getXIParty(ADDR_AGENCY_EAN, ADDR_SCHEMA_GLN, fromParty);							 
						if ( (fromXIParty != null) && (fromXIParty.getParty() != null) && (fromXIParty.getParty().length() > 0)) {
							TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT, "Address normalization for party: {0} is: {1}", new Object[] { fromParty, fromXIParty.getParty()});
							fromParty = fromXIParty.getParty();
						}
						else	
							TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT, "Address normalization is not defined for party: {0}", new Object[] { fromParty });

						Service toXIService = normalizer.getXIService(toParty, ADDR_SCHEMA_GLN, toService);
						if ( (toXIService != null) && (toXIService.getService() != null) && (toXIService.getService().length() > 0)) {
							TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT, "Address normalization for service: {0} is: {1}", new Object[] { toService, toXIService.getService()});
							toService = toXIService.getService();
						}
						else	
							TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT, "Address normalization is not defined for service: {0}", new Object[] { toService });

						Party toXIParty = normalizer.getXIParty(ADDR_AGENCY_EAN, ADDR_SCHEMA_GLN, toParty);							 
						if ( (toXIParty != null) && (toXIParty.getParty() != null) && (toXIParty.getParty().length() > 0)) {
							TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT, "Address normalization for party: {0} is: {1}", new Object[] { toParty, toXIParty.getParty()});
							toParty = toXIParty.getParty();
						}
						else	
							TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT, "Address normalization is not defined for party: {0}", new Object[] { toParty });
					}

					// CS_HDNORM2 END
					CPAInboundRuntimeLookupManager channelLookup = CPAFactory.getInstance().createInboundRuntimeLookupManager(adapterType, adapterNamespace, fromParty, toParty, fromService, toService, action, actionNS);
					channel = channelLookup.getChannel();
					// CS_AMMSG END
		
					if (channel != null) {
						channelId = channel.getObjectId();                  
					}
					else
					{
						TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "The channel ID cannot be determined. Reason: No agreement (binding) for the FP,TP,FS,TS,A combination available. Message will be processed later!");
						return;
					}
				}

				// Normalize wildcards and null's to "non-specified" address value
				if ( (fromParty == null) || (fromParty.equals("*")) )
					fromParty = new String("");
				if ( (fromService == null) || (fromService.equals("*")) )
					fromService = new String("");
				if ( (toParty == null) || (toParty.equals("*")) )
					toParty = new String("");
				if ( (toService == null) || (toService.equals("*")) )
					toService = new String("");
				if ( (action == null) || (action.equals("*")) )
					action = new String(""); 
				if ( (actionNS == null) || (actionNS.equals("*")) )
					actionNS = new String(""); 
		
				TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "The following address data were extracted (FP,TP,FS,TS,A): " +
				             fromParty + "," + toParty + "," + fromService + "," + toService + "," + action);
				TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "The channel ID is: " + channelId);
	
				// CS_MSGCRE START	
				Message msg = mf.createMessageRecord(fromParty, toParty, fromService, toService, action, actionNS);
				// CS_MSGCRE END	
	
				// Set the QOS accordingly to the channel configuration. Normally the Qos is determined by the 
				// capabilities of the external system or protocol
				if (qos.equalsIgnoreCase(QOS_BE))
					msg.setDeliverySemantics(DeliverySemantics.BestEffort);
				else if (qos.equalsIgnoreCase(QOS_EOIO))
					msg.setDeliverySemantics(DeliverySemantics.ExactlyOnceInOrder);
				else	
					msg.setDeliverySemantics(DeliverySemantics.ExactlyOnce);
				
				// Check whether the payload is a XML document. If not, treat it as binary to demonstrate how
				// binary main documents work
				// First create a XML Payload				
				XMLPayload xp = msg.createXMLPayload();

				// In case of XML documents it is not necessary to set the contentType or encoding
				// But: Take care that the encoding definiton in the XML document corresponds to the encoding used
				if (msgText.indexOf("<?xml") != -1) {
					xp.setText(msgText);
					xp.setName("MainDocument");
					xp.setDescription("XI AF Sample Adapter Input: XML document as MainDocument");
					// xp.setContentType("text/xml"); Not necessary. Set as default in XMLPayload
				}
				// In case of binary documents use the Payload super class methods to fill the XMLPayload object
				else {
					xp.setContent(msgText.getBytes("UTF-8"));
					xp.setContentType("application/octet-stream");				
					xp.setName("MainDocument");
					xp.setDescription("XI AF Sample Adapter Input: Binary as MainDocument");
				}

				// CS_SETASMA START
				// Starting with SP15 XI supports adapter specific message attributes (ASMA)
				// With that an adapter can set arbitrary string attributes in the XI message header.
				// ASMAs are declared in the adapter meta data and are evaluated by other XI components such
				// as routing (receiver determination), mapping, BPE and other adapters.
				// ASMAs of different adapters are differentiated by namespaces.
				if (set_asma) {
					msg.setMessageProperty(adapterNamespace + "/" + adapterType, ASMA_NAME, channelId);
					TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "The adapter specific message attribute (ASMA) {0} was set.", new Object[] {ASMA_NAME});
				}
				else
					TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "The adapter specific message attribute (ASMA) {0} was not set since the setting is switched off in the channel configuration.", new Object[] {ASMA_NAME});
				// CS_SETASMA END
				
				// Finally set the main document in the message				
				msg.setDocument(xp);
		
				TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Message object created and filled.");
		
				ModuleData md = new ModuleData();
				md.setPrincipalData(msg);
				
				// In case of asynchronous EO(IO) message work with transactions and do not expect a synchronous response back
				if (!qos.equalsIgnoreCase(QOS_BE))
				{
					TransactionTicket txTicket = null; // See explanations below
					try {
						// CS_LUWBEGIN START
						// Start the LUW of the inbound transaction
						// Attention: Do not create LUW's for synchronous messages (i.e. QoS = BestEffort)
						// Reason: The XI AF MS must perform a commit to enqueue the synchronous message
						// Creating LUW's for sync. messages will end in an exception thrown by the XI AF MP
						// 7.1 Change: Instead of txMgr.begin() use now the transaction ticket concept of SAP J2EE to start the own tx.
						TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Get transaction ticket now.");
						txTicket = TxManager.required();
						TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Got transaction ticket: {0}", new Object[] {txTicket.toString()});
						// CS_LUWBEGIN START
		
						// CS_TRAUD START
						// The capability of writing audit log entries without audit key is removed in 7.1. I.e.
						// the caller must create a messageKey/message ID first.
						// Not possible anymore: audit.addAuditLogEntry(AuditLogStatus.SUCCESS, "The XI AF JCA sample adapter has received a message via channel: {0}.", new Object[] {channel.getChannelName()});
	
						// Later, when the XI message ID is determined the audit log entries must be written with the MessageKey
						// First of all create a audit message key which links the audit log entries with the XI message
						// Use an XI message ID format without dashes.
						xiMsgId = msg.getMessageId();
						
						MessageKey amk = new MessageKey(xiMsgId, MessageDirection.OUTBOUND);
						md.setSupplementalData("audit.key", amk);
						// Please note: The AuditDirection.OUTBOUND and INBOUND class contants follow the Java "typesafe enum" pattern
						// (see "Effective Java", J. Bloch, ISBN 0-201-31005-8, item 21, pg.104ff)
						// Hence a comparison based on references is allowed. E.g. this is always true:
						if (MessageDirection.OUTBOUND == MessageDirection.valueOf("OUTBOUND"))
							TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "AuditDirection typesaf enum works well!");
						else
							TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "AuditDirection typesaf enum works quite bad!");
						// The same is true with AuditLogStatus (and in different other places of this API):
						if (AuditLogStatus.ERROR == AuditLogStatus.valueOf("ERR"))
							TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "AuditLogStatus typesaf enum works well!");
						else
							TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "AuditLogStatus typesaf enum works quite bad!");
						// This amk creation before was equivilant to this:
						MessageKey amk2 = msg.getMessageKey();					
						if (amk2.equals(amk))
							TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "MessageKey amk and amk2 are equal!");
						else
							TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "MessageKey amk and amk2 are not equal!");
						TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "The last audit message key being used was: amk: {0}, dir: {1}, msgid: {2}, msgkey: {3}, stat: {4}.", 
							new Object[] {amk.toString(), amk.getDirection().toString(), amk.getMessageId().toString(), amk.toString(), AuditLogStatus.SUCCESS.toString()});
	
						// Now write real, sensible entries with key 
						audit.addAuditLogEntry(amk, AuditLogStatus.SUCCESS, "Asynchronous message was read from file and will be forwarded to the XI AF MS now.");
						audit.addAuditLogEntry(amk, AuditLogStatus.SUCCESS, "Name of the processed file: {0}.", new Object[] {inFileName});
						audit.addAuditLogEntry(amk, AuditLogStatus.WARNING, "Demo: This is a warning audit log message");
	
						// And flush them into the DB
						audit.flushAuditLogEntries(amk);
	
						// Remove of audit log is not permitted with 7.1 anymore:
						// audit.removeAuditLogEntries(amk, false);
						// Even with the following (unnecessary) flush the removed entries won't be visible!					
						audit.flushAuditLogEntries(amk);
						// CS_TRAUD END
	
						// Hand over the message to the XI AF (module processor) now
						// CS_AFMPCALL START
						TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Message will be forwarded to XI AF MP and channel: " + channelId);

						//Lookup ModuleProcessor and process the message
						lookUpModuleProcessor(1).process(channelId, md);

						TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "The message with ID " + msg.getMessageId() + " was forwarded to the XI AF succesfully.");
						// CS_AFMPCALL END

						// Create the message ID map for later duplicate detection. Keep the map for at least 24h. Call PMI seperatly (callPMIAgent = false), see com.sap.engine.interfaces.messaging.api.pmi for details
						// Very important! Participate in the "outter" LUW for ID Map storage in the DB (transactional = true).
						// I.e.: The ID map will be comitted together with the message in the XI AF
						// CS_IDMAPINSERT START
						messageIDMapper.createIDMap(extMsgId, xiMsgId, System.currentTimeMillis() + 1000*60*60*24, true);  
						// CS_IDMAPINSERT END
	
						// Just for demonstration purposes: If the channel parameter "raiseError" is set to "rollback" then throw an exception
						// It illustrates a rollback of the message in the AF and the message ID map.
						// => The message is not recognized as duplicated when it is sent again
						if (0 == raiseError.compareToIgnoreCase(ERR_ROLLBACK)) {
							audit.addAuditLogEntry(amk, AuditLogStatus.ERROR, "Channel error mode is set to " + ERR_ROLLBACK + ". An Exception is thrown now to demonstrate a rollback behavior.");
							audit.flushAuditLogEntries(amk);
							TRACE.infoT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Channel error mode is set to " + ERR_ROLLBACK + ". An Exception is thrown now to demonstrate a rollback behavior.");
							// CS_PROCALERT START
							// The next lines illustrate how to create and push a process state that will trigger a SAP CCMS Alert to the SAP alert framework 
							// Use the process state if you intend to report channel (connection) oriented as well as message (processing) oriented alerts
							// In order to trigger alerts process status with ProcessState.FATAL must be pushed. Other process states will be displayed in the AAM monitor but
							// won't trigger alerts. 
							try {
								MonitoringManager mm = MonitoringManagerFactory.getInstance().getMonitoringManager();
								ProcessContextFactory.ParamSet ps = ProcessContextFactory.getParamSet().message(msg).channel(channel);
								ProcessContext pc = ProcessContextFactory.getInstance().createProcessContext(ps);
								mm.reportProcessStatus(this.adapterNamespace, this.adapterType, ChannelDirection.SENDER , ProcessState.FATAL, "Rollback triggered (as demo) since channel error mode was set to " + ERR_ROLLBACK, pc);
							} catch (Exception e) {
								TRACE.catching(SIGNATURE, e);
								TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Process state propagation failed due to: {0}", new Object[] {e.getMessage()});
							}	
							// CS_PROCALERT END
							Exception e = new Exception("Sample rollback simulation test exception");
							TRACE.throwing(SIGNATURE,e);
							throw e;
						}
	
						// Rename the file which marks it as "processed" (is equivalent to an external commit!)
						// Very Important! Do this "commit" after the DB commit! If the system breaks between the DB commit and this
						// rename (=external commit) step then the message is processed again but recognized as duplicate since the
						// ID map was saved with the DB commit
						if (0 == processMode.compareToIgnoreCase(PM_RENAME)) 
							renameFile(inFileName, inputFile);
		            } catch (TxRollbackException e) {
						TRACE.catching(SIGNATURE, e);
		                // The commit has turned into a rollback; maybe there are some
		                // cleanups necessary, otherwise you can omit this catch block
						TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Rollback was performed explicitly!. Reason: {0}. Message will be processed again later.", new Object[] {e.getMessage()});
		            } catch (TxException e) {
						TRACE.catching(SIGNATURE, e);
		                // The transaction manager has encountered an unexpected error
		                // situation; turn this into an according application exception
						TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Internal transaction manager exception received. Rollback is performed!. Reason: {0}. Message will be processed again later.", new Object[] {e.getMessage()});
		            } catch (Exception e) {
						TRACE.catching(SIGNATURE, e);
		            	// Message processing error. Rollback transaction and report error
						//   START
						TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Inbound processing failed, transaction is being rollback'ed. Reason: {0}.Message will be processed again later.", new Object[] {e.getMessage()});
						TxManager.setRollbackOnly();
						// CS_LUWROLLBACK END
					}
					finally {
						// CS_LUWCOMMIT START
						// In contrast to JTA, the SAP J2EE transaction concept allows to end a transaction level. If setRollbackOnly() was called before 
						// then this LUW (layer) will be rollback'ed, otherwise it will be comitted.
						if (txTicket == null)
							TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Got no valid transaction ticket (was null).");
						else 
						{
							TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Transaction level will be committed now.");
							try {
								TxManager.commitLevel(txTicket);
			            	} catch (Exception e) {
			            		//$JL-EXC$ No other handling required-.
								TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Internal transaction manager exception received. Rollback is performed!. Reason: {0}. Message will be processed again later.", new Object[] {e.getMessage()});
			            	}
							TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Transaction level was committed succesfully.");
						// CS_LUWCOMMIT END
						}
					}
				}
				// In case of synchronous BE message do not use transactions and process the synchronous response
				else {
					try {
						// Even for synchronous messages audit log entries can be written. They are kept in the memory since 
						// the sychronous message is not be persisted, too. Hence those logs must not be flushed!
						xiMsgId = msg.getMessageId();
						MessageKey amk = new MessageKey(xiMsgId, MessageDirection.OUTBOUND);
						md.setSupplementalData("audit.key", amk);
						audit.addAuditLogEntry(amk, AuditLogStatus.SUCCESS, "Synchronous message was read from file and will be forwarded to the XI AF MS now.");
						// Hand over the message to the XI AF (module processor) now
						TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Message will be forwarded to XI AF MP and channel: " + channelId);

						ModuleData result = lookUpModuleProcessor(1).process(channelId, md);                       
						
						TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "The synchronous message with ID " + msg.getMessageId() + " was processed by the XI AF succesfully.");
						Object principal = result.getPrincipalData();
						if (principal instanceof Message) {
							Message response = (Message) principal;
							TRACE.infoT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Got back a response message. ID/FP/FS/TP/TS/IF/IFNS/Class: {0}/{1}/{2}/{3}/{4}/{5}/{6}/{7}", 
								new Object[] {response.getMessageId(), response.getFromParty().toString(), response.getFromService().toString(), response.getToParty().toString(), 
									          response.getToService().toString(), response.getAction().getName(), response.getAction().getType(), response.getMessageClass().toString()});
							
							Payload payload = response.getDocument();
							if (payload instanceof TextPayload) {
								TextPayload text = (TextPayload) payload;
								TRACE.infoT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Payload: {0}", new Object[] {text.getText()});
							}
							else
								TRACE.infoT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Received a binary response {0}", new Object[] {new String(payload.getContent())});
							
							//In case aof loopback'ed ApplicationErrors this response may contain a binary attachment
							Payload att = response.getAttachment("Attachment");
							if ((att != null) & (att instanceof TextPayload)) {
								TextPayload text = (TextPayload) att;
								TRACE.infoT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Payload: {0}", new Object[] {text.getText()});
							}
							else if (att != null)
								TRACE.infoT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Received a binary response {0}", new Object[] {new String(att.getContent())});
						}
						else {
							TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Received not a XI message as response. Class is: {0}", new Object[] {principal.getClass().getName()});
						}
						// Rename the file which marks it as "processed"
						if (0 == processMode.compareToIgnoreCase(PM_RENAME)) 
							renameFile(inFileName, inputFile);
					}
					catch (Exception e) {
						TRACE.catching(SIGNATURE, e);
						TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Synchronous inbound processing failed. Received exception: " + e.getMessage());
					}
				}
			}
			catch (Exception e) {
				TRACE.catching(SIGNATURE, e);
				TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Received exception: " + e.getMessage());
			}	
		}
	}

	/**
	 * Helper function to rename a sent file 
	 * @param inFileName File name of the input file
	 * @param inputFile File handle of the input file
	 * @throws File I/O exceptions
	 * (ra implementation specific)
	 */
	private void renameFile(String inFileName, File inputFile) throws Exception {
		final String SIGNATURE = "renameFile(String inFileName, File inputFile)";
		try {
			File renamed = new File(inFileName + ".sent");
			renamed.delete();
			if (false == inputFile.renameTo(renamed))
				TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Input file " + inFileName + " cannot be renamed. It will be sent again!");
		}
		catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
			TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Input file " + inFileName + " cannot be renamed. Received exception: " + e.getMessage());
			throw e;
		}
	}

	/**
	 * Helper function to determine a value from an arbitrary text 
	 * @param key Key name of searched value
	 * @param text Text which might contain the value
	 * @return The key value if found, "*" if not found
	 * (ra implementation specific)
	 */
	private String findValue(String key, String text) {
		final String SIGNATURE = "findValue(String key, String text)";
		int startIndex = text.indexOf(key);
		if (startIndex < 0)
			return new String(""); // Do never return '*' since '*' must not be set as lookup value e.g. for fromParty
		
		startIndex = startIndex + key.length();

		int endIndex = text.indexOf(";", startIndex);
		if (endIndex < 0)
			endIndex = text.lastIndexOf(text);
	
		String value = text.substring(startIndex, endIndex);	
		TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "findValue data (key,value,start,end): " + key + "," + value + "," + Integer.toString(startIndex) + "," + Integer.toString(endIndex));
		return value;
	}

	/**
	 * Getter for the adapterNamespace for JCA ra configuration.
	 * The adapterNamespace must be equal to related one configured in the XI IR.
	 * @return String current adapterNamespace
	 */
	public String getAdapterNamespace() {
		final String SIGNATURE = "getAdapterNamespace()";
		TRACE.entering(SIGNATURE);
		TRACE.exiting(SIGNATURE);
		return adapterNamespace;
	}

	/**
	 * Getter for the adapterType for JCA ra configuration.
	 * The adapterType must be equal to related one configured in the XI IR.
	 * @return String current adapterType
	 */
	public String getAdapterType() {
		return adapterType;
	}

	/**
	 * Setter for the adapterNamespace for JCA ra configuration.
	 * The adapterNamespace must be equal to related one configured in the XI IR.
	 * @param adapterNamespace for XI CPA lookup
	 */
	public void setAdapterNamespace(String adapterNamespace) {
		final String SIGNATURE = "setAdapterNamespace(String adapterNamespace)";
		TRACE.entering(SIGNATURE, new Object[] {adapterNamespace});
		this.adapterNamespace = adapterNamespace;
		TRACE.exiting(SIGNATURE);
	}

	/**
	 * Setter for the adapterType for JCA ra configuration.
	 * The adapterType must be equal to related one configured in the XI IR.
	 * @param adapterType for XI CPA lookup
	 */
	public void setAdapterType(String adapterType) {
		final String SIGNATURE = "setAdapterType(String adapterType)";
		TRACE.entering(SIGNATURE, new Object[] {adapterType});
		this.adapterType = adapterType;
		TRACE.exiting(SIGNATURE);
	}

	/**
	 * Getter for the MCF GUID
	 * @return GUID Guid of this MCF
	 * (ra implementation specific)
	 */
	public GUID getMcfLocalGuid() {
		return mcfLocalGuid;
	}

	/**
	 * An timertask object of this class <code>XIManagedConnectionFactoryController</code> prints out
	 * the GUID of this MCF instance periodically
	 * This timer is optional and only needed for debugging purposes. It is not used to detect duplicate
	 * MCF instances anymore since the start() and stop() method allows a specific start and stop of
	 * the inbound processing.
	 * (ra implementation specific)
	 **/
	class XIManagedConnectionFactoryController extends TimerTask {

		private SPIManagedConnectionFactory controlledMcf; 
		
		/**
		 * Creates a controller for a given MCF.
		 * @param mcf: The MCF that should be controlled
		 */
		public XIManagedConnectionFactoryController(SPIManagedConnectionFactory mcf, InitialContext ctx) {
			super();
			controlledMcf = mcf;
		}
		
		/**
		 * Control method which is called by java.util.Timer periodically 
		 * @see java.lang.Runnable#run()
		 */
		public void run() {
			final String SIGNATURE = "XIManagedConnectionFactoryController.run()";
			String controlledMcfGuid = null;
			try {
				if (controlledMcf != null)
					controlledMcfGuid = controlledMcf.getMcfLocalGuid().toHexString();
					TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "MCF with GUID {0} is running. ({1})", new Object [] {controlledMcfGuid.toString(), SPIManagedConnectionFactory.class.getClassLoader()});
			}
			catch(Exception e) {
				TRACE.catching(SIGNATURE, e);
				TRACE.warningT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Processing of control timer failed. Reason: " + e.getMessage());
			}
		}
	}

	/**
	 * The <code>start()</code> is called by the J2EE JCA container when the JCA adapter
	 * is started via the J2EE admin console (e.g. explicitly in the connector service or
	 * implicitly during a deployment). The inbound processing MUST be started now. 
	 * @see com.sap.engine.interfaces.connector.ManagedConnectionFactoryActivation#start()
	 * (ra implementation specific)
	**/
	// CS_MCFASTART START
	public void start() {
		final String SIGNATURE = "start()";
		TRACE.entering(SIGNATURE);
		String controlledMcfGuid = this.getMcfLocalGuid().toHexString();
		TRACE.infoT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "MCF with GUID {0} is started now. ({1})", new Object [] {controlledMcfGuid.toString(), SPIManagedConnectionFactory.class.getClassLoader()});

		// Please note: The sample does not contain a status of the MCF. You should add status like init, start, stop, starting and stopping
		// to control the start() and stop() calls coming from the J2EE JCA container
		
		// Get access to the audit log
		// CS_AUDITACCESS START
		try {
			audit = PublicAPIAccessFactory.getPublicAPIAccess().getAuditAccess();
		} catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
			TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT, "Unable to access the XI AF audit log. Reason: {0}. Adapter cannot not start the inbound processing!", new Object[] {e});
			TRACE.exiting(SIGNATURE);
			return;
		}
		// CS_AUDITACCESS START

		// Access the XI AF message ID mapper singleton
		// CS_IDMAPACCESS START
		messageIDMapper = MessageIDMapper.getInstance();
		if (messageIDMapper == null) {
			TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT, "Gut null as MessageIDMapper singleton instance. Adapter cannot not start the inbound processing!");
			TRACE.exiting(SIGNATURE);
			return;
		}
		// CS_IDMAPACCESS END

		// Create a message factory for XI messages
		try {
			mf = new XIMessageFactoryImpl(adapterType, adapterNamespace);
		} catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
			TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT, "Unable to create XI message factory. Adapter cannot not start the inbound processing!");
			TRACE.exiting(SIGNATURE);
			return;
		}
		
		// start the inbound processing
		try {
			this.startMCF();
			this.startTimer();  //Optional, for debugging only
			TRACE.infoT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "MCF with GUID {0} was started successfully.", new Object [] {controlledMcfGuid.toString()}); 
		}
		catch(Exception e) {
			TRACE.catching(SIGNATURE, e);
			TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Start of MCF failed. Reason: {0}", new Object[] {e.getMessage()});
		}
		
		TRACE.exiting(SIGNATURE);
	}
	// CS_MCFASTART END

	/**
	 * The <code>stop()</code> is called by the J2EE JCA container when the JCA adapter
	 * is stopped via the J2EE admin console (e.g. explicitly in the connector service or
	 * implicitly during a deployment). The inbound processing MUST be stopped now, shared
	 * ressources such as ports MUST be released now. 
	 * @see com.sap.engine.interfaces.connector.ManagedConnectionFactoryActivation#stop()
	 * (ra implementation specific)
	**/
	// CS_MCFASTOP START
	public void stop() {
		final String SIGNATURE = "stop()";
		TRACE.entering(SIGNATURE);
		String controlledMcfGuid = this.getMcfLocalGuid().toHexString();
		TRACE.infoT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "The running MCF with GUID {0} will be stopped now", 
			new Object [] {controlledMcfGuid.toString()});
		try {
			//Add cleanup of ressources here if necessary
			this.stopMCF();
			this.stopTimer(); //Optional, for debugging only
		} catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
		}	
		TRACE.infoT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "MCF with GUID {0} was stopped successfully.", new Object [] {controlledMcfGuid.toString()}); 
		TRACE.exiting(SIGNATURE);
	}
	// CS_MCFASTOP END

	/**
	 * Checks whether this mcf has a running inbound processing thread
	 * @return true if running. If false is returned no inbound messages can be sent
	 * (ra implementation specific)
	**/
	public boolean isRunning() {
		if (threadStatus == TH_STARTED) 
			return true;
		else 
			return false;
	}	
}


