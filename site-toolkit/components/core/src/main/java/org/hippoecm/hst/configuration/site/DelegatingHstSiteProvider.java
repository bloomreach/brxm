/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration.site;


import org.hippoecm.hst.core.request.HstRequestContext;

public class DelegatingHstSiteProvider  {


    private HstSiteProvider channelManagerHstSiteProvider = (compositeHstSite, requestContext) -> compositeHstSite.getMaster();
    private HstSiteProvider websiteHstSiteProvider = (compositeHstSite, requestContext) -> compositeHstSite.getMaster();

    /**
     * this setter can be used by enterprise / end project modules to inject a custom HstSiteProvider for the channel mngr
     */
    public void setChannelManagerHstSiteProvider(final HstSiteProvider channelManagerHstSiteProvider) {
        this.channelManagerHstSiteProvider = channelManagerHstSiteProvider;
    }

    /**
     * this setter can be used by enterprise / end project modules to inject a custom HstSiteProvider for the website
     */
    public void setWebsiteHstSiteProvider(final HstSiteProvider websiteHstSiteProvider) {
        this.websiteHstSiteProvider = websiteHstSiteProvider;
    }

    public HstSite getHstSite(final CompositeHstSite compositeHstSite, final HstRequestContext requestContext) {
        if (requestContext == null) {
            return compositeHstSite.getMaster();
        }
        if (requestContext.isCmsRequest()) {
            return channelManagerHstSiteProvider.getHstSite(compositeHstSite, requestContext);
        }

        return websiteHstSiteProvider.getHstSite(compositeHstSite, requestContext);
    }
}
