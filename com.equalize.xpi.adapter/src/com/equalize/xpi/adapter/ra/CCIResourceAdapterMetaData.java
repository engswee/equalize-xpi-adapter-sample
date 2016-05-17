/*
 * (C) 2006 SAP XI 7.1 Adapter Framework Resource Adapter Skeleton
 */

package com.equalize.xpi.adapter.ra;

import javax.resource.cci.ResourceAdapterMetaData;

/**
 * <code>CciResourceAdapterMetaData</code> returns descriptive and configuration information 
 * of the current sample resource adapter as specified by the JCA 1.0 <code>ResourceAdapterMetaData</code>
 * interface.
 * @version: $Id: //tc/xpi.external/NW07_07_REL/src/_sample_rar_module/rar/src/com/sap/aii/af/sample/adapter/ra/CCIResourceAdapterMetaData.java#1 $
 **/

public class CCIResourceAdapterMetaData implements ResourceAdapterMetaData {

	private static final XITrace TRACE = new XITrace(CCIResourceAdapterMetaData.class.getName());
	
	private String vendorName = AdapterConstants.vendorName ;
    private String adapterVersion = AdapterConstants.adapterVersion;
    private String specVersion = AdapterConstants.specVersion;
    private String adapterName = AdapterConstants.adapterName;
    private String description = AdapterConstants.description;

    public CCIResourceAdapterMetaData() {
    }

	/**
	 * The following getter methods are required by JCA 1.0 to deliver adapter name, version, etc.
	 * (CCI JCA 1.0)
	 **/

    public String getAdapterVersion() {
        return adapterVersion;
    }
                              
    public String getSpecVersion() {
        return specVersion;
    }

    public String getAdapterName() {
        return adapterName;
    }

    public String getAdapterVendorName() {
        return vendorName ;
    }

    public String getAdapterShortDescription() {
        return this.description;
    }

	/**
	 * The following setter methods are not required by JCA 1.0 but are added by the sample 
	 * implementation as it is allowed by the JCA specification
	 * (ra implementation specific)
	 * 
	 **/

    public void setAdapterVersion(String version) {
       this.adapterVersion = version;
    }
                              
    public void setSpecVersion(String version) {
       this.specVersion = version;
    }

    public void setAdapterName(String name) {
        this.adapterName = name;
    }

    public void setAdapterVendorName(String name) {
       this.vendorName = name;
    }

    public void setAdapterShortDescription(String description) {
        this.description = description;
    }
    
	/**
	 * A XI AF compliant resource adapter MUST return the <code>XiInteractionSpec</code> 
	 * as the one and only supported interaction spec.
	 * (signature CCI JCA 1.0, ra implementation specific return value)
	 * 
	 **/
    
    public String[] getInteractionSpecsSupported() {
		final String SIGNATURE = "CciConnection(SpiManagedConnection)";
		TRACE.entering(SIGNATURE);
        String[] str = new String[1];
        str[0]=new String("com.sap.aii.af.ra.ms.cci.XiInteractionSpec");
		TRACE.exiting(SIGNATURE);
        return str;
    }

	/** 
	 * The sample ra supports <code>interAction.execute(in, out)</code>
	 * (CCI JCA 1.0)
	 * @see javax.resource.cci.ResourceAdapterMetaData#supportsExecuteWithInputAndOutputRecord()
	 * @return Always true
	 */
    public boolean supportsExecuteWithInputAndOutputRecord() {
        return true;
    }

	/** 
	 * The sample ra supports <code>out = interAction.execute(in)</code>
	 * (CCI JCA 1.0)
	 * @see javax.resource.cci.ResourceAdapterMetaData#supportsExecuteWithInputAndOutputRecord()
	 * @return Always true
	 */
    public boolean supportsExecuteWithInputRecordOnly() {
        return true;
    }

	/** 
	 * The sample ra supports currently no local transactions
	 * (CCI JCA 1.0)
	 * @see javax.resource.cci.ResourceAdapterMetaData#supportsLocalTransactionDemarcation()
	 * @return Always false
	 */
    public boolean supportsLocalTransactionDemarcation() {
        return false;
    }
}

