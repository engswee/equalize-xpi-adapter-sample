/*
 * (C) 2006 SAP XI 7.1 Adapter Framework Resource Adapter Skeleton
 */
package com.equalize.xpi.adapter.ra;

import com.sap.aii.af.service.administration.api.i18n.LocalizationCallback;
import com.sap.aii.af.service.administration.api.i18n.ResourceBundleLocalizationCallback;

/**
 * An utility class providing a localization callback for the Sample Adapter's channel monitoring information.
 * @version: $Id: //tc/xpi.external/NW07_07_REL/src/_sample_rar_module/rar/src/com/sap/aii/af/sample/adapter/ra/XILocalizationUtilities.java#1 $
 **/
public class XILocalizationUtilities {
    /**
     * Private constructor.
     */
    private XILocalizationUtilities() {
    }

    /**
     * Obtains the class loader to use when loading the resource bundles.
     * @return the class loader
     */
    public static LocalizationCallback getLocalizationCallback() {
        return new ResourceBundleLocalizationCallback(
        		XILocalizationUtilities.class.getPackage().getName() + ".rb_JCAAdapter_ChannelMonitor",
        		XILocalizationUtilities.class.getClassLoader());
    }
}
