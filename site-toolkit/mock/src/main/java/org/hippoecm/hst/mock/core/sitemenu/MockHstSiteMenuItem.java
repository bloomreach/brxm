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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.sitemenu.HstSiteMenu;
import org.hippoecm.hst.core.sitemenu.HstSiteMenuItem;

/**
 * Mock implementation of {@link org.hippoecm.hst.core.sitemenu.HstSiteMenuItem}.
 */
public class MockHstSiteMenuItem implements HstSiteMenuItem {

    private final List<HstSiteMenuItem> siteMenuItems = new ArrayList<HstSiteMenuItem>();

    private String name;
    private boolean isExpanded;

    public MockHstSiteMenuItem(String name) {
        this(name, false);
    }

    public MockHstSiteMenuItem(String name, boolean isExpanded) {
        this.name = name;
        this.isExpanded = isExpanded;
    }

    public List<HstSiteMenuItem> getChildMenuItems() {
        return siteMenuItems;
    }

    public HstSiteMenuItem getParentItem() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public HstSiteMenu getHstSiteMenu() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public String getName() {
        return name;
    }

    public HstLink getHstLink() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public String getExternalLink() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public ResolvedSiteMapItem resolveToSiteMapItem(HstRequest request) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public Map<String, Object> getProperties() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public boolean isRepositoryBased() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public int getDepth() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public boolean isSelected() {
        throw new UnsupportedOperationException("Not implemented");
    }

    // Methods supporting org.hippoecm.hst.mock configuration

    public MockHstSiteMenuItem addSiteMenuItem(String name) {
        return addSiteMenuItem(new MockHstSiteMenuItem(name, false));
    }

    public MockHstSiteMenuItem addSiteMenuItem(HstSiteMenuItem siteMenuItem) {
        this.siteMenuItems.add(siteMenuItem);
        return this;
    }

    public String getLocalParameter(String name) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public Map<String, String> getLocalParameters() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public String getParameter(String name) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public Map<String, String> getParameters() {
        throw new UnsupportedOperationException("Not implemented");
    }
}
