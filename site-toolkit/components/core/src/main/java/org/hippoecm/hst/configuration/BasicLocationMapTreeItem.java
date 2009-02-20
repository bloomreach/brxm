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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicLocationMapTreeItem implements LocationMapTreeItem{

    private final static Logger log = LoggerFactory.getLogger(LocationMapTreeItem.class);
    
    private List<HstSiteMapItem> hstSiteMapItems = new ArrayList<HstSiteMapItem>();
    
    private Map<String, LocationMapTreeItem> children = new HashMap<String, LocationMapTreeItem>();
    
    
    public void add(List<String> pathFragment, HstSiteMapItem hstSiteMapItem){
        if(pathFragment.isEmpty()) {
            // when the pathFragments are empty, the entire relative content path is processed.
            if(!hstSiteMapItems.isEmpty()) {
                log.warn("Adding a second HstSiteMapItem '{}' to the same LocationMapTreeItem. This potentially returns the wrong HstSiteMapItem because" +
                        "we cannot differentiate between the 'relative content paths'.", hstSiteMapItem.getPath());
            } 
            this.hstSiteMapItems.add(hstSiteMapItem);
            return;
        }
        BasicLocationMapTreeItem child = (BasicLocationMapTreeItem) getChild(pathFragment.get(0));
        if(child == null) {
            child = new BasicLocationMapTreeItem();
            this.children.put(pathFragment.get(0), child);
        }
        pathFragment.remove(0);
        child.add(pathFragment , hstSiteMapItem);
    }
    
    
    public List<HstSiteMapItem> getHstSiteMapItems() {
        return hstSiteMapItems;
    }

    public LocationMapTreeItem getChild(String name) {
        return this.children.get(name);
    }

}
