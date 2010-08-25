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
package org.hippoecm.hst.content.beans.standard;

/**
 * Interface for all nodes of type 'hippo:facetnavigation'
 */
public interface HippoFacetNavigationBean extends HippoFolderBean {
    
    /**
     * the value of the 'hippo:count' property
     * @return Long value for the count
     */
    Long getCount();
    
    /**
     * 
     * @return the result set below this faceted navigation item
     */
    HippoResultSetBean getResultSet();
    
    /**
     * @return the root facetNavigationBean of this HippoFacetNavigationBean. If this HippoFacetNavigationBean is a HippoFacetChildNavigationBean
     * it returns the root, otherwise it might return itself.
     */
    HippoFacetNavigationBean getRootFacetNavigationBean();
}
