/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.pagecomposer.jaxrs.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.hippoecm.hst.configuration.internal.CanonicalInfo;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;

public class SiteMapItemRepresentation {

    String id;
    String name;
    Map<String, String> localParameters;
    Set<String> roles;
    String componentConfigurationId;
    boolean cacheable;
    boolean workspaceConfiguration;
    String relativeContentPath;
    String scheme;
    boolean wildCard;
    boolean any;
    boolean containsWildCard;
    boolean containsAny;
    boolean isExplicitElement;


    private List<SiteMapItemRepresentation> children = new ArrayList<>();

    public SiteMapItemRepresentation represent(HstSiteMapItem item)
            throws IllegalArgumentException {
        if (!(item instanceof CanonicalInfo)) {
            throw new IllegalArgumentException("Expected object of type CanonicalInfo");
        }
        name = item.getValue();
        id = ((CanonicalInfo) item).getCanonicalIdentifier();
        componentConfigurationId = item.getComponentConfigurationId();
        cacheable = item.isCacheable();
        workspaceConfiguration = ((CanonicalInfo) item).isWorkspaceConfiguration();
        relativeContentPath = item.getRelativeContentPath();
        scheme = item.getScheme();
        wildCard = item.isWildCard();
        any = item.isAny();
        containsWildCard = item.containsWildCard();
        containsAny = item.containsAny();
        isExplicitElement = !(wildCard || any || containsAny || containsAny);
        localParameters = item.getLocalParameters();
        roles = item.getRoles();
        Map<String, SiteMapItemRepresentation> orderedChildren = new TreeMap<>();
        for (HstSiteMapItem childItem : item.getChildren()) {
            SiteMapItemRepresentation child = new SiteMapItemRepresentation();
            child.represent(childItem);
            orderedChildren.put(child.getName(), child);
        }
        children.addAll(orderedChildren.values());
        return this;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getComponentConfigurationId() {
        return componentConfigurationId;
    }

    public void setComponentConfigurationId(final String componentConfigurationId) {
        this.componentConfigurationId = componentConfigurationId;
    }

    public boolean isCacheable() {
        return cacheable;
    }

    public void setCacheable(final boolean cacheable) {
        this.cacheable = cacheable;
    }

    public boolean isWorkspaceConfiguration() {
        return workspaceConfiguration;
    }

    public void setWorkspaceConfiguration(final boolean workspaceConfiguration) {
        this.workspaceConfiguration = workspaceConfiguration;
    }

    public String getRelativeContentPath() {
        return relativeContentPath;
    }

    public void setRelativeContentPath(final String relativeContentPath) {
        this.relativeContentPath = relativeContentPath;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(final String scheme) {
        this.scheme = scheme;
    }

    public boolean isWildCard() {
        return wildCard;
    }

    public void setWildCard(final boolean wildCard) {
        this.wildCard = wildCard;
    }

    public boolean isAny() {
        return any;
    }

    public void setAny(final boolean any) {
        this.any = any;
    }

    public boolean isContainsWildCard() {
        return containsWildCard;
    }

    public void setContainsWildCard(final boolean containsWildCard) {
        this.containsWildCard = containsWildCard;
    }

    public boolean isContainsAny() {
        return containsAny;
    }

    public void setContainsAny(final boolean containsAny) {
        this.containsAny = containsAny;
    }

    public boolean isExplicitElement() {
        return isExplicitElement;
    }

    public void setExplicitElement(final boolean explicitElement) {
        isExplicitElement = explicitElement;
    }

    public Map<String, String> getLocalParameters() {
        return localParameters;
    }

    public void setLocalParameters(final Map<String, String> localParameters) {
        this.localParameters = localParameters;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(final Set<String> roles) {
        this.roles = roles;
    }

    public List<SiteMapItemRepresentation> getChildren() {
        return children;
    }

    public void setChildren(final List<SiteMapItemRepresentation> children) {
        this.children = children;
    }
}
