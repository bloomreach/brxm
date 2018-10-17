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
package org.hippoecm.hst.platform.configuration.sitemapitemhandler;

import java.util.HashMap;
import java.util.Map;

import org.onehippo.cms7.services.HippoServiceRegistry;

public class HstSiteMapItemHandlerFactoriesImpl implements HstSiteMapItemHandlerFactories {

    private final Map<String, HstSiteMapItemHandlerFactory> hstSiteMapItemHandlerFactories = new HashMap<>();

    public void init() {
        HippoServiceRegistry.register(this, HstSiteMapItemHandlerFactories.class);
    }

    public void destroy() {
        HippoServiceRegistry.unregister(this, HstSiteMapItemHandlerFactories.class);
    }

    @Override
    public synchronized void register(final String contextPath, final HstSiteMapItemHandlerFactory hstSiteMapItemHandlerFactory) {
        hstSiteMapItemHandlerFactories.put(contextPath, hstSiteMapItemHandlerFactory);
    }

    @Override
    public synchronized void unregister(final String contextPath) {
        hstSiteMapItemHandlerFactories.remove(contextPath);
    }

    @Override
    public HstSiteMapItemHandlerFactory getHstSiteMapItemHandlerFactory(final String contextPath) {
        return hstSiteMapItemHandlerFactories.get(contextPath);
    }
}
