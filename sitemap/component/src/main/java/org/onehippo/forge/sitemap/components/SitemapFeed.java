/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.sitemap.components;

import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.sitemenu.HstSiteMenus;
import org.onehippo.forge.sitemap.generator.SitemapGenerator;


public class SitemapFeed extends BaseHstComponent {

    public static final int DEFAULT_DEPTH = 2;
    
    public void doBeforeRender(HstRequest request, HstResponse response) {
        String depthStr =getComponentParameter("depth");
        int maxdepth = DEFAULT_DEPTH;
        if (depthStr != null){
            maxdepth = Integer.valueOf(depthStr);  
        }
        HstSiteMenus siteMenus = request.getRequestContext().getHstSiteMenus();
        request.setAttribute("sitemap", new SitemapGenerator(request.getRequestContext(), RequestContextProvider.get().getContentBeansTool().getObjectConverter())
                .createSitemap(siteMenus, maxdepth));
    }

}
