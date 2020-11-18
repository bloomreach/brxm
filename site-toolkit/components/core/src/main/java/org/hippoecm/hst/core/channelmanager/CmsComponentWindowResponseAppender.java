/*
 *  Copyright 2013-2020 Hippo B.V. (http://www.onehippo.com)
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

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.internal.CanonicalInfo;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDocumentBean;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.HstComponentWindow;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.util.HstRequestUtils;
import org.hippoecm.hst.util.JcrSessionUtils;
import org.hippoecm.repository.api.HippoSession;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.core.channelmanager.ChannelManagerConstants.HST_IS_PRIMARYDOCUMENT_VERSION_HISTORY;
import static org.hippoecm.hst.core.container.ContainerConstants.RENDER_VARIANT;
import static org.hippoecm.hst.platform.services.channel.ChannelManagerPrivileges.CHANNEL_WEBMASTER_PRIVILEGE_NAME;
import static org.hippoecm.hst.platform.services.channel.ChannelManagerPrivileges.XPAGE_REQUIRED_PRIVILEGE_NAME;
import static org.hippoecm.hst.util.JcrSessionUtils.isInRole;

public class CmsComponentWindowResponseAppender extends AbstractComponentWindowResponseAppender implements ComponentWindowResponseAppender {

    private static final Logger log = LoggerFactory.getLogger(CmsComponentWindowResponseAppender.class);

    private static final String WORKSPACE_PATH_ELEMENT = "/" + HstNodeTypes.NODENAME_HST_WORKSPACE + "/";

    private List<ComponentWindowAttributeContributor> attributeContributors = Collections.emptyList();

    public void setAttributeContributors(final List<ComponentWindowAttributeContributor> attributeContributors) {
        this.attributeContributors = attributeContributors;
    }

    @Override
    public void process(final HstComponentWindow rootWindow, final HstComponentWindow rootRenderingWindow, final HstComponentWindow window, final HstRequest request, final HstResponse response) {
        if (!isChannelManagerPreviewRequest(request)) {
            return;
        }

        final HstComponentConfiguration compConfig = (HstComponentConfiguration)window.getComponentInfo();


        // we are in render host mode. Add the wrapper elements that are needed for the composer around all components
        if (isContainerOrContainerItem(compConfig)) {
            if (compConfig.isInherited()) {
                // for inherited components we do not add any html comments since component is never editable for user any way
                log.debug("Component '{}' not editable because inherited", compConfig.toString());
                return;
            }
            if (userInRole(request, compConfig)) {
                populateComponentMetaData(request, response, window);
            } else {
                log.debug("Component '{}' not editable because user is not in right role", compConfig.toString());
            }
        } else if (isTopHstResponse(rootWindow, rootRenderingWindow, window)) {

            final CmsSessionContext cmsSessionContext = HstRequestUtils.getCmsSessionContext(request);

            if (cmsSessionContext == null) {
                throw new IllegalStateException("cmsSessionContext should never be null here.");
            }

            populatePageMetaData(request, response, cmsSessionContext, compConfig);
        }
    }

    private void populatePageMetaData(final HstRequest request, final HstResponse response, final CmsSessionContext cmsSessionContext, final HstComponentConfiguration compConfig) {
        final HstRequestContext requestContext = request.getRequestContext();
        final Mount mount = requestContext.getResolvedMount().getMount();
        final Map<String, String> pageMetaData = new HashMap<>();

        pageMetaData.put(ChannelManagerConstants.HST_MOUNT_ID, mount.getIdentifier());
        pageMetaData.put(ChannelManagerConstants.HST_SITE_ID, mount.getHstSite().getCanonicalIdentifier());
        pageMetaData.put(ChannelManagerConstants.HST_PAGE_ID, compConfig.getCanonicalIdentifier());

        final HippoBean primaryRequestBean = requestContext.getContentBean();

        if (primaryRequestBean instanceof HippoDocumentBean) {
            // in the channel mgr, in case of a document bean, it will be the unpublished document variant
            pageMetaData.put(ChannelManagerConstants.HST_UNPUBLISHED_VARIANT_ID, primaryRequestBean.getValueProvider().getIdentifier());
        }

        pageMetaData.put(HST_IS_PRIMARYDOCUMENT_VERSION_HISTORY, String.valueOf(requestContext.isRenderingHistory()));

        pageMetaData.put(ChannelManagerConstants.HST_BRANCH_ID, HstRequestUtils.getBranchIdFromContext(requestContext));

        // provide info for CM that the page is an experience page: The top hst component for experience pages
        // always has compConfig.isExperiencePageComponent() = true
        final ResolvedSiteMapItem resolvedSiteMapItem = requestContext.getResolvedSiteMapItem();

        pageMetaData.put(ChannelManagerConstants.HST_EXPERIENCE_PAGE,  String.valueOf(resolvedSiteMapItem.isExperiencePage()));

        if (resolvedSiteMapItem != null) {
            final HstSiteMapItem hstSiteMapItem = resolvedSiteMapItem.getHstSiteMapItem();
            pageMetaData.put(ChannelManagerConstants.HST_SITEMAPITEM_ID, ((CanonicalInfo) hstSiteMapItem).getCanonicalIdentifier());
            final HstSiteMap siteMap = hstSiteMapItem.getHstSiteMap();
            if (siteMap instanceof CanonicalInfo) {
                final CanonicalInfo canonicalInfo = (CanonicalInfo) siteMap;
                pageMetaData.put(ChannelManagerConstants.HST_SITEMAP_ID, canonicalInfo.getCanonicalIdentifier());
            } else {
                log.warn("Expected sitemap of subtype {}. Cannot set sitemap id.", CanonicalInfo.class.getName());
            }
        }


        Serializable variant = cmsSessionContext.getContextPayload().get(RENDER_VARIANT);

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

        if (requestContext.isPageModelApiRequest()) {
            // in case of PageModelApi pipeline, we need the parent mount to get the channel since the page model
            // api pipeline never has a channel, see org.hippoecm.hst.configuration.hosting.PageModelApiMount.getChannel()
            pageMetaData.put(ChannelManagerConstants.HST_CHANNEL_ID, mount.getParent().getChannel().getId());
        } else {
            pageMetaData.put(ChannelManagerConstants.HST_CHANNEL_ID, mount.getChannel().getId());
        }
        pageMetaData.put(ChannelManagerConstants.HST_CONTEXT_PATH, mount.getContextPath());
        response.addEpilogue(createCommentWithAttr(pageMetaData, response));
    }

    private void populateComponentMetaData(final HstRequest request, final HstResponse response,
                                           final HstComponentWindow window) {

        final HstComponentConfiguration config = (HstComponentConfiguration)window.getComponentInfo();

        if (config.getCanonicalStoredLocation().contains(WORKSPACE_PATH_ELEMENT) || config.isExperiencePageComponent()) {
            final Map<String, String> preambleAttributes = new HashMap<>();
            final Map<String, String> epilogueAttributes = new HashMap<>();
            populateAttributes(window, request, preambleAttributes, epilogueAttributes);
            response.addPreamble(createCommentWithAttr(preambleAttributes, response));
            response.addEpilogue(createCommentWithAttr(epilogueAttributes, response));
        } else {
            log.debug("Component '{}' not editable as not part of hst:workspace configuration and not part of " +
                    "an experience page", config.toString());
        }
    }


    final void populateAttributes(HstComponentWindow window, HstRequest request,
                                  Map<String, String> preambleAttributes, Map<String, String> epilogueAttributes) {
        for (ComponentWindowAttributeContributor attributeContributor : attributeContributors) {
            attributeContributor.contributePreamble(window, request, preambleAttributes);
            attributeContributor.contributeEpilogue(window, request, epilogueAttributes);
        }
    }

    private boolean userInRole(final HstRequest request, final HstComponentConfiguration compConfig) {

        final HippoSession cmsUser = JcrSessionUtils.getCmsUser(request.getRequestContext());

        // note that EVEN if the backing JCR node for compConfig is from version history, because we decorate
        // the JCR Node to HippoBeanFrozenNode in ObjectConverterImpl.getActualNode(), the #getPath is decorated
        // to always return a workspace path! Hence #getCanonicalStoredLocation gives right location
        if (compConfig.isExperiencePageComponent()) {
            // check whether cmsUser has the right role on the xpage component document (aka handle)
            // note that even if the backing JCR Node from 'getContentBean' is a frozen jcr node, #getParent on
            // that frozen node will return the workspace handle, see HippoBeanFrozenNodeUtils.getWorkspaceFrozenNode()
            final String handlePath;
            try {
                handlePath = request.getRequestContext().getContentBean().getNode().getParent().getPath();
                if (!compConfig.getCanonicalStoredLocation().startsWith(handlePath)) {
                    if (compConfig.isUnresolvedXpageLayoutContainer()) {
                        log.info("Component '{}' for XPage '{}' has been most likely added later on to the XPage Layout, " +
                                "on usage, the container should be created in the XPage document", compConfig.getCanonicalStoredLocation(), handlePath);
                    } else {
                        log.error("Component '{}' for XPage '{}' expected to be a descendant of handle but was not the case, return " +
                                "false for user in role", compConfig.getCanonicalStoredLocation(), handlePath);
                    }
                }
                return isInRole(cmsUser, handlePath, XPAGE_REQUIRED_PRIVILEGE_NAME);
            } catch (RepositoryException e) {
                log.error("Exception while checking user in role, return false" , e);
                return false;
            }
        } else {
            return isInRole(cmsUser, compConfig.getCanonicalStoredLocation(), CHANNEL_WEBMASTER_PRIVILEGE_NAME);
        }

    }
}
