/*
 * Copyright 2008 Hippo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    /** Private constructor prevents from instantiation. */
    private HSTNodeTypes() {
        super();
    }
    
    // Node mixin type
    public static final String NT_HST_PAGE = "hst:page";
//    public static final String NT_HST_PAGEDEF = "hst:pagedef";
//    public static final String NT_HST_URLMAPPING = "hst:urlmapping";
//    public static final String NT_HST_URLMAPPINGFOLDER = "hst:urlmappingfolder";

    // Node names
    public static final String HST_DISPLAYPAGE = "hst:displaypage";

    // Node properties
    public static final String HST_NODETYPE = "hst:nodetype";
//    public static final String HST_PAGETYPE = "hst:pageType";
    public static final String HST_PAGEFILE = "hst:pageFile";
    public static final String PROPERTY_IS_MENU_ITEM = "hst:isMenuItem";
    public static final String PROPERTY_MENU_LABEL = "hst:menuLabel";
}
