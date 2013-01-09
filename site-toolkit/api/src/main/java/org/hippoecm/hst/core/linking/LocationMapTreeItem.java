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

import java.util.List;

import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;

/**
 * Expert: A <code>LocationMapTreeItem</code> is an item in the tree of the containing <code>LocationMapTree</code>.  
 * It contains references to the <code>HstSiteMapItem</code>'s that belong to this <code>LocationMapTreeItem</code>. A
 * <code>HstSiteMapItem</code> must not belong to multiple <code>LocationMapTreeItem<code>'s. 
 * 
 */
public interface LocationMapTreeItem {

    /**
     * @return List of <code>HstSiteMapItem</code>'s belonging to this LocationMapTreeItem. When no HstSiteMapItem belong to this item, an 
     * empty list is returned
     */
    List<HstSiteMapItem> getHstSiteMapItems();
    
    /**
     * 
     * @param name the name of the child <code>LocationMapTreeItem</code>
     * @return The child <code>LocationMapTreeItem</code> with this <code>name</code> or <code>null</code> if there 
     * exists no child with this name
     */
    LocationMapTreeItem getChild(String name);
    
    /**
     * 
     * @return the parent <code>LocationMapTreeItem</code> of this item or <code>null</code> when it is a root <code>LocationMapTreeItem</code>
     */
    LocationMapTreeItem getParentItem();
    
}
