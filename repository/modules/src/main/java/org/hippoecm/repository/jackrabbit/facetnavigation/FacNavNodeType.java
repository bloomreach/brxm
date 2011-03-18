/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.repository.jackrabbit.facetnavigation;

public interface FacNavNodeType {
    final static String SVN_ID = "$Id$";

    /**
     * 
     */
    final public static String NT_ABSTRACTFACETNAVIGATION = "hippofacnav:abstractfacetnavigation";
    /**
     * 
     */
    final public static String NT_FACETNAVIGATION = "hippofacnav:facetnavigation";
    /**
     * 
     */
    final public static String NT_FACETSAVAILABLENAVIGATION = "hippofacnav:facetsavailablenavigation";
    /**
     * 
     */
    final public static String NT_FACETSUBNAVIGATION = "hippofacnav:facetsubnavigation";

    /**
     * 
     */
    final public static String HIPPOFACNAV_FACETS = "hippofacnav:facets";

    /**
     * 
     */
    final public static String HIPPOFACNAV_FACETNODENAMES = "hippofacnav:facetnodenames";

    /**
     * 
     */
    final public static String HIPPOFACNAV_FACETLIMIT = "hippofacnav:limit";
    
    /**
     * 
     */
    final public static String HIPPOFACNAV_LEAF = "hippofacnav:leaf";

    /**
     * 
     */
    final public static String HIPPOFACNAV_FACETSORTBY = "hippofacnav:sortby";

    /**
     * 
     */
    final public static String HIPPOFACNAV_FACETSORTORDER = "hippofacnav:sortorder";

    /**
     * 
     */
    final public static String HIPPOFACNAV_FILTERS = "hippofacnav:filters";

}
