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

import org.hippoecm.hst.core.linking.HstLink;

/**
 *
 */
public interface HstSiteMenuItem extends Serializable{
    /**
     * 
     * @return the name of this SiteMenuItem
     */
    String getName();
    
    /**
     * 
     * @return all direct child SiteMenuItem's of this item
     */
    List<HstSiteMenuItem> getChildMenuItems();
    
    /**
     * Returns the currently selected sitemenu items. It makes sense to have only a single sitemenu item selected, but as 
     * multiple sitemenu items <i>could</i> point to the same selected sitemap item, it can happen that multiple sitemenu items
     * are selected. It is up to the implementation how to handle this
     * @return the currently selected sitemenu items. 
     */
    List<HstSiteMenuItem> getSelectedSiteMenuItems();
    
    /**
     * 
     * @return parent <code>HstSiteMenuItem</code> or <code>null</code> if it is a root item 
     */
    HstSiteMenuItem getParentItem();
    
    /**
     * 
     * @return the container <code>HstSiteMenu</code> of this <code>HstSiteMenuItem</code>
     */
    HstSiteMenu getHstSiteMenu();
    
    /**
     * 
     * @return a HstLink that contains a link for this SiteMenuItem
     */
    HstLink getHstLink();
    
    /**
     * @return <code>true</code> is the SiteMenuItem is selected
     */
    boolean isSelected();
}
