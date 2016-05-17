/*
 * (C) 2006 SAP XI 7.1 Adapter Framework Resource Adapter Skeleton
 */

package com.equalize.xpi.adapter.ra;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Iterator;
import java.util.Set;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.SecurityException;
import javax.resource.spi.security.PasswordCredential;
import javax.security.auth.Subject;

/**
 * This ultility class is used to map subject data to <code>PasswordCredential</code> objects.
 * Secondly it is able to compare credentials.
 * (ra implementation specific)
 * @version: $Id: //tc/xpi.external/NW07_07_REL/src/_sample_rar_module/rar/src/com/sap/aii/af/sample/adapter/ra/XISecurityUtilities.java#1 $
 **/
public class XISecurityUtilities {
	private static final XITrace TRACE = new XITrace(XISecurityUtilities.class.getName());

	/**
	 * Returns determined credentials of the current user  
	 * (ra implementation specific)
	 *
	 * @param mcf Related managed connection factory
	 * @param subject JAAS authentification data with logon credentials to open the physical connection
	 * @param info <code>ConnectionRequestInfo</code> with additional information to open the managed connection
 	 * @return The corresponding credentials objects
 	 * @throws SecurityException If credentials cannot be determined
	 **/    
	static public PasswordCredential getPasswordCredential(final ManagedConnectionFactory mcf, final Subject subject, ConnectionRequestInfo info) throws ResourceException {
		final String SIGNATURE = "getPasswordCredential(final ManagedConnectionFactory mcf, final Subject subject, ConnectionRequestInfo info)";
		TRACE.entering(SIGNATURE, new Object[] {mcf, subject, info});
    
		PasswordCredential credential = null;

        if (subject == null) {
            if (info == null) 
				credential = null;
            else {
                CCIConnectionRequestInfo myinfo = (CCIConnectionRequestInfo) info;
				credential = new PasswordCredential(myinfo.getUserName(), myinfo.getPassword().toCharArray());
				credential.setManagedConnectionFactory(mcf);
            }
        } else {
			credential = (PasswordCredential) AccessController.doPrivileged(new PrivilegedAction() {
            	public Object run() {
                	Set creds = subject.getPrivateCredentials(PasswordCredential.class);
                    Iterator iter = creds.iterator();
                    while (iter.hasNext()) {
                       PasswordCredential temp =(PasswordCredential) iter.next();
                       if (temp.getManagedConnectionFactory().equals(mcf)) {
					      return temp;
                       }
                    }
					return null;
                }
            });
            if (credential == null)
                throw new SecurityException("No PasswordCredential found");
        }
        
		TRACE.exiting(SIGNATURE);       
        return credential;
    }

	/**
	 * Compares two string with <code>null</code> consideration
	 * (ra implementation specific)
	 *
	 * @param a string to compare
	 * @param b string to compare
	 * @return True if both strings are equal or both null
	 **/    
    static public boolean isEqual(String a, String b) {
        if (a == null) {
            return (b == null);
        } else {
            return a.equals(b);
        }
    }

	/**
	 * Compares two credentials with <code>null</code> consideration
	 * (ra implementation specific)
	 *
	 * @param a credential to compare
	 * @param b credential to compare
	 * @return True if both credential are equal or both null
	 **/    
    static public boolean isPasswordCredentialEqual(PasswordCredential a, PasswordCredential b) {
		final String SIGNATURE = "isPasswordCredentialEqual(PasswordCredential a, PasswordCredential b)";
		TRACE.entering(SIGNATURE, new Object[] {a,b});
		boolean equal = false;
        if (a == b) 
        	equal = true;
        else if ((a == null) && (b != null)) 
        	equal = false;
        else if ((a != null) && (b == null)) 
            equal = false;
        else if (!isEqual(a.getUserName(), b.getUserName())) 
            equal = false;
        else {
	        String p1 = null;
	        String p2 = null;
	        if (a.getPassword() != null) {
	            p1 = new String(a.getPassword());
	        }
	        if (b.getPassword() != null) {
	            p2 = new String(b.getPassword());
	        }
	        equal = isEqual(p1, p2);
        }
		TRACE.exiting(SIGNATURE);       
        return equal;
    }
}
