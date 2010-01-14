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
package org.hippoecm.repository.jackrabbit.facetnavigation;

import net.sf.json.JSONException;
import net.sf.json.JSONObject;

import org.hippoecm.repository.jackrabbit.facetnavigation.AbstractFacetNavigationProvider.FacetNavigationEntry;
import org.hippoecm.repository.jackrabbit.facetnavigation.AbstractFacetNavigationProvider.FacetNavigationEntryComparator;

public class FacetNodeView {

    String facetNodeName;
    
    // the comparator used for sorting the facet values for this facet
    FacetNavigationEntryComparator<FacetNavigationEntry> comparator;
    
    // default, there is no limit
    int limit = Integer.MAX_VALUE;
    
    public FacetNodeView(String facetNodeNameConfiguration) throws IllegalArgumentException{
        if(facetNodeNameConfiguration == null) {
            throw new IllegalArgumentException("the facetNodeNameConfiguration is not allowed to be null");
        }
        
        if(facetNodeNameConfiguration.indexOf("$") == -1) {
            this.facetNodeName = facetNodeNameConfiguration;
            return;
        }
        
        this.facetNodeName = facetNodeNameConfiguration.substring(0, facetNodeNameConfiguration.indexOf("$"));
        String jsonString = facetNodeNameConfiguration.substring(facetNodeNameConfiguration.indexOf("$")+1);
        
        if("".equals(jsonString)) {
            throw new IllegalArgumentException("Not allowed to end the '"+FacNavNodeType.HIPPOFACNAV_FACETNODENAMES+"' with a '$'");
        }
        
        try {
            JSONObject jsonObject = JSONObject.fromObject( jsonString );  
            Object sortby = jsonObject.get("sortby");
            if(sortby != null && !(sortby instanceof String)) {
                throw new IllegalArgumentException("'sortby' most be of type String if configured: not a valid json format for '"+FacNavNodeType.HIPPOFACNAV_FACETNODENAMES+"' : '"+jsonString+"'");
            }
            
            Object sortorder = jsonObject.get("sortorder");
            if(sortorder!= null && !(sortorder instanceof String)) {
                throw new IllegalArgumentException("'sortorder' most be of type String if configured: not a valid json format for '"+FacNavNodeType.HIPPOFACNAV_FACETNODENAMES+"' : '"+jsonString+"'");
            }
            
            Object newLimit = jsonObject.get("limit");
            if(newLimit != null) {
                if(!(newLimit instanceof Integer)) {
                    throw new IllegalArgumentException("'limit' most be of type Integer if configured: not a valid json format for '"+FacNavNodeType.HIPPOFACNAV_FACETNODENAMES+"' : '"+jsonString+"'");
                }
                if((Integer)newLimit < 0 ) {
                    throw new IllegalArgumentException("'limit' can not be a negative number: not a valid json format for '"+FacNavNodeType.HIPPOFACNAV_FACETNODENAMES+"' : '"+jsonString+"'");
                }
                this.limit = (Integer)newLimit;
            }
            
            comparator = AbstractFacetNavigationProvider.getComparator((String)sortby, (String)sortorder);
            
        } catch (JSONException e) {
            throw new IllegalArgumentException("Not a valid json format for '"+FacNavNodeType.HIPPOFACNAV_FACETNODENAMES+"' : '"+jsonString+"'", e);
        }
        
    }
}
