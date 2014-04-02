/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.cxf.CXFJaxrsHstConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PageComposerContextService {

    private static final Logger log = LoggerFactory.getLogger(PageComposerContextService.class);

    public HstRequestContext getRequestContext() {
        return RequestContextProvider.get();
    }

    public String getRequestConfigIdentifier() {
        return (String) getRequestContext().getAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER);
    }

    public Node getRequestConfigNode(final String expectedNodeType) throws RepositoryException {
        String id = getRequestConfigIdentifier();
        if(id == null) {
            log.warn("Cannot get requestConfigNode because no attr '{}' on request. Return null", CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER);
        }
        try {
            Node configNode = getRequestContext().getSession().getNodeByIdentifier(id);
            if (configNode.isNodeType(expectedNodeType)) {
                return configNode;
            } else {
                log.warn("Expected node was of type '' but actual node is of type '{}'. Return null.", expectedNodeType, configNode.getPrimaryNodeType().getName());
                return null;
            }
        } catch (ItemNotFoundException e) {
            log.warn("Cannot find requestConfigNode because session for user '{}' most likely has no read-access for '{}'",
                    getRequestContext().getSession().getUserID(), id);
            throw e;
        }
    }

    public String getRenderingMountId() {
        final String renderingMountId = (String)getRequestContext().getServletRequest().getSession(true).getAttribute(ContainerConstants.CMS_REQUEST_RENDERING_MOUNT_ID);
        if (renderingMountId == null) {
            throw new IllegalStateException("Cound not find rendering mount id on request session.");
        }
        return renderingMountId;
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
            return StringUtils.substringAfterLast(channelPath, "-preview");
        }
        // there is no preview yet: Live and preview are same paths
        return channelPath;
    }

    public String getEditingPreviewChannelPath() {
        final Mount previewMount = getEditingMount();
        return  previewMount.getChannelPath();
    }


    public HstSite getEditingPreviewSite() {
        final Mount previewMount = getEditingMount();
        return previewMount.getHstSite();
    }

    public Mount getEditingMount() {
        final HstRequestContext requestContext = getRequestContext();
        final String renderingMountId = getRenderingMountId();
        Mount mount = requestContext.getVirtualHost().getVirtualHosts().getMountByIdentifier(renderingMountId);
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
     * @return the preview {@link org.hippoecm.hst.configuration.channel.Channel} and <code>null</code> if there is no preview channel available
     */
    public Channel getEditingPreviewChannel() {
        return getEditingMount().getChannel();
    }

    public boolean hasPreviewConfiguration() {
        return getEditingPreviewSite().hasPreviewConfiguration();
    }

}
