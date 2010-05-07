/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.hst.site.request;

import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.configuration.hosting.MatchException;
import org.hippoecm.hst.configuration.hosting.SiteMount;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.request.ResolvedSiteMount;
import org.hippoecm.hst.core.request.ResolvedVirtualHost;
import org.hippoecm.hst.util.HstRequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResolvedSiteMountImpl implements ResolvedSiteMount{


    private final static Logger log = LoggerFactory.getLogger(ResolvedSiteMountImpl.class);
    
    private SiteMount siteMount;
    private ResolvedVirtualHost resolvedVirtualHost;
    private String resolvedMountPrefix;
    
    
    public ResolvedSiteMountImpl(SiteMount siteMount, ResolvedVirtualHost resolvedVirtualHost, String resolvedPathInfoPrefix){
        this.siteMount = siteMount;
        this.resolvedVirtualHost = resolvedVirtualHost;
        this.resolvedMountPrefix = resolvedPathInfoPrefix;
    }
    
    public SiteMount getSiteMount() {
        return siteMount;
    }

    public ResolvedVirtualHost getResolvedVirtualHost() {
        return resolvedVirtualHost;
    }
    
    public String getResolvedMountPrefix() {
        return resolvedMountPrefix;
    }

    public String getNamedPipeline() {
       return siteMount.getNamedPipeline();
    }


    public ResolvedSiteMapItem matchSiteMapItem(HttpServletRequest request) throws MatchException {
        String requestPath = HstRequestUtils.getRequestPath(request);
        if(!requestPath.startsWith(getResolvedMountPrefix())) {
            throw new MatchException("It is not allowed that the pathInfo from the request is different then the resolvedPathInfoPrefix from the ResolvedSiteMount");
        }
        String siteMapPathInfo = requestPath.substring(getResolvedMountPrefix().length());
        
        if("".equals(siteMapPathInfo) || "/".equals(siteMapPathInfo)) {
           log.debug("siteMapPathInfo is '' or '/'. If there is a homepage path configured, we try to map this path to the sitemap");
           siteMapPathInfo = siteMount.getHomePage();
           if(siteMapPathInfo == null || "".equals(siteMapPathInfo)) {
               log.warn("SiteMount '{}' for host '{}' does not have a homepage configured and the pathInfo is empty. Cannot map to sitemap item. Return null", getSiteMount().getName(), getResolvedVirtualHost().getResolvedHostName());
               return null;
           } else {
               log.debug("Trying to map homepage '{}' to the sitemap for SiteMount '{}'", siteMapPathInfo, getSiteMount().getName());
           }
        }
        
        HstSiteMapMatcher matcher = getSiteMount().getHstSiteMapMatcher();
        if(matcher == null) {
            throw new MatchException("The VirtualHostManager does not have a HstSiteMapMatcher configured. Cannot match request to a sitemap without this");        
        }
        return matcher.match(siteMapPathInfo, this);
       
    }

}
