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
package org.hippoecm.hst.content.beans.standard.facetnavigation;

import java.util.List;

import org.hippoecm.hst.content.beans.standard.HippoFacetChildNavigationBean;

/**
 * The base class for child item of faceted navigation
 */
abstract public class AbstractHippoFacetChildNavigation extends HippoFacetNavigation implements HippoFacetChildNavigationBean {
    
    /**
     * The ancestors and self of this AbstractHippoFacetChildNavigation. Note that only the ancestors of the same bean type are returned. 
     * @return the ancestors (only ancestors of the same type as 'this') + self list of AbstractHippoFacetChildNavigation's
     */
    abstract List<? extends AbstractHippoFacetChildNavigation> getAncestorsAndSelf();
    
    /**
     * he ancestors and self of this AbstractHippoFacetChildNavigation. Note that only the ancestors of the same bean type are returned. 
     * @return the ancestor list of AbstractHippoFacetChildNavigation's or an empty list if no ancestors present
     */
    abstract List<? extends AbstractHippoFacetChildNavigation> getAncestors();
    
}
