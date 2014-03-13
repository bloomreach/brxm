/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.channelmanager;

import java.util.HashMap;

import javax.servlet.http.HttpSession;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.internal.ConfigurationLockInfo;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.MutableMount;
import org.hippoecm.hst.configuration.internal.CanonicalInfo;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.component.HstURL;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.HstComponentWindow;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Comment;

public class CmsComponentWindowResponseAppender extends AbstractComponentWindowResponseAppender implements ComponentWindowResponseAppender {

    private static final Logger log = LoggerFactory.getLogger(CmsComponentWindowResponseAppender.class);

    private static final String WORKSPACE_PATH_ELEMENT = "/" + HstNodeTypes.NODENAME_HST_WORKSPACE + "/";

    @Override
    public void process(final HstComponentWindow rootWindow, final HstComponentWindow rootRenderingWindow, final HstComponentWindow window, final HstRequest request, final HstResponse response) {
        if (!isCmsRequest(request)) {
            return;
        }

        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new IllegalStateException("HttpSession should never be null here.");
        }

        // we are in render host mode. Add the wrapper elements that are needed for the composer around all components
        HstComponentConfiguration compConfig  = ((HstComponentConfiguration)window.getComponentInfo());
        final HstRequestContext requestContext = request.getRequestContext();
        Mount mount = requestContext.getResolvedMount().getMount();
        if (isTopHstResponse(rootWindow, rootRenderingWindow, window)) {
            response.addHeader(ChannelManagerConstants.HST_MOUNT_ID, mount.getIdentifier());
            response.addHeader(ChannelManagerConstants.HST_SITE_ID, mount.getHstSite().getCanonicalIdentifier());
            response.addHeader(ChannelManagerConstants.HST_PAGE_ID, compConfig.getCanonicalIdentifier());

            final ResolvedSiteMapItem resolvedSiteMapItem = requestContext.getResolvedSiteMapItem();
            if (resolvedSiteMapItem != null) {
                final HstSiteMapItem hstSiteMapItem = resolvedSiteMapItem.getHstSiteMapItem();
                response.addHeader(ChannelManagerConstants.HST_SITEMAPITEM_ID, ((CanonicalInfo)hstSiteMapItem).getCanonicalIdentifier());
                final HstSiteMap siteMap = hstSiteMapItem.getHstSiteMap();
                if (siteMap instanceof CanonicalInfo) {
                    final CanonicalInfo canonicalInfo = (CanonicalInfo) siteMap;
                    response.addHeader(ChannelManagerConstants.HST_SITEMAP_ID, canonicalInfo.getCanonicalIdentifier());
                    if (canonicalInfo.getCanonicalPath().contains(WORKSPACE_PATH_ELEMENT) &&
                            canonicalInfo.getCanonicalPath().startsWith(mount.getHstSite().getConfigurationPath())) {
                        // sitemap item is part of workspace && of current site configuration (thus not inherited)
                        response.addHeader(ChannelManagerConstants.HST_PAGE_EDITABLE, "true");
                    } else {
                        response.addHeader(ChannelManagerConstants.HST_PAGE_EDITABLE, "false");
                    }
                } else {
                    log.warn("Expected sitemap of subtype {}. Cannot set sitemap id.", CanonicalInfo.class.getName());
                }
            }

            Object variant = session.getAttribute(ContainerConstants.RENDER_VARIANT);
            if (variant == null) {
                variant = ContainerConstants.DEFAULT_PARAMETER_PREFIX;
            }
            response.addHeader(ChannelManagerConstants.HST_RENDER_VARIANT, variant.toString());
            response.addHeader(ChannelManagerConstants.HST_SITE_HAS_PREVIEW_CONFIG, String.valueOf(mount.getHstSite().hasPreviewConfiguration()));
        } else if (isComposerMode(request)) {
            if (isNotContainerOrContainerItem(compConfig)) {
                return;
            }
            if (!compConfig.getCanonicalStoredLocation().contains(WORKSPACE_PATH_ELEMENT)) {
                log.debug("Component '{}' not editable as not part of hst:workspace configuration", compConfig.toString());
                return;
            }

            HashMap<String, String> attributes = new HashMap<String, String>();
            attributes.put("uuid", compConfig.getCanonicalIdentifier());
            if(compConfig.getXType() != null) {
                attributes.put("xtype", compConfig.getXType());
            }
            if(compConfig.isInherited()) {
                attributes.put("inherited", "true");
            }
            attributes.put("type", compConfig.getComponentType().toString());
            HstURLFactory urlFactory = requestContext.getURLFactory();
            HstURL url = urlFactory.createURL(HstURL.COMPONENT_RENDERING_TYPE,
                    window.getReferenceNamespace(), null,
                    requestContext);
            attributes.put("url", url.toString());
            attributes.put("refNS", window.getReferenceNamespace());
            if (mount instanceof MutableMount) {
                if (compConfig instanceof ConfigurationLockInfo) {
                    ConfigurationLockInfo lockInfo = (ConfigurationLockInfo)compConfig;
                    if (lockInfo.getLockedBy() != null) {
                        String cmsUserId = (String)session.getAttribute(ContainerConstants.CMS_USER_ID_ATTR);
                        attributes.put(ChannelManagerConstants.HST_LOCKED_BY, lockInfo.getLockedBy());
                        if (lockInfo.getLockedBy().equals(cmsUserId)) {
                            attributes.put(ChannelManagerConstants.HST_LOCKED_BY_CURRENT_USER, "true");
                        } else {
                            attributes.put(ChannelManagerConstants.HST_LOCKED_BY_CURRENT_USER, "false");
                        }
                        if (lockInfo.getLockedOn() != null) {
                            attributes.put(ChannelManagerConstants.HST_LOCKED_ON, String.valueOf(lockInfo.getLockedOn().getTimeInMillis()));
                        }
                    }
                }
                if (compConfig.getLastModified() != null) {
                    attributes.put(ChannelManagerConstants.HST_LAST_MODIFIED, String.valueOf(compConfig.getLastModified().getTimeInMillis()));
                }
            }
            Comment comment = createCommentWithAttr(attributes, response);
            response.addPreamble(comment);
        }

    }

    private boolean isNotContainerOrContainerItem(final HstComponentConfiguration compConfig) {
        if (HstComponentConfiguration.Type.CONTAINER_ITEM_COMPONENT.equals(compConfig.getComponentType())) {
            return true;
        }
        if (HstComponentConfiguration.Type.CONTAINER_COMPONENT.equals(compConfig.getComponentType())) {
            return true;
        }
        return false;
    }

}
