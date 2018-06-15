/*
 *  Copyright 2009-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.linking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.internal.CollectionOptimizer;
import org.hippoecm.hst.core.internal.StringPool;
import org.hippoecm.hst.core.linking.LocationMapTreeItem;

public class LocationMapTreeItemImpl implements LocationMapTreeItem {

    private List<HstSiteMapItem> hstSiteMapItems = new ArrayList<>();
    
    private Map<String, LocationMapTreeItem> children = new HashMap<>();
    
    private LocationMapTreeItem parentItem;
    
    private boolean isWildCard;
    private boolean isAny;
    
    void addSiteMapItem(final String[] pathFragments, final HstSiteMapItem hstSiteMapItem, final int position) {
        if(pathFragments.length == position) {
            // when the pathFragments are empty, the entire path is processed.
            hstSiteMapItems.add(hstSiteMapItem);
            return;
        }
        LocationMapTreeItemImpl child = (LocationMapTreeItemImpl) getChild(pathFragments[position]);
        if(child == null) {
            child = new LocationMapTreeItemImpl();
            children.put(StringPool.get(pathFragments[position]), child);
            child.setParentItem(this);
            if(HstNodeTypes.WILDCARD.equals(pathFragments[position])){
                child.isWildCard = true;
            } else if (HstNodeTypes.ANY.equals(pathFragments[position])){
                child.isAny =true;
            }
        }
        child.addSiteMapItem(pathFragments , hstSiteMapItem, position + 1);
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
        return isAny;
    }


    public boolean isWildCard() {
        return isWildCard;
    }

    void optimize() {
        children = CollectionOptimizer.optimizeHashMap(children);
        hstSiteMapItems = CollectionOptimizer.optimizeArrayList(hstSiteMapItems);
        for (LocationMapTreeItem child : children.values()) {
            ((LocationMapTreeItemImpl)child).optimize();
        }
    }


}
