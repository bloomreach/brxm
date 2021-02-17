/*
 * Copyright 2014-2020 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.internal.CanonicalInfo;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.pagecomposer.jaxrs.services.experiencepage.XPageUtils;
import org.hippoecm.hst.util.HstSiteMapUtils;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.NodeNameCodec;

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

    public SiteMapPageRepresentation represent(final HstSiteMapItem item,
                                               final String parentId,
                                               final String mountPath,
                                               final String homePagePathInfo,
                                               final String previewConfigurationPath) throws IllegalArgumentException {

        id = ((CanonicalInfo)item).getCanonicalIdentifier();
        this.parentId = parentId;
        name = NodeNameCodec.decode(item.getValue());
        pageTitle = item.getPageTitle();
        pathInfo =  HstSiteMapUtils.getPath(item, null);

        if (StringUtils.isBlank(pathInfo)) {
            pathInfo = "/";
            renderPathInfo = StringUtils.isBlank(mountPath) ? "/" : mountPath;
        } else if (pathInfo.equals(homePagePathInfo)) {
            pathInfo = "/";
            renderPathInfo = StringUtils.isBlank(mountPath) ? "/" : mountPath;
        } else {
            if (pathInfo.startsWith("/")){
                renderPathInfo = mountPath + pathInfo;
            } else {
               renderPathInfo = mountPath + "/" + pathInfo;
            }
        }

        componentConfigurationId = item.getComponentConfigurationId();
        workspaceConfiguration = ((CanonicalInfo) item).isWorkspaceConfiguration();
        inherited = !((CanonicalInfo) item).getCanonicalPath().startsWith(previewConfigurationPath + "/");
        relativeContentPath = item.getRelativeContentPath();
        return this;
    }

    public SiteMapPageRepresentation represent(final HstLink hstLink, final Node handleNode) throws RepositoryException {
        id = handleNode.getIdentifier();
        parentId = handleNode.getParent().getIdentifier();

        if (hstLink.representsIndex()) {
            // the hstLink was the result of a document matching the _index_ sitemap item : as a result, it will be
            // accessed via the 'parent url'. Instead of typically having 'index' as name in the sitemap, use the
            // parent name to get the logical sitemap name in the CM
            if (hstLink.getHstSiteMapItem().isWildCard()) {
                name = handleNode.getParent().getName();
            } else {
                name = hstLink.getHstSiteMapItem().getValue();
            }
        } else {
            name = handleNode.getName();
        }

        pageTitle = ((HippoNode)handleNode).getDisplayName();
        // from the pathInfo, remove the 'Mount path part' just like SiteMapPageRepresentation for a sitemap item above
        final Mount mount = hstLink.getMount();
        pathInfo = hstLink.getPath();
        if (StringUtils.isBlank(pathInfo)) {
            renderPathInfo = mount == null ? "/" : mount.getMountPath();
        } else {
            renderPathInfo = mount == null ? "/" + pathInfo : mount.getMountPath() + "/" + pathInfo;
        }

        experiencePage = XPageUtils.isXPageDocument(handleNode);
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

    public boolean isExperiencePage() {
        return experiencePage;
    }

    public void setExperiencePage(final boolean experiencePage) {
        this.experiencePage = experiencePage;
    }
}
