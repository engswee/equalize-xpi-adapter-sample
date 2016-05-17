/*
 * (C) 2006 SAP XI 7.1 Adapter Framework Resource Adapter Skeleton
 */

package com.equalize.xpi.adapter.ra;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.resource.spi.security.PasswordCredential;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;

import com.sap.aii.af.service.cpa.Channel;
import com.sap.aii.af.service.resource.SAPSecurityResources;
import com.sap.aii.security.lib.KeyStoreManager;
import com.sap.aii.security.lib.PermissionMode;
import com.sap.security.api.ssf.ISsfProfile;

/**
 * A <code>SpiManagedConnection</code> represents a physical, managed connection from
 * the resource adapter to the connected external system.
 * In this sample resource adapter it represents a simple file.
 * @version: $Id: //tc/xpi.external/NW07_07_REL/src/_sample_rar_module/rar/src/com/sap/aii/af/sample/adapter/ra/SPIManagedConnection.java#1 $
 **/

public class SPIManagedConnection implements ManagedConnection {

	private static final XITrace TRACE = new XITrace(SPIManagedConnection.class.getName());

    private XIConnectionEventListenerManager cciListener;
    private PasswordCredential credential;
    private SPIManagedConnectionFactory mcf;
    private PrintWriter logWriter;
    private boolean supportsLocalTx;
    private boolean destroyed;
    private Set connectionSet;  // Multiple CCI connections can operate on one managed connection. Stored in this connectionSet
	private FileOutputStream physicalConnection; // File representing the physical connection
	private String outFileNamePrefix = null;     // Prefix of this file
	private String channelID = null;     		 // Corresponding XI channel ID
	private Channel channel = null;     		 // Corresponding XI channel as CPA object
	private String fileMode = null;     		 // File process mode (new, replace)
	private String directory = null;     		 // output file directory
	private String prefix = null;     		 	 // output file name starts with this string
	private File outFile = null;                 // The one and only file if file mode = "replace"
	private boolean asmaGet = false;			 // True if the adapter specific message property must be read
	private boolean asmaError = false;			 // True if an error must be thrown when the ASMA is not set

    /** @link dependency
     * @stereotype instantiate*/
    /*# SpiManagedConnectionFactory lnkSpiManagedConnectionFactory; */

    /**
	 * Creates a new managed connection object to access a physical connection to the external (EIS) system.
	 * With instantiation of the <code>ManagedConnection</code> object the physical connection is opened.
	 * Although the relation between managed and physical connection may be n:1 it is a 1:1 relationship
	 * within this sample implementation
	 * At the end an example is given how the SAP J2EE keystore can be accessed. Since this access works with code
	 * based permissions, the runtime permission XiSecurityRuntimePermission must be set for this adapter.
	 * The constructor is called by the <code>SpiManagedConnectionFactory</code> only.
	 *
	 * (ra implementation specific)
	 *
	 * @param mcf The <code>SpiManagedConnectionFactory</code> that creates this managed connection
	 * @param credential Credential previously built from the userName/password data, used for the physical connection
	 * @param supportsLocalTx True, if this managed connection must support local tx. which is currently not possible
	 * @param channelID Channel ID of the XI channel for which this connection was established
	 * @param channel Corresponding CPA channel object that describes this channel
	 * @throws ResourceException thrown if physical connection (i.e. the file) cannot be opened.
	 * @throws NotSupportedException If supportsLocalTx equals true since local transactions are currently not supported
	 */
	SPIManagedConnection(SPIManagedConnectionFactory mcf, PasswordCredential credential, boolean supportsLocalTx, String channelID, Channel channel) throws ResourceException, NotSupportedException {
		final String SIGNATURE = "SpiManagedConnection(ManagedConnectionFactory mcf, PasswordCredential credential, boolean supportsLocalTx, String channelID)";
		TRACE.entering(SIGNATURE, new Object[] {mcf, credential, new Boolean(supportsLocalTx), channelID, fileMode});
		String outFileName = "(not set)";
		String privKeyView= null;
		String privKeyAlias= null;

		if (supportsLocalTx == true)
			throw new NotSupportedException("Local transactions are not supported!");

		this.mcf = mcf;
        this.credential = credential;
        this.supportsLocalTx = supportsLocalTx;
        this.channelID = channelID;
		this.channel = channel;

        connectionSet = new HashSet();
        cciListener = new XIConnectionEventListenerManager(this);

		// Determine the channel settings from the CPA channel object
		try {
			directory = channel.getValueAsString("fileOutDir");
			prefix = channel.getValueAsString("fileOutPrefix");
			fileMode = channel.getValueAsString("fileMode");
			asmaGet = channel.getValueAsBoolean("enableDynConfigReceiver");
			if (asmaGet)
				asmaGet = channel.getValueAsBoolean("dynConfigJCAChannelID");
			asmaError = channel.getValueAsBoolean("dynConfigFailOnMissingProperties");
		}
		catch(Exception e) {
			TRACE.catching(SIGNATURE, e);
			TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Cannot access the channel parameters of channel: " + channelID + ". Defaults will be set.");
		}

		if ((directory == null) || (directory.length() == 0)) {
			TRACE.warningT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Unable to determine output file directory. Take default: " + SPIManagedConnectionFactory.OUT_DIR);
			directory = SPIManagedConnectionFactory.OUT_DIR;
		}

		if ((prefix == null) || (prefix.length() == 0)) {
			TRACE.warningT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Unable to determine output file prefix. Take default: " + SPIManagedConnectionFactory.OUT_PREFIX);
			prefix = SPIManagedConnectionFactory.OUT_PREFIX;
		}

		if ((fileMode == null) || (fileMode.length() == 0)) {
			TRACE.warningT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Unable to determine output file mode. Take default: " + SPIManagedConnectionFactory.FM_NEW);
			fileMode = SPIManagedConnectionFactory.FM_NEW;
		}

		outFileNamePrefix = new String(directory + "/" + prefix);
		TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Files of this connection will start with: " + outFileNamePrefix + ".File mode is: " + fileMode);

		// CS_ASECGETCERT START
		// The next section shows how the J2EE keystore can be accessed
		// Please note that the XISecurityRuntimePermission must be assigned to the "protection domain"
		// com.sap.aii.af.sample.ra subtree by the administrator that allows a component to access
		// It could be used for client authentication, encryption, etc. on transport level (e.g. like HTTPS)
		try {
			privKeyView = channel.getValueAsString("secViewPrivateKey");
			privKeyAlias = channel.getValueAsString("secAliasPrivateKey");

			if ((privKeyView != null) && (privKeyAlias != null)) {
				TRACE.infoT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Read configured private key now. View: {0} Alias: {1}", new Object[] {privKeyView, privKeyAlias});
				SAPSecurityResources secRes = SAPSecurityResources.getInstance();
				KeyStoreManager ksMgr = secRes.getKeyStoreManager(PermissionMode.SYSTEM_LEVEL, new String[]{ "sap.com/com.sap.aii.adapter.sample.ra" } );
				java.security.KeyStore ks = ksMgr.getKeyStore(privKeyView);
				ISsfProfile privKeyProf = ksMgr.getISsfProfile(ks, privKeyAlias, null); //Since code based permission is used no password needs to be supplied
				java.security.PrivateKey privKey = privKeyProf.getPrivateKey();
				TRACE.infoT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Got configured private key {0}", new Object[] {privKey.toString()});
			}
			else if (privKeyView == null)
				TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Private key won't be read since view is not configured.");
			else if (privKeyAlias == null)
				TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Private key won't be read since alias is not configured.");
		}
		catch(Exception e) {
			TRACE.catching(SIGNATURE, e);
			TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Unable to retrieve selected private key alias from channel configuration due to {0}", new Object[] {e.getMessage()});
		}
		// CS_ASECGETCERT END

		// Create the "physical connection", in this sample a file
        try {
        	// Take the one and only file, delete it and create an empty one
        	if (0 == fileMode.compareToIgnoreCase(SPIManagedConnectionFactory.FM_REPLACE)) {
				outFile = new File(outFileNamePrefix);
				physicalConnection = new FileOutputStream(outFile);
        	}
        	// Use the enumeration mcf support for new files
        	else {  // Default is new
				outFileName = mcf.getOutFileName(outFileNamePrefix);
				physicalConnection = new FileOutputStream(outFileName);
        	}
        }
		catch(Exception e) {
			TRACE.catching(SIGNATURE, e);
            throw new ResourceException(e.getMessage());
        }
		TRACE.infoT(SIGNATURE, XIAdapterCategories.CONNECT, "Physical connection, the file, was opened sucessfuly. Filename: " + outFileName + ".Filemode: " + fileMode);
		TRACE.exiting(SIGNATURE);
    }

	/**
	 * Returns access to the physical connection, i.e. the file, for the <code>CciInteraction</code> implementation.
	 * It allows <code>CciInteraction</code> to dump the XI message into this file.
	 *
	 * (ra implementation specific)
	 *
	 * @return FileOutputStream access to the file representing the physical connection of this managed connection. May be null in case of errors
	 */
	FileOutputStream getOutFile() {
		final String SIGNATURE = "getOutFile()";
		TRACE.entering(SIGNATURE);
		if (0 == fileMode.compareToIgnoreCase(SPIManagedConnectionFactory.FM_REPLACE)) {
			try {
				outFile.delete();
				outFile.createNewFile();
				physicalConnection = new FileOutputStream(outFile);
			} catch(Exception e) {
				TRACE.catching(SIGNATURE, e);
				TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT, "Error during reset of the output file. Filename: " + outFileNamePrefix);
				return null;
			}
		}
		TRACE.exiting(SIGNATURE);
        return physicalConnection;
    }

	/**
	 * Adapter specific message attribute switch getter
	 *
	 * (ra implementation specific)
	 *
	 * @return True if adapter specific message attribute must be evaluated
	 */
	boolean getAsmaGet() {
		return asmaGet;
	}

	/**
	 * Adapter specific message attribute switch error handling
	 *
	 * (ra implementation specific)
	 *
	 * @return True if an error must be thrown if the adapter specific message attribute is missing
	 */
	boolean getAsmaError() {
		return asmaError;
	}

	/**
	 * Returns the channelID of the channel that uses this managed connection
	 * In this sample the relationship XI channel : managedConnection is 1:1
	 * In more sophisticated implementations the relationship probably is more n:m with n>m
	 *
	 * (ra implementation specific)
	 *
	 * @return channelID of the channel
	 */
	String getChannelID() {
		return channelID;
	}

	/**
	 * Switches on the support of local transactions.
	 *
	 * (ra implementation specific)
	 *
	 * @param ltx True, if local transactions must be supported, else false. Must be false currently.
	 * @throws NotSupportedException If supportsLocalTx equals true since local transactions are currently not supported
	 */
    public void setSupportsLocalTx(boolean ltx) throws NotSupportedException {
		if (ltx == true)
			throw new NotSupportedException("Local transactions are not supported!");

        this.supportsLocalTx = ltx;
    }

	/**
	 * Returns true if local transactions are supported.
	 *
	 * (ra implementation specific)
	 *
	 * @return true, if local transactions are supported, else false
	 */
	public boolean getSupportsLocalTx() {
		return this.supportsLocalTx;
	}

	/**
	 * Assignes this <code>ManagedConnection</code> to a (new) <code>ManagedConnectionFactory</code>
	 *
	 * (ra implementation specific)
	 *
	 * @param mcf <code>ManagedConnectionFactory</code> to which this connection must be assigned to
	 */
	public void setManagedConnectionFactory(SPIManagedConnectionFactory mcf) {
        this.mcf = mcf;
    }

	/**
	 * Returns the associated <code>ManagedConnectionFactory</code>
	 *
	 * (ra implementation specific)
	 *
	 * @return Associated <code>ManagedConnectionFactory</code>
	 */
    public ManagedConnectionFactory getManagedConnectionFactory() {
        return this.mcf;
    }

	/**
	 * Returns a CCI connection that operates on this managed connection. The <code>CciConnection</code>
	 * object is always created newly. The credentials must be equal to the one which was used
	 * when the <code>ManagedConnectionFactory</code> was created.
	 * (SPI JCA 1.0)
	 *
	 * @return CCI connection for the XI AF to work with
	 * @throws SecurityException Subject does not fit to the original specified credentials
	 * @throws ResourceException CCI connection cannot be created
	 * @throws IllegalStateException Managed connection is already destroyed
	 */
    public Object getConnection(Subject subject, ConnectionRequestInfo info) throws ResourceException {
		final String SIGNATURE = "getConnection(Subject subject, ConnectionRequestInfo info)";
		TRACE.entering(SIGNATURE, new Object[] {subject, info});

        // Check credentials
        PasswordCredential newCredential = XISecurityUtilities.getPasswordCredential(mcf, subject, info);
        if (!XISecurityUtilities.isPasswordCredentialEqual(newCredential, credential)) {
            throw new javax.resource.spi.SecurityException("Principal does not match." + "Reauthentication not supported");
        }

		// Create CCI connection
        checkIfDestroyed();
        CCIConnection cciConnection = new CCIConnection(this);
        addCciConnection(cciConnection);
		TRACE.exiting(SIGNATURE);
        return cciConnection;
    }

	/**
	 * Destroys the underlying physical connection (i.e. the output file is closed)
	 * and invalidates the registered CCI connections. This managed connection cannot
	 * be used further more.
	 * (SPI JCA 1.0)
	 *
	 * @throws ResourceException CCI connection cannot be invalidated or file cannot be closed
	 */
	 public void destroy() throws ResourceException {
		final String SIGNATURE = "destroy()";
		TRACE.entering(SIGNATURE);
		destroy(false);
		TRACE.exiting(SIGNATURE);
    }

	/**
	 * @see com.equalize.xpi.adapter.ra.SPIManagedConnection#destroy()
	 * This variant informs the MCF to remove the MC from the internal pool, if called by the JCA container
	 * (ra implementation specific)
	 *
	 * @param fromMCF True if the MC destroy was initiated by the MCF, else by the JCA container
	 * @throws ResourceException CCI connection cannot be invalidated or file cannot be closed
	 */
	 void destroy(boolean fromMCF) throws ResourceException {
		final String SIGNATURE = "destroy(boolean fromMCF)";
		TRACE.entering(SIGNATURE, new Object[] {new Boolean(fromMCF)});
		if (!destroyed) {
			try {
				destroyed = true;
				Iterator it = connectionSet.iterator();
				while (it.hasNext()) {
					CCIConnection cciCon = (CCIConnection) it.next();
					cciCon.invalidate();
				}
				connectionSet.clear();
				physicalConnection.close();
			} catch (Exception ex) {
				TRACE.catching(SIGNATURE, ex);
				throw new ResourceException(ex.getMessage());
			}
		}
		if (!fromMCF)
			mcf.removeManagedConnection(this.channelID);
		TRACE.exiting(SIGNATURE);
	}

	/**
	 * Cleanups this managed connection. The current output file is closed if file mode
	 * is set to "new".
	 * It invalidates the registered CCI connections. This managed connection can be
	 * be used later, but writes to another file than before.
	 * (SPI JCA 1.0)
	 *
	 * @throws ResourceException CCI connection cannot be invalidated or file cannot be closed
	 */
    public void cleanup() throws ResourceException {
		final String SIGNATURE = "cleanup()";
		TRACE.entering(SIGNATURE);
        try {
            checkIfDestroyed();
            Iterator it = connectionSet.iterator();
            while (it.hasNext()) {
                CCIConnection cciCon = (CCIConnection) it.next();
                cciCon.invalidate();
            }
            connectionSet.clear();

			if (0 == fileMode.compareToIgnoreCase(SPIManagedConnectionFactory.FM_REPLACE)) {
				// Do nothing: Keep the one and only file open. Old content will be deleted in the next interaction
			}
			else {
				physicalConnection.close();
				String outFileName = mcf.getOutFileName(outFileNamePrefix);
				physicalConnection = new FileOutputStream(outFileName);
				TRACE.infoT(SIGNATURE, XIAdapterCategories.CONNECT, "Physical connection was cleaned and a new file was opened sucessfuly. Filename: " + outFileName);
			}
        } catch (Exception ex) {
			TRACE.catching(SIGNATURE, ex);
            throw new ResourceException(ex.getMessage());
        }
		TRACE.exiting(SIGNATURE);
    }

	/**
	 * Associate a previously created CCI connection to this managed connection
	 * (SPI JCA 1.0)
	 *
	 * @param connection CciConnection which has to be assigned to this managed connection
	 * @throws IllegalStateException connection is not instanceof <code>CciConnection</code>
	 */
    public void associateConnection(Object connection) throws ResourceException {
		final String SIGNATURE = "associateConnection(Object connection)";
		TRACE.entering(SIGNATURE);

        checkIfDestroyed();
        if (connection instanceof CCIConnection) {
            CCIConnection cciCon = (CCIConnection) connection;
            cciCon.associateConnection(this);
        } else {
        	IllegalStateException ise = new IllegalStateException("Invalid connection object: " + connection);
        	TRACE.throwing(SIGNATURE, ise);
            throw ise;
        }
		TRACE.exiting(SIGNATURE);
    }

	/**
	 * Store a new connection event listener (instantiated by the (J2EE server) container)
	 * for this managed connection. Connection change events will be send to that listener from now on.
	 * (SPI JCA 1.0)
	 *
	 * @param listener <code>ConnectionEventListener</code> to store.
	 */
    public void addConnectionEventListener(ConnectionEventListener listener) {
		final String SIGNATURE = "addConnectionEventListener(ConnectionEventListener listener)";
		TRACE.entering(SIGNATURE);
        cciListener.addConnectorListener(listener);
		TRACE.exiting(SIGNATURE);
    }

	/**
	 * Remove a connection event listener (instantiated by the (J2EE server) container)
	 * from this managed connection. Connection change events won't be send to that listener anymore.
	 * (SPI JCA 1.0)
	 *
	 * @param listener <code>ConnectionEventListener</code> to remove.
	 */
    public void removeConnectionEventListener (ConnectionEventListener listener) {
		final String SIGNATURE = "removeConnectionEventListener(ConnectionEventListener listener)";
		TRACE.entering(SIGNATURE);
        cciListener.removeConnectorListener(listener);
		TRACE.exiting(SIGNATURE);
    }

	/**
	 * Get the two-way JTA transaction if this kind of transaction is supported by the adapter.
	 * Since this sample implementation does not supports <code>XAResources</code> it throws always an exception.
	 * (SPI JCA 1.0)
	 *
	 * @return JTA transaction as <code>XAResource</code> to remove.
	 * @throws NotSupportedException Always thrown
	 */
    public XAResource getXAResource() throws ResourceException {
            throw new NotSupportedException("XA transaction not supported");
    }

	/**
	 * Get the JCA local transaction if this kind of transaction is supported by the adapter.
	 * Since this sample implementation does not supports <code>LocalTransaction</code> it throws always an exception.
	 * (SPI JCA 1.0)
	 *
	 * @return JTA transaction as <code>XAResource</code> to remove.
	 * @throws NotSupportedException Always thrown
	 */
    public javax.resource.spi.LocalTransaction getLocalTransaction() throws ResourceException {
            throw new NotSupportedException("Local transaction not supported");
    }

	/**
	 * Returns the descriptive and configuration data for this managed connection as <code>ManagedConnectionMetaData</code>.
	 * (SPI JCA 1.0)
	 *
	 * @return Managed connection meta data
	 * @throws IllegalStateException Thrown if connection is already deleted
	 */
    public ManagedConnectionMetaData getMetaData() throws ResourceException {
        checkIfDestroyed();
        return new SPIManagedConnectionMetaData(this);
    }

	/**
	 * Sets the JCA J2EE logwriter <code>PrintWriter</code> object. Although JCA specifies
	 * this mechansim it is not being used by XI AF. Instead the resource adapters should
	 * use the XI AF trace service classes as done here in this sample.
	 *
	 * (SPI JCA 1.0)
	 *
	 * @param out <code>PrintWriter</code> print writer for logging purposes
	 */
    public void setLogWriter(PrintWriter out) throws ResourceException {
		final String SIGNATURE = "setLogWriter(PrintWriter out)";
		TRACE.entering(SIGNATURE, new Object[] {out});
        this.logWriter = out;
		out.print("XI AF " + AdapterConstants.adapterName + " has received a J2EE container log writer.");
		out.print("XI AF " + AdapterConstants.adapterName + " will not use the J2EE container log writer. See the trace file for details.");
		TRACE.exiting(SIGNATURE);
    }

	/**
	 * Gets the JCA J2EE logwriter <code>PrintWriter</code> object.
	 * (SPI JCA 1.0)
	 *
	 * @return <code>PrintWriter</code> print writer for logging purposes
	 */
    public PrintWriter getLogWriter() throws ResourceException {
        return logWriter;
    }

	/* Now the non SPI methods start */

	/**
	 * Returns the "state" of this managed connection
	 * (ra implementation specific)
	 *
	 * @return true, if this managed connection is already destroyed, else false
	 */
    boolean isDestroyed() {
        return destroyed;
    }

	/**
	 * Returns the userName/password credential used by this managed connection
	 * (ra implementation specific)
	 *
	 * @return credential of this managed connection
	 */
    PasswordCredential getPasswordCredential() {
        return credential;
    }

	/**
	 * Sends a <code>ConnectionEvent</code> to all registered <code>ConnectionEventListeners</code>.
	 * (ra implementation specific)
	 *
	 * @param eventType	One of the <code>ConnectionEvent</code> event types
	 * @param ex Exception if event is related to an exception, might be <code>null</code>
	 */
	public void sendEvent(int eventType, Exception ex) {
        cciListener.sendEvent(eventType, ex, null);
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
        cciListener.sendEvent(eventType, ex, connectionHandle);
    }

	/**
	 * Adds a CCI connection in order to operate on this managed connection
	 * (ra implementation specific)
	 *
	 * @param cciCon CCI connection to add
	 */
	public void addCciConnection(CCIConnection cciCon) {
		connectionSet.add(cciCon);
	}

	/**
	 * Removes a CCI connection which operates on this managed connection
	 * (ra implementation specific)
	 *
	 * @param cciCon CCI connection which must be removed
	 */
    public void removeCciConnection(CCIConnection cciCon) {
        connectionSet.remove(cciCon);
    }

	/**
	 * Propagates the start request to the mcf
	 * (ra implementation specific)
	 */
	public void start() throws ResourceException {
		mcf.startMCF();
	}

	/**
	 * Propagates the stop request to the mcf
	 * (ra implementation specific)
	 */
	public void stop() throws ResourceException {
		mcf.stopMCF();
	}

	/**
	 * Checks whether this managed connection is already destroyed. If so, it throws an exception.
	 * (ra implementation specific)
	 *
	 * @throws IllegalStateException Thrown if connection is already deleted
	 */
    private void checkIfDestroyed() throws ResourceException {
        if (destroyed) {
            throw new javax.resource.spi.IllegalStateException("Managed connection is closed");
        }
    }
}
