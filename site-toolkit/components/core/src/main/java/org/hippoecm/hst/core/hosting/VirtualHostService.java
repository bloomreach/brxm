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
import org.hippoecm.hst.service.AbstractJCRService;
import org.hippoecm.hst.service.Service;
import org.hippoecm.hst.service.ServiceException;
import org.hippoecm.hst.util.HstRequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VirtualHostService extends AbstractJCRService implements VirtualHost, Service {
    
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(VirtualHostService.class);
    
    private Map<String, VirtualHostService> childVirtualHosts = new HashMap<String, VirtualHostService>();
   
    private String id;
    private String name;
    private VirtualHosts virtualHosts;
    private VirtualHostService parentHost;
    private SiteMount rootSiteMount;
    
    private String jcrPath;
    private boolean portVisible;
    // default mounted is false
    private boolean mounted = false;
    private int portNumber;
    private boolean contextPathInUrl;
    private String scheme;

    public VirtualHostService(VirtualHostsService virtualHosts,Node virtualHostNode, VirtualHostService parentHost) throws ServiceException {
        super(virtualHostNode);

        this.parentHost = parentHost;
        this.virtualHosts = virtualHosts;
        this.id = this.getValueProvider().getPath();
        this.jcrPath = this.id;
        
        if(this.getValueProvider().hasProperty(HstNodeTypes.VIRTUALHOST_PROPERTY_PORT)) {
            this.portNumber = this.getValueProvider().getLong(HstNodeTypes.VIRTUALHOST_PROPERTY_PORT).intValue();
        } else {
            // try to get the one from the parent
            if(parentHost != null) {
                this.portNumber = parentHost.portNumber;
            } else {
                this.portNumber = virtualHosts.getPortNumber();
            }
        }
        if(this.getValueProvider().hasProperty(HstNodeTypes.VIRTUALHOST_PROPERTY_SHOWPORT)) {
            this.portVisible = this.getValueProvider().getBoolean(HstNodeTypes.VIRTUALHOST_PROPERTY_SHOWPORT);
        } else {
            // try to get the one from the parent
            if(parentHost != null) {
                this.portVisible = parentHost.portVisible;
            } else {
                this.portVisible = virtualHosts.isPortVisible();
            }
        }
        
        if(this.getValueProvider().hasProperty(HstNodeTypes.VIRTUALHOST_PROPERTY_SHOWCONTEXTPATH)) {
            this.contextPathInUrl = this.getValueProvider().getBoolean(HstNodeTypes.VIRTUALHOST_PROPERTY_SHOWCONTEXTPATH);
        } else {
            // try to get the one from the parent
            if(parentHost != null) {
                this.contextPathInUrl = parentHost.contextPathInUrl;
            } else {
                this.contextPathInUrl = virtualHosts.isContextPathInUrl();
            }
        }
        
        if(this.getValueProvider().hasProperty(HstNodeTypes.VIRTUALHOST_PROPERTY_SCHEME)) {
            this.scheme = this.getValueProvider().getString(HstNodeTypes.VIRTUALHOST_PROPERTY_SCHEME);
            if(this.scheme == null || "".equals(this.scheme)) {
                this.scheme = VirtualHostsService.DEFAULT_SCHEME;
            }
        } else {
           // try to get the one from the parent
            if(parentHost != null) {
                this.scheme = parentHost.scheme;
            } else {
                this.scheme = virtualHosts.getScheme();
            }
        }
        
        String fullName = this.getValueProvider().getName();
        String[] nameSegments = fullName.split("\\.");
        
        VirtualHostService attachSiteMountToHost = this;
        
        if(nameSegments.length > 1) {
            // if the fullName is for example www.hippoecm.org, then this items name is 'org', its child is hippoecm, and
            // the last child is 'www'
            this.name = nameSegments[nameSegments.length - 1];
            // add child host services
            int depth = nameSegments.length - 2;
            if(depth > -1 ) {
                VirtualHostService childHost = new VirtualHostService(this, nameSegments, depth);
                this.childVirtualHosts.put(childHost.name, childHost);
                // we need to switch the attachSiteMountToHost to the last host
            }
            while(depth > -1) {
                if(attachSiteMountToHost == null) {
                    throw new ServiceException("Something went wrong because attachSiteMountToHost should never be possible to be null");
                }
                attachSiteMountToHost = (VirtualHostService)attachSiteMountToHost.getChildHost(nameSegments[depth]);
                depth--;
            }
        } else {
            this.name = this.getValueProvider().getName();
        }
        
        try {
            if(virtualHostNode.hasNode(HstNodeTypes.SITEMOUNT_HST_ROOTNAME)) {
                // we have a configured root sitemount node. Let's populate this sitemount for this host
                Node siteMount = virtualHostNode.getNode(HstNodeTypes.SITEMOUNT_HST_ROOTNAME);
                if(siteMount.isNodeType(HstNodeTypes.NODETYPE_HST_SITEMOUNT)) {
                    attachSiteMountToHost.rootSiteMount = new SiteMountService(siteMount, null, attachSiteMountToHost);
                }
            } 
        } catch (ServiceException e) {
            log.warn("The host '{}' contains an incorrect configured SiteMount. The hist cannot be used for hst request processing:", name, e);
        } catch (RepositoryException e) {
            throw new ServiceException("Error during creating sitemounts: ", e);
        }
        
        // check whether this Host is correctly mounted
        if(attachSiteMountToHost.rootSiteMount != null && attachSiteMountToHost.rootSiteMount.getHstSite() != null) {
            attachSiteMountToHost.mounted = true;
        }
        
        try {
            NodeIterator childHosts = virtualHostNode.getNodes();
            while(childHosts.hasNext()) {
                Node childHostNode = childHosts.nextNode();
                if (childHostNode == null || !childHostNode.isNodeType(HstNodeTypes.NODETYPE_HST_VIRTUALHOST)) {
                    continue;
                }
                VirtualHostService childHost = new VirtualHostService(virtualHosts, childHostNode, this);
                this.childVirtualHosts.put(childHost.name, childHost);
            }
        } catch (RepositoryException e) {
            throw new ServiceException("Error during initializing hosts", e);
        }
        
    }

    
    public VirtualHostService(VirtualHostService parent, String[] nameSegments, int position) {
        super(null);
        this.parentHost = parent;
        this.virtualHosts = parent.virtualHosts;
        this.id = parent.id+"_";
        this.jcrPath = parent.jcrPath;
        this.portNumber = parent.portNumber;
        this.scheme = parent.scheme;
        this.portVisible = parent.portVisible;
        this.contextPathInUrl = parent.contextPathInUrl;
        this.name = nameSegments[position];
        // add child host services
        if(--position > -1 ) {
            VirtualHostService childHost = new VirtualHostService(this,nameSegments, position);
            this.childVirtualHosts.put(childHost.name, childHost);
        }
      
    }
    
    public String getName(){
        return name;
    }
    
    public String getHostName(){
        StringBuilder builder = new StringBuilder(name);
        VirtualHostService ancestor = parentHost;
        while(ancestor != null) {
            builder.append(".").append(ancestor.name);
            ancestor = ancestor.parentHost;
        }
        return builder.toString();
    }
    
    public String getId(){
        return id;
    }
    
    public boolean isMounted() {
        return mounted;
    }


    public ResolvedSiteMapItem match(HttpServletRequest request) throws MatchException {
        throw new MatchException("Not yet implemented");
    }
    
    public SiteMount getRootSiteMount(){
        return this.rootSiteMount;
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
    
    public VirtualHosts getVirtualHosts() {
        return this.virtualHosts;
    }

    public Service[] getChildServices() {
        // the services are the child host AND the root sitemount if this one is not null
        Service[] childServices = childVirtualHosts.values().toArray(new Service[childVirtualHosts.values().size()]);
        if(this.rootSiteMount != null) {
            Service[] servicesPlusSiteMount = new Service[childServices.length + 1];
            System.arraycopy(childServices, 0, servicesPlusSiteMount, 0, childServices.length);
            // and add to the end the sitemount
            servicesPlusSiteMount[childServices.length] = (Service)rootSiteMount;
            return servicesPlusSiteMount;
        } 
        return childServices;
    }


    public VirtualHost getChildHost(String name) {
        return childVirtualHosts.get(name);
    }


    public String getBaseURL(HttpServletRequest request) {
        StringBuilder builder = new StringBuilder();
        
        String scheme = this.getScheme();
        
        if (scheme == null) {
            scheme = "http";
        }
        
        String serverName = HstRequestUtils.getRequestServerName(request);
        
        int port = this.getPortNumber();
        
        if (port == 0) {
            port = HstRequestUtils.getRequestServerPort(request);
        }
        
        if ((port == 80 && "http".equals(scheme)) || (port == 443 && "https".equals(scheme))) {
            port = 0;
        }
        
        builder.append(scheme);
        builder.append("://").append(serverName);
        
        if (this.isPortVisible() && port != 0) {
            builder.append(":").append(port);
        }
        return builder.toString();
    }


    public List<VirtualHost> getChildHosts() {
        return new ArrayList<VirtualHost>(childVirtualHosts.values());
    }

   


}
