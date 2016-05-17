/*
 * (C) 2006 SAP XI 7.1 Adapter Framework Resource Adapter Skeleton
 */

package com.equalize.xpi.adapter.ra;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.cci.ConnectionMetaData;
import javax.resource.cci.Interaction;
import javax.resource.cci.ResultSetInfo;
import javax.resource.spi.ConnectionEvent;

/**
 * <code>CciConnection</code> represents an JCA 1.0 compliant connection implementation.
 * It is used by the XI AF module processor to access the resource adapter for the 
 * outbound (XI->ra) direction.
 * @version: $Id: //tc/xpi.external/NW07_07_REL/src/_sample_rar_module/rar/src/com/sap/aii/af/sample/adapter/ra/CCIConnection.java#1 $
 **/

public class CCIConnection implements javax.resource.cci.Connection {

	private static final XITrace TRACE = new XITrace(CCIConnection.class.getName());
    private SPIManagedConnection mc = null;

    /** @link dependency 
     * @stereotype instantiate*/
    /*# CciConnectionFactory lnkCciConnectionFactory; */

	/**
	 * Creates a new CCI connection object used by the XI AF module processor 
	 * The constructor is called by the <code>SpiManagedConnection</code> only.
	 * (ra implementation specific)
	 *
	 * @param mc		Each connection is associated with managedConnection provided by the
	 * 					resource adapter implemenmtation. This association is created during the creation of a CCI connection
	 */
	CCIConnection(SPIManagedConnection mc) {
		final String SIGNATURE = "CciConnection(SpiManagedConnection)";
		TRACE.entering(SIGNATURE, new Object[] {mc});
        this.mc = mc;
		TRACE.exiting(SIGNATURE);
    }

	/**
	 * Factory method to create an interaction that operates on this connections
	 * According to JCA the relationship between connection:interaction is 1:n 
	 * (CCI JCA 1.0)
	 *
	 * @return New created interaction associated with this connection
	 * @throws ResourceException	Thrown if managed connection is invalid
	 */    
	public Interaction createInteraction() throws ResourceException {
		final String SIGNATURE = "createInteraction()";
		TRACE.entering(SIGNATURE);
		if (mc == null) {
			throw new ResourceException("Connection is invalid");
		}
		CCIInteraction interaction = new CCIInteraction(this);
		TRACE.exiting(SIGNATURE);
        return interaction;
    }
    
	/**
	 * Returns new local transaction.
	 * Since local transactions are not supported in this sample, a ResourceException is thrown.
	 * Note that supporting local transaction may become a MUST for XI AF compliant adapters 
	 * (CCI JCA 1.0)
	 *
	 * @return New created local transaction
	 * @throws NotSupportedException	Always thrown
	 */    
    public javax.resource.cci.LocalTransaction getLocalTransaction() 
      throws ResourceException {
	    throw new NotSupportedException("Local Transaction not supported!!");
        // if it is supported: return new CciLocalTransactionImpl(mc);
    }

	/**
	 * Returns a result set 
	 * Since result sets MUST NOT be used in the context of XI AF. Hence a a ResourceException is thrown.
	 * (CCI JCA 1.0)
	 *
	 * @return A CCI result set
	 * @throws NotSupportedException	Always thrown
	 */    
    public ResultSetInfo getResultSetInfo() throws ResourceException {
         throw new NotSupportedException("ResultSet is not supported.");
    }

	/**
	 * The XI AF module processor calls <code>close()</code> to free a connection if it will not be used anymore.
	 * According to JCA 1.0 the CCI connection must delegate this information to the underlying managed connection.
	 * How this is done and whether the managed connection will be closed is ra implementation specific.   
	 * In this sample the managedConnection is informed via <code>removeCciConnection()</code>.
	 * (CCI JCA 1.0)
	 *
	 * @throws ResourceException	Passed if thrown below
	 */    
    public void close() throws ResourceException {
		final String SIGNATURE = "close()";
		TRACE.entering(SIGNATURE);
        if (mc == null) 
        	return;  // already be closed
        mc.removeCciConnection(this);
        mc.sendEvent(ConnectionEvent.CONNECTION_CLOSED, null, this);
        mc = null;
		TRACE.exiting(SIGNATURE);
    }  

	/**
	 * Returns the descriptive connection meta data of this connection 
	 * (CCI JCA 1.0)
	 *
	 * @return The connection meta data as specified by JCA 1.0
	 * @throws NotSupportedException	Thrown if meta data object cannot be created
	 */    
    public ConnectionMetaData getMetaData() throws ResourceException {
	final String SIGNATURE = "getMetaData()";
	TRACE.entering(SIGNATURE);
	CCIConnectionMetaData cmd = new CCIConnectionMetaData(mc);
	TRACE.exiting(SIGNATURE);
	return cmd;
    }

	/**
	 * Binds another managed connection to this CCI connection. 
	 * (ra implementation specific)
	 *
	 * @param newMC	Managed connection to bind
	 * @throws IllegalStateException	Thrown if managed connection is invalid
	 */    
    void associateConnection(SPIManagedConnection newMc) throws ResourceException {
		final String SIGNATURE = "associateConnection(SPIManagedConnection newMc)";
		TRACE.entering(SIGNATURE);
        
        try {
            checkIfValid();
        } catch (ResourceException ex) {
			TRACE.catching(SIGNATURE, ex);
            throw new javax.resource.spi.IllegalStateException("Connection is invalid");
        }
        // dissociate handle with current managed connection
        mc.removeCciConnection(this);
        // associate handle with new managed connection
        newMc.addCciConnection(this);
        mc = newMc;
		TRACE.exiting(SIGNATURE);
    }

	/**
	 * Returns the associated managed connection 
	 * (ra implementation specific)
	 *
	 * @return The managed connection object if the connection is valid, null otherwise
	 */
	SPIManagedConnection getManagedConnection() {
		final String SIGNATURE = "getManagedConnection()";
		TRACE.entering(SIGNATURE);
		TRACE.exiting(SIGNATURE);
		return mc;
	}

	/**
	 * Checks whether this connection is associated to a valid managed connection 
	 * (ra implementation specific)
	 *
	 * @throws ResourceException 	Is thrown if managed connection is invalid
	 */
    void checkIfValid() throws ResourceException {
		final String SIGNATURE = "checkIfValid()";
		TRACE.entering(SIGNATURE);
        if (mc == null) {
            throw new ResourceException("Connection is invalid");
        }
		TRACE.exiting(SIGNATURE);
    }
  
	/**
	 * Invalidates the associated managed connection since that e.g. was closed externally 
	 * (ra implementation specific)
	 */
    void invalidate() {
		final String SIGNATURE = "invalidate()";
		TRACE.entering(SIGNATURE);
        mc = null;
		TRACE.exiting(SIGNATURE);
    }
}
