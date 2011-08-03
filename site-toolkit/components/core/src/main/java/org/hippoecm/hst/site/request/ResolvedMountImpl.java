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
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedVirtualHost;
import org.hippoecm.hst.util.HstSiteMapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResolvedMountImpl implements ResolvedMount{


    private final static Logger log = LoggerFactory.getLogger(ResolvedMountImpl.class);
    
    private Mount mount;
    private ResolvedVirtualHost resolvedVirtualHost;
    private String resolvedMountPath;
    
    public ResolvedMountImpl(Mount mount, ResolvedVirtualHost resolvedVirtualHost, String resolvedMountPath){
        this.mount = mount;
        this.resolvedVirtualHost = resolvedVirtualHost;
        this.resolvedMountPath = resolvedMountPath;
    }
    
    public Mount getMount() {
        return mount;
    }

    public ResolvedVirtualHost getResolvedVirtualHost() {
        return resolvedVirtualHost;
    }
    
    public String getResolvedMountPath() {
        return resolvedMountPath;
    }
    
    public String getNamedPipeline() {
       return mount.getNamedPipeline();
    }

    public ResolvedSiteMapItem matchSiteMapItem(String siteMapPathInfo) throws MatchException {
        // test whether this Mount actually has a HstSite attached. If not, we return null as we can not match to a SiteMapItem when there is no HstSite object
        if(getMount().getHstSite() == null) {
            throw new MatchException("No HstSite attached to Mount '"+ getMount().getName()+"'. The path '"+siteMapPathInfo+"' thus not be matched to a sitemap item");
        }
        
        if(siteMapPathInfo == null) {
          throw new MatchException("SiteMapPathInfo is not allowed to be null");
        }
        
        if("".equals(siteMapPathInfo) || "/".equals(siteMapPathInfo)) {
           log.debug("siteMapPathInfo is '' or '/'. If there is a homepage path configured, we try to map this path to the sitemap");
           siteMapPathInfo = HstSiteMapUtils.getPath(mount, mount.getHomePage());
           if(siteMapPathInfo == null || "".equals(siteMapPathInfo) || "/".equals(siteMapPathInfo)) {
               log.warn("Mount '{}' for host '{}' does not have a homepage configured and the path info is empty. Cannot map to sitemap item. Return null", getMount().getName(), getResolvedVirtualHost().getResolvedHostName());
               throw new MatchException("No homepage configured and empty path after Mount");
           } else {
               log.debug("Trying to map homepage '{}' to the sitemap for Mount '{}'", siteMapPathInfo, getMount().getName());
           }
        }
        
        HstSiteMapMatcher matcher = getMount().getHstSiteMapMatcher();
        if(matcher == null) {
            throw new MatchException("The VirtualHostManager does not have a HstSiteMapMatcher configured. Cannot match request to a sitemap without this");        
        }
        ResolvedSiteMapItem item = null;
        try {
            item = matcher.match(siteMapPathInfo, this);
        } catch(NotFoundException e){
            log.debug("Cannot match '{}'. Try getting the pagenotfound", siteMapPathInfo);
            String pageNotFound = HstSiteMapUtils.getPath(mount, mount.getPageNotFound());
            if(pageNotFound == null) {
                throw new MatchException("There is no pagenotfound configured for '"+mount.getName()+"'");
            }
            // if pageNotFound cannot be matched, again a NotFoundException is thrown which extends MatchException so is allowed
            item = matcher.match(pageNotFound, this);
        }
        return item; 
    }
    
    public boolean isAuthenticated() {
        return mount.isAuthenticated();
    }
    
    public Set<String> getRoles() {
        return mount.getRoles();
    }
    
    public Set<String> getUsers() {
        return mount.getUsers();
    }
    
    public boolean isSubjectBasedSession() {
        return mount.isSubjectBasedSession();
    }
    
    public boolean isSessionStateful() {
        return mount.isSessionStateful();
    }
    
    public String getFormLoginPage() {
        return mount.getFormLoginPage();
    }
    
}
