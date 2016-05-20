/*
 * (C) 2006 SAP XI 7.1 Adapter Framework Resource Adapter Skeleton
 */

package com.equalize.xpi.adapter.ra;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.cci.InteractionSpec;
import javax.resource.cci.Record;
import javax.resource.cci.ResourceWarning;

import com.equalize.xpi.util.converter.ConversionDOMInput;
import com.equalize.xpi.util.converter.Converter;
import com.sap.engine.interfaces.messaging.api.Action;
import com.sap.engine.interfaces.messaging.api.ErrorInfo;
import com.sap.engine.interfaces.messaging.api.Message;
import com.sap.engine.interfaces.messaging.api.MessageKey;
import com.sap.engine.interfaces.messaging.api.Payload;
import com.sap.engine.interfaces.messaging.api.XMLPayload;
import com.sap.engine.interfaces.messaging.api.AckType;

import com.sap.engine.interfaces.messaging.api.MessageDirection; //was in 3.0/7.0: AuditDirection
import com.sap.engine.interfaces.messaging.api.auditlog.AuditLogStatus;
//import com.sap.aii.af.service.auditlog.AuditMessageKey; not used anymore
import com.sap.engine.interfaces.messaging.api.auditlog.AuditAccess; //was in 3.0/7.0: com.sap.aii.af.service.auditlog.Audit

import com.sap.aii.af.lib.ra.cci.XIAdapterException;
import com.sap.aii.af.lib.ra.cci.XIInteraction;
import com.sap.aii.af.lib.ra.cci.XIInteractionSpec;
import com.sap.aii.af.lib.ra.cci.XIMessageRecord;
import com.sap.aii.af.service.cpa.BinaryData;
import com.sap.aii.af.service.cpa.Binding;
import com.sap.aii.af.service.cpa.CPAObjectType;
import com.sap.aii.af.service.cpa.Channel;
import com.sap.aii.af.service.cpa.Direction;
import com.sap.aii.af.service.cpa.NormalizationManager;
import com.sap.aii.af.service.cpa.PartyIdentifier;
import com.sap.aii.af.service.cpa.ServiceIdentifier;
import com.sap.aii.af.service.cpa.TableData;
import com.sap.aii.af.service.headermapping.HeaderMapper;
import com.sap.aii.af.service.headermapping.HeaderMappingException;
import com.sap.aii.af.service.idmap.MessageIDMapper;

import com.sap.aii.af.service.administration.api.cpa.CPAFactory;
import com.sap.aii.af.service.administration.api.cpa.CPAOutboundRuntimeLookupManager;

/**
 * <code>CciInteraction</code> represents the XI 3.0 AF compliant interaction implementation.
 * It processes the message processing for the outbound (AF->ra) direction.
 * @version: $Id: //tc/xpi.external/NW07_07_REL/src/_sample_rar_module/rar/src/com/sap/aii/af/sample/adapter/ra/CCIInteraction.java#1 $
 **/

public class CCIInteraction implements XIInteraction {

	private static final XITrace TRACE = new XITrace(CCIInteraction.class.getName());
	private static final String ADDR_AGENCY_EAN = "009"; //Values see XI IB
	private static final String ADDR_SCHEMA_GLN = "GLN"; //Values see XI IB
    private javax.resource.cci.Connection connection;
	private XIMessageFactoryImpl mf = null;
	private SPIManagedConnection mc = null;
	private SPIManagedConnectionFactory mcf = null;
	private AuditAccess audit = null;

    /** @link dependency 
     * @stereotype instantiate*/
    /*# CciConnection lnkCciInteraction; */

    /**
     * Creates a new CCI interaction object to call "methods" of an ra 
	 * The constructor is called by the CciConnection only.
	 * (ra implementation specific)
	 *
	 * @param cciConnection The corresponding connection on which this interaction takes place
	 * @throws ResourceException Thrown if message factory cannot be instantiated
	 */
	public CCIInteraction(javax.resource.cci.Connection cciConnection) throws ResourceException {
		final String SIGNATURE = "CciInteraction(javax.resource.cci.Connection)";
		TRACE.entering(SIGNATURE, new Object[] {cciConnection});

		// Get access to the referred managed connection (factory)
		if (cciConnection == null) {
			ResourceException re = new ResourceException("No related CCI connection in Interaction (cciConnection is null).");
			TRACE.throwing(SIGNATURE, re);
			throw re;
		}

		this.connection = cciConnection;
		this.mc = ((com.equalize.xpi.adapter.ra.CCIConnection)connection).getManagedConnection();		

		if (mc == null) {
			ResourceException re = new ResourceException("No related managed connection in CCI connection (mc is null)."); 
			TRACE.throwing(SIGNATURE, re);
			throw re;
		}

		this.mcf = (SPIManagedConnectionFactory) mc.getManagedConnectionFactory();

		if (mcf == null) {
			ResourceException re = new ResourceException("No related managed connection factory in managed connection (mcf is null)."); 
			TRACE.throwing(SIGNATURE, re);
			throw re;
		}

		// Get access to XI AF audit log
		audit = mcf.getAuditAccess();
		
		// Create an own XI message factory
		mf = mcf.getXIMessageFactoryImpl(); 
		
		TRACE.exiting(SIGNATURE);        
    }
    
	/**
	 * Returns the CCI connection that is related to this interaction 
	 * (CCI JCA 1.0)
	 * 
	 * @return connection		The related connection
	 */
	public javax.resource.cci.Connection getConnection() {
        return connection;
    }

	/**
	 * Disconnects the current interaction from its current CCI connection 
	 * (CCI JCA 1.0)
	 */
    public void close() throws ResourceException {  
		final String SIGNATURE = "close()";
		TRACE.entering(SIGNATURE);
        connection = null;
		TRACE.exiting(SIGNATURE);
    }

	/**
	 * Execute the interaction. This flavour of the execute() signature is mapped to the second flavor
	 * For details see: public Record execute (InteractionSpec ispec, Record input)
	 * (CCI JCA 1.0)
	 * 
	 * @param ispec		Defines the interaction to be executed
	 * @param input		The input CCI record related to the ispec 
	 * @param output	The output CCI record related to the ispec 
	 * @return True if execution was succesful
	 */
    public boolean execute (InteractionSpec ispec, Record input, Record output)
            throws ResourceException {

		final String SIGNATURE = "execute(InteractionSpec ispec, Record input, Record output)";
		TRACE.entering(SIGNATURE);
		
		if (!(output instanceof XIMessageRecord)) {
			ResourceException re = new ResourceException("Output record is no XI AF XIMessageRecord."); 
			TRACE.throwing(SIGNATURE, re);
			throw re;
		}
		
		XIMessageRecord callerOutput = (XIMessageRecord) output;
		XIMessageRecord localOutput = (XIMessageRecord) execute(ispec, input);
		
		try {
			callerOutput.setXIMessage(localOutput.getXIMessage());
			callerOutput.setRecordName(localOutput.getRecordName());
			callerOutput.setRecordShortDescription(localOutput.getRecordShortDescription());
		}
		catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
			TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT, "Exception during output record transfer. Reason: {0}", new Object[] { e.getMessage()});
			ResourceException re = new ResourceException("Output record cannot be filled. Reason: " + e.getMessage()); 
			TRACE.throwing(SIGNATURE, re);
			throw re;
		}
		

		TRACE.exiting(SIGNATURE);
        return true;
    }

	/**
	 * Execute the interaction. The execution is controlled by the InteractionSpec.
	 * The standard XI AF CCI usage requires that the InteractionSpec is a XiInteractionSpec
	 * This sample implementation dumps every interAction to file and gives back an appropiate output record.
	 * (CCI JCA 1.0)
	 *
	 * @param ispec		Defines the interaction to be executed
	 * @param input		The input CCI record related to the ispec 
	 * @return			The output CCI record related to the ispec 
	 * @throws ResourceException	Thrown if no connection, wrong input
	 */
    public Record execute (InteractionSpec ispec, Record input) throws ResourceException {
		final String SIGNATURE = "execute(InteractionSpec ispec, Record input)";
		TRACE.entering(SIGNATURE, new Object[] {ispec, input});
		Record output = null;

		// Check InteractionSpec
		if (ispec == null) {
			ResourceException re = new ResourceException("Input ispec is null."); 
			TRACE.throwing(SIGNATURE, re);
			throw re;
		}

		if (!(ispec instanceof XIInteractionSpec)) {
			ResourceException re = new ResourceException("Input ispec is no XI AF InteractionSpec."); 
			TRACE.throwing(SIGNATURE, re);
			throw re;
		}
/*		
		XIInteractionSpec XIIspec = (XIInteractionSpec)ispec;
		String method = XIIspec.getFunctionName();

		// Dispatch to the approriate function routine
		if      (method.compareTo(XIInteractionSpec.SEND) == 0) 
			output = send(XIIspec, input, mc);
		else if (method.compareTo(XIInteractionSpec.CALL) == 0)
			output = call(XIIspec, input, mc);
		else {
			ResourceException re = new ResourceException("Unknown function name in ispec: " + method); 
			TRACE.throwing(SIGNATURE, re);
			throw re;
		}*/
		output = retrieveFile(ispec, input, this.mc);
		
		TRACE.exiting(SIGNATURE);
        return output;
    }
    
    private Record retrieveFile (InteractionSpec ispec, Record input, SPIManagedConnection mc) throws ResourceException {

   		Message msg = ((XIMessageRecord) input).getXIMessage();
   		MessageKey amk = new MessageKey(msg.getMessageId(), MessageDirection.INBOUND);
       	try {
       		// Parse the XML input using DOM and evaluate the XPath expression
    		ConversionDOMInput domIn = new ConversionDOMInput(msg.getDocument().getText());
    		String xpathToFile = mc.getChannel().getValueAsString("xpathToFile");    		
    		String inFile = domIn.evaluateXPathToString(xpathToFile);
    		this.audit.addAuditLogEntry(amk, AuditLogStatus.SUCCESS, "XPath expression: " + xpathToFile);
    		this.audit.addAuditLogEntry(amk, AuditLogStatus.SUCCESS, "XPath expression value: " + inFile);
    		
    		// Retrieve the file contents
    		InputStream inpStr = new FileInputStream(new File(inFile));
    		ByteArrayOutputStream baos = Converter.toBAOS(inpStr);	

    		// Create response XI message
    		this.audit.addAuditLogEntry(amk, AuditLogStatus.SUCCESS, "File retrieved, constructing response message");
    		XIMessageRecordImpl output = new XIMessageRecordImpl(msg.getToParty(),msg.getFromParty(),
											    				msg.getToService(), msg.getFromService(),
											    				msg.getAction());
    		Message response = output.getXIMessage();

    		// Populate payload and attributes of response message
    		XMLPayload payload = response.createXMLPayload();
    		payload.setContent(baos.toByteArray());
    		payload.setName("MainDocument");
    		payload.setDescription("EQ Adapter Synchronous Response");
    		payload.setContentType("application/xml");

    		response.setDocument(payload);
    		response.setRefToMessageId(msg.getMessageId());	
    		this.audit.addAuditLogEntry(amk, AuditLogStatus.SUCCESS, "Response message construction completed");
    		return (Record) output;
    		
    	} catch (Exception e) {
    		this.audit.addAuditLogEntry(amk, AuditLogStatus.ERROR, e.getMessage());
    		ResourceException re = new ResourceException(e.getMessage()); 
			TRACE.throwing("retrieveFile (InteractionSpec ispec, Record input, SPIManagedConnection mc)", re);
			throw re;
    	}
    }

	/**
	 * Returns list of warnings occured in this interaction. This sample does not support warnings
	 * hence it returns null
	 * (CCI JCA 1.0)
	 *
	 * @return			ResourceWarnings occured in this interaction  
	 * @throws ResourceException	Never thrown
	 */
    public ResourceWarning getWarnings() throws ResourceException {
       return null;
    }

	/**
	 * Resets warning list in this interaction. It is not supported in this sample.
	 * (CCI JCA 1.0)
	 */
    public void clearWarnings() throws ResourceException {    
    }

	/**
	 * <code>getMappedHeaderFieldsAndNormalize()</code> demonstrates the usage of the
	 * XI header mapping in combination with the XI address normalization feature.
	 * Possible use case: An IDoc with product information must be sent to an UCCnet
	 * data pool. In this case the following address processing steps must be made:
	 * 1. Set a from (sender) party with the header mapping since IDoc is not aware of party identifers
	 * 2. Normalize the from and to parties from XI address values to UCCNet GLN identifers
	 * The implementation is:
	 * 1. Evaluate the receiver agreement header mapping. If defined, return the mapped
	 * header fields in the from* and to* fields. If not defined, return the field value
	 * of the corresponding message fields.
	 * 2. Take the (eventually mapped) party and service identfiers and normalize them to GLNs
 	 * (ra implementation specific)
 	 * 
	 * @param string channelID for which the corresponding receiver agreement must be evaluated
	 * @param msg XI message that has to be mapped
	 * @return String[] with address values (fromParty, fromService, toParty, toService)
	 */
	private String[] getMappedHeaderFieldsAndNormalize(String channelID, Message msg) {
		final String SIGNATURE = "getMappedHeaderFields(String channelID, Message msg, String fromParty, String fromService, String toParty, String toService)";
		TRACE.entering(SIGNATURE);

		String fromParty = null;
		String fromService = null;
		String toParty = null;
		String toService = null;
		PartyIdentifier fromPartyIdentifier = null;
		PartyIdentifier toPartyIdentifier = null;
		ServiceIdentifier fromServiceIdentifier = null;
		ServiceIdentifier toServiceIdentifier = null;

		// 1. Step: Process the header mapping
		try {
			// CS_OUTBIND START
			// If the header mapping and/or normalization has to be used (usually in case of B2B protocols only)
			// then first the agreement (called "binding" in the APIs) must be determined. Therefore two approaches exist:
			// 1. Use the OutboundRuntimeLookup access class to read the channel and binding  (preferred way!)    OR
			// 2. Use the LookupManager to determine the agreement with the channel ID
			// The second approach is problematic if the same channel is used in multiple agreements.
			// Hence this must be excluded by documentation
			TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT, "Get receiver agreement with OutboundRuntimeLookup now.");
			CPAOutboundRuntimeLookupManager outLookup = CPAFactory.getInstance().createOutboundRuntimeLookupManager(
					mcf.getAdapterType(), mcf.getAdapterNamespace(), 
					msg.getFromParty().toString(), msg.getToParty().toString(), 
					msg.getFromService().toString(), msg.getToService().toString(), 
					msg.getAction().getName(), msg.getAction().getType());
			Binding binding = outLookup.getBinding();

			// Here the second possibilty (not preferred)
			// If the channel is assigned to more than one agreement then binding and bindingByChannel could be different
			TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT, "Get receiver agreement for channel ID {0} now.", new Object[] { channelID });
			Binding bindingByChannel = CPAFactory.getInstance().getLookupManager().getBindingByChannelId(channelID);

			// It is possible to use the binding object for reading the channel and agreement configuration data:
			readSampleConfiguration(outLookup, bindingByChannel); 
			
			// And you can access the channel object 
			Channel channelFromBinding = outLookup.getChannel();
			// ... as well as the raw data of header mapping. However, it is not recommended to work with that data directly
			// but with the HeaderMapper object like it is illustrated below
			byte[] rawHeaderMappingData = outLookup.getHeaderMappingConfig();
			// CS_OUTBIND END

			// CS_HDMAP START
			// Secondly read the header mappings:
			TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT, "Get header mappings for message with ID {0} and receiver agreement with ID {1} now.", new Object[] {msg.getMessageId(), binding.getObjectId()});
			try {
				// You can work with the HeaderMapper class or object. As usual, class is prefered
				HeaderMapper hm = new HeaderMapper();   // Not used in this sample
				
				java.util.Map mappedFields = HeaderMapper.getMappedHeader(msg, binding);
				if ( (mappedFields != null) && (!mappedFields.isEmpty())) {
					if ((fromParty = (String)mappedFields.get(HeaderMapper.FROM_PARTY)) != null) {
						TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT, "Header mapping: From party {0} is mapped to {1}", new Object[] { msg.getFromParty().toString(), fromParty });
					}	
					if ((fromService = (String)mappedFields.get(HeaderMapper.FROM_SERVICE)) != null) {
						TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT, "Header mapping: From service {0} is mapped to {1}", new Object[] { msg.getFromService().toString(), fromService });
					}	
					if ((toParty = (String)mappedFields.get(HeaderMapper.TO_PARTY)) != null) {
						TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT, "Header mapping: To party {0} is mapped to {1}", new Object[] { msg.getToParty().toString(), toParty });
					}	
					if ((toService = (String)mappedFields.get(HeaderMapper.TO_SERVICE)) != null) {
						TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT, "Header mapping: To service {0} is mapped to {1}", new Object[] { msg.getToService().toString(), toService });
					}	
				}
				else 
					TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT, "Header mapping is not defined for receiver agreement: {0}", new Object[] { binding.getStringRepresentation() });
			}
			catch (HeaderMappingException he) {
				TRACE.catching(SIGNATURE, he);
				throw new HeaderMappingException(he.getMessage());
			}
		}
		catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
			TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT, "Exception during header mapping. Reason: {0}. Error will be ignored.", new Object[] { e.getMessage()});
		}

		// Set message values if no header mapping values were set 
		if (fromParty == null)
			fromParty = msg.getFromParty().toString();
		if (fromService == null)
			fromService = msg.getFromService().toString();
		if (toParty == null)
			toParty = msg.getToParty().toString();
		if (toService == null)
			toService = msg.getToService().toString();
		// CS_HDMAP END

		// CS_HDNORM START
		// 2. Step: Call the normalization manager
		try {
			TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT, "Access the normalization manager now.");
			NormalizationManager normalizer = NormalizationManager.getInstance();

			//Note: Normalize first the service, then the party in order to do the service normalization with the XI party identifer
			fromServiceIdentifier = normalizer.getAlternativeServiceIdentifier(fromParty, fromService, ADDR_SCHEMA_GLN);
			if ( (fromServiceIdentifier != null) && (fromServiceIdentifier.getServiceIdentifier() != null) && (fromServiceIdentifier.getServiceIdentifier().length() > 0)) {
				TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT, "Address normalization for service: {0} is: {1}", new Object[] { fromService, fromServiceIdentifier.getServiceIdentifier()});
				fromService = fromServiceIdentifier.getServiceIdentifier();
			}
			else	
				TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT, "Address normalization is not defined for service: {0}", new Object[] { fromService });

			fromPartyIdentifier = normalizer.getAlternativePartyIdentifier(ADDR_AGENCY_EAN, ADDR_SCHEMA_GLN, fromParty);
			if ( (fromPartyIdentifier != null) && (fromPartyIdentifier.getParty() != null) && (fromPartyIdentifier.getParty().length() > 0)) {
				TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT, "Address normalization for party: {0} is: {1}", new Object[] { fromParty, fromPartyIdentifier.getPartyIdentifier()});
				fromParty = fromPartyIdentifier.getPartyIdentifier();
			}
			else	
				TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT, "Address normalization is not defined for party: {0}", new Object[] { fromParty });

			toServiceIdentifier = normalizer.getAlternativeServiceIdentifier(toParty, toService, ADDR_SCHEMA_GLN);
			if ( (toServiceIdentifier != null) && (toServiceIdentifier.getServiceIdentifier() != null) && (toServiceIdentifier.getServiceIdentifier().length() > 0)) {
				TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT, "Address normalization for service: {0} is: {1}", new Object[] { toService, toServiceIdentifier.getServiceIdentifier()});
				toService = toServiceIdentifier.getServiceIdentifier();
			}
			else	
				TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT, "Address normalization is not defined for service: {0}", new Object[] { toService });

			toPartyIdentifier = normalizer.getAlternativePartyIdentifier(ADDR_AGENCY_EAN, ADDR_SCHEMA_GLN, toParty);
			if ( (toPartyIdentifier != null) && (toPartyIdentifier.getParty() != null) && (toPartyIdentifier.getParty().length() > 0)) {
				TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT, "Address normalization for party: {0} is: {1}", new Object[] { toParty, toPartyIdentifier.getPartyIdentifier()});
				toParty = toPartyIdentifier.getPartyIdentifier();
			}
			else	
				TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT, "Address normalization is not defined for party: {0}", new Object[] { toParty });

		}
		catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
			TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT, "Exception during address normalization. Reason: {0}. Error will be ignored.", new Object[] { e.getMessage()});
		}
		// CS_HDNORM END

		String[] result = new String[4];
		result[0] = fromParty;
		result[1] = fromService;
		result[2] = toParty;
		result[3] = toService;

		TRACE.exiting(SIGNATURE);
		return result;
	}

	/**
	 * <code>getFaultIF()</code> reads the configured fault interface for synchronous error responses
	 * 
	 * @param string channelID for which the corresponding receiver agreement must be evaluated
	 * @return String[] with fault interface values (name, namespace)
	 */
	private String[] getFaultIF(String channelID) {
		final String SIGNATURE = "getFaultIF(String channelID)";
		TRACE.entering(SIGNATURE, new Object[] {channelID});
		String[] result = new String[2];

		try {
			TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT, "Get channel CPA object with channelID {0}", new Object[] {channelID});
			Channel channel = (Channel) CPAFactory.getInstance().getLookupManager().getCPAObject(CPAObjectType.CHANNEL, channelID);
			result[0] = channel.getValueAsString("faultInterface");
			result[1] = channel.getValueAsString("faultInterfaceNamespace");
			TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Read this fault interface value: Name: {0} Namespace: {1}", new Object[] {result[0], result[1]});
		} catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
			result[0] = "XIAFJCASampleFault";
			result[1] = "http://sap.com/xi/XI/sample/JCA";
			TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Fault interface cannot be read from channel configuration due to {0}. Take defaults value: Name: {1} Namespace: {2}", new Object[] {e.getMessage(), result[0], result[1]});
		}

		TRACE.exiting(SIGNATURE);
		return result;
	}

	/**
	 * <code>readSampleConfiguration()</code> illustrates how the XI AF CPA configuration
	 * data can be read by using the OutboundRuntimeLookup and Binding object. This method is not being really used
	 * by this sample adapter since it reads the sample attributes only
	 * @param outLookup The OutboundRuntimeLookup object to read the config data
	 * @param binding The binding object to read data from 
	 */
	private void readSampleConfiguration(CPAOutboundRuntimeLookupManager outLookup, Binding binding) {
		final String SIGNATURE = "readSampleConfiguration(OutboundRuntimeLookup outLookup)";
		TRACE.entering(SIGNATURE, new Object[] {outLookup});
		
		// CS_OUTLOOK START
		try {
			// First read the binding data first which typically represent security settings
			String sampleStringValue = outLookup.getBindingValueAsString("sampleString");
			long sampleLongValue = outLookup.getBindingValueAsLong("sampleLong");
			int sampleIntValue = outLookup.getBindingValueAsInt("sampleInteger");
			boolean sampleBooleanValue = outLookup.getBindingValueAsBoolean("sampleBoolean");
			Object sampleObjectValue = outLookup.getBindingValueAsString("sampleString");
			BinaryData sampleBinaryValue = outLookup.getBindingValueAsBinary("sampleBinary");
			TableData sampleTableValue = outLookup.getBindingValueAsTable("sampleTable");

			// Print out the sample values as example
			TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Sample binding configuration read: String {0}, long {1}, int {2}, boolean {3}, Object {4}", 
				new Object[] {sampleStringValue, String.valueOf(sampleLongValue), String.valueOf(sampleIntValue), String.valueOf(sampleBooleanValue), sampleObjectValue.toString()});

			// Treat passwords carefully!
			String samplePasswordValue = outLookup.getBindingValueAsString("samplePassword");
			if (true == outLookup.isBindingValuePassword("samplePassword"))
				TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Binding passwords must not be printed out in logfiles!");
			else
				TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "The binding 'samplePassword' parameter was no password? Value: {0}", new Object[] {samplePasswordValue});

			// Secondly read the channel data
			sampleStringValue = outLookup.getChannelValueAsString("sampleString");
			sampleLongValue = outLookup.getChannelValueAsLong("sampleLong");
			sampleIntValue = outLookup.getChannelValueAsInt("sampleInteger");
			sampleBooleanValue = outLookup.getChannelValueAsBoolean("sampleBoolean");
			sampleObjectValue = outLookup.getChannelValueAsString("sampleString");
			sampleBinaryValue = outLookup.getChannelValueAsBinary("sampleBinary");
			sampleTableValue = outLookup.getChannelValueAsTable("sampleTable");

			// Print out the sample values as example
			TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Sample channel configuration read: String {0}, long {1}, int {2}, boolean {3}, Object {4}",
				new Object[] {sampleStringValue, String.valueOf(sampleLongValue), String.valueOf(sampleIntValue), String.valueOf(sampleBooleanValue), sampleObjectValue.toString()});
			
			// Treat passwords carefully!
			samplePasswordValue = outLookup.getChannelValueAsString("samplePassword");
			if (true == outLookup.isChannelValuePassword("samplePassword"))
				TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Channel passwords must not be printed out in logfiles!");
			else
				TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "The channel 'samplePassword' parameter was no password? Value: {0}", new Object[] {samplePasswordValue});
		} catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
			TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Sample configuration read with OutboundRuntimeLookup failed. Error is ignored!");
		}
		// CS_OUTLOOK END

		// CS_BINDING START
		try {
			TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "This binding is received: Channel/Direction/Attributes = {0}/{1}/{2}", 
				new Object[] {binding.getChannelId(), binding.getDirection().toString(), binding.getAttributes()});
			// First read the address data stored in the binding
			TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Binding address data: FP/FS/TP/TS/IF/IFNS = {0}/{1}/{2}/{3}/{4}/{5}", 
				new Object[] {binding.getFromParty(), binding.getFromService(), binding.getToParty(), binding.getToService(), binding.getActionName(), binding.getAdapterNamespace()});
			// Secondly read the adapter data
			TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Binding adapter data: Type/NameSpace/SWCV = {0}/{1}/{2}",
				new Object[] {binding.getAdapterType(), binding.getAdapterNamespace(), binding.getAdapterSWCV()});
			// Thirdly, read all about mapping
			TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Binding mapping data: Id/Class/NewIF/NewIFNS = {0}/{1}/{2}/{3}",
				new Object[] {binding.getMappingId(), binding.getMappingClassName(), binding.getMappedActionName(), binding.getMappedActionNamespace()});
			// The mapping configuration itself can be read but only interpreted by the HeaderMapper as illustrated before
			byte[] mapping = binding.getHeaderMappingConfig();
			// At last, read the generic object description
			TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Binding object data: Id/Name = {0}/{1}",
				new Object[] {binding.getObjectId(), binding.getObjectName()});
			// Trace all about direction:	
			Direction direction = binding.getDirection();
			String strDir = "Unknown";
			if (direction == Direction.INBOUND)
				strDir = "INBOUND";
			if (direction == Direction.OUTBOUND)
				strDir = "OUTBOUND";
			TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Binding direction data: DB/String = {0}/{1}",
				new Object[] {direction.getDBFlag(), strDir});
				
				
		} catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
			TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Sample configuration read with Binding failed. Error is ignored!");
		}
		// CS_BINDING END

		TRACE.exiting(SIGNATURE);
	}

	/**
	 * Returns the NetWeaver XI specific InteractionSpec
	 * (XI specific)
	 * @see com.sap.aii.af.lib.ra.cci.XIInteraction#getXIInteractionSpec(java.lang.String)
	 * 
	 * @return The XI specific InteractionSpec
	 */
	public XIInteractionSpec getXIInteractionSpec() throws NotSupportedException {
		return new XIInteractionSpecImpl();
	}
}
