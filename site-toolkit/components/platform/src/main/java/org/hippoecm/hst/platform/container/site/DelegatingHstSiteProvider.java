/*
 *  Copyright 2017-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.container.site;


import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.container.site.CompositeHstSite;
import org.hippoecm.hst.container.site.CustomWebsiteHstSiteProviderService;
import org.hippoecm.hst.container.site.HstSiteProvider;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.hst.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.core.container.ContainerConstants.RENDER_BRANCH_ID;

public class DelegatingHstSiteProvider  {

    private static final Logger log = LoggerFactory.getLogger(DelegatingHstSiteProvider.class);

    private static final String HST_SITE_CONTEXT_ATTR = DelegatingHstSiteProvider.class.getName() + ".hstSite";

    private HstSiteProvider channelManagerHstSiteProvider = (master, branches, requestContext) -> master;
    private HstSiteProvider websiteHstSiteProvider = (master, branches, requestContext) -> master;

    /**
     * this setter can be used by enterprise paltform webapp to inject a custom HstSiteProvider for the channel mngr
     */
    public void setChannelManagerHstSiteProvider(final HstSiteProvider channelManagerHstSiteProvider) {
        this.channelManagerHstSiteProvider = channelManagerHstSiteProvider;
    }

    /**
     * this setter can be used by enterprise platform webapp to inject a custom HstSiteProvider for the website
     */
    public void setWebsiteHstSiteProvider(final HstSiteProvider websiteHstSiteProvider) {
        this.websiteHstSiteProvider = websiteHstSiteProvider;
    }

    public HstSite getHstSite(final CompositeHstSite compositeHstSite, final HstRequestContext requestContext) {
        if (requestContext == null) {
            return compositeHstSite.getMaster();
        }

        final HstSite computed = (HstSite) requestContext.getAttribute(HST_SITE_CONTEXT_ATTR);
        if (computed != null) {
            return computed;
        }

        final HstSite hstSite;
        if (requestContext.isCmsRequest()) {
            hstSite = channelManagerHstSiteProvider.getHstSite(compositeHstSite.getMaster(), compositeHstSite.getBranches(), requestContext);
        } else {
            final CustomWebsiteHstSiteProviderService customWebsiteHstSiteProviderService = HippoServiceRegistry.getService(CustomWebsiteHstSiteProviderService.class);
            final HstSiteProvider customSiteProvider = customWebsiteHstSiteProviderService.get(requestContext.getServletRequest().getContextPath());
            if (customSiteProvider == null) {
                hstSite = websiteHstSiteProvider.getHstSite(compositeHstSite.getMaster(), compositeHstSite.getBranches(), requestContext);
            } else {
                HstSite custom;
                try {
                    custom = customSiteProvider.getHstSite(compositeHstSite.getMaster(), compositeHstSite.getBranches(), requestContext);
                } catch (Exception e) {
                    log.warn("Exception in custom site provider {}. Fallback to default hst site provider",
                            customSiteProvider.getClass(), e);
                    custom = websiteHstSiteProvider.getHstSite(compositeHstSite.getMaster(), compositeHstSite.getBranches(), requestContext);
                }
                if (custom == null) {
                    log.warn("");
                }
                hstSite = custom;
            }

        }

        // never compute for the same request context again which HstSite to serve
        requestContext.setAttribute(HST_SITE_CONTEXT_ATTR, hstSite);

        final Channel channel = hstSite.getChannel();
        if (channel != null && channel.getBranchId() != null) {
            // by setting the RENDER_BRANCH_ID, all further request processing in HST can know which branch
            // to render
            requestContext.setAttribute(RENDER_BRANCH_ID, channel.getBranchId());
        }
        return hstSite;
    }
}
