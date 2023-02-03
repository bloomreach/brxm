/*
 * Copyright 2014-2023 Bloomreach
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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.internal.CanonicalInfo;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.pagecomposer.jaxrs.services.experiencepage.XPageUtils;
import org.hippoecm.hst.util.HstSiteMapUtils;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.NodeNameCodec;

import static org.apache.commons.lang3.StringUtils.substringAfterLast;
import static org.hippoecm.hst.configuration.HstNodeTypes.INDEX;

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
    // flag to indicate whether this 'sitemap page' (not a real sitemap page) is the result of an experience page or not
    private boolean experiencePage;
    private boolean expandable;
    private boolean structural;

    public static SiteMapPageRepresentation structural(final String pathInfo, final boolean expandable) {
        final SiteMapPageRepresentation structuralRepresentation = new SiteMapPageRepresentation();
        structuralRepresentation.pathInfo = pathInfo;
        structuralRepresentation.id = getIdFromPathInfo(pathInfo);
        structuralRepresentation.name = structuralRepresentation.id;
        structuralRepresentation.expandable = expandable;
        structuralRepresentation.structural = true;
        return structuralRepresentation;
    }

    private static String getIdFromPathInfo(final String pathInfo) {
        if (StringUtils.isEmpty(pathInfo)) {
            return "/";
        }
        if (pathInfo.contains("/")) {
            return StringUtils.substringAfterLast(pathInfo, "/");
        }
        return pathInfo;
    }

    public SiteMapPageRepresentation represent(final HstSiteMapItem item,
                                               final String parentId,
                                               final String mountPath,
                                               final String homePagePathInfo,
                                               final String previewConfigurationPath) throws IllegalArgumentException {

        id = ((CanonicalInfo)item).getCanonicalIdentifier();
        this.parentId = parentId;
        name = NodeNameCodec.decode(item.getValue());
        pageTitle = item.getPageTitle();

        final boolean pageExists;
        if (item.getComponentConfigurationId() == null && item.getRelativeContentPath() == null) {
            // structural sitemap item, not for rendering a page so do not set pathInfo/renderPathInfo
            pageExists = false;
        } else {
            pageExists = true;
        }
        pathInfo = HstSiteMapUtils.getPath(item, null);
        if (StringUtils.isBlank(pathInfo)) {
            pathInfo = "/";
            renderPathInfo = pageExists ? (StringUtils.isBlank(mountPath) ? "/" : mountPath) : null ;
        } else if (pathInfo.equals(homePagePathInfo)) {
            pathInfo = "/";
            renderPathInfo = pageExists ? (StringUtils.isBlank(mountPath) ? "/" : mountPath) : null;
        } else {
            if (pathInfo.startsWith("/")) {
                renderPathInfo = mountPath + pathInfo;
            } else {
                renderPathInfo = pageExists ? mountPath + "/" + pathInfo : null;
            }
        }

        componentConfigurationId = item.getComponentConfigurationId();
        workspaceConfiguration = ((CanonicalInfo) item).isWorkspaceConfiguration();
        inherited = !((CanonicalInfo) item).getCanonicalPath().startsWith(previewConfigurationPath + "/");
        relativeContentPath = item.getRelativeContentPath();

        expandable = item.getChildren().stream()
                .filter(child -> SiteMapPagesRepresentation.isIncludedSitemapItem(child))
                .findAny()
                .isPresent();

        return this;
    }

    public SiteMapPageRepresentation represent(final HstLink hstLink, final Node handleNode) throws RepositoryException {
        id = handleNode.getIdentifier();
        parentId = handleNode.getParent().getIdentifier();

        name = getName(hstLink, handleNode);

        pageTitle = HstSiteMapUtils.getPageTitle(hstLink, handleNode);

        // from the pathInfo, remove the 'Mount path part' just like SiteMapPageRepresentation for a sitemap item above
        final Mount mount = hstLink.getMount();
        pathInfo = hstLink.getPath();
        if (StringUtils.isBlank(pathInfo)) {
            // homepage
            pathInfo = "/";
            renderPathInfo = mount == null ? "/" : mount.getMountPath();
        } else {
            renderPathInfo = mount == null ? "/" + pathInfo : mount.getMountPath() + "/" + pathInfo;
        }
        if (StringUtils.isEmpty(renderPathInfo)) {
            renderPathInfo = "/";
        }


        experiencePage = XPageUtils.isXPageDocument(handleNode);
        return this;
    }

    public static String getName(final HstLink hstLink, final Node handleNode) throws RepositoryException {
        final String path = hstLink.getPath();
        if (StringUtils.isEmpty(path)) {
            return handleNode.getName();
        } else if (path.contains("/")) {
            return substringAfterLast(path, "/");
        } else {
            return path;
        }
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

    public boolean isExperiencePage() {
        return experiencePage;
    }

    public void setExperiencePage(final boolean experiencePage) {
        this.experiencePage = experiencePage;
    }

    public boolean isExpandable() {
        return expandable;
    }

    public void setExpandable(final boolean expandable) {
        this.expandable = expandable;
    }

    @JsonIgnore
    public boolean isStructural() {
        return structural;
    }
}
