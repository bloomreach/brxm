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

public class SiteMapPageRepresentation {

    private String id;
    // parentId if the backing sitemap item contains a parent item. If this is a root item (directly below
    // siteMap, then the parentId is null)
    private String parentId;
    private String name;
    private String pageTitle;
    private String pathInfo;
    // mountPath + sitemap item path = renderPathInfo
    private String renderPathInfo;
    private String componentConfigurationId;
    private boolean workspaceConfiguration;
    private boolean inherited;
    private String relativeContentPath;


    public SiteMapPageRepresentation represent(final SiteMapItemRepresentation siteMapItemRepresentation,
                                               final String parentId, final String mountPath) throws IllegalArgumentException {

        id = siteMapItemRepresentation.getId();
        this.parentId = parentId;
        name = siteMapItemRepresentation.getName();
        pageTitle = siteMapItemRepresentation.getPageTitle();
        pathInfo = siteMapItemRepresentation.getPathInfo();
        if (siteMapItemRepresentation.getIsHomePage()) {
            renderPathInfo = mountPath;
        } else if (pathInfo.startsWith("/")){
            renderPathInfo = mountPath + pathInfo;
        } else {
            renderPathInfo = mountPath + "/" + pathInfo;
        }
        componentConfigurationId = siteMapItemRepresentation.getComponentConfigurationId();
        workspaceConfiguration = siteMapItemRepresentation.isWorkspaceConfiguration();
        inherited = siteMapItemRepresentation.isInherited();
        relativeContentPath = siteMapItemRepresentation.getRelativeContentPath();
        return this;
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

    public String getRenderPathInfo() {
        return renderPathInfo;
    }

    public void setRenderPathInfo(final String renderPathInfo) {
        this.renderPathInfo = renderPathInfo;
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

}
