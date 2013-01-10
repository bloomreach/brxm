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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class containing all the {@link FacetNodeView}'s. Node that this object is set on every {@link AbstractFacetNavigationProvider.FacetNavigationNodeId} object.
 * 
 * As there can be <b>MANY</b> AbstractFacetNavigationProvider.FacetNavigationNodeId objects in memory, we have optimized this class to only clone and modify those {@link FacetNodeView}'s
 * that actually need to be changed. This to avoid using a lot of memory
 */
public class FacetNodeViews implements Iterable<FacetNodeView>{
    
    private final Logger log = LoggerFactory.getLogger(FacetNodeViews.class);
    
    
    List<FacetNodeView> facetNodeViews = new ArrayList<FacetNodeView>();
    
    /*
     * if the configuration contains information that runtime changes this FacetNodeViews, then this attribute
     * will be true. For example when 'remove' or 'after' is configured for a FacetNodeView, this will be the case
     */
    boolean modifiable = false;
    

    private FacetNodeViews() {
        // only class internal constructor
    }
    
    public FacetNodeViews(String[] facets, String[] facetNodeNames) throws IllegalArgumentException {
        if(facets == null || facets.length == 0) {
            throw new IllegalArgumentException("No facets configured in '"+FacNavNodeType.HIPPOFACNAV_FACETS+"'. Return");
            
        }
        if(facetNodeNames != null && facets.length != facetNodeNames.length) {
            throw new IllegalArgumentException("When using multivalued property '"+FacNavNodeType.HIPPOFACNAV_FACETNODENAMES+"', it must have equal number of values" + "as for property '"+FacNavNodeType.HIPPOFACNAV_FACETS+"'" );
        }
        
        int i = -1;
        for(String facet : facets) {
            i++;
            FacetNodeView facetNodeView;
            if(facetNodeNames != null) {
                facetNodeView = new FacetNodeView(facet, facetNodeNames[i]);
            } else {
                facetNodeView = new FacetNodeView(facet);
            }
            facetNodeViews.add(facetNodeView);
            if(facetNodeView.afterFacet != null || facetNodeView.hideFacet != null ) {
                // we have a modifiable FacetNodeViews object. For efficiency, we set this property
                modifiable = true;
            }
        }
    }


    public Iterator<FacetNodeView> iterator() {
        return facetNodeViews.iterator();
    }


    /*
     * when the currentFacetNodeView modifies this FacetNodeViews, we return a modified version. Otherwise, we return just this instance 
     */
    public FacetNodeViews getFacetNodeViews(FacetNodeView currentFacetNodeView) {
        if(!modifiable) {
            return this;
        }
        List<FacetNodeView> newFacetNodeViewsList = new ArrayList<FacetNodeView>();
        boolean modified = false;
        
        // check whether facet node views have to become visible after the currentFacetNodeView
        for(FacetNodeView fnv : facetNodeViews) {
            if(!fnv.disabled && !fnv.visible && fnv.afterFacet != null && fnv.afterFacet.equals(currentFacetNodeView.displayName)){
                // from now on it is visible. First clone the entry before modifying it of course
                FacetNodeView clone = (FacetNodeView)fnv.clone();
                clone.visible = true;
                newFacetNodeViewsList.add(clone);
                modified = true;
            } else {
                newFacetNodeViewsList.add(fnv);
            }
        }
        
        // check whether some facet node views need to be disable due to the currentFacetNodeView
        if(currentFacetNodeView.hideFacet != null) {
            // let's set the facet to disable that is hided
            Map<FacetNodeView, FacetNodeView> replaceMap = new HashMap<FacetNodeView, FacetNodeView>();
            for(FacetNodeView fnv : newFacetNodeViewsList) {
                if(currentFacetNodeView.hideFacet.contains(fnv.displayName)) {
                 // first clone the object
                    FacetNodeView clone = (FacetNodeView)fnv.clone();
                    clone.disabled = true;
                    replaceMap.put(fnv, clone);
                    modified = true;
                }
            }
            if(!replaceMap.isEmpty()) {
                for(Map.Entry<FacetNodeView, FacetNodeView> entry : replaceMap.entrySet()) {
                    int replaceIndex = newFacetNodeViewsList.indexOf(entry.getKey());
                    if(replaceIndex == -1) {
                        // not possible.
                        log.error("the index cannot be -1");
                    } else {
                        newFacetNodeViewsList.set(replaceIndex, entry.getValue());
                    }
                }
            }
        }
        
        if(modified) {
            FacetNodeViews newFacetNodeViews = new FacetNodeViews();
            newFacetNodeViews.facetNodeViews = newFacetNodeViewsList;
            newFacetNodeViews.modifiable = this.modifiable;
            return newFacetNodeViews;
        } else {
            return this;
        }
    }

}
