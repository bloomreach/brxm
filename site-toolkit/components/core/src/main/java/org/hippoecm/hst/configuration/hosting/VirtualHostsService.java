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
package org.hippoecm.hst.configuration.hosting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.request.ResolvedSiteMount;
import org.hippoecm.hst.core.request.ResolvedVirtualHost;
import org.hippoecm.hst.service.AbstractJCRService;
import org.hippoecm.hst.service.Service;
import org.hippoecm.hst.service.ServiceException;
import org.hippoecm.hst.site.request.ResolvedVirtualHostImpl;
import org.hippoecm.hst.util.HstRequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VirtualHostsService extends AbstractJCRService implements VirtualHosts, Service {
    
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(VirtualHostsService.class);

    private final static String WILDCARD = "_default_";
    
    public final static String DEFAULT_SCHEME = "http";

    private VirtualHostsManager virtualHostsManager;
    private Map<String, VirtualHostService> rootVirtualHosts = new HashMap<String, VirtualHostService>();
    private List<VirtualHost> allVirtualHosts = new ArrayList<VirtualHost>();
    private String defaultHostName;
    private boolean virtualHostsConfigured;
    private String jcrPath;
    private boolean portVisible;
    private int portNumber;
    private String scheme;
    private boolean contextPathInUrl;
    private String[] prefixExclusions;
    private String[] suffixExclusions;

  
    public VirtualHostsService(Node virtualHostsNode, VirtualHostsManager virtualHostsManager) {
        super(virtualHostsNode);
        this.virtualHostsManager = virtualHostsManager;
        this.virtualHostsConfigured = true;
        this.jcrPath = this.getValueProvider().getPath();
        this.portNumber = this.getValueProvider().getLong(HstNodeTypes.VIRTUALHOSTS_PROPERTY_PORT).intValue();
        this.portVisible = this.getValueProvider().getBoolean(HstNodeTypes.VIRTUALHOSTS_PROPERTY_SHOWPORT);
        this.contextPathInUrl = this.getValueProvider().getBoolean(HstNodeTypes.VIRTUALHOSTS_PROPERTY_SHOWCONTEXTPATH);
        this.prefixExclusions = this.getValueProvider().getStrings(HstNodeTypes.VIRTUALHOSTS_PROPERTY_PREFIXEXCLUSIONS);
        this.suffixExclusions = this.getValueProvider().getStrings(HstNodeTypes.VIRTUALHOSTS_PROPERTY_SUFFIXEXCLUSIONS);
        this.scheme = this.getValueProvider().getString(HstNodeTypes.VIRTUALHOSTS_PROPERTY_SCHEME);
        this.defaultHostName  = this.getValueProvider().getString(HstNodeTypes.VIRTUALHOSTS_PROPERTY_DEFAULTHOSTNAME);
        if(scheme == null || "".equals(scheme)) {
            this.scheme = DEFAULT_SCHEME;
        }
        try {
            init(virtualHostsNode);
        } catch (RepositoryException e) {
            log.error("Failed to inialize hosts for '{}' : {}", jcrPath, e);
        }
        /*
         * After initialization, all needed jcr properties and nodes have to be loaded. The underlying jcr nodes in 
         * the value providers now will all be closed.
         */
        this.closeValueProvider(true);
    }
    


    public VirtualHostsManager getVirtualHostsManager() {
        return virtualHostsManager;
    }

    
    
    public boolean isExcluded(String pathInfo) {
        // test prefix
        if(prefixExclusions != null) {
            for(String excludePrefix : prefixExclusions) {
                if(pathInfo.startsWith(excludePrefix)) {
                    return true;
                }
            }
        }
        // test suffix
        if(suffixExclusions != null) {
            for(String excludeSuffix : suffixExclusions) {
                if(pathInfo.endsWith(excludeSuffix)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    
    public ResolvedSiteMapItem matchSiteMapItem(HttpServletRequest request)  throws MatchException{
        
        ResolvedVirtualHost resolvedVirtualHost = matchVirtualHost(request);
        if(resolvedVirtualHost == null) {
            // logging is already done when it is null
            return null;
        }
        ResolvedSiteMount resolvedSiteMount  = resolvedVirtualHost.matchSiteMountItem(request);
        if(resolvedSiteMount == null) {
            // logging is already done when it is null
            return null;
        }
        return resolvedSiteMount.matchSiteMapItem(request);
    }

    public ResolvedSiteMount matchSiteMountItem(HttpServletRequest request) throws MatchException {
        ResolvedVirtualHost resolvedVirtualHost = matchVirtualHost(request);
        ResolvedSiteMount resolvedSiteMount = null;
        if(resolvedVirtualHost != null) {
            resolvedSiteMount  = resolvedVirtualHost.matchSiteMountItem(request);
        }
        return resolvedSiteMount;
    }
    
    public ResolvedVirtualHost matchVirtualHost(HttpServletRequest request) throws MatchException {
        if(!virtualHostsConfigured) {
            throw new MatchException("No correct virtual hosts configured. Cannot continue request");
        }
        
        String requestServerName = HstRequestUtils.getRequestServerName(request);
        ResolvedVirtualHost host = matchVirtualHost(requestServerName);
        
        // no host found. Let's try the default host, if there is one configured:
        if(host == null && this.getDefaultHostName() != null) {
            log.debug("Cannot find a mapping for servername '{}'. We try the default servername '{}'", requestServerName, this.getDefaultHostName());
            host = matchVirtualHost(this.getDefaultHostName());
        }
        if(host == null) {
           log.warn("We cannot find a servername mapping for '{}'. Even the default servername '{}' cannot be found. Return null", requestServerName, this.getDefaultHostName());
          
        }
        return host;
    }
    
    
    /**
     * Override this method if you want a different algorithm to resolve hostName
     * @param hostName
     * @param host
     * @return the matched virtual host
     */
    protected ResolvedVirtualHost matchVirtualHost(String hostName) {
        for(VirtualHostService virtualHost : rootVirtualHosts.values()) {
            // as there can be multiple root virtual hosts with the same name, the rootVirtualHosts are stored in the map
            // with their id, hence, we cannot get them directly, but have to test them all
            String[] requestServerNameSegments = hostName.split("\\.");
            int depth = requestServerNameSegments.length - 1;
            if(requestServerNameSegments[depth].equals(virtualHost.getName())) {
               VirtualHost host = traverseInToHost(virtualHost, requestServerNameSegments, depth);
               if(host == null) {
                   return null;
               }
               return new ResolvedVirtualHostImpl(host, hostName);
            }
        }
        return null;
    }
    
    /**
     * Override this method if you want a different algorithm to resolve requestServerName
     * @param matchedHost
     * @param hostNameSegments
     * @param depth
     * @return
     */
    protected VirtualHost traverseInToHost(VirtualHost matchedHost, String[] hostNameSegments, int depth) {
        if(depth == 0) {
           return matchedHost;
        }
        
        --depth;
        
        VirtualHost vhost = matchedHost.getChildHost(hostNameSegments[depth]);
        if(vhost == null) {
            if( (vhost = matchedHost.getChildHost(WILDCARD)) != null) {
                return vhost;
            }
        } else {
            return traverseInToHost(vhost, hostNameSegments, depth);
        }
        return null;
    }

    private void init(Node virtualHostsNode) throws RepositoryException {
       NodeIterator nodes = virtualHostsNode.getNodes();
       while(nodes.hasNext()) {
           Node virtualHostNode = nodes.nextNode();
           if(virtualHostNode == null) {continue;}
           try {
               VirtualHostService virtualHost = new VirtualHostService(this, virtualHostNode, (VirtualHostService)null);
               this.rootVirtualHosts.put(virtualHost.getId(), virtualHost);
           } catch (ServiceException e) {
               log.warn("Unable to initialize VirtualHost for '{}'. Skipping. {}", virtualHostNode.getPath(), e.getMessage());
           }
       }
       
       // populate the allVirtualHosts 
       for(VirtualHost host : rootVirtualHosts.values()) {
           populateAllVHosts(host);
       }
       
    }

    private void populateAllVHosts(VirtualHost host) {
        allVirtualHosts.add(host);
        for(VirtualHost childHost : host.getChildHosts()) {
            populateAllVHosts(childHost);
        }
    }

    public Service[] getChildServices() {
        return rootVirtualHosts.values().toArray(new Service[rootVirtualHosts.values().size()]);
    }
 

    public boolean isPortVisible() {
        return portVisible;
    }


    public int getPortNumber() {
        return portNumber;
    }

    public boolean isContextPathInUrl() {
        return contextPathInUrl;
    }

    public String getScheme(){
        return this.scheme;
    }

    public String getDefaultHostName() {
        return this.defaultHostName;
    }

    public List<VirtualHost> getVirtualHosts(boolean mountedOnly) {
        if(mountedOnly) {
            List<VirtualHost> mountedHosts = new ArrayList<VirtualHost>();
            for(VirtualHost host :allVirtualHosts) {
                if(host.getRootSiteMount() != null){
                    mountedHosts.add(host);
                }
            }
            return mountedHosts;
        } else {
            return allVirtualHosts;
        }
    }
}
