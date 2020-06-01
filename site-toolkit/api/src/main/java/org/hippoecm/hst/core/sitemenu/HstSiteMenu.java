/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.List;

/**
 * The interface for a SiteMenu implementation, containing possibly a tree of {@link HstSiteMenuItem}'s
 *
 */
public interface HstSiteMenu extends CommonMenu {
    /**
     * 
     * @return the currently selected <code>HstSiteMenuItem</code> or <code>null</code> if none is selected
     */
    HstSiteMenuItem getSelectSiteMenuItem();
    
    /**
     * @return returns all direct child {@link HstSiteMenuItem}'s of this SiteMenu
     */
    List<HstSiteMenuItem> getSiteMenuItems();
    
    /**
     * 
     * @return the <code>HstSiteMenus</code> container for this HstSiteMenu
     */
    HstSiteMenus getHstSiteMenus();
    
    /**
     * This utility method is valuable for creating repository based navigations, as you can easily get the deepest selected item, which might
     * in turn need repository based menu build below it
     * @return the <code>HstSiteMenuItem</code> that is the last one in the tree of expanded items, or <code>null</code> if none is expanded
     */
    HstSiteMenuItem getDeepestExpandedItem();
    
    /**
     * Returns an <code>{@link EditableMenu}</code> instance from this HstSiteMenu. Note that changing the <code>{@link EditableMenu}</code> using the setters and adders (like adding a
     * {@link EditableMenuItem}) will not being reflected in this HstSiteMenu instance. 
     * @return an <code>EditableMenu</code> instance of this HstSiteMenu. 
     */
    EditableMenu getEditableMenu();

}
