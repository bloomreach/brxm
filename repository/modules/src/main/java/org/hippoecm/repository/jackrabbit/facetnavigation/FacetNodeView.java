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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.json.JSONException;
import net.sf.json.JSONObject;

import org.hippoecm.repository.jackrabbit.facetnavigation.AbstractFacetNavigationProvider.FacetNavigationEntry;
import org.hippoecm.repository.jackrabbit.facetnavigation.AbstractFacetNavigationProvider.FacetNavigationEntryComparator;

public class FacetNodeView implements Cloneable {

    
    String facet;
    String facetNodeNameConfiguration;
    
    String displayName;
    
    // the comparator used for sorting the facet values for this facet
    FacetNavigationEntryComparator<FacetNavigationEntry> comparator;
    
    // default, there is no limit
    int limit = Integer.MAX_VALUE;
    
    // default, a facet is visible
    boolean visible = true;

    // if this facet is only visible after some other facet is chosen, this facet is held by 'afterFacet'
    String afterFacet = null;
    
    // if this facet hides another facet or itself, it is configured in this attribute
    Set<String> hideFacet = null;
    
    // if this facet is hidden, this boolean is true
    boolean disabled = false;
    
    String sortorder;
    String sortby;
    
    
    
    public FacetNodeView(String facet) throws IllegalArgumentException {
        this( facet, null );
    }
    
    public FacetNodeView(String facet, String facetNodeNameConfiguration) throws IllegalArgumentException {
       this.facet = facet;
       this.facetNodeNameConfiguration = facetNodeNameConfiguration;
        
       if(facetNodeNameConfiguration == null) {
           this.displayName = facet;
       } else {
           if(facetNodeNameConfiguration.indexOf("$") == -1) {
               this.displayName = facetNodeNameConfiguration;
               return;
           }
           
           this.displayName = facetNodeNameConfiguration.substring(0, facetNodeNameConfiguration.indexOf("$"));
           String jsonString = facetNodeNameConfiguration.substring(facetNodeNameConfiguration.indexOf("$")+1);
           
           if("".equals(jsonString)) {
               throw new IllegalArgumentException("Not allowed to end the '"+FacNavNodeType.HIPPOFACNAV_FACETNODENAMES+"' with a '$'");
           }
           
           try {
               JSONObject jsonObject = JSONObject.fromObject( jsonString );  
               Object sortbyObj = jsonObject.get("sortby");
               if(sortbyObj != null && !(sortbyObj instanceof String)) {
                   throw new IllegalArgumentException("'sortby' most be of type String if configured: not a valid json format for '"+FacNavNodeType.HIPPOFACNAV_FACETNODENAMES+"' : '"+jsonString+"'");
               }
               sortby = (String)sortbyObj;
               
               Object sortorderObj = jsonObject.get("sortorder");
               if(sortorderObj!= null && !(sortorderObj instanceof String)) {
                   throw new IllegalArgumentException("'sortorder' most be of type String if configured: not a valid json format for '"+FacNavNodeType.HIPPOFACNAV_FACETNODENAMES+"' : '"+jsonString+"'");
               }
               sortorder = (String)sortorderObj;
               
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
               
               Object afterObj = jsonObject.get("after");
               if(afterObj != null && afterObj instanceof String) {
                   this.visible = false;
                   this.afterFacet = (String)afterObj;
               }
               
               Object hideObj = jsonObject.get("hide");
               if(hideObj != null) {
                   this.hideFacet = new HashSet<String>();
                   if(hideObj instanceof String) {
                       this.hideFacet.add((String)hideObj);
                   } else if(hideObj instanceof List) {
                       for(Object hide : (List)hideObj) {
                           if(hide instanceof String) {
                               this.hideFacet.add((String)hide);
                           } else {
                               throw new IllegalArgumentException("Not a valid json format for '"+FacNavNodeType.HIPPOFACNAV_FACETNODENAMES+"' : '"+jsonString+"'. Only String and a List of Strings is supported");
                           }
                       }
                   } else {
                       throw new IllegalArgumentException("Not a valid json format for '"+FacNavNodeType.HIPPOFACNAV_FACETNODENAMES+"' : '"+jsonString+"'. Only String and a List of Strings is supported");
                   }
               }
               comparator = AbstractFacetNavigationProvider.getComparator(sortby, sortorder);
               
           } catch (JSONException e) {
               throw new IllegalArgumentException("Not a valid json format for '"+FacNavNodeType.HIPPOFACNAV_FACETNODENAMES+"' : '"+jsonString+"'", e);
           }
       }
    }

    @Override
    protected Object clone() {
        try {
            FacetNodeView clone = (FacetNodeView) super.clone();
            // note that the clone shares the same comparator instance, but this is never an issue as many instances share
            // the same comparator instances (they are static final's)
            return clone;
        } catch (CloneNotSupportedException e) {
            // this cannot happen, since we are Cloneable
            throw new InternalError();
        }

    }

}
