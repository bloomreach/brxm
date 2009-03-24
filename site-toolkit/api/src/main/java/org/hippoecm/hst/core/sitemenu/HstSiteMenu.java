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
package org.hippoecm.hst.core.sitemenu;

import java.io.Serializable;
import java.util.List;

/**
 * The interface for a SiteMenu implementation, containing possibly a tree of {@link HstSiteMenuItem}'s
 *
 */
public interface HstSiteMenu extends Serializable{
    /**
     * Returns the name of this SiteMenu. For example, you could have a "topmenu", "leftmenu" and "footermenu" on your site/portal,
     * where these names might be appropriate 
     * @return the name of this SiteMenu
     */
    String getName();
    
    /**
     * Based on the request, the implementation should be able to indicate whether this HstSiteMenu has a selected HstSiteMenuItem
     * 
     * @return <code>true</code> when a HstSiteMenuItem in this HstSiteMenu container is selected
     */
    boolean isSelected();
    
    /**
     * Returns the currently selected sitemenu items. It makes sense to have only a single sitemenu item selected, but as 
     * multiple sitemenu items <i>could</i> point to the same selected sitemap item, it can happen that multiple sitemenu items
     * are selected. It is up to the implementation how to handle this
     * @return the currently selected sitemenu items. 
     */
    List<HstSiteMenuItem> getSelectedSiteMenuItems();
    
    /**
     * @return returns all direct child {@link HstSiteMenuItem}'s of this SiteMenu
     */
    List<HstSiteMenuItem> getSiteMenuItems();
    
    /**
     * 
     * @return the <code>HstSiteMenus</code> container for this HstSiteMenu
     */
    HstSiteMenus getHstSiteMenus();
}
