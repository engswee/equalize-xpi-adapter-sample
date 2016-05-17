/*
 * (C) 2006 SAP XI 7.1 Adapter Framework Resource Adapter Skeleton
 */
package com.equalize.xpi.adapter.ra;

import com.sap.aii.af.lib.ra.cci.XIConnectionSpec;

/**
 * <code>CciConnectionSpec</code> allows to pass connection selection parameters from the XI AF
 * to the resource adapter when a connection must be allocated. 
 * Whereas the properties userName and password are JCA 1.0 standard, the XI AF <code>CciConnectionSpec</code>
 * defines more XI AF specific selection criterias which MAY be used by the resource adapter as e.g.
 * the channel ID. The channel ID was determined by the XI RA mdb before and propagated to the module processor
 * chain.
 * @version: $Id: //tc/xpi.external/NW07_07_REL/src/_sample_rar_module/rar/src/com/sap/aii/af/sample/adapter/ra/XIConnectionSpecImpl.java#1 $
 **/
public class XIConnectionSpecImpl implements XIConnectionSpec {

	private static final XITrace TRACE = new XITrace(XIConnectionSpecImpl.class.getName());
    private String userName;	// Standard JCA 1.0 property
    private String password;    // Standard JCA 1.0 property
    private String channelId;   // XI AF specific: Channel for this message TBD: Type of channelId in XI AF?
	private String type;        // SAP NW specific: Type of requested connection 

	/**
	 * Creates a new CCI <code>connectionSpec</code> and fills all its properties
	 *
	 * (ra implementation specific)
	 *
	 * @param userName	Name of the user if external connection requires a logon
	 * @param password	Password of the user if external connection requires a logon
	 * @param channelId	ID of the channel selected for those messages that are put into the <code>Interaction.execute()</code>
	 * 		  			call of Interactions, for which this connection is needed.
	 * @param type	    Type of requested connection (XI, EP, ...)
	 */	
    public XIConnectionSpecImpl(String userName, String password, String channelId, String type) {
        this.userName = userName;
        this.password = password;
        this.channelId = channelId;
    	this.type = type;
    }

	/**
	 * Creates an empty new CCI <code>connectionSpec</code>
	 *
	 * (ra implementation specific)
	 */	
    public XIConnectionSpecImpl() {
    }

	/**
	 * As recommended by JCA 1.0, each <code>ConnectionSpec</code> property must have
	 * an corresponding setter and getter method.
	 * 
	 * (CCI JCA 1.0)
	 * 
	 **/

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

	public String getChannelId() {
		return channelId;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setChannelId(String channelId) {
		this.channelId = channelId;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
}
