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
package org.hippoecm.hst.core.container;

import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.configuration.HstSite;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.domain.DomainMapping;
import org.hippoecm.hst.core.request.HstMatchedSiteMapItem;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.HstSiteMapMatcher.MatchResult;
import org.hippoecm.hst.site.request.HstRequestContextImpl;

public class ContextResolvingValve extends AbstractValve
{
    
    @Override
    public void invoke(ValveContext context) throws ContainerException
    {
        HttpServletRequest servletRequest = (HttpServletRequest) context.getServletRequest();
        HstRequestContext requestContext = (HstRequestContext) servletRequest.getAttribute(HstRequestContext.class.getName());
        
        String domainName = servletRequest.getServerName();
        DomainMapping domainMapping = this.domainMappings.findDomainMapping(domainName);
        
        if (domainMapping == null) {
            throw new ContainerException("No domain mapping for " + domainName);
        }

        String siteName = domainMapping.getSiteName();
        HstSite hstSite = this.hstSites.getSite(siteName);
        
        if (hstSite == null) {
            throw new ContainerException("No site found for " + siteName);
        }
        
        String pathInfo = servletRequest.getPathInfo();
        
        MatchResult matchResult = null;
        
        try {
            matchResult = this.siteMapMatcher.match(pathInfo, hstSite);
        } catch (Exception e) {
            throw new ContainerException("No match for " + pathInfo, e);
        }
        
        if (matchResult == null) {
            throw new ContainerException("No match for " + pathInfo);
        }
        
        final String remainder = matchResult.getRemainder();
        final HstSiteMapItem hstSiteMapItem = matchResult.getSiteMapItem();
        
        ((HstRequestContextImpl) requestContext).setMatchedSiteMapItem(new HstMatchedSiteMapItem(){

            public String getRemainder() {
                return remainder;
            }

            public HstSiteMapItem getSiteMapItem() {
                return hstSiteMapItem;
            }
            
        }
        );
        
        HstComponentConfiguration rootComponentConfig = matchResult.getCompontentConfiguration();
        
        try {
            HstComponentWindow rootComponentWindow = getComponentWindowFactory().create(context.getServletConfig(), requestContext, rootComponentConfig, getComponentFactory());
            context.setRootComponentWindow(rootComponentWindow);
        } catch (Exception e) {
            throw new ContainerException("Failed to create component window for the configuration, " + rootComponentConfig.getId(), e);
        }
        
        // continue
        context.invokeNext();
    }
    
}
