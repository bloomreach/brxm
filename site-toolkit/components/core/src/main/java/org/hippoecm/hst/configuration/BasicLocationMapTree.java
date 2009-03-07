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
package org.hippoecm.hst.configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicLocationMapTree implements LocationMapTree{

    private final static Logger log = LoggerFactory.getLogger(BasicLocationMapTree.class);
    
    private Map<String, LocationMapTreeItem> children = new HashMap<String, LocationMapTreeItem>();
    private String canonicalSiteContentPath;
   
    public BasicLocationMapTree(String canonicalSiteContentPath) {
        this.canonicalSiteContentPath = canonicalSiteContentPath;
    }
    
    public void add(List<String> pathFragment, HstSiteMapItem hstSiteMapItem){
        
        BasicLocationMapTreeItem child = (BasicLocationMapTreeItem) getTreeItem(pathFragment.get(0));
        if(child == null) {
            child = new BasicLocationMapTreeItem();
            this.children.put(pathFragment.get(0), child);
        }
        pathFragment.remove(0);
        child.add(pathFragment , hstSiteMapItem);
    }
    
    public LocationMapTreeItem getTreeItem(String name) {
        return children.get(name);
    }

    public LocationMapTreeItem find(String path) {
        path = PathUtils.normalizePath(path);
        List<String> names = new ArrayList<String>(Arrays.asList(path.split("/")));
        if(names.isEmpty() || getTreeItem(names.get(0)) == null) {
            return null;
        } 
        LocationMapTreeItem childMapTreeItem = getTreeItem(names.get(0));
        if(childMapTreeItem == null) {
            log.warn("Cannot find a matching LocationMapTreeItem for '{}'. Return null ", path);
            return null;
        }
        
        names.remove(0);
        
        LocationMapTreeItem bestLocationTreeMapItem = null;
        if(!childMapTreeItem.getHstSiteMapItems().isEmpty()) {
            bestLocationTreeMapItem = childMapTreeItem;
        }
        LocationMapTreeItem bestMatchingLocationTreeMapItem =  findDeepestLocationMapTreeItem(names, childMapTreeItem, bestLocationTreeMapItem);
        
        if(bestMatchingLocationTreeMapItem == null) {
            log.warn("Cannot find a matching LocationMapTreeItem for '{}'. Return null ", path);
        }
        return bestMatchingLocationTreeMapItem;
    }

    private LocationMapTreeItem findDeepestLocationMapTreeItem(List<String> names, LocationMapTreeItem crLocationMapTreeItem, LocationMapTreeItem bestLocationTreeMapItem){
        if(names.isEmpty() || crLocationMapTreeItem.getChild(names.get(0)) == null) {
            return bestLocationTreeMapItem;
        } else {
            LocationMapTreeItem childMapTreeItem = crLocationMapTreeItem.getChild(names.get(0));
            if(childMapTreeItem == null) {
                return bestLocationTreeMapItem;
            }
            if(!childMapTreeItem.getHstSiteMapItems().isEmpty()) {
                // the childMap has at least 1 SiteMapItem so this is for now the best matching locationMap, untill we
                // find a better one
                bestLocationTreeMapItem = childMapTreeItem;
            }
            names.remove(0);
            return findDeepestLocationMapTreeItem(names, childMapTreeItem,  bestLocationTreeMapItem);
        }      
    }
    
    public String getCanonicalSiteContentPath() {
      return this.canonicalSiteContentPath;
    }

}
