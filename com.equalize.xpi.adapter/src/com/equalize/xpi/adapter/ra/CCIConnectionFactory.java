/*
 * (C) 2006 SAP XI 7.1 Adapter Framework Resource Adapter Skeleton
 */
package com.equalize.xpi.adapter.ra;

import java.io.Serializable;
import javax.naming.Reference;
import javax.resource.NotSupportedException;
import javax.resource.Referenceable;
import javax.resource.ResourceException;
import javax.resource.cci.ConnectionSpec;
import javax.resource.cci.RecordFactory;
import javax.resource.cci.ResourceAdapterMetaData;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnectionFactory;

import com.sap.aii.af.lib.ra.cci.XIConnectionSpec;
import com.sap.aii.af.lib.ra.cci.XIConnectionFactory;
import com.sap.aii.af.lib.ra.cci.XIRecordFactory;

/**
 * <code>CciConnectionFactory</code> serves as factory for the sample CCI connections.
 * In addition it is able to deliver the message RecordFactory.
 * The <code>CciConnectionFactory</code> itself is created by the <code>ManagedConnectionFactory</code>
 * connection to an adapter within the XI AF.
 * For JNDI look-up purposes a <code>ConnectionFactory</code> like this must also implement
 * Serializable and Referenceable.
 * @version: $Id: //tc/xpi.external/NW07_07_REL/src/_sample_rar_module/rar/src/com/sap/aii/af/sample/adapter/ra/CCIConnectionFactory.java#1 $
 **/
public class CCIConnectionFactory implements XIConnectionFactory, Serializable, Referenceable {

	static final long serialVersionUID = -6045039600076914577L;   //See Serializable IF
	private static final XITrace TRACE = new XITrace(CCIConnectionFactory.class.getName());
    private ManagedConnectionFactory mcf = null;
    private ConnectionManager cm = null;
    private XIRecordFactory rf = null;
    private Reference reference = null;

	/**
	 * The JCA specification requires a default constructor for <code>ConnectionFactory</code> (pg. 199)
	 * although it is actually not used by the <code>ManagedConnectionFactory</code>.
	 * 
	 * It may be used for the two-tier approach and is not relevant for the XI AF. 
	 * (CCI JCA 1.0)
	 * @throws ResourceException If manager creation fails or access to XI MessageFactory returns an error 
	 */
	public CCIConnectionFactory() throws ResourceException {
		final String SIGNATURE = "CciConnectionFactory()";
		TRACE.entering(SIGNATURE);
		SPIManagedConnectionFactory smcf = new SPIManagedConnectionFactory();
		this.mcf = smcf;
		this.rf = new XIMessageFactoryImpl(smcf.getAdapterType(), smcf.getAdapterNamespace()); 
		this.cm = new SPIConnectionManager();
		TRACE.exiting(SIGNATURE);
	}

	/**
	 * In case of the two-tier approach non <code>ConnectionManager</code> object is passed to the
	 * <code>ManagedConnectionFactory</code> whereas in the J2EE approach it is. The JCA specification requires
	 * that resource adapters have a default <code>ConnectionManager</code> implementation for the first case 
	 * that must not be used in the second. To fulfill the JCA specification this sample <code>ConnectionFactory</code>
	 * class offers a two-tier instantiation method which is not relevant for XI AF.
	 *
	 * (ra implementation specific)
	 *
	 * @param mcf	<code>ManagedConnectionFactory</code> that creates this <code>CciConnectionFactory</code>
	 * @throws ResourceException Thrown if mcf is not of sample 
	 */
	public CCIConnectionFactory(ManagedConnectionFactory mcf) throws ResourceException {
		final String SIGNATURE = "CciConnectionFactory(ManagedConnectionFactory mcf)";
		TRACE.entering(SIGNATURE, new Object[] {mcf});

		SPIManagedConnectionFactory smcf = null;
		if (mcf == null) {
			TRACE.warningT(SIGNATURE, XIAdapterCategories.SERVER_JCA, "ManagedConnectionFactory was null, local instance created instead!");
			smcf = new SPIManagedConnectionFactory();
		} else if (! (mcf instanceof SPIManagedConnectionFactory)) {
			ResourceException re = new ResourceException("Received ManagedConnectionFactory is not the one of the sample adapter.");
			TRACE.throwing(SIGNATURE, re);
			throw re;
		} else {
			smcf = (SPIManagedConnectionFactory) mcf;
		}

		this.mcf = smcf;
		this.rf = new XIMessageFactoryImpl(smcf.getAdapterType(), smcf.getAdapterNamespace()); 
		this.cm = new SPIConnectionManager();
		TRACE.exiting(SIGNATURE);
	}

	/**
	 * This constructor is used in the context of XI AF: The J2EE server passed a <code>ConnectionManager</code> object
	 * via the <code>createConnectionFactory</code> call to the <code>ManagedConnectionFactory</code>, which in turn
	 * will call this constructor with itself and the <code>ConenctionManager</code> object as argument. 
     *
	 * (ra implementation specific)
	 *
	 * @param mcf	<code>ManagedConnectionFactory</code> that creates this <code>CciConnectionFactory</code>
	 * @param cm	<code>ConnectionManager</code> of the J2EE server
	 * @throws ResourceException Thrown if mcf is not of sample 
	 **/
    public CCIConnectionFactory(ManagedConnectionFactory mcf, ConnectionManager cm) throws ResourceException {
		final String SIGNATURE = "CciConnectionFactory(ManagedConnectionFactory mcf, ConnectionManager cm)";
		TRACE.entering(SIGNATURE, new Object[] {mcf, cm});

		SPIManagedConnectionFactory smcf = null;
		if (mcf == null) {
			TRACE.warningT(SIGNATURE, XIAdapterCategories.SERVER_JCA, "ManagedConnectionFactory was null, local instance created instead!");
			smcf = new SPIManagedConnectionFactory();
		} else if (! (mcf instanceof SPIManagedConnectionFactory)) {
			ResourceException re = new ResourceException("Received ManagedConnectionFactory is not the one of the sample adapter.");
			TRACE.throwing(SIGNATURE, re);
			throw re;
		} else {
			smcf = (SPIManagedConnectionFactory) mcf;
		}

		this.mcf = smcf;
		this.rf = new XIMessageFactoryImpl(smcf.getAdapterType(), smcf.getAdapterNamespace()); 

        if (cm == null) {
			TRACE.warningT(SIGNATURE, XIAdapterCategories.SERVER_JCA, "ConnectionManager was null, local instance created instead (two-tier)!");
            this.cm = new SPIConnectionManager();
        } else {
            this.cm = cm;
        }
		TRACE.exiting(SIGNATURE);
    }
   
    
	/**
	 * The main task for the <code>ConnectionFactory</code> is to create/return CCI connections for the XI AF.
	 * The first of two getter methods returns a connection without any additional selection criterias.
	 * The <code>ConnectionFactory</code> delegates this request to the (J2EE) ConnectionManager which in turn
	 * communicates with the <code>ManagedConnectionFactory</code> to retrieve an existing connection or 
	 * create a new one.
	 * 
	 * @return CCI connection ready to process interactions
	 * 
	 * (CCI JCA 1.0)
	 */
    public javax.resource.cci.Connection getConnection() throws ResourceException {
		final String SIGNATURE = "getConnection()";
		TRACE.entering(SIGNATURE);
        javax.resource.cci.Connection con = null;
        con = (javax.resource.cci.Connection) cm.allocateConnection(mcf, null);
		TRACE.exiting(SIGNATURE);
	    return con;
    }

	/**
	 * The second getter method returns a connection based on additional (selection) information which are
	 * contained in the <code>ConnectionSpec</code> object. For the XI AF objects of
	 * class <code>XIConnectionSpec</code> must be used here. <code>XIConnectionSpec</code> implements
	 * <code>ConnectionSpec</code>
	 * 
	 * @param spec	<code>ConnectionSpec</code> with a kind of selection specification to find a connection
	 * 				with particular properties.
	 * @return CCI connection ready to process interactions
	 * @throws ResourceException Thrown if spec is not a CciConnectionSpec or of connection cannot be allocated
	 * 
	 * (CCI JCA 1.0)
	 */
    public javax.resource.cci.Connection getConnection(ConnectionSpec spec)  throws ResourceException {
		final String SIGNATURE = "getConnection(ConnectionSpec spec)";
		TRACE.entering(SIGNATURE, new Object[] {spec});

		// CS_CIDMCF START
		if (!(spec instanceof XIConnectionSpec)) {
			ResourceException re = new ResourceException("ConnectionSpec is not instance of CciConnectionSpec.");
			TRACE.throwing(SIGNATURE, re);
			throw re;
		}

        javax.resource.cci.Connection con = null;
        ConnectionRequestInfo info = new CCIConnectionRequestInfo(
            ((XIConnectionSpec)spec).getUserName(),
		    ((XIConnectionSpec)spec).getPassword(),
		    ((XIConnectionSpec)spec).getChannelId());	  
		     
        con = (javax.resource.cci.Connection) cm.allocateConnection(mcf, info);
		// CS_CIDMCF END
		TRACE.exiting(SIGNATURE);
		return con;
    }

	/**
	 * <code>getMetaData()</code> returns meta data information of the resource adapter as specified in JCA
	 * 
	 * @return Meta data of the resource adapter
	 * @throws ResourceException Thrown if meta data object cannot be instantiated
	 * 
	 * (CCI JCA 1.0)
	 */
    public ResourceAdapterMetaData getMetaData() throws ResourceException {
		final String SIGNATURE = "getMetaData()";
		TRACE.entering(SIGNATURE);
		CCIResourceAdapterMetaData meta = new CCIResourceAdapterMetaData();
		TRACE.exiting(SIGNATURE);
        return meta;         
    }
    
	/**
	 * <code>getRecordFactory()</code> must return the XI AF <code>messageFactory</code>.
	 * The XI AF uses this returned message factory to create the messages which are given to
	 * the resource adapter in the interaction afterwards.
	 * Note that the messageFactory can be used for the inbound direction as well to create the
	 * messages which are given to the XI AF via <code>ejb.onMessage()</code>
	 * The usage of the XI AF <code>MessageFactory</code> ensures that the customer records
	 * implement the <code>Message<code> interface AND that the implementing object is a XI message. 
	 * 
	 * @return RecordFactory implementing object of class <code>MessageFactory</code>
	 * @throws ResourceException Thrown if record factory object cannot be instantiated
	 * 
	 * (CCI JCA 1.0)
	 */
    public RecordFactory getRecordFactory() throws ResourceException {
		return (RecordFactory) rf; 
    }

	/**
	 * <code>ConnectionFactory</code> implementing classes must also implement the <code>Referenceable</code>
	 * interface (see pg. 199 JCA specification). The reason is that <code>ConnectionFactory</code> objects
	 * are retrieved by JNDI lookups. 
	 * (CCI JCA 1.0)
	 * 
	 * @param ref	<code>Reference</code> object to store.
	 */
    public void setReference(Reference ref) {
		final String SIGNATURE = "setReference()";
		TRACE.entering(SIGNATURE, new Object[] {ref});
		this.reference = ref;
		TRACE.exiting(SIGNATURE);		
    }

	/**
	 * Returns the stored <code>Reference</code> object.
	 * (CCI JCA 1.0)
	 * 
	 * @return reference	<code>Reference</code> object if stored, null otherwise.
	 */
    public Reference getReference() {
		return reference;
    }

	/**
	 * Returns a specific CCI ConnectionSpec for NetWeaver XI
	 * (XI specific)
	 * 
	 * @return XIConnectionSpec
	 */
	public XIConnectionSpec getXIConnectionSpec() throws NotSupportedException {
		return new XIConnectionSpecImpl();
	}

	/**
	 * Returns the NetWeaver XI record factory
	 * (NetWeaver specific)
	 * 
	 * @return XIRecordFactory
	 */
	public XIRecordFactory getXIRecordFactory() throws NotSupportedException, ResourceException {
		return rf;
	}
}
