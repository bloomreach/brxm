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

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.hosting.MatchException;
import org.hippoecm.hst.configuration.hosting.PortMount;
import org.hippoecm.hst.configuration.hosting.SiteMount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.core.request.ResolvedSiteMount;
import org.hippoecm.hst.core.request.ResolvedVirtualHost;
import org.hippoecm.hst.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResolvedVirtualHostImpl implements ResolvedVirtualHost{

    private final static Logger log = LoggerFactory.getLogger(ResolvedVirtualHostImpl.class);
    
    private VirtualHost virtualHost;
    
    private String hostName;
    private int portNumber;
    
    private String pathSuffixDelimiter;
    
    public ResolvedVirtualHostImpl(VirtualHost virtualHost, String hostName, int portNumber) {
        this.virtualHost = virtualHost;
        this.hostName = hostName;
        this.portNumber = portNumber;
    }

    public VirtualHost getVirtualHost() {
        return virtualHost;
    }
    
    public ResolvedSiteMount matchSiteMount(String contextPath, String requestPath) throws MatchException {
        PortMount portMount = virtualHost.getPortMount(portNumber);
        if(portMount == null && portNumber != 0) {
            log.debug("Could not match the request to port '{}'. If there is a default port '0', we'll try this one");
            portMount = virtualHost.getPortMount(0);
            if(portMount == null) {
                log.warn("Virtual Host '{}' is not (correctly) mounted for portnumber '{}': We cannot return a ResolvedSiteMount. Return null", virtualHost.getHostName(), String.valueOf(portNumber));
                return null;
            }
        }
        
        if(portMount.getRootSiteMount() == null) {
            log.warn("Virtual Host '{}' for portnumber '{}' is not (correctly) mounted: We cannot return a ResolvedSiteMount. Return null", virtualHost.getHostName(), String.valueOf(portNumber)); 
            return null;
        }
        
        SiteMount siteMount = portMount.getRootSiteMount();
        
        String mountPath = PathUtils.normalizePath(requestPath);
        String pathSuffix = null;
        
        if (pathSuffixDelimiter == null) {
            pathSuffixDelimiter = siteMount.getPathSuffixDelimiter();
        }
        
        String [] mountPathAndPathSuffix = StringUtils.splitByWholeSeparatorPreserveAllTokens(requestPath, pathSuffixDelimiter, 2);
        if (mountPathAndPathSuffix != null && mountPathAndPathSuffix.length > 1) {
            // strip leading and trailing slashes
            mountPath = PathUtils.normalizePath(mountPathAndPathSuffix[0]);
            pathSuffix = PathUtils.normalizePath(mountPathAndPathSuffix[1]);
        }
        
        String[] requestPathSegments = mountPath.split("/");
        int position = 0;
        
        while(position < requestPathSegments.length) {
            if(siteMount.getChildMount(requestPathSegments[position]) != null) {
                siteMount = siteMount.getChildMount(requestPathSegments[position]);
            } else {
                // we're done: we have the deepest site mount
                break;
            }
            position++;
        }
        
        // ensure "valid" matching ROOT contextPath which could be derived as "" -> turn it into "/"
        if (contextPath != null && contextPath.length() == 0) {
        	contextPath = "/";
        }
        
        // let's find a siteMount that has a valid 'onlyForContextPath' : if onlyForContextPath is not null && not equal to the contextPath, we need to try the parent sitemount until we have a valid one or have a sitemount that is null
        while(siteMount != null && contextPath != null && (siteMount.onlyForContextPath() != null && !siteMount.onlyForContextPath().equals(contextPath) )) {
            log.debug("SiteMount '{}' cannot be used because the contextPath '{}' is not valid for this siteMount, because it is only for context path. Let's try parent siteMounts if present.'"+siteMount.onlyForContextPath()+"' ", siteMount.getName(), contextPath);
            siteMount = siteMount.getParent();
        }
        
        if(siteMount == null) {
            log.warn("Virtual Host '{}' is not (correctly) mounted for portnumber '{}': We cannot return a ResolvedSiteMount. Return null", virtualHost.getHostName(), String.valueOf(portMount.getPortNumber()));
            return null;
        }
        
        
        // reconstruct the prefix that needs to be stripped of from the request because it belongs to the site mount
        // we thus create the resolvedPathInfoPrefix
        StringBuilder builder = new StringBuilder();
        while(position > 0) {
            builder.insert(0,requestPathSegments[--position]).insert(0,"/");
           
        }
        String resolvedMountPath = builder.toString();
        
        ResolvedSiteMount resolvedSiteMount = new ResolvedSiteMountImpl(siteMount, this, resolvedMountPath, pathSuffix);
        log.debug("Found ResolvedSiteMount is '{}' and the mount prefix for it is :", resolvedSiteMount.getResolvedMountPath());
        
        return resolvedSiteMount;
    }

    public String getResolvedHostName() {
        return hostName;
    }
    
    public int getPortNumber() {
    	return portNumber;
    }

}
