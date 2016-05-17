/*
 * (C) 2006 SAP XI 7.1 Adapter Framework Resource Adapter Skeleton
 */

package com.equalize.xpi.adapter.ra;

import javax.resource.NotSupportedException;
import com.sap.aii.af.lib.ra.cci.XIInteractionSpec;

/**
 * <code>CciInteractionSpec</code> defines the XI 3.0 AF interaction specification.
 * It must be used by a ra implementation if the standard AF ejbs modules are used.
 * @version: $Id: //tc/xpi.external/NW07_07_REL/src/_sample_rar_module/rar/src/com/sap/aii/af/sample/adapter/ra/XIInteractionSpecImpl.java#1 $
 **/

public class XIInteractionSpecImpl implements XIInteractionSpec
{
    static final long serialVersionUID = 380007446040217047L; //See Serializable
	private static final XITrace TRACE = new XITrace(XIInteractionSpecImpl.class.getName());
	
	/**
	 * Define the <code>InteractionSpec</code> standard properties as listed below.
	 * For details see: <code>InteractionSpec</code> interface pg. 107
	 * (CCI JCA 1.0)
	 */
	protected String functionName;
	private Integer executionTimeout;
	protected int interactionVerb;

    public XIInteractionSpecImpl()
    {
    }

    /**
	 * Sets one of the XI AF defined function names (see constants)
	 * (CCI JCA 1.0)
	 * @param functionName Name of the function to execute
	 * @throws NotSupportedException Thrown if an unknown function name should be set
	 */
	public void setFunctionName(String functionName) throws NotSupportedException
    {
        String SIGNATURE = "setFunctionName(String)";
        TRACE.entering(SIGNATURE, new Object[] {functionName} );
        
        if(!functionName.equals(XIInteractionSpec.SEND) && 
           !functionName.equals(XIInteractionSpec.CALL)) {
            NotSupportedException nse = new NotSupportedException("Invalid function name: " + functionName);
            TRACE.throwing(SIGNATURE, nse);
            throw nse;
        } 
        else {
			this.functionName = functionName;
            TRACE.exiting(SIGNATURE);
            return;
        }
    }

    /**
	 * Gets the current XI AF defined function name (see constants)
	 * (CCI JCA 1.0)
	 * @return function name
	 */
	public String getFunctionName()
    {
        String SIGNATURE = "getFunctionName()";
        TRACE.entering(SIGNATURE);
        TRACE.exiting(SIGNATURE);
        return functionName;
    }

    /**
	 * Sets maximum execution time for sync calls
	 * (CCI JCA 1.0)
	 * @param timeout Time to wait for execution
	 * @throws NotSupportedException If time is equal 0
	 */
	public void setExecutionTimeout(Integer timeout) throws NotSupportedException
    {
        String SIGNATURE = "setExecutionTimeout(Integer)";
        TRACE.entering(SIGNATURE, new Object[] {timeout});
        executionTimeout = timeout;
        if(timeout.intValue() < 0) {
            NotSupportedException nse = new NotSupportedException("Invalid timeout: " + timeout);
            TRACE.throwing(SIGNATURE, nse);
            throw nse;
        } else {
			executionTimeout = timeout;
            TRACE.exiting(SIGNATURE);
            return;
        }
    }

	/**
	 * Returns the current maximum execution time for sync calls
	 * (CCI JCA 1.0)
	 * @return Current imeout value
	 */
    public Integer getExecutionTimeout()
    {
        String SIGNATURE = "getExecutionTimeout()";
        TRACE.entering(SIGNATURE);
        TRACE.exiting(SIGNATURE, executionTimeout);
        return executionTimeout;
    }

	/**
	 * Sets one of the CCI defined interaction verbs (see constants)
	 * (CCI JCA 1.0)
	 * @param interactionVerb One of the CCI interaction verbs
	 * @throws NotSupportedException Thrown if vern is unknown
	 */
    public void setInteractionVerb(int interactionVerb) throws NotSupportedException
    {
        String SIGNATURE = "setInteractionVerb(int)";
        TRACE.entering(SIGNATURE, new Object[] { new Integer(interactionVerb) });
        this.interactionVerb = interactionVerb;
        if(interactionVerb < 0 || interactionVerb > 2) {
            NotSupportedException nse = new NotSupportedException("Invalid interaction verb: " + interactionVerb);
            TRACE.throwing(SIGNATURE, nse);
            throw nse;
        } 
        else {
            TRACE.exiting(SIGNATURE);
            return;
        }
    }

	/**
	 * Gets the current interaction verb (see constants)
	 * (CCI JCA 1.0)
	 * @return interaction verb
	 */
    public int getInteractionVerb()
    {
        String SIGNATURE = "getInteractionVerb()";
        TRACE.entering(SIGNATURE);
        TRACE.exiting(SIGNATURE, new Integer(interactionVerb));
        return interactionVerb;
    }

	/**
	 * Validates the current InteractionSpec instance
	 * (CCI JCA 1.0)
	 * @return True if valid
	 */
    public boolean isValid()
    {
        String SIGNATURE = "validate()";
        TRACE.entering(SIGNATURE);
        boolean isValid = false;
        if(interactionVerb == XIInteractionSpec.SYNC_SEND && 
           functionName.equals(XIInteractionSpec.SEND))
            isValid = true;
        if(interactionVerb == XIInteractionSpec.SYNC_SEND_RECEIVE && 
           functionName.equals(XIInteractionSpec.CALL) && 
           executionTimeout.intValue() > 0)
            isValid = true;
        if(!functionName.equals(XIInteractionSpec.SEND) && 
           !functionName.equals(XIInteractionSpec.CALL))
            isValid = true;
        TRACE.exiting(SIGNATURE, new Boolean(isValid));
        return isValid;
    }
}
