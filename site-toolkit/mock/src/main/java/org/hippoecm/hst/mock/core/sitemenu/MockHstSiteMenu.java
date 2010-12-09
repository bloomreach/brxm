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
package org.hippoecm.hst.mock.core.sitemenu;

import org.hippoecm.hst.core.sitemenu.EditableMenu;
import org.hippoecm.hst.core.sitemenu.HstSiteMenu;
import org.hippoecm.hst.core.sitemenu.HstSiteMenuItem;
import org.hippoecm.hst.core.sitemenu.HstSiteMenus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mock implementation of {@link org.hippoecm.hst.core.sitemenu.HstSiteMenu}.
 *
 */
public class MockHstSiteMenu implements HstSiteMenu {

    private List<HstSiteMenuItem> siteMenuItems = new ArrayList<HstSiteMenuItem>();

    public HstSiteMenuItem getSelectSiteMenuItem() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public List<HstSiteMenuItem> getSiteMenuItems() {
        return Collections.unmodifiableList(siteMenuItems);
    }

    public HstSiteMenus getHstSiteMenus() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public HstSiteMenuItem getDeepestExpandedItem() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public EditableMenu getEditableMenu() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public String getName() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public boolean isExpanded() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public MockHstSiteMenu addSiteMenuItem(String name, boolean isExpanded) {
        addSiteMenuItem(new MockHstSiteMenuItem(name, isExpanded));
        return this;
    }

    public MockHstSiteMenu addSiteMenuItem(String name) {
        return addSiteMenuItem(name, false);
    }

    public void addSiteMenuItem(HstSiteMenuItem siteMenuItem) {
        this.siteMenuItems.add(siteMenuItem);
    }
}
