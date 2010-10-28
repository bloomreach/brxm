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

import java.util.Set;

import org.hippoecm.hst.configuration.hosting.MatchException;
import org.hippoecm.hst.configuration.hosting.NotFoundException;
import org.hippoecm.hst.configuration.hosting.SiteMount;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.request.ResolvedSiteMount;
import org.hippoecm.hst.core.request.ResolvedVirtualHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResolvedSiteMountImpl implements ResolvedSiteMount{


    private final static Logger log = LoggerFactory.getLogger(ResolvedSiteMountImpl.class);
    
    private SiteMount siteMount;
    private ResolvedVirtualHost resolvedVirtualHost;
    private String resolvedMountPath;
    
    public ResolvedSiteMountImpl(SiteMount siteMount, ResolvedVirtualHost resolvedVirtualHost, String resolvedMountPath){
        this.siteMount = siteMount;
        this.resolvedVirtualHost = resolvedVirtualHost;
        this.resolvedMountPath = resolvedMountPath;
    }
    
    public SiteMount getSiteMount() {
        return siteMount;
    }

    public ResolvedVirtualHost getResolvedVirtualHost() {
        return resolvedVirtualHost;
    }
    
    public String getResolvedMountPath() {
        return resolvedMountPath;
    }
    
    public String getNamedPipeline() {
       return siteMount.getNamedPipeline();
    }

    public ResolvedSiteMapItem matchSiteMapItem(String siteMapPathInfo) throws MatchException {
        // test whether this SiteMount actually has a HstSite attached. If not, we return null as we can not match to a SiteMapItem when there is no HstSite object
        if(getSiteMount().getHstSite() == null) {
            throw new MatchException("No HstSite attached to SiteMount '"+ getSiteMount().getName()+"'. The path '"+siteMapPathInfo+"' thus not be matched to a sitemap item");
        }
        
        if(siteMapPathInfo == null) {
          throw new MatchException("SiteMapPathInfo is not allowed to be null");
        }
        
        if("".equals(siteMapPathInfo) || "/".equals(siteMapPathInfo)) {
           log.debug("siteMapPathInfo is '' or '/'. If there is a homepage path configured, we try to map this path to the sitemap");
           siteMapPathInfo = siteMount.getHomePage();
           if(siteMapPathInfo == null || "".equals(siteMapPathInfo) || "/".equals(siteMapPathInfo)) {
               log.warn("SiteMount '{}' for host '{}' does not have a homepage configured and the path info is empty. Cannot map to sitemap item. Return null", getSiteMount().getName(), getResolvedVirtualHost().getResolvedHostName());
               throw new MatchException("No homepage configured and empty path after sitemount");
           } else {
               log.debug("Trying to map homepage '{}' to the sitemap for SiteMount '{}'", siteMapPathInfo, getSiteMount().getName());
           }
        }
        
        HstSiteMapMatcher matcher = getSiteMount().getHstSiteMapMatcher();
        if(matcher == null) {
            throw new MatchException("The VirtualHostManager does not have a HstSiteMapMatcher configured. Cannot match request to a sitemap without this");        
        }
        ResolvedSiteMapItem item = null;
        try {
            item = matcher.match(siteMapPathInfo, this);
        } catch(NotFoundException e){
            log.debug("Cannot match '{}'. Try getting the pagenotfound", siteMapPathInfo);
            String pageNotFound = siteMount.getPageNotFound();
            if(pageNotFound == null) {
                throw new MatchException("There is no pagenotfound configured for '"+siteMount.getName()+"'");
            }
            // if pageNotFound cannot be matched, again a NotFoundException is thrown which extends MatchException so is allowed
            item = matcher.match(pageNotFound, this);
        }
        return item; 
    }
    
    public boolean isSecured() {
        return siteMount.isSecured();
    }
    
    public Set<String> getRoles() {
        return siteMount.getRoles();
    }
    
    public Set<String> getUsers() {
        return siteMount.getUsers();
    }
    
    public boolean isSubjectBasedSession() {
        return siteMount.isSubjectBasedSession();
    }
    
    public boolean isSessionStateful() {
        return siteMount.isSessionStateful();
    }

}
