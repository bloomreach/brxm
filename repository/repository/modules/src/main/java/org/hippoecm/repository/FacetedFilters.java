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
package org.hippoecm.repository;

import org.hippoecm.repository.jackrabbit.HippoVirtualProvider;

public class FacetedFilters {

    /**
     * tries to parse this these filters to a FacetedFilters object. If this fails, an IllegalArgumentException will be thrown
     * @param filter
     * @param provider 
     * @throws IllegalArgumentException
     */
    public FacetedFilters(String[] filters, HippoVirtualProvider provider) throws IllegalArgumentException {
        
    }

    //public List<Facet>
    
    /**
     * Parses a string for a FacetedFilter.
     *
     * @param facetFilterString the formatted FacetedFilter String to parse.
     * @return FacetedFilter if the facetFilterString can be parsed to a FacetedFilter
     * @throws IllegalArgumentException the String must be a properly formatted
     *                                  
     */
    public static FacetedFilters fromString(String facetFilterString) throws IllegalArgumentException {
        return null;
    }
    
    /**
     * Returns a formatted string representation of the FacetedFilter.
     * @return a formatted string representation of the FacetedFilter 
     */
    public String toFacetFiltersString(){
        return null;
    }
}
