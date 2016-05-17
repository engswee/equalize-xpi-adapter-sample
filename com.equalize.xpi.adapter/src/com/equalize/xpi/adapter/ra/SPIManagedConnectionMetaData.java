/*
 * (C) 2006 SAP XI 7.1 Adapter Framework Resource Adapter Skeleton
 */

package com.equalize.xpi.adapter.ra;

import javax.resource.ResourceException;
import javax.resource.spi.IllegalStateException;
import javax.resource.spi.ManagedConnectionMetaData;

import com.equalize.xpi.adapter.ra.SPIManagedConnection;

/**
 * <code>SpiManagedConnectionMetaData</code> returns descriptive and configuration information 
 * related to a given managed connection as specified by the JCA 1.0 <code>ManagedConnectionMetaData</code>
 * interface.
 * @version: $Id: //tc/xpi.external/NW07_07_REL/src/_sample_rar_module/rar/src/com/sap/aii/af/sample/adapter/ra/SPIManagedConnectionMetaData.java#1 $
 **/
public class SPIManagedConnectionMetaData implements ManagedConnectionMetaData {

	private static final XITrace TRACE = new XITrace(SPIManagedConnectionMetaData.class.getName());

    private SPIManagedConnection mc;

	//private static final String version = new String("1.0");
	//private static final String name = new String("SAP XI JCA 1.0 Sample Resource Adapter File System Connection");

	/**
	 * Constructs the managed connection meta data object.
	 * (SPI JCA 1.0)
	 * @param mc	Managed connection for which this meta data object is created
	 */    
	public SPIManagedConnectionMetaData(SPIManagedConnection mc) {
        this.mc = mc;
    }

	/**
	 * Returns the product name of the connected external system 
	 * (SPI JCA 1.0)
	 * @return	The product name of the connected external system 
	 */    
	public String getEISProductName() throws ResourceException {
		return AdapterConstants.EISProductName;
    }

	/**
	 * Returns the product version of the connected external system 
	 * (SPI JCA 1.0)
	 * @return	The product version of the connected external system 
	 */    
	public String getEISProductVersion() throws ResourceException {
		return AdapterConstants.EISProductVersion;
    }


	/**
	 * Returns the maximum number of concurrently opened physical connections.
	 * Since there is no reasonable number for concurrently opened files this
	 * method returns always 0 as required by the JCA specification. 
	 * (SPI JCA 1.0)
	 * @return	Always 0, i.e. number is unlimited or unknown 
	 */    
    public int getMaxConnections() throws ResourceException {
		return 0;
    }
  
	/**
	 * Returns the userName which is used in the related managed connection for
	 * the physical connection.
	 * (SPI JCA 1.0)
	 * @return	userName as string, might be <code>null</code> if not set
	 * @throws  IllegalStateException if the related managed connection is already destroyed
	 */    
    public String getUserName() throws ResourceException {
		final String SIGNATURE = "getUserName()";
		TRACE.entering(SIGNATURE);
        if (mc.isDestroyed()) 
            throw new IllegalStateException ("ManagedConnection has been destroyed");
        
     	String userName = null;
     	
     	if (mc.getPasswordCredential() != null)
            userName = mc.getPasswordCredential().getUserName();
            
		TRACE.exiting(SIGNATURE);
        return userName;    
    }
}

