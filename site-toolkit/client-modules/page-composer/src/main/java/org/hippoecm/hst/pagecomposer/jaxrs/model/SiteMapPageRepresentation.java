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

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.components.HstComponentsConfiguration;

public class SiteMapPageRepresentation {

    private String id;
    // parentId if the backing sitemap item contains a parent item. If this is a root item (directly below
    // siteMap, then the parentId is null)
    private String parentId;
    private String name;
    private String pageTitle;
    private String pathInfo;
    private String componentConfigurationId;
    private boolean workspaceConfiguration;
    private boolean inherited;
    private String relativeContentPath;

    // whether the page has at least one container item in its page definition.
    // Note that although the backing {@link HstComponentConfiguration} might have containers,
    // this does not mean the component has it in its page definition: The page definition
    // is the canonical configuration, without inheritance (and thus the container might be present in inherited config only)
    private boolean hasContainerItemInPageDefinition;

    public SiteMapPageRepresentation represent(final SiteMapItemRepresentation siteMapItemRepresentation,
                                               final HstComponentsConfiguration hstComponentsConfiguration, final String parentId,
                                               final String parentPathInfo,
                                               final String homePagePathInfo) throws IllegalArgumentException {

        id = siteMapItemRepresentation.getId();
        this.parentId = parentId;
        name = siteMapItemRepresentation.getName();
        pageTitle = siteMapItemRepresentation.getPageTitle();
        if (StringUtils.isEmpty(parentPathInfo)) {
            pathInfo = name;
        } else {
            pathInfo = parentPathInfo + "/" + name;
        }
        if (pathInfo.equals(homePagePathInfo)) {
            pathInfo = "/";
        }
        componentConfigurationId = siteMapItemRepresentation.getComponentConfigurationId();

        hasContainerItemInPageDefinition = hasContainerItemInPageDefinition(
                hstComponentsConfiguration.getComponentConfiguration(componentConfigurationId));

        workspaceConfiguration = siteMapItemRepresentation.isWorkspaceConfiguration();
        inherited = siteMapItemRepresentation.isInherited();
        relativeContentPath = siteMapItemRepresentation.getRelativeContentPath();
        return this;
    }

    private boolean hasContainerItemInPageDefinition(final HstComponentConfiguration root) {
        if (root == null) {
            return false;
        }
        return hasContainerItemInPageDefinition(root, root.getCanonicalStoredLocation());
    }

    private boolean hasContainerItemInPageDefinition(final HstComponentConfiguration config, final String pageDefinitionRootPath) {
        if (HstComponentConfiguration.Type.CONTAINER_ITEM_COMPONENT.equals(config.getComponentType())
                && config.getCanonicalStoredLocation().startsWith(pageDefinitionRootPath)) {
            return true;
        }
        for (HstComponentConfiguration child : config.getChildren().values()) {
            if (hasContainerItemInPageDefinition(child, pageDefinitionRootPath)) {
                return true;
            }
        }
        return false;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(final String parentId) {
        this.parentId = parentId;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getPageTitle() {
        return pageTitle;
    }

    public void setPageTitle(final String pageTitle) {
        this.pageTitle = pageTitle;
    }

    public String getPathInfo() {
        return pathInfo;
    }

    public void setPathInfo(final String pathInfo) {
        this.pathInfo = pathInfo;
    }

    public String getComponentConfigurationId() {
        return componentConfigurationId;
    }

    public void setComponentConfigurationId(final String componentConfigurationId) {
        this.componentConfigurationId = componentConfigurationId;
    }

    public boolean isWorkspaceConfiguration() {
        return workspaceConfiguration;
    }

    public void setWorkspaceConfiguration(final boolean workspaceConfiguration) {
        this.workspaceConfiguration = workspaceConfiguration;
    }

    public boolean isInherited() {
        return inherited;
    }

    public void setInherited(final boolean inherited) {
        this.inherited = inherited;
    }

    public String getRelativeContentPath() {
        return relativeContentPath;
    }

    public void setRelativeContentPath(final String relativeContentPath) {
        this.relativeContentPath = relativeContentPath;
    }

    public boolean isHasContainerItemInPageDefinition() {
        return hasContainerItemInPageDefinition;
    }

    public void setHasContainerItemInPageDefinition(final boolean hasContainerItemInPageDefinition) {
        this.hasContainerItemInPageDefinition = hasContainerItemInPageDefinition;
    }
}
