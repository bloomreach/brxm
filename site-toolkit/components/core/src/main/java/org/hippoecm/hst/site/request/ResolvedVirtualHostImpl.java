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
package org.hippoecm.hst.site.request;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.hosting.MatchException;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.PortMount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedVirtualHost;
import org.hippoecm.hst.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ResolvedVirtualHostImpl
 * @version $Id$
 */
public class ResolvedVirtualHostImpl implements ResolvedVirtualHost {

    private final static Logger log = LoggerFactory.getLogger(ResolvedVirtualHostImpl.class);
    
    private VirtualHost virtualHost;
    private PortMount portMount;
    
    private String hostName;
    
    public ResolvedVirtualHostImpl(VirtualHost virtualHost, String hostName, PortMount portMount) {
        this.virtualHost = virtualHost;
        this.hostName = hostName;
        this.portMount = portMount;
    }

    public VirtualHost getVirtualHost() {
        return virtualHost;
    }

    public ResolvedMount matchMount(String contextPath, String requestPath) throws MatchException {
        
        if(portMount.getRootMount() == null) {
            log.error("Virtual Host '{}' for portnumber '{}' is not (correctly) mounted: We cannot return a ResolvedMount. Return null", virtualHost.getHostName(), String.valueOf(portMount.getPortNumber())); 
            return null;
        }
        
        // strip leading and trailing slashes
        String path = PathUtils.normalizePath(requestPath);
        
        String matchingIgnoredPrefix = null;
        // check whether the requestPath starts with the cmsPreviewPrefix path: If so, first strip this prefix off and append it later to the resolvedMountPath
        if(!StringUtils.isEmpty(virtualHost.getVirtualHosts().getCmsPreviewPrefix())) {
           if (path.equals(virtualHost.getVirtualHosts().getCmsPreviewPrefix())) {
               matchingIgnoredPrefix = virtualHost.getVirtualHosts().getCmsPreviewPrefix();
               path = "";
           } else if (path.startsWith(virtualHost.getVirtualHosts().getCmsPreviewPrefix() + "/")){
               matchingIgnoredPrefix = virtualHost.getVirtualHosts().getCmsPreviewPrefix();
               path = path.substring(virtualHost.getVirtualHosts().getCmsPreviewPrefix().length() +1);
           }
        }
        
        String[] requestPathSegments = path.split("/");

        int position = 0;
        
        Mount mount = portMount.getRootMount();
        
        while(position < requestPathSegments.length) {
            if(mount.getChildMount(requestPathSegments[position]) != null) {
                mount = mount.getChildMount(requestPathSegments[position]);
            } else {
                // we're done: we have the deepest Mount
                break;
            }
            position++;
        }
        
        if(contextPath != null && mount.getContextPath() != null) {
            // Check if the Mount that has a valid contextpath
            while(mount != null && contextPath != null && (mount.getContextPath() != null && !mount.getContextPath().equals(contextPath) )) {
                log.debug("Mount '{}' cannot be used because the contextPath '{}' is not valid for this Mount, because it is only for context path. Try parent Mount's if present.'"
                        +mount.getContextPath()+"' ", mount.getName(), contextPath);
                mount = mount.getParent();
            }
        }
        
        if(mount == null) {
            log.warn("Virtual Host '{}' is not (correctly) mounted for portnumber '{}' and contextpath '{}': " +
                    "We cannot return a ResolvedMount. Return null",
                    virtualHost.getHostName(), String.valueOf(portMount.getPortNumber()), contextPath);
            return null;
        }
        
        
        // reconstruct the prefix that needs to be stripped of from the request because it belongs to the Mount
        // we thus create the resolvedPathInfoPrefix
        StringBuilder builder = new StringBuilder();
        while(position > 0) {
            builder.insert(0,requestPathSegments[--position]).insert(0,"/");
        }
        String resolvedMountPath = builder.toString();
        
        ResolvedMount resolvedMount = new ResolvedMountImpl(mount, this , resolvedMountPath, matchingIgnoredPrefix, portMount.getPortNumber());
        log.debug("Found ResolvedMount is '{}' and the mount prefix for it is :", resolvedMount.getResolvedMountPath());
        
        return resolvedMount;
    }

    @Deprecated
    public String getResolvedHostName() {
        return hostName;
    }

    @Deprecated
    public int getPortNumber() {
    	return portMount.getPortNumber();
    }

}
