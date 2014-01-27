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

import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuItemConfiguration;

public class SiteMenuItemRepresentation {

    String id;
    String name;
    // when there is a parent SiteMenuItemRepresentation, the parentId contains it id. Otherwise
    // it is null
    String parentId;
    String externalLink;
    String siteMapItemPath;
    boolean repositoryBased;
    Map<String, String> parameters;
    Map<String, String> localParameters;
    Set<String> roles;

    private List<SiteMenuItemRepresentation> children = new ArrayList<>();

    public SiteMenuItemRepresentation represent(HstSiteMenuItemConfiguration rootItem) {
        return represent(rootItem, null);
    }

    public SiteMenuItemRepresentation represent(HstSiteMenuItemConfiguration item, HstSiteMenuItemConfiguration parent) {
        name = item.getName();
        id = item.getCanonicalIdentifier();
        if (parent != null) {
            parentId = parent.getCanonicalIdentifier();
        }
        externalLink = item.getExternalLink();
        siteMapItemPath = item.getSiteMapItemPath();
        repositoryBased = item.isRepositoryBased();
        parameters = item.getParameters();
        localParameters = item.getLocalParameters();
        roles = item.getRoles();
        for (HstSiteMenuItemConfiguration childItem : item.getChildItemConfigurations()) {
            SiteMenuItemRepresentation child = new SiteMenuItemRepresentation();
            child.represent(childItem, item);
            children.add(child);
        }
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

    public String getParentId() {
        return parentId;
    }

    public void setParentId(final String parentId) {
        this.parentId = parentId;
    }

    public String getExternalLink() {
        return externalLink;
    }

    public void setExternalLink(final String externalLink) {
        this.externalLink = externalLink;
    }

    public String getSiteMapItemPath() {
        return siteMapItemPath;
    }

    public void setSiteMapItemPath(final String siteMapItemPath) {
        this.siteMapItemPath = siteMapItemPath;
    }

    public boolean isRepositoryBased() {
        return repositoryBased;
    }

    public void setRepositoryBased(final boolean repositoryBased) {
        this.repositoryBased = repositoryBased;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(final Map<String, String> parameters) {
        this.parameters = parameters;
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

    public List<SiteMenuItemRepresentation> getChildren() {
        return children;
    }

    public void setChildren(final List<SiteMenuItemRepresentation> children) {
        this.children = children;
    }
}
