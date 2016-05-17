/*
 * (C) 2006 SAP XI 7.1 Adapter Framework Resource Adapter Skeleton
 */
package com.equalize.xpi.adapter.ra;

import javax.resource.ResourceException;

import com.sap.aii.af.lib.ra.cci.XIMessageRecord;
import com.sap.engine.interfaces.messaging.api.Action;
import com.sap.engine.interfaces.messaging.api.Message;
import com.sap.engine.interfaces.messaging.api.MessageFactory;
import com.sap.engine.interfaces.messaging.api.Party;
import com.sap.engine.interfaces.messaging.api.PublicAPIAccess;
import com.sap.engine.interfaces.messaging.api.PublicAPIAccessFactory;
import com.sap.engine.interfaces.messaging.api.Service;

/**
 * The <code>XIMessageRecordImpl</code> implements a CCI custom record that contains a XI message object
 * @version: $Id: //tc/xpi.external/NW07_07_REL/src/_sample_rar_module/rar/src/com/sap/aii/af/sample/adapter/ra/XIMessageRecordImpl.java#1 $
 **/
public class XIMessageRecordImpl implements XIMessageRecord {
	
	private static final String AF_MSGFCT_TYPE = "XI";
	static final long serialVersionUID = 6501765867377473542L; //See serializable
	private static final XITrace TRACE = new XITrace(XIMessageRecordImpl.class.getName());
	
	private Message msg = null;
	private final String recordName = "XiAfCciMessageRecord";
	private String recordShortDescription = "XI AF CCI record for messages";
	private PublicAPIAccess pubAPI = null;
	private MessageFactory mf = null;
	
	/**
	 * Create a new CCI message record by creating a XI AF message internally
	 * for delegation.
	 * (XI AF CCI specific)
	 */
	private void accessMessageFactory() throws ResourceException {
		final String SIGNATURE = "accessMessageFactory()";
		TRACE.entering(SIGNATURE);
		try {
			pubAPI = PublicAPIAccessFactory.getPublicAPIAccess();
			mf = pubAPI.createMessageFactory(AF_MSGFCT_TYPE);
		}
		catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
			ResourceException re = new ResourceException(e.getMessage());
			TRACE.throwing(SIGNATURE, re);
			throw re;
		}
		TRACE.exiting(SIGNATURE);
	}
	
	/**
	 * Creates a new empty message record with only party, service, action and the
	 * message id header fields set.
	 *
	 * If any of the parameters is <code>null</code>, an
	 * <code>InvalidParamException<code> will be thrown.
	 *
	 * @param fromParty   the originating party
	 * @param toParty     the destination party
	 * @param fromService the originating service
	 * @param toService   the destination service
	 * @param action      the action of the targeting process
	 * @param actionNs    the namespace of the action
	 * 
	 * @throws ResourceException if the message couldn't be created.
	 */
	public XIMessageRecordImpl(Party fromParty, Party toParty, Service fromService, Service toService, Action action) throws ResourceException {
		final String SIGNATURE = "CciMessage(Party fromParty, Party toParty, Service fromService, Service toService, Action action)";
		TRACE.entering(SIGNATURE, new Object[] {fromParty, toParty, fromService, toService, action});
		try {
			accessMessageFactory();
			msg = mf.createMessage(fromParty, toParty, fromService, toService, action);
		}
		catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
			ResourceException re = new ResourceException(e.getMessage());
			TRACE.throwing(SIGNATURE, re);
			throw re;
		}
		TRACE.exiting(SIGNATURE);
	}
	
	/**
	 * Creates a new empty message record with only party, service, action and the
	 * message id header fields set.
	 *
	 * If any of the parameters is <code>null</code>, an
	 * <code>InvalidParamException<code> will be thrown.
	 *
	 * @param fromParty   the originating party
	 * @param toParty     the destination party
	 * @param fromService the originating service
	 * @param toService   the destination service
	 * @param action      the action of the targeting process
	 * @param actionNs    the namespace of the action
	 * @param messageId   the Message ID.
	 *
	 * @throws ResourceException if the message couldn't be created.
	 */
	public XIMessageRecordImpl(Party fromParty, Party toParty, Service fromService, Service toService, Action action, String messageId) throws ResourceException {
		final String SIGNATURE = "CciMessage(Party fromParty, Party toParty, Service fromService, Service toService, Action action, String messageId)";
		TRACE.entering(SIGNATURE, new Object[] {fromParty, toParty, fromService, toService, action, messageId});
		try {
			accessMessageFactory();
			msg = mf.createMessage(fromParty, toParty, fromService, toService, action, messageId);
		}
		catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
			ResourceException re = new ResourceException(e.getMessage());
			TRACE.throwing(SIGNATURE, re);
			throw re;
		}
		TRACE.exiting(SIGNATURE);
	}
	
	/**
	 * Create a new CCI message record but store an existing reference to XI AF message
	 * for delegation.
	 * (XI AF CCI specific)
	 */
	public XIMessageRecordImpl(Message msg) {
		this.msg = msg;
	}
	
	/**
	 * Returns the record name.
	 * (CCI JCA 1.0)
	 *
	 * @see javax.resource.cci.Record#getRecordName()
	 */
	public String getRecordName() {
		return recordName;
	}
	
	/**
	 * Sets the record name. However since this record is always a XI AF CciMessage record, it is ignored.
	 * (CCI JCA 1.0)
	 *
	 * @param name Not used
	 */
	public void setRecordName(String name) {
	}
	
	/**
	 * Gets the record description.
	 * (CCI JCA 1.0)
	 *
	 * @see javax.resource.cci.Record#setRecordShortDescription(java.lang.String)
	 */
	public void setRecordShortDescription(String recordShortDescription) {
		this.recordShortDescription = recordShortDescription;
	}
	
	/**
	 * Sets the record description.
	 * (CCI JCA 1.0)
	 *
	 * @see javax.resource.cci.Record#getRecordShortDescription()
	 */
	public String getRecordShortDescription() {
		return recordShortDescription;
	}
	
	/**
	 * Redefine clone to resolve Record/Object ambiguity
	 * (ra implementation specific)
	 * @return Cloned XIMessageRecord
	 */
	public Object clone() throws CloneNotSupportedException {
		final String SIGNATURE = "clone()";
		TRACE.entering(SIGNATURE);
		XIMessageRecordImpl cloned = null;
		try {
			cloned = new XIMessageRecordImpl(this.msg.getFromParty(), this.msg.getToParty(), this.msg.getFromService(), this.msg.getToService(), this.msg.getAction(), this.msg.getMessageId());
			Message clonedMsg = cloned.getXIMessage();
			clonedMsg.setSequenceId(this.msg.getSequenceId());
			clonedMsg.setDeliverySemantics(this.msg.getDeliverySemantics());
			clonedMsg.setDocument(this.msg.getDocument());
			clonedMsg.setRefToMessageId(this.msg.getRefToMessageId());
			cloned.setRecordName(this.getRecordName());
			cloned.setRecordShortDescription(this.getRecordShortDescription());
		} catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
			CloneNotSupportedException ce = new CloneNotSupportedException(e.getMessage());
			TRACE.throwing(SIGNATURE, ce);
			throw ce;
		}
		TRACE.exiting(SIGNATURE);
		return cloned;
	}
	
	/**
	 * Associate a new XI message object with this record
	 * (XI specific)
	 * @param message New XI message object
	 */
	public void setXIMessage(Message message) {
		msg = message;
	}
	
	/**
	 * Return the associated XI message object
	 * (XI specific)
	 * @return XI message object
	 */
	public Message getXIMessage() {
		return msg;
	}
	
}
