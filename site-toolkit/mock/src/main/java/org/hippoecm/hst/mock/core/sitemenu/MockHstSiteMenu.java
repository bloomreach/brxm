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
package org.hippoecm.hst.mock.core.sitemenu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.hippoecm.hst.core.sitemenu.EditableMenu;
import org.hippoecm.hst.core.sitemenu.HstSiteMenu;
import org.hippoecm.hst.core.sitemenu.HstSiteMenuItem;
import org.hippoecm.hst.core.sitemenu.HstSiteMenus;

/**
 * Mock implementation of {@link org.hippoecm.hst.core.sitemenu.HstSiteMenu}.
 *
 */
public class MockHstSiteMenu implements HstSiteMenu {

    private List<HstSiteMenuItem> siteMenuItems = new ArrayList<HstSiteMenuItem>();
    private HstSiteMenus hstSiteMenus;
    private HstSiteMenuItem deepestExpandedItem;
    private HstSiteMenuItem selectSiteMenuItem;
    private EditableMenu editableMenu;
    private String name;
    private boolean expanded;
    private String canonicalIdentifier = UUID.randomUUID().toString();
    private boolean inherited;
    
    public List<HstSiteMenuItem> getSiteMenuItems() {
        return Collections.unmodifiableList(siteMenuItems);
    }
    
    public void setSiteMenuItems(List<HstSiteMenuItem> siteMenuItems) {
        this.siteMenuItems = siteMenuItems;
    }
    
    public void addSiteMenuItem(HstSiteMenuItem siteMenuItem) {
        siteMenuItems.add(siteMenuItem);
    }
    
    public void clearSiteMenuItems() {
        siteMenuItems.clear();
    }
    
    public HstSiteMenus getHstSiteMenus() {
        return hstSiteMenus;
    }
    
    public void setHstSiteMenus(HstSiteMenus hstSiteMenus) {
        this.hstSiteMenus = hstSiteMenus;
    }
    
    public HstSiteMenuItem getDeepestExpandedItem() {
        return deepestExpandedItem;
    }
    
    public void setDeepestExpandedItem(HstSiteMenuItem deepestExpandedItem) {
        this.deepestExpandedItem = deepestExpandedItem;
    }
    
    public HstSiteMenuItem getSelectSiteMenuItem() {
        return selectSiteMenuItem;
    }
    
    public void setSelectSiteMenuItem(HstSiteMenuItem selectSiteMenuItem) {
        this.selectSiteMenuItem = selectSiteMenuItem;
    }
    
    public EditableMenu getEditableMenu() {
        return editableMenu;
    }
    
    public void setEditableMenu(EditableMenu editableMenu) {
        this.editableMenu = editableMenu;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public boolean isExpanded() {
        return expanded;
    }
    
    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    @Override
    public String getCanonicalIdentifier() {
        return canonicalIdentifier;
    }

    @Override
    public boolean isInherited() {
        return inherited;
    }
}
