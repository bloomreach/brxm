/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.model;

import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.platform.configuration.cache.HstConfigurationLoadingCache;
import org.hippoecm.hst.platform.configuration.cache.HstNodeLoadingCache;
import org.hippoecm.hst.platform.configuration.hosting.VirtualHostsService;
import org.hippoecm.hst.platform.linking.DefaultHstLinkCreator;
import org.hippoecm.hst.platform.matching.BasicHstSiteMapMatcher;

public class HstModelImpl implements HstModel {

    private final VirtualHosts virtualHosts;
    private final BasicHstSiteMapMatcher hstSiteMapMatcher;
    private final DefaultHstLinkCreator hstLinkCreator;

    public HstModelImpl(final String contextPath, final HstNodeLoadingCache hstNodeLoadingCache, final HstConfigurationLoadingCache hstConfigurationLoadingCache) {
        virtualHosts = new VirtualHostsService(contextPath, hstNodeLoadingCache, hstConfigurationLoadingCache);
        hstSiteMapMatcher = new BasicHstSiteMapMatcher();
        hstLinkCreator = new DefaultHstLinkCreator();
    }

    @Override
    public VirtualHosts getVirtualHosts() {
        return virtualHosts;
    }

    @Override
    public HstSiteMapMatcher getHstSiteMapMatcher() {
        return hstSiteMapMatcher;
    }

    @Override
    public HstLinkCreator getHstLinkCreator() {
        return hstLinkCreator;
    }

    // internal package accessor only
    BasicHstSiteMapMatcher getHstSiteMapMatcherImpl() {
        return hstSiteMapMatcher;
    }

    DefaultHstLinkCreator getHstLinkCreatorImpl() {
        return hstLinkCreator;
    }
}
