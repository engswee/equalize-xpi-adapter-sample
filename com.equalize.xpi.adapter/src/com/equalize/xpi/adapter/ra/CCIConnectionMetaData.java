/*
 * (C) 2006 SAP XI 7.1 Adapter Framework Resource Adapter Skeleton
 */

package com.equalize.xpi.adapter.ra;

import javax.resource.ResourceException;
import javax.resource.cci.ConnectionMetaData;
import javax.resource.spi.security.PasswordCredential;

/**
 * <code>CciConnectionMetaData</code> returns descriptive and configuration information 
 * related to a given managed connection as specified by the JCA 1.0 <code>ConnectionMetaData</code>
 * interface.
 * @version: $Id: //tc/xpi.external/NW07_07_REL/src/_sample_rar_module/rar/src/com/sap/aii/af/sample/adapter/ra/CCIConnectionMetaData.java#1 $
 **/
public class CCIConnectionMetaData implements ConnectionMetaData {

	private static final XITrace TRACE = new XITrace(CCIConnectionMetaData.class.getName());
    private SPIManagedConnection mc;

	private static final String version = new String("1.0");
	private static final String name = new String("SAP XI JCA 1.0 Sample Resource Adapter File System Connection");

    /**
     * Constructs the connection meta data object.
	 * (CCI JCA 1.0)
	 * @param mc	Managed connection for which this meta data object is created
	 */
	public CCIConnectionMetaData(SPIManagedConnection mc) {
        this.mc = mc;
    }

    /**
     * Returns the product name of the connected external system 
	 * (CCI JCA 1.0)
	 * @see javax.resource.cci.ConnectionMetaData#getEISProductName()
	 * @return	The product name of the connected external system 
	 */
	public String getEISProductName() throws ResourceException {
		return AdapterConstants.EISProductName;
    }

    /**
     * Returns the product version of the connected external system 
	 * (CCI JCA 1.0)
	 * @see javax.resource.cci.ConnectionMetaData#getEISProductVersion()
	 * @return	The product version of the connected external system 
	 */
	public String getEISProductVersion() throws ResourceException {
		return AdapterConstants.EISProductVersion;
    }
  
    /**
	 * Returns the user name (might be a technical user only) which is connected to the
	 * external system.
	 * (CCI JCA 1.0)
	 * @see javax.resource.cci.ConnectionMetaData#getUserName()
	 * @return	The user name used for the connection to the external system
	 * @throws ResourceException, if managed connection is destroyed already 
	 */
	public String getUserName() throws ResourceException {
		final String SIGNATURE = "getUserName()";
		TRACE.entering(SIGNATURE);
		
		String userName = null;
		
		if (mc.isDestroyed())
            throw new ResourceException ("ManagedConnection is destroyed");
        
		PasswordCredential cred = mc.getPasswordCredential();

		if (cred != null)
			userName = cred.getUserName();
		
		TRACE.entering(SIGNATURE);
		return userName;
    }

}

