/*
 * (C) 2006 SAP XI 7.1 Adapter Framework Resource Adapter Skeleton
 */

package com.equalize.xpi.adapter.ra;

import javax.resource.ResourceException;
import javax.resource.cci.IndexedRecord;
import javax.resource.cci.MappedRecord;

import com.sap.aii.af.lib.ra.cci.XIMessageRecord;
import com.sap.aii.af.lib.ra.cci.XIRecordFactory;
import com.sap.engine.interfaces.messaging.api.PublicAPIAccessFactory;
import com.sap.engine.interfaces.messaging.api.ack.AckFactory;
import com.sap.engine.interfaces.messaging.api.MessageFactory;
import com.sap.engine.interfaces.messaging.api.Message;
import com.sap.engine.interfaces.messaging.api.MessageKey;
import com.sap.engine.interfaces.messaging.api.Party;
import com.sap.engine.interfaces.messaging.api.Service;
import com.sap.engine.interfaces.messaging.api.Action;
import com.sap.engine.interfaces.messaging.api.AckType;
import com.sap.engine.interfaces.messaging.api.exception.MessagingException;

/**
 * The <code>XiMessageFactory</code> allows to create an XI AF message via
 * the XI AF message factory. It uses the XI Messaging Service API to instantiate
 * XI message objects directly. Additionally it offers a wrapper to create XI acks.
 * Secondly it serves as RecordFactory implemenation.
 * @version: $Id: //tc/xpi.external/NW07_07_REL/src/_sample_rar_module/rar/src/com/sap/aii/af/sample/adapter/ra/XIMessageFactoryImpl.java#1 $
 **/
public class XIMessageFactoryImpl implements XIRecordFactory {

	private static final String AF_MSGFCT_TYPE = "XI";   // XI AF adapters MUST use this message factory type always
	private static final XITrace TRACE = new XITrace(XIMessageFactoryImpl.class.getName());
	private MessageFactory mf = null;
	private AckFactory af = null;
	private String ackfct = null;

	/**
	 * Creates a XI AF compliant message factory. Internally it uses the XI AF
	 * Messaging Service message factory to instantiate the messages objects directly.
	 */
	public XIMessageFactoryImpl(String adapterType, String adapterNamespace) throws ResourceException {
		final String SIGNATURE = "XIMessageFactoryImpl(String adapterType, String adapterNamespace)";
		TRACE.entering(SIGNATURE, new Object[] {adapterType, adapterNamespace});
		// CS_MSGFCT START
		try {
			mf = PublicAPIAccessFactory.getPublicAPIAccess().createMessageFactory(AF_MSGFCT_TYPE);
			af = PublicAPIAccessFactory.getPublicAPIAccess().createAckFactory();
			//Adapter must use this ack connection name. adapteType and adapterNamespace must equal to the
			//values which were specified during the adapter metadata upload and which are ususally
			//specified as mcf property in the JCA adapter.
			ackfct = adapterType + "_" + adapterNamespace;
		}
		catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
			ResourceException re = new ResourceException(e.getMessage());
			TRACE.throwing(SIGNATURE, re);
			throw re;
		}
		// CS_MSGFCT END
		TRACE.exiting(SIGNATURE);
	}

  /**
   * Creates a new message record.
   *
   * If any of the parameters is <code>null</code>, an
   * <code>InvalidParamException<code> will be thrown.
   * (ra implementation specific)
   *
   * @param fromParty   the originating party
   * @param toParty     the destination party
   * @param fromService the originating service
   * @param toService   the destination service
   * @param action      the action of the targeting process. The XI GUIs use the term interface instead of action
   * @param actionNS    the action (or interface) namespace of the targeting process
   *
   * @return Message an empty message with only party, service, action and the
   * message id header fields set.
   *
   * @throws ResourceException if the message couldn't be created.
   */
	public Message createMessageRecord(String fromParty, String toParty, String fromService, String toService, String action, String actionNS)  throws ResourceException {
		final String SIGNATURE = "createMessageRecord(String fromParty, String toParty, String fromService, String toService, String action)";
		TRACE.entering(SIGNATURE, new Object[] {fromParty, toParty, fromService, toService, action});
		Message msg = null;
		try {
			Party fp = new Party(fromParty);
			Party tp = new Party(toParty);
			Service fs = new Service(fromService);
			Service ts = new Service(toService);
			Action a = new Action(action, actionNS);
			msg = mf.createMessage(fp, tp, fs, ts, a);
		}
		catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
			ResourceException re = new ResourceException(e.getMessage());
			TRACE.throwing(SIGNATURE, re);
			throw re;
		}
		return msg;
	}

	/**
	 * Creates a new message record. Analogous to createMessageRecord with string arguments.
	 * (ra implementation specific)
	 */
	public Message createMessageRecord(Party fromParty, Party toParty, Service fromService, Service toService, Action action) throws ResourceException {
		final String SIGNATURE = "createMessageRecord(Party fromParty, Party toParty, Service fromService, Service toService, Action action)";
		TRACE.entering(SIGNATURE, new Object[] {fromParty, toParty, fromService, toService, action});
		Message msg = null;
		try {
			msg = mf.createMessage(fromParty, toParty, fromService, toService, action);
		}
		catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
			ResourceException re = new ResourceException(e.getMessage());
			TRACE.throwing(SIGNATURE, re);
			throw re;
		}
		return msg;
	}

  /**
   * Creates a new message record.
   * If any of the parameters is <code>null</code>, an
   * <code>InvalidParamException<code> will be thrown.
   * (ra implementation specific)
   *
   * @param fromParty   the originating party
   * @param toParty     the destination party
   * @param fromService the originating service
   * @param toService   the destination service
   * @param action      the action of the targeting process
   * @param actionNS    the action (or interface) namespace of the targeting process
   * @param messageId   the Message ID.
   *
   * @return Message an empty message with only party, service, action and the
   * message id header fields set.
   *
   * @throws ResourceException if the message couldn't be created.
   */
	public Message createMessageRecord(String fromParty, String toParty, String fromService, String toService, String action, String actionNS, String messageId)  throws ResourceException {
		final String SIGNATURE = "createMessageRecord(String fromParty, String toParty, String fromService, String toService, String action, String messageId)";
		TRACE.entering(SIGNATURE, new Object[] {fromParty, toParty, fromService, toService, action, messageId});
		Message msg = null;
		try {
			Party fp = new Party(fromParty);
			Party tp = new Party(toParty);
			Service fs = new Service(fromService);
			Service ts = new Service(toService);
			Action a = new Action(action, actionNS);
			msg = mf.createMessage(fp, tp, fs, ts, a, messageId);
		}
		catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
			ResourceException re = new ResourceException(e.getMessage());
			TRACE.throwing(SIGNATURE, re);
			throw re;
		}
		return msg;
  	}

	/**
	 * Creates a new message record. Analogous to createMessageRecord with string arguments.
	 * (ra implementation specific)
	 */
	public Message createMessageRecord(Party fromParty, Party toParty, Service fromService, Service toService, Action action, String messageId) throws ResourceException {
		final String SIGNATURE = "createMessageRecord(Party fromParty, Party toParty, Service fromService, Service toService, Action action, String messageId)";
		TRACE.entering(SIGNATURE, new Object[] {fromParty, toParty, fromService, toService, action, messageId});
		Message msg = null;
		try {
			msg = mf.createMessage(fromParty, toParty, fromService, toService, action, messageId);
		}
		catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
			ResourceException re = new ResourceException(e.getMessage());
			TRACE.throwing(SIGNATURE, re);
			throw re;
		}
		return msg;
	}

	/**
	 * According to the RecordFactory interface a method to create map records must be provided.
	 * However, it is not being used.
	 * (CCI JCA 1.0)
	 *
	 * @throws ResourceException Always thrown
	 */
	public MappedRecord createMappedRecord(String arg0) throws ResourceException {
		throw new ResourceException("Map records are not supported.");
	}

	/**
	 * According to the RecordFactory interface a method to create index records must be provided.
	 * However, it is not being used.
	 * (CCI JCA 1.0)
	 *
	 * @throws ResourceException Always thrown
	 */
	public IndexedRecord createIndexedRecord(String arg0) throws ResourceException {
		throw new ResourceException("Index records are not supported");
	}

	/**
	 * Create a empty message record to be used by the application for the outbound case.
	 * (XI specific)
	 * @return Instance of XIMessageRecord with empty XI message
	 **/
	public XIMessageRecord createXIMessageRecord() {
		return new XIMessageRecordImpl(null);
	}

	/**
	 * Publish if acks are not supported
	 * (XI specific)
	 * @param messageKey identifies the message that cannot be ack'ed
	 * @param acksNotSupported identfies the not supported ACK type: 'deliveryAck', 'deliveryErrorAck', 'applicationAck', 'applicationErrorAck'
	 * @throws MessagingException Exception if ACK name is wrong. 
	 **/
	public void ackNotSupported(MessageKey messageKey, AckType[] acksNotSupported) throws MessagingException {
		af.ackNotSupported(ackfct, messageKey, acksNotSupported);
	}

	/**
	 * Send an application ACK
	 * (XI specific)
	 * @param messageToAck identifies the message for that an application ack has to be sent
	 **/
	public void applicationAck(MessageKey messageToAck) {
		final String SIGNATURE = "applicationAck(MessageKey messageToAck)";
		try {
			af.applicationAck(ackfct,messageToAck);
		}
		catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
		}	
	}

	/**
	 * Send an application error ACK
	 * (XI specific)
	 * @param messageToAck identifies the message for that an application ack has to be sent
	 * @param error Exception that describes the application error
	 **/
	public void applicationErrorAck(MessageKey messageToAck,java.lang.Exception error) {
		final String SIGNATURE = "applicationErrorAck(MessageKey messageToAck,java.lang.Exception error)";
		try {
			af.applicationErrorAck(ackfct, messageToAck, error);
		}
		catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
		}	
	}

	/**
	 * Send an delivery ACK
	 * (XI specific)
	 * @param messageToAck identifies the message for that a delivery ack has to be sent
	 **/
	public void deliveryAck(MessageKey messageToAck) {
		final String SIGNATURE = "deliveryAck(MessageKey messageToAck)";
		try {
			af.deliveryAck(ackfct, messageToAck);
		}
		catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
		}	
	}

	/**
	 * Send an delivery error ACK
	 * (XI specific)
	 * @param messageToAck identifies the message for that an delivery ack has to be sent
	 * @param error Exception that describes the application error
	 **/
	public void deliveryErrorAck(MessageKey messageToAck,java.lang.Exception error) {
		final String SIGNATURE = "deliveryErrorAck(MessageKey messageToAck,java.lang.Exception error)";
		try {
			af.deliveryErrorAck(ackfct, messageToAck, error);
		}
		catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
		}	
	}
}
