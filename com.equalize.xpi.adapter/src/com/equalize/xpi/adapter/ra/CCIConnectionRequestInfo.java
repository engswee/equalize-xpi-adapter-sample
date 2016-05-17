/*
 * (C) 2006 SAP XI 7.1 Adapter Framework Resource Adapter Skeleton
 */

package com.equalize.xpi.adapter.ra;

import javax.resource.spi.ConnectionRequestInfo;

/**
 * <code>CciConnectionRequestInfo</code> is the analogon to the <code>CciConnectionSpec</code>. Whereas 
 * <code>CciConnectionSpec</code> is used to specify connection selection criterias on CCI level, the
 * <code>CciConnectionRequestInfo</code> carries these information on spi level.
 * In addition <code>CciConnectionRequestInfo</code> MUST implement equals() and hashCode() to allow the
 * J2EE server a connection pre-selection based on <code>CciConnectionRequestInfo</code> information. 
 * @version: $Id: //tc/xpi.external/NW07_07_REL/src/_sample_rar_module/rar/src/com/sap/aii/af/sample/adapter/ra/CCIConnectionRequestInfo.java#1 $
 **/
public class CCIConnectionRequestInfo implements ConnectionRequestInfo {
	private static final XITrace TRACE = new XITrace(CCIConnectionRequestInfo.class.getName());
	private String userName;	// Standard JCA 1.0 property
	private String password;    // Standard JCA 1.0 property
	private String channelId;

   // XI AF specific: Channel for this message TBD: Type of channelId in XI AF?

	/**
	 * In contrast to <code>ConnectionSpec</code> no JCA requirements regarding
	 * property setter and getter methods are formulated for <code>CciConnectionRequestInfo</code>
	 * However, getters are used to retrieve the information in the <code>ManagedConnectionFactory</code>
	 * 
	 * (ra implementation specific)
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

	/**
	 * A valid XI AF <code>CciConnectionRequestInfo</code> can only be created together
	 * with all its properties. The constructor is called by the CciConnection only.
	 *
	 * (ra implementation specific)
	 *
	 * @param userName	Name of the user if external connection requires a logon
	 * @param password	Password of the user if external connection requires a logon
	 * @param channelId	ID of the channel selected for those messages that are put into the <code>Interaction.execute()</code>
	 * 		  			call of Interactions, for which this connection is needed.
	 */	
	public CCIConnectionRequestInfo(String userName, String password, String channelId) {
		final String SIGNATURE = "CciConnectionRequestInfo(String userName, String password, String channelId)";
		TRACE.entering(SIGNATURE, new Object[] {userName, password, channelId});
		this.userName = userName;
		this.password = password;
		this.channelId = channelId;
		TRACE.exiting(SIGNATURE);
	}

	/**
	 * A JCA 1.0 compliant <code>CciConnectionRequestInfo</code> must implement
	 * the <code>equals()</code> to allow the J2EE <code>ConnectionManager</code> a
	 * pre-selection of connections. The equality MUST be defined on the complete
	 * property set (see pg. 47) 
	 *
	 * (CCI JCA 1.0)
	 *
	 * @param obj	Object to compare
	 * @return True of obj is equal (but not necessarily identical) with this <code>CciConnectionRequestInfo</code>, false otherwise 
	 */	
    public boolean equals(Object obj) {
		final String SIGNATURE = "equals(Object obj)";
		TRACE.entering(SIGNATURE, new Object[] {obj});
		boolean equal = false;
        if (obj instanceof CCIConnectionRequestInfo) {
            CCIConnectionRequestInfo other = (CCIConnectionRequestInfo) obj;
            equal =  (isEqual(this.userName, other.userName) &&
                      isEqual(this.password, other.password) &&
					  isEqual(this.channelId, other.channelId));
        }
		TRACE.exiting(SIGNATURE);
        return equal;
    }

	/**
	 * Internal helper routine to compare two objects
	 *
	 * @param o1	First object to compare
	 * @param o2	Second object to compare
	 * @return True of objects are equal 
	 */	
	private boolean isEqual(Object o1, Object o2) {
		if (o1 == null) {
			return (o2 == null);
		} else {
			return o1.equals(o2);
		}
	}

	/**
	 * A JCA 1.0 compliant <code>CciConnectionRequestInfo</code> must implement
	 * the <code>hashCode()</code> to allow the J2EE <code>ConnectionManager</code> a
	 * management of created connections.
	 *
	 * (CCI JCA 1.0)
	 *
	 * @return Integer value representing the hash code of this <code>CciConnectionRequestInfo</code> 
	 */	
    public int hashCode() {
		final String SIGNATURE = "hashCode()";
		TRACE.entering(SIGNATURE);
        String result = "" + userName + password + channelId;
		TRACE.exiting(SIGNATURE);
        return result.hashCode();
    }
}
