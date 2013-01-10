/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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

    /**
     * 
     */
    public static final String NT_ABSTRACTFACETNAVIGATION = "hippofacnav:abstractfacetnavigation";
    /**
     * 
     */
    public static final String NT_FACETNAVIGATION = "hippofacnav:facetnavigation";
    /**
     * 
     */
    public static final String NT_FACETSAVAILABLENAVIGATION = "hippofacnav:facetsavailablenavigation";
    /**
     * 
     */
    public static final String NT_FACETSUBNAVIGATION = "hippofacnav:facetsubnavigation";

    /**
     * 
     */
    public static final String HIPPOFACNAV_FACETS = "hippofacnav:facets";

    /**
     * 
     */
    public static final String HIPPOFACNAV_FACETNODENAMES = "hippofacnav:facetnodenames";

    /**
     * 
     */
    public static final String HIPPOFACNAV_FACETLIMIT = "hippofacnav:limit";
    
    /**
     * 
     */
    public static final String HIPPOFACNAV_LEAF = "hippofacnav:leaf";

    /**
     * 
     */
    public static final String HIPPOFACNAV_FACETSORTBY = "hippofacnav:sortby";

    /**
     * 
     */
    public static final String HIPPOFACNAV_FACETSORTORDER = "hippofacnav:sortorder";

    /**
     * 
     */
    public static final String HIPPOFACNAV_FILTERS = "hippofacnav:filters";
    
    /**
     * 
     */
    public static final String HIPPOFACNAV_SKIP_RESULTSET_FOR_FACET_NAVIGATION_ROOT =  "hippofacnav:skipresultsetfacetnavigationroot";
    
    /**
     * 
     */
    public static final String HIPPOFACNAV_SKIP_RESULTSET_FOR_FACETS_AVAILABLE = "hippofacnav:skipresultsetfacetsavailable";

}
