/*
 *  Copyright 2008 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.util;

/**
 * This constant class defines the node types, node names and property names
 * that are in use by the Hippo Site Toolkit.
 *
 * This file has to be kept in sync with:
 * hippo-ecm-hst-addon-repository/src/main/resources/org/hippoecm/repository/hst-model.cnd
 */
public final class HSTNodeTypes {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    /** Private constructor prevents from instantiation. */
    private HSTNodeTypes() {
        super();
    }

    // Node mixin type
    public static final String NT_HST_PAGE = "hst:page";
//    public static final String NT_HST_PAGEDEF = "hst:pageDef";
//    public static final String NT_HST_URLMAPPING = "hst:urlMapping";
//    public static final String NT_HST_URLMAPPINGFOLDER = "hst:urlMappingFolder";
    public static final String HST_MENU_ITEM = "hst:menuItem";
    public static final String HST_NON_SITE_MAP_ITEM = "hst:nonSiteMapItem";
    public static final String HST_CHILDLESS = "hst:childless";

    // Node name
//    public static final String HST_DISPLAYPAGE = "hst:displayPage";

    // Node properties
    public static final String HST_NODETYPE = "hst:nodeType";
//    public static final String HST_PAGETYPE = "hst:pageType";
    public static final String HST_PAGEFILE = "hst:pageFile";
    public static final String HST_LABEL = "hst:label";
    public static final String HST_I18N_LABELS = "hst:i18nLabels";
}
