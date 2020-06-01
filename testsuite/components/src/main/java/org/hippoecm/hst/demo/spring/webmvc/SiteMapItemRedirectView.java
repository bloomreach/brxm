/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.demo.spring.webmvc;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.util.HstRequestUtils;
import org.springframework.web.servlet.view.RedirectView;

public class SiteMapItemRedirectView extends RedirectView {
    
    protected String siteMapItemId;
    protected boolean isChild;
    
    public SiteMapItemRedirectView(String siteMapItemId, boolean isChild) {
        this.siteMapItemId = siteMapItemId;
        this.isChild = isChild;
    }

    protected void renderMergedOutputModel(Map model, HttpServletRequest request, HttpServletResponse response) throws IOException {

        HstRequest hstRequest = HstRequestUtils.getHstRequest(request);
        HstResponse hstResponse = HstRequestUtils.getHstResponse(request, response);
        
        if (hstRequest != null && hstResponse != null) {
            String resolvedSiteMapItemId = null;
            
            if (isChild) {
                HstSiteMapItem resolvedSiteMapItem = hstRequest.getRequestContext().getResolvedSiteMapItem().getHstSiteMapItem().getChild(siteMapItemId);
                
                if (resolvedSiteMapItem != null) {
                    resolvedSiteMapItemId = resolvedSiteMapItem.getId();
                }
            } else {
                resolvedSiteMapItemId = siteMapItemId;
            }
            
            if (resolvedSiteMapItemId != null) {
                HstLinkCreator linkCreator = hstRequest.getRequestContext().getHstLinkCreator();
                HstLink link = linkCreator.createByRefId(resolvedSiteMapItemId, hstRequest.getRequestContext().getResolvedMount().getMount());
    
                StringBuilder url = new StringBuilder();
                
                for (String elem : link.getPathElements()) {
                    String enc = response.encodeURL(elem);
                    url.append("/").append(enc);
                }
    
                String urlString = hstResponse.createNavigationalURL(url.toString()).toString();
                
                hstResponse.sendRedirect(urlString);
            }
        }
    }


}
