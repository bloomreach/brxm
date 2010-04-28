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

import org.hippoecm.hst.configuration.hosting.SiteMount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.core.request.ResolvedSiteMount;
import org.hippoecm.hst.core.request.ResolvedVirtualHost;
import org.hippoecm.hst.util.HstRequestUtils;
import org.hippoecm.hst.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResolvedVirtualHostImpl implements ResolvedVirtualHost{

    private final static Logger log = LoggerFactory.getLogger(ResolvedVirtualHostImpl.class);
    
    private VirtualHost virtualHost;
    
    private String hostName;
    
    public ResolvedVirtualHostImpl(VirtualHost virtualHost, String hostName) {
        this.virtualHost = virtualHost;
        this.hostName = hostName;
    }

    public VirtualHost getVirtualHost() {
        return virtualHost;
    }

    public ResolvedSiteMount matchSiteMountItem(HttpServletRequest request) {
        SiteMount siteMount = virtualHost.getRootSiteMount();
        if(siteMount == null) {
            log.debug("Virtual Host '{}' is not mounted: We cannot return a ResolvedSiteMount. Return null", virtualHost.getHostName());
        }
        String pathInfo = HstRequestUtils.getPathInfo(request);
        // strip leading and trailing slashes
        pathInfo = PathUtils.normalizePath(pathInfo);
        String[] pathInfoSegments = pathInfo.split("/");
        int position = 0;
        while(position < pathInfoSegments.length) {
            if(siteMount.getChildMount(pathInfoSegments[position]) != null) {
                siteMount = siteMount.getChildMount(pathInfoSegments[position]);
            } else {
                // we're done: we have the deepest site mount
                break;
            }
            position++;
        }
        
        // reconstruct the prefix that needs to be stripped of from the request because it belongs to the site mount
        // we thus create the resolvedPathInfoPrefix
        StringBuilder builder = new StringBuilder();
        while(position > 0) {
            builder.insert(0,pathInfoSegments[--position]).insert(0,"/");
           
        }
        
        String resolvedPathInfoPrefix = builder.toString();
        ResolvedSiteMount resolvedSiteMount = new ResolvedSiteMountImpl(siteMount, this , resolvedPathInfoPrefix);
        log.debug("Found ResolvedSiteMount is '{}' and the prefix path for it is :", resolvedSiteMount.getResolvedPathInfoPrefix());
        
        return resolvedSiteMount;
    }

    public String getResolvedHostName() {
        return hostName;
    }

}
