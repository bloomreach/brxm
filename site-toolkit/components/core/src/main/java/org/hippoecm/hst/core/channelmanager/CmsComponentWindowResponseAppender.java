/*
 *  Copyright 2013-2016 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.internal.CanonicalInfo;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.HstComponentWindow;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CmsComponentWindowResponseAppender extends AbstractComponentWindowResponseAppender implements ComponentWindowResponseAppender {

    private static final Logger log = LoggerFactory.getLogger(CmsComponentWindowResponseAppender.class);

    private static final String WORKSPACE_PATH_ELEMENT = "/" + HstNodeTypes.NODENAME_HST_WORKSPACE + "/";

    private List<ComponentWindowAttributeContributor> attributeContributors = Collections.emptyList();

    public void setAttributeContributors(final List<ComponentWindowAttributeContributor> attributeContributors) {
        this.attributeContributors = attributeContributors;
    }

    @Override
    public void process(final HstComponentWindow rootWindow, final HstComponentWindow rootRenderingWindow, final HstComponentWindow window, final HstRequest request, final HstResponse response) {
        if (!isApplicableRequest(request)) {
            return;
        }

        HttpSession session = request.getSession(false);
        if (session == null && isComposerMode(request)) {
            throw new IllegalStateException("HttpSession should never be null here.");
        }

        // we are in render host mode. Add the wrapper elements that are needed for the composer around all components
        HstComponentConfiguration compConfig = ((HstComponentConfiguration) window.getComponentInfo());

        if (isContainerOrContainerItem(compConfig)) {
            if (isComponentMetadataAppilcableRequest(request)) {
                populateComponentMetaData(request, response, window);
            }
        } else if (isTopHstResponse(rootWindow, rootRenderingWindow, window)) {
            populatePageMetaData(request, response, session, compConfig);
        }
    }

    @Override
    protected boolean isComponentMetadataAppilcableRequest(final HstRequest request) {
        return true;
    }

    private void populatePageMetaData(final HstRequest request, final HstResponse response, final HttpSession session, final HstComponentConfiguration compConfig) {
        final HstRequestContext requestContext = request.getRequestContext();
        final Mount mount = requestContext.getResolvedMount().getMount();
        final Map<String, String> pageMetaData = new HashMap<>();

        pageMetaData.put(ChannelManagerConstants.HST_MOUNT_ID, mount.getIdentifier());
        pageMetaData.put(ChannelManagerConstants.HST_SITE_ID, mount.getHstSite().getCanonicalIdentifier());
        pageMetaData.put(ChannelManagerConstants.HST_PAGE_ID, compConfig.getCanonicalIdentifier());

        final ResolvedSiteMapItem resolvedSiteMapItem = requestContext.getResolvedSiteMapItem();
        if (resolvedSiteMapItem != null) {
            final HstSiteMapItem hstSiteMapItem = resolvedSiteMapItem.getHstSiteMapItem();
            pageMetaData.put(ChannelManagerConstants.HST_SITEMAPITEM_ID, ((CanonicalInfo) hstSiteMapItem).getCanonicalIdentifier());
            final HstSiteMap siteMap = hstSiteMapItem.getHstSiteMap();
            if (siteMap instanceof CanonicalInfo) {
                final CanonicalInfo canonicalInfo = (CanonicalInfo) siteMap;
                pageMetaData.put(ChannelManagerConstants.HST_SITEMAP_ID, canonicalInfo.getCanonicalIdentifier());
                if (canonicalInfo.getCanonicalPath().contains(WORKSPACE_PATH_ELEMENT) &&
                        canonicalInfo.getCanonicalPath().startsWith(mount.getHstSite().getConfigurationPath())) {
                    // sitemap item is part of workspace && of current site configuration (thus not inherited)
                    pageMetaData.put(ChannelManagerConstants.HST_PAGE_EDITABLE, "true");
                } else {
                    pageMetaData.put(ChannelManagerConstants.HST_PAGE_EDITABLE, "false");
                }
            } else {
                log.warn("Expected sitemap of subtype {}. Cannot set sitemap id.", CanonicalInfo.class.getName());
            }
        }

        Object variant = (session != null) ? session.getAttribute(ContainerConstants.RENDER_VARIANT) : null;
        if (variant == null) {
            variant = ContainerConstants.DEFAULT_PARAMETER_PREFIX;
        }
        pageMetaData.put(ChannelManagerConstants.HST_RENDER_VARIANT, variant.toString());
        pageMetaData.put(ChannelManagerConstants.HST_SITE_HAS_PREVIEW_CONFIG, String.valueOf(mount.getHstSite().hasPreviewConfiguration()));

        for (Map.Entry<String, String> entry : pageMetaData.entrySet()) {
            response.addHeader(entry.getKey(), entry.getValue());
        }
        pageMetaData.put(ChannelManagerConstants.HST_TYPE, ChannelManagerConstants.HST_TYPE_PAGE_META_DATA);
        pageMetaData.put(ChannelManagerConstants.HST_PATH_INFO, requestContext.getBaseURL().getPathInfo());
        pageMetaData.put(ChannelManagerConstants.HST_CHANNEL_ID, mount.getChannel().getId());
        pageMetaData.put(ChannelManagerConstants.HST_CONTEXT_PATH, mount.getContextPath());
        response.addEpilogue(createCommentWithAttr(pageMetaData, response));
    }

    private void populateComponentMetaData(final HstRequest request, final HstResponse response,
                                           final HstComponentWindow window) {
        final HstComponentConfiguration config = (HstComponentConfiguration)window.getComponentInfo();

        if (!config.getCanonicalStoredLocation().contains(WORKSPACE_PATH_ELEMENT)) {
            log.debug("Component '{}' not editable as not part of hst:workspace configuration", config.toString());
            return;
        }

        final Map<String, String> preambleAttributes = new HashMap<>();
        final Map<String, String> epilogueAttributes = new HashMap<>();
        populateAttributes(window, request, preambleAttributes, epilogueAttributes);
        response.addPreamble(createCommentWithAttr(preambleAttributes, response));
        response.addEpilogue(createCommentWithAttr(epilogueAttributes, response));
    }

    final void populateAttributes(HstComponentWindow window, HstRequest request,
                                  Map<String, String> preambleAttributes, Map<String, String> epilogueAttributes) {
        for (ComponentWindowAttributeContributor attributeContributor : attributeContributors) {
            attributeContributor.contributePreamble(window, request, preambleAttributes);
            attributeContributor.contributeEpilogue(window, request, epilogueAttributes);
        }
    }
}
