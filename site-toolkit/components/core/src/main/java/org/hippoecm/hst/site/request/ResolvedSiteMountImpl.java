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
import org.hippoecm.hst.configuration.hosting.VirtualHostsManager;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.request.ResolvedSiteMount;
import org.hippoecm.hst.core.request.ResolvedVirtualHost;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.util.HstRequestUtils;

public class ResolvedSiteMountImpl implements ResolvedSiteMount{

    private SiteMount siteMount;
    private ResolvedVirtualHost resolvedVirtualHost;
    private String resolvedPathInfoPrefix;
    
    
    public ResolvedSiteMountImpl(SiteMount siteMount, ResolvedVirtualHost resolvedVirtualHost, String resolvedPathInfoPrefix){
        this.siteMount = siteMount;
        this.resolvedVirtualHost = resolvedVirtualHost;
        this.resolvedPathInfoPrefix = resolvedPathInfoPrefix;
    }
    
    public SiteMount getSiteMount() {
        return siteMount;
    }

    public ResolvedVirtualHost getResolvedVirtualHost() {
        return resolvedVirtualHost;
    }
    
    public String getResolvedPathInfoPrefix() {
        return resolvedPathInfoPrefix;
    }

    public String getNamedPipeline() {
       return siteMount.getNamedPipeline();
    }


    public ResolvedSiteMapItem matchSiteMapItem(HttpServletRequest request) throws MatchException {
        String pathInfo = HstRequestUtils.getPathInfo(request);
        if(!pathInfo.startsWith(getResolvedPathInfoPrefix())) {
            throw new MatchException("It is not allowed that the pathInfo from the request is different then the resolvedPathInfoPrefix from the ResolvedSiteMount");
        }
        String siteMapPathInfo = pathInfo.substring(getResolvedPathInfoPrefix().length());
        
        HstSiteMapMatcher matcher = getSiteMount().getHstSiteMapMatcher();
        if(matcher == null) {
            throw new MatchException("The VirtualHostManager does not have a HstSiteMapMatcher configured. Cannot match request to a sitemap without this");        
        }
        return matcher.match(siteMapPathInfo, this);
       
    }

}
