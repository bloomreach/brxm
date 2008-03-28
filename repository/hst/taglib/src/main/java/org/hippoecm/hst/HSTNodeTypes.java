package org.hippoecm.hst;

/**
 * This interface defines the node types and item names that are in use by
 * the Hippo Site Toolkit ECM edition.
 * 
 * This file has to be kept in sync with:
 * hippo-ecm-hst-addon-repository/src/main/resources/org/hippoecm/repository/hst-model.cnd
 */
public interface HSTNodeTypes {

    // Node types
    final public static String NT_HST_PAGE = "hst:page";
    final public static String NT_HST_PAGEDEF = "hst:pagedef";
    final public static String NT_HST_URLMAPPING = "hst:urlmapping";
    final public static String NT_HST_URLMAPPINGFOLDER = "hst:urlmappingfolder";

    // Item names
    final public static String HST_NODETYPE = "hst:nodetype";
    final public static String HST_DISPLAYPAGE = "hst:displaypage";
    final public static String HST_PAGETYPE = "hst:pageType";
    final public static String HST_PAGEFILE = "hst:pageFile";
    
}
