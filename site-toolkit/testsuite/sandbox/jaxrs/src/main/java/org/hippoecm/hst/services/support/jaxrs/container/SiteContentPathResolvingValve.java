/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.hst.services.support.jaxrs.container;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.configuration.HstSite;
import org.hippoecm.hst.configuration.HstSitesManager;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.Valve;
import org.hippoecm.hst.core.container.ValveContext;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.MatchedMapping;
import org.hippoecm.hst.services.support.jaxrs.content.BaseHstContentService;

public class SiteContentPathResolvingValve implements Valve {
    
    protected Map<String, HstSitesManager> sitesManagers;
    
    public void setSitesManagers(Map<String, HstSitesManager> sitesManagers) {
        this.sitesManagers = sitesManagers;
    }
    
    public void initialize() throws ContainerException {
    }
    
    public void invoke(ValveContext context) throws ContainerException {
        HttpServletRequest servletRequest = context.getServletRequest();
        HstRequestContext requestContext = (HstRequestContext) servletRequest.getAttribute(ContainerConstants.HST_REQUEST_CONTEXT);
        MatchedMapping matchedMapping = requestContext.getMatchedMapping();
        HstSite hstSite = sitesManagers.get(context.getServletRequest().getServletPath()).getSites().getSite(matchedMapping.getSiteName());
        servletRequest.setAttribute(BaseHstContentService.SITE_CONTENT_PATH, hstSite.getContentPath());
        
        // continue
        context.invokeNext();
    }
    
    public void destroy() {
    }
    
}
