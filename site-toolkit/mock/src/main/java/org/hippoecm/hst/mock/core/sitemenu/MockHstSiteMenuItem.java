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
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    private HstSiteMenuItem parentItem;
    private HstSiteMenu hstSiteMenu;
    private HstLink hstLink;
    private String externalLink;
    private boolean repositoryBased;
    private int depth;
    private boolean selected;
    private Map<String, String> parameters = new LinkedHashMap<String, String>();
    private Map<String, String> localParameters = new LinkedHashMap<String, String>();
    private Map<String, Object> properties = new HashMap<String, Object>();
    
    public MockHstSiteMenuItem() {
        this(null);
    }
    
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

    public String getName() {
        return name;
    }

    public ResolvedSiteMapItem resolveToSiteMapItem(HstRequest request) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public Map<String, Object> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    public void putAllProperties(Map<String, Object> properties) {
        this.properties.clear();
        this.properties.putAll(properties);
    }
    
    public HstSiteMenuItem addSiteMenuItem(HstSiteMenuItem siteMenuItem) {
        this.siteMenuItems.add(siteMenuItem);
        return this;
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public void setExpanded(boolean isExpanded) {
        this.isExpanded = isExpanded;
    }

    public HstSiteMenuItem getParentItem() {
        return parentItem;
    }

    public void setParentItem(HstSiteMenuItem parentItem) {
        this.parentItem = parentItem;
    }

    public HstSiteMenu getHstSiteMenu() {
        return hstSiteMenu;
    }

    public void setHstSiteMenu(HstSiteMenu hstSiteMenu) {
        this.hstSiteMenu = hstSiteMenu;
    }

    public HstLink getHstLink() {
        return hstLink;
    }

    public void setHstLink(HstLink hstLink) {
        this.hstLink = hstLink;
    }

    public String getExternalLink() {
        return externalLink;
    }

    public void setExternalLink(String externalLink) {
        this.externalLink = externalLink;
    }

    public boolean isRepositoryBased() {
        return repositoryBased;
    }

    public void setRepositoryBased(boolean repositoryBased) {
        this.repositoryBased = repositoryBased;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public List<HstSiteMenuItem> getSiteMenuItems() {
        return siteMenuItems;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParameter(String name) {
        return parameters.get(name);
    }

    public Map<String, String> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    public void addParameter(String name, String value) {
        parameters.put(name, value);
    }
    
    public void removeParameter(String name) {
        parameters.remove(name);
    }
    
    public String getLocalParameter(String name) {
        return localParameters.get(name);
    }

    public Map<String, String> getLocalParameters() {
        return Collections.unmodifiableMap(localParameters);
    }

    public void addLocalParameter(String name, String value) {
        localParameters.put(name, value);
    }
    
    public void removeLocalParameter(String name) {
        localParameters.remove(name);
    }

}
