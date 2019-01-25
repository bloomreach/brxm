/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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

import java.util.HashMap;
import java.util.Map;

import org.hippoecm.hst.container.site.HstSiteProvider;
import org.hippoecm.hst.container.site.CustomWebsiteHstSiteProviderService;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.context.HippoWebappContext;
import org.onehippo.cms7.services.context.HippoWebappContextRegistry;

import static org.hippoecm.hst.platform.utils.ProxyUtils.createProxy;

public class CustomWebsiteHstSiteProviderServiceImpl implements CustomWebsiteHstSiteProviderService {

    private final Map<String, HstSiteProvider> providers = new HashMap<>();

    public void init() {
        HippoServiceRegistry.register(this, CustomWebsiteHstSiteProviderService.class);
    }

    public void destroy() {
        HippoServiceRegistry.unregister(this, CustomWebsiteHstSiteProviderService.class);
    }

    @Override
    public HstSiteProvider get(final String contextPath) {
        return providers.get(contextPath);
    }

    @Override
    public void set(final String contextPath, final HstSiteProvider hstSiteProvider) {
        // the injected hst site provider should run with the class loader that injects the site provider

        final HippoWebappContext context = HippoWebappContextRegistry.get().getContext(contextPath);
        final HstSiteProvider proxy = createProxy(context.getServletContext().getClassLoader(),
                HstSiteProvider.class, hstSiteProvider);
        providers.put(contextPath, proxy);
    }

    @Override
    public void remove(final String contextPath) {
        providers.remove(contextPath);
    }
}
