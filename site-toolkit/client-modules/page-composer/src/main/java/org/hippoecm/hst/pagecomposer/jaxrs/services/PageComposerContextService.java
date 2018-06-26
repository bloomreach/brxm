/*
 * Copyright 2014-2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst.pagecomposer.jaxrs.services;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.cxf.CXFJaxrsHstConfigService;
import org.hippoecm.hst.platform.model.HstModel;
import org.onehippo.cms7.services.hst.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.core.container.ContainerConstants.CMS_REQUEST_RENDERING_MOUNT_ID;

public class PageComposerContextService {

    private static final Logger log = LoggerFactory.getLogger(PageComposerContextService.class);

    public static final String LIVE_EDITING_HST_MODEL_ATTR = PageComposerContextService.class.getName() + ".live";
    public static final String PREVIEW_EDITING_HST_MODEL_ATTR = PageComposerContextService.class.getName() + ".preview";

    public HstRequestContext getRequestContext() {
        return RequestContextProvider.get();
    }

    public String getRequestConfigIdentifier() {
        return (String) getRequestContext().getAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER);
    }

    public Node getRequestConfigNode(final String expectedNodeType) throws RepositoryException {
        final String id = getRequestConfigIdentifier();
        return getRequestConfigNodeById(id, expectedNodeType, getRequestContext().getSession());
    }

    public Node getRequestConfigNodeById(final String id, final String expectedNodeType, final Session session) throws RepositoryException {
        if (id == null) {
            throw new IllegalStateException(String.format("Cannot get requestConfigNode because no attr '%s' on request. Return null",
                    CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER));
        }
        try {
            Node configNode = session.getNodeByIdentifier(id);
            if (configNode.isNodeType(expectedNodeType)) {
                return configNode;
            } else {
                log.warn("Expected node was of type '{}' but actual node is of type '{}'. Return null.", expectedNodeType, configNode.getPrimaryNodeType().getName());
                return null;
            }
        } catch (ItemNotFoundException e) {
            log.warn("Cannot find requestConfigNode because session for user '{}' most likely has no read-access for '{}'",
                    session.getUserID(), id);
            throw e;
        }
    }

    public String getRenderingMountId() {
        final String renderingMountId = (String) getRequestContext().getServletRequest().getSession(true).getAttribute(CMS_REQUEST_RENDERING_MOUNT_ID);
        if (renderingMountId == null) {
            throw new IllegalStateException("Could not find rendering mount id on request session.");
        }
        return renderingMountId;
    }

    public void removeRenderingMountId() {
        getRequestContext().getServletRequest().getSession(true).removeAttribute(CMS_REQUEST_RENDERING_MOUNT_ID);
    }


    public boolean isRenderingMountSet() {
        return getRequestContext().getServletRequest().getSession(true).getAttribute(CMS_REQUEST_RENDERING_MOUNT_ID) != null;
    }

    public String getEditingLiveConfigurationPath() {
        String editingPreviewConfigurationPath = getEditingPreviewConfigurationPath();
        if (editingPreviewConfigurationPath.endsWith("-preview")) {
            return StringUtils.substringBeforeLast(editingPreviewConfigurationPath, "-preview");
        }
        // there is no preview yet: Live and preview are same paths
        return editingPreviewConfigurationPath;
    }

    public String getEditingPreviewConfigurationPath() {
        return getEditingPreviewSite().getConfigurationPath();
    }

    public String getEditingLiveChannelPath() {
        final String channelPath = getEditingPreviewChannelPath();
        if (channelPath.endsWith("-preview")) {
            return StringUtils.substringBeforeLast(channelPath, "-preview");
        }
        // there is no preview yet: Live and preview are same paths
        return channelPath;
    }

    public String getEditingPreviewChannelPath() {
        final Mount previewMount = getEditingMount();
        return previewMount.getChannelPath();
    }


    public HstSite getEditingPreviewSite() {
        final Mount previewMount = getEditingMount();
        return previewMount.getHstSite();
    }

    public Mount getEditingMount() {
        final String renderingMountId = getRenderingMountId();
        Mount mount = getEditingPreviewVirtualHosts().getMountByIdentifier(renderingMountId);
        if (mount == null) {
            String msg = String.format("Could not find a Mount for identifier + '%s'", renderingMountId);
            throw new IllegalStateException(msg);
        }
        if (!Mount.PREVIEW_NAME.equals(mount.getType())) {
            String msg = String.format("Expected a preview (decorated) mount but '%s' is not of " +
                    "type preview.", mount.toString());
            throw new IllegalStateException(msg);
        }
        return mount;
    }

    /**
     * @return the preview {@link Channel} and <code>null</code> if there is no
     * preview channel available
     */
    public Channel getEditingPreviewChannel() {
        return getEditingMount().getChannel();
    }

    public VirtualHosts getEditingPreviewVirtualHosts() {
        return ((HstModel)getRequestContext().getAttribute(PREVIEW_EDITING_HST_MODEL_ATTR)).getVirtualHosts();
    }

    public VirtualHosts getEditingLiveVirtualHosts() {
        return ((HstModel)getRequestContext().getAttribute(LIVE_EDITING_HST_MODEL_ATTR)).getVirtualHosts();
    }


    public boolean hasPreviewConfiguration() {
        return getEditingPreviewSite().hasPreviewConfiguration();
    }

    /**
     * @return the site content identifier for <code>mount</code> and <code>null</code> if not found
     */
    public String getSiteContentIdentifier(final Mount mount) {
        String contentPath = mount.getContentPath();
        if (contentPath == null) {
            return null;
        }
        try {
            return getRequestContext().getSession().getNode(contentPath).getIdentifier();
        } catch (RepositoryException e) {
            log.info("Cannot find site content identifier for mount {} for content path {}", mount, contentPath);
            return null;
        }
    }
}
