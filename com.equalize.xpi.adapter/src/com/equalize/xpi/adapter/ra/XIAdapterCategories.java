/*
 * (C) 2006 SAP XI 7.1 Adapter Framework Resource Adapter Skeleton
 */
 
package com.equalize.xpi.adapter.ra;

import com.sap.tc.logging.Category;

/**
 * As illustrated in the trace sample, a XI AF resource adapter should define its
 * own hierachical subcategories and plug them into the structure at the root for
 * the 3rd party adapter
 * (ra implementation specific)
 * @version: $Id: //tc/xpi.external/NW07_07_REL/src/_sample_rar_module/rar/src/com/sap/aii/af/sample/adapter/ra/XIAdapterCategories.java#1 $
 **/
public class XIAdapterCategories {

  //CS_TRCAT START
  //Must: Define the adapter root category based on the foreseen 3rdPartyRootCategory
  //plus the own namespace of the 3rd party.

  //Attention: For some reasons the Category names are not allowed to contain dots (".")
  public final static Category MY_ADAPTER_ROOT = Category.getCategory(Category.getRoot(), "Applications/ExchangeInfrastructure/Adapter/" + AdapterConstants.adapterType);  

  //Should: Sensible differentiation of categories in the adapter
  public final static Category CONFIG = Category.getCategory(MY_ADAPTER_ROOT, "Configuration");
  public final static Category SERVER = Category.getCategory(MY_ADAPTER_ROOT, "Server");
  public final static Category SERVER_HTTP = Category.getCategory(SERVER, "HTTP");
  public final static Category SERVER_JNDI = Category.getCategory(SERVER, "Naming");
  public final static Category SERVER_JCA = Category.getCategory(SERVER, "JCA");
  public final static Category CONNECT = Category.getCategory(MY_ADAPTER_ROOT, "Connection");
  public final static Category CONNECT_EIS = Category.getCategory(SERVER, "EIS");
  public final static Category CONNECT_AF = Category.getCategory(SERVER, "Adapter Framework");
  //CS_TRCAT END
}
