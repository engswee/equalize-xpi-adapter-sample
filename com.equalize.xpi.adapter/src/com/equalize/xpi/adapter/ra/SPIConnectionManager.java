/*
 * (C) 2006 SAP XI 7.1 Adapter Framework Resource Adapter Skeleton
 */

package com.equalize.xpi.adapter.ra;

import java.io.Serializable;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;

/**
 * The default <code>ConnectionManager</code> implementation for the non-managed scenario
 * (two-tier approach, non-J2EE approach). Although this JCA mandatory feature is not used
 * in the XI AF it is implemented here to fulfill the JCA 1.0 specification.
 * @version: $Id: //tc/xpi.external/NW07_07_REL/src/_sample_rar_module/rar/src/com/sap/aii/af/sample/adapter/ra/SPIConnectionManager.java#1 $
 **/
public class SPIConnectionManager implements ConnectionManager, Serializable {

    static final long serialVersionUID = 7827628139113836017L; //See Serializable
	private static final XITrace TRACE = new XITrace(SPIConnectionManager.class.getName());

    public SPIConnectionManager() {
    }

	/**
	 * Returns a managed connection. This sample implementation does not support any sophisticated connection
	 * pooling algorithm but returns always <code>CciConnection</code> for new managed connections.
	 *  
	 * (SPI JCA 1.0)
	 *
	 * @param mcf <code>ManagedConnectionFactory</code> to use to create a new managed connection
	 * @param info Additional <code>ConnectionRequestInfo</code> data to select the connection
	 * @return Returns a new <code>CciConnection</code> of a new managed connection
	 * @throws NotSupportedException	Thrown if managed connection cannot be created or if <code>CciConnection</code> cannot be allocated
	 */  
	public Object allocateConnection(ManagedConnectionFactory mcf, ConnectionRequestInfo info) throws ResourceException {
		final String SIGNATURE = "allocateConnection(ManagedConnectionFactory mcf, ConnectionRequestInfo info)";
		TRACE.entering(SIGNATURE, new Object[] {mcf});        
		ManagedConnection mc = 
            mcf.createManagedConnection(null, info);
        Object cciConnection = mc.getConnection(null, info);
		TRACE.exiting(SIGNATURE);
        return cciConnection;
    }
}
