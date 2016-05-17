/*
 * (C) 2006 SAP XI 7.1 Adapter Framework Resource Adapter Skeleton
 */
package com.equalize.xpi.adapter.ra;

import java.util.Vector;

import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ManagedConnection;

/**
 * JCA provides an event callback mechanism that enables a J2EE application server to receive 
 * notifications from a <code>ManagedConnection</code> instance. 
 * The J2EE server implements this class in order to listen to event notifications from 
 * <code>ManagedConnectiony</code> instances.
 * <code>XiConnectionEventListenerManager</code> manages the received J2EE <code>ConnectionEventLIsteners</code>
 * and sends events to them if requested by the the <code>sendEvent()</code> method. It is neither a JCA CCI nor spi
 * related interface implementation.
 * (ra implementation specific) 
 * @version: $Id: //tc/xpi.external/NW07_07_REL/src/_sample_rar_module/rar/src/com/sap/aii/af/sample/adapter/ra/XIConnectionEventListenerManager.java#1 $
 **/
public class XIConnectionEventListenerManager {

	private static final XITrace TRACE = new XITrace(XIConnectionEventListenerManager.class.getName());
    private Vector listeners;
    private ManagedConnection mc = null;

    /**
     * Creates a event listener manager for one particular <code>ManagedConenction</code>.
	 * (ra implementation specific)
	 *  
	 * @param mc	Managed connection for which the <code>ConnectionEventListener</code> are registered.
	 */
	public XIConnectionEventListenerManager(ManagedConnection mc) {
		final String SIGNATURE = "CciConnectionEventListenerManager(ManagedConnection mc)";
		TRACE.entering(SIGNATURE, new Object[] {mc});
        listeners = new Vector();
        this.mc = mc;
		TRACE.exiting(SIGNATURE);
    }

	/**
	 * Sends a <code>ConnectionEvent</code> to all registered <code>ConnectionEventListeners</code>.
	 * (ra implementation specific)
	 *  
	 * @param eventType	One of the <code>ConnectionEvent</code> event types
	 * @param ex Exception if event is related to an exception, might be <code>null</code>
	 * @param connectionHandle An object that represents the "handle" to the <code>ManagedConnection</code>, might be <code>null</code>
	 */
    public void sendEvent(int eventType, Exception ex, Object connectionHandle) {
		final String SIGNATURE = "sendEvent(int eventType, Exception ex, Object connectionHandle)";
		TRACE.entering(SIGNATURE, new Object[] {new Integer(eventType), ex, connectionHandle});

        Vector list = (Vector) listeners.clone();
        
        // Create the JCA spi connection event object
        ConnectionEvent ce = null;

        if (ex == null) {
            ce = new ConnectionEvent(mc, eventType);
        } else {
            ce = new ConnectionEvent(mc, eventType, ex);
        }
        
        if (connectionHandle != null) {
            ce.setConnectionHandle(connectionHandle);
        }
        
        // Fire the connection event
        
        int size = list.size();
        for (int i=0; i<size; i++) {
            ConnectionEventListener l = (ConnectionEventListener) list.elementAt(i);
            switch (eventType) {
            case ConnectionEvent.CONNECTION_CLOSED:
                l.connectionClosed(ce);
                break;
            case ConnectionEvent.LOCAL_TRANSACTION_STARTED:
                l.localTransactionStarted(ce);
                break;
            case ConnectionEvent.LOCAL_TRANSACTION_COMMITTED:
                l.localTransactionCommitted(ce);
                break;
            case ConnectionEvent.LOCAL_TRANSACTION_ROLLEDBACK:
                l.localTransactionRolledback(ce);
                break;
            case ConnectionEvent.CONNECTION_ERROR_OCCURRED:
                l.connectionErrorOccurred(ce);
                break;
            default:
                throw new IllegalArgumentException("Illegal eventType: " + eventType);
            }
        }
		TRACE.exiting(SIGNATURE);
    }


	/**
	 * Add a <code>ConnectionEventListener</code> to send events to it
	 * (ra implementation specific)
	 *  
	 * @param listener	<code>ConnectionEventListener</code> to add
	 */
    public void addConnectorListener(ConnectionEventListener listener) {
		final String SIGNATURE = "addConnectorListener(ConnectionEventListener listener)";
		TRACE.entering(SIGNATURE, new Object[] {listener});
        if (listener != null)
          listeners.addElement(listener);
		TRACE.exiting(SIGNATURE);
    }

	/**
	 * Remove a given <code>ConnectionEventListener</code> from the manager
	 * (ra implementation specific)
	 *  
	 * @param listener	<code>ConnectionEventListener</code> to remove
	 */
    public void removeConnectorListener(ConnectionEventListener listener) {
		final String SIGNATURE = "removeConnectorListener(ConnectionEventListener listener)";
		TRACE.entering(SIGNATURE, new Object[] {listener});
		if (listener != null)
	        listeners.removeElement(listener);
		TRACE.exiting(SIGNATURE);
	}
}
