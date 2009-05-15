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
package org.hippoecm.hst.core.hosting;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.Configuration;
import org.hippoecm.hst.core.request.MatchedMapping;
import org.hippoecm.hst.service.AbstractJCRService;
import org.hippoecm.hst.service.Service;
import org.hippoecm.hst.service.ServiceException;
import org.hippoecm.hst.site.request.MatchedMappingImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VirtualHostsService extends AbstractJCRService implements VirtualHosts, Service {
    
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(VirtualHostsService.class);

    private final static String WILDCARD = "_default_";
    
    private Map<String, VirtualHostService> rootVirtualHosts = new HashMap<String, VirtualHostService>();
    private String defaultSiteName;
    private boolean virtualHostsConfigured;
    private String jcrPath;
    private boolean portVisible;
    private int portNumber;
    private boolean contextPathInUrl;
    private String[] prefixExclusions;
    private String[] suffixExclusions;

    /*
     * Constructor when running without configured virtual hosts
     */
    public VirtualHostsService(String defaultSiteName){
        super(null);
        this.defaultSiteName = defaultSiteName;
        this.virtualHostsConfigured = false;
    }
    
    public VirtualHostsService(Node virtualHostsNode) {
        super(virtualHostsNode);
        this.virtualHostsConfigured = true;
        this.jcrPath = this.getValueProvider().getPath();
        this.portNumber = this.getValueProvider().getLong(Configuration.VIRTUALHOSTS_PROPERTY_PORT).intValue();
        this.portVisible = this.getValueProvider().getBoolean(Configuration.VIRTUALHOSTS_PROPERTY_SHOWPORT);
        this.contextPathInUrl = this.getValueProvider().getBoolean(Configuration.VIRTUALHOSTS_PROPERTY_SHOWCONTEXTPATH);
        this.prefixExclusions = this.getValueProvider().getStrings(Configuration.VIRTUALHOSTS_PROPERTY_PREFIXEXCLUSIONS);
        this.suffixExclusions = this.getValueProvider().getStrings(Configuration.VIRTUALHOSTS_PROPERTY_SUFFIXEXCLUSIONS);
           
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
    
    public MatchedMapping findMapping(String hostName,String pathInfo) {
        if(!virtualHostsConfigured) {
            // return the default site + dummy MatchedMapping
            return getDefaultSiteNameMapping();
        }
        Mapping mapping = null;
        if(hostName == null || "".equals(hostName)) {
            log.warn("Cannot get a mapping for hostName '{}' which is empty");
            return null;
        }
        
        if(rootVirtualHosts != null) {
            String[] hostNameSegments = hostName.split("\\.");
            int position = hostNameSegments.length - 1;
           
            for(VirtualHostService virtualHost : rootVirtualHosts.values()) {
                // as there can be multiple root virtual hosts with the same name, the rootVirtualHosts are stored in the map
                // with their id, hence, we cannot get them directly, but have to test them all
                if(hostNameSegments[position].equals(virtualHost.getName())) {
                    VirtualHost host = traverseInToHost(virtualHost, hostNameSegments, position);
                    if(host == null) {
                        // try next root virtual host where the name matches
                        continue;
                    }
                    // now we have the matched host, try to find the mapping associated with it.
                    
                    mapping = host.getMapping(pathInfo);
                    if(mapping == null) {
                        // try another host
                        continue;
                    }
                    return new MatchedMappingImpl(mapping);
                    
                }
            }
        }
        log.warn("The host hostName '{}' and pathInfo '{}' cannot be matched for the configured virtual hosts");
        return null;
    }
    
    
    private MatchedMapping getDefaultSiteNameMapping() {
        MatchedMapping m = new MatchedMapping(){
            public String getSiteName() {
                return VirtualHostsService.this.defaultSiteName;
            }
            public Mapping getMapping() {
                return null;
            }
            public boolean isURIMapped() {
                return false;
            }
            public String mapToExternalURI(String pathInfo) {
                return pathInfo;
            }

            public String mapToInternalURI(String pathInfo) {
                return pathInfo;
            }
        };
        return m;
    }

    private VirtualHost traverseInToHost(VirtualHost matchedHost, String[] hostNameSegments, int position) {
        if(position == 0) {
            return matchedHost;
        }
        
        --position;
        
        VirtualHost vhost = matchedHost.getChildHost(hostNameSegments[position]);
        if(vhost == null) {
            if( (vhost = matchedHost.getChildHost(WILDCARD)) != null) {
                return vhost;
            }
        } else {
            return traverseInToHost(vhost, hostNameSegments, position);
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
               log.warn("Unable to initialize VirtualHost for '{}'. Skipping. {}", virtualHostNode.getPath(), e);
           }
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

}
