/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.linking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.internal.CollectionOptimizer;
import org.hippoecm.hst.core.internal.StringPool;

public class LocationMapTreeItemImpl implements LocationMapTreeItem {

    private List<HstSiteMapItem> hstSiteMapItems = new ArrayList<HstSiteMapItem>();
    
    private Map<String, LocationMapTreeItem> children = new HashMap<String, LocationMapTreeItem>();
    
    private LocationMapTreeItem parentItem;
    
    private boolean isWildCard;
    private boolean isAny;
    
    void addSiteMapItem(List<String> pathFragment, HstSiteMapItem hstSiteMapItem) {
        if(pathFragment.isEmpty()) {
            // when the pathFragments are empty, the entire relative content path is processed.
            this.hstSiteMapItems.add(hstSiteMapItem);
            return;
        }
        LocationMapTreeItemImpl child = (LocationMapTreeItemImpl) getChild(pathFragment.get(0));
        if(child == null) {
            child = new LocationMapTreeItemImpl();
            this.children.put(StringPool.get(pathFragment.get(0)), child);
            child.setParentItem(this);
            if(HstNodeTypes.WILDCARD.equals(pathFragment.get(0))){
                child.setIsWildCard(true);
            } else if (HstNodeTypes.ANY.equals(pathFragment.get(0))){
                child.setIsAny(true);
            }
        }
        pathFragment.remove(0);
        child.addSiteMapItem(pathFragment , hstSiteMapItem);
    }
    
    
    public List<HstSiteMapItem> getHstSiteMapItems() {
        return hstSiteMapItems;
    }

    public LocationMapTreeItem getChild(String name) {
        return this.children.get(name);
    }

    public LocationMapTreeItem getParentItem() {
        return this.parentItem;
    }
    
    void setParentItem(LocationMapTreeItem parentItem){
        this.parentItem = parentItem;
    }
    
    public boolean isAny() {
        return this.isAny;
    }


    public boolean isWildCard() {
        return this.isWildCard;
    }
    
    void setIsAny(boolean isAny) {
        this.isAny = isAny;
    }

    void setIsWildCard(boolean isWildCard) {
        this.isWildCard = isWildCard;
    }

    void optimize() {
        children = CollectionOptimizer.optimizeHashMap(children);
        hstSiteMapItems = CollectionOptimizer.optimizeArrayList(hstSiteMapItems);
        for (LocationMapTreeItem child : children.values()) {
            ((LocationMapTreeItemImpl)child).optimize();
        }
    }


}
