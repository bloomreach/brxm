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
import org.hippoecm.hst.service.AbstractJCRService;
import org.hippoecm.hst.service.Service;
import org.hippoecm.hst.service.ServiceException;
import org.hippoecm.hst.util.HstRequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VirtualHostService extends AbstractJCRService implements VirtualHost, Service {
    
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(VirtualHostService.class);
    
    private Map<String, VirtualHostService> childVirtualHosts = VirtualHostsService.createVirtualHostHashMap();
   
    private String name;
    private String hostName;
    /**
     * The homepage for this VirtualHost. When the backing configuration does not contain a homepage, then, the homepage from the backing {@link VirtualHosts} is 
     * taken (which still might be <code>null</code> though)
     */
    private String homepage;
    /**
     * The pageNotFound for this VirtualHost. When the backing configuration does not contain a pageNotFound, then, the pageNotFound from the backing {@link VirtualHosts} is 
     * taken (which still might be <code>null</code> though)
     */
    private String pageNotFound;
    
    /**
     * Whether the {@link SiteMount}'s contained by this VirtualHostService should show the hst version as a response header when they are a preview SiteMount
     */
    private boolean versionInPreviewHeader;
    
    private VirtualHosts virtualHosts;
    private VirtualHostService parentHost;
    
    private Map<Integer, PortMount> portMounts = new HashMap<Integer, PortMount>();
    
    private String jcrPath;
    private boolean contextPathInUrl;
    private String scheme;

    public VirtualHostService(VirtualHostsService virtualHosts,Node virtualHostNode, VirtualHostService parentHost) throws ServiceException {        
        super(virtualHostNode);
      
        this.parentHost = parentHost;
        this.virtualHosts = virtualHosts;
        this.jcrPath =  this.getValueProvider().getPath();
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
        
        if(this.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROPERTY_HOMEPAGE)) {
            this.homepage = this.getValueProvider().getString(HstNodeTypes.GENERAL_PROPERTY_HOMEPAGE);
        } else {
           // try to get the one from the parent
            if(parentHost != null) {
                this.homepage = parentHost.homepage;
            } else {
                this.homepage = virtualHosts.getHomePage();
            }
        }
        
        if(this.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROPERTY_PAGE_NOT_FOUND)) {
            this.pageNotFound = this.getValueProvider().getString(HstNodeTypes.GENERAL_PROPERTY_PAGE_NOT_FOUND);
        } else {
           // try to get the one from the parent
            if(parentHost != null) {
                this.pageNotFound = parentHost.pageNotFound;
            } else {
                this.pageNotFound = virtualHosts.getPageNotFound();
            }
        }
        
        if(this.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROPERTY_VERSION_IN_PREVIEW_HEADER)) {
            this.versionInPreviewHeader = this.getValueProvider().getBoolean(HstNodeTypes.GENERAL_PROPERTY_VERSION_IN_PREVIEW_HEADER);
        } else {
           // try to get the one from the parent
            if(parentHost != null) {
                this.versionInPreviewHeader = parentHost.versionInPreviewHeader;
            } else {
                this.versionInPreviewHeader = virtualHosts.isVersionInPreviewHeader();
            }
        }
        
        
        String fullName = this.getValueProvider().getName();
        String[] nameSegments = fullName.split("\\.");
        
        VirtualHostService attachPortMountToHost = this;
        
        if(nameSegments.length > 1) {
            // if the fullName is for example www.hippoecm.org, then this items name is 'org', its child is hippoecm, and
            // the last child is 'www'
            this.name = nameSegments[nameSegments.length - 1];
            // add child host services
            int depth = nameSegments.length - 2;
            if(depth > -1 ) {
                VirtualHostService childHost = new VirtualHostService(this, nameSegments, depth);
                this.childVirtualHosts.put(childHost.name, childHost);
                // we need to switch the attachPortMountToHost to the last host
            }
            while(depth > -1) {
                if(attachPortMountToHost == null) {
                    throw new ServiceException("Something went wrong because attachSiteMountToHost should never be possible to be null.");
                }
                attachPortMountToHost = (VirtualHostService)attachPortMountToHost.getChildHost(nameSegments[depth]);
                depth--;
            }
        } else {
            this.name = this.getValueProvider().getName();
        }
        
        hostName = buildHostName();
        
        try {
            if(virtualHostNode.hasNode(HstNodeTypes.SITEMOUNT_HST_ROOTNAME)) {
                log.info("Host '{}' does have a root SiteMount configured without PortMount. This SiteMount is port agnostic ", this.getHostName());
                // we have a configured root sitemount node without portmount. Let's populate this sitemount. This site mount will be added to 
                // a portmount service with portnumber 0, which means any port
                Node siteMount = virtualHostNode.getNode(HstNodeTypes.SITEMOUNT_HST_ROOTNAME);
                if(siteMount.isNodeType(HstNodeTypes.NODETYPE_HST_SITEMOUNT)) {
                    SiteMount mount = new SiteMountService(siteMount, null, attachPortMountToHost);
                    // 
                    PortMount portMount = new PortMountService(mount, this);
                    attachPortMountToHost.portMounts.put(portMount.getPortNumber(), portMount);
                }
            }
        } catch (ServiceException e) {
            log.warn("The host '{}' contains an incorrect configured SiteMount. The host cannot be used for hst request processing: {}", name, e.getMessage());
        } catch (RepositoryException e) {
            throw new ServiceException("Error during creating sitemounts: ", e);
        }
        
        try {
            NodeIterator childHosts = virtualHostNode.getNodes();
            while(childHosts.hasNext()) {
                Node childNode = childHosts.nextNode();
                if (childNode == null) {
                    continue;
                }
                if(childNode.isNodeType(HstNodeTypes.NODETYPE_HST_VIRTUALHOST)) {
                    VirtualHostService childHost = new VirtualHostService(virtualHosts, childNode, attachPortMountToHost);
                    attachPortMountToHost.childVirtualHosts.put(childHost.name, childHost);
                } else if(childNode.isNodeType(HstNodeTypes.NODETYPE_HST_PORTMOUNT)) {
                    PortMount portMount = new PortMountService(childNode, attachPortMountToHost);
                    attachPortMountToHost.portMounts.put(portMount.getPortNumber(), portMount);
                }
            }
        } catch (RepositoryException e) {
            throw new ServiceException("Error during initializing hosts", e);
        }
     
    }


    
    public VirtualHostService(VirtualHostService parent, String[] nameSegments, int position) {
        super(null);
        this.parentHost = parent;
        this.virtualHosts = parent.virtualHosts;
        this.jcrPath = parent.jcrPath;
        this.scheme = parent.scheme;
        this.homepage = parent.homepage;
        this.pageNotFound = parent.pageNotFound;
        this.versionInPreviewHeader = parent.versionInPreviewHeader;
        this.contextPathInUrl = parent.contextPathInUrl;
        this.name = nameSegments[position];
        // add child host services
        if(--position > -1 ) {
            VirtualHostService childHost = new VirtualHostService(this,nameSegments, position);
            this.childVirtualHosts.put(childHost.name, childHost);
        }
        hostName = buildHostName();
    }
    
    
    public String getName(){
        return name;
    }
    
    public String getHostName(){
        return hostName;
    }
    
    public boolean isContextPathInUrl() {
        return contextPathInUrl;
    }
    
    public String getScheme(){
        return this.scheme;
    }
    
    public String getHomePage() {
        return homepage;
    }

    public String getPageNotFound() {
        return pageNotFound;
    }

    public boolean isVersionInPreviewHeader() {
        return versionInPreviewHeader;
    }

    public VirtualHosts getVirtualHosts() {
        return this.virtualHosts;
    }
    
    public PortMount getPortMount(int portNumber) {
        return portMounts.get(portNumber);
    }

    public Service[] getChildServices() {
        // the services are the child host AND the root sitemount if this one is not null
        Service[] childServices1 = childVirtualHosts.values().toArray(new Service[childVirtualHosts.values().size()]);
        Service[] childServices2 = portMounts.values().toArray(new Service[portMounts.values().size()]);
        
        Service[] allChilds = new Service[childServices1.length + childServices2.length];
        System.arraycopy(childServices1, 0, allChilds, 0, childServices1.length);
        System.arraycopy(childServices2, 0, allChilds, childServices1.length, childServices2.length);
        return allChilds;
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
        
        String serverName = HstRequestUtils.getFarthestRequestHost(request);
        
        builder.append(scheme);
        builder.append("://").append(serverName);
       
        return builder.toString();
    }


    public List<VirtualHost> getChildHosts() {
        return new ArrayList<VirtualHost>(childVirtualHosts.values());
    }


    private String buildHostName() {
        StringBuilder builder = new StringBuilder(name);
        VirtualHostService ancestor = this.parentHost;
        while(ancestor != null) {
            builder.append(".").append(ancestor.name);
            ancestor = ancestor.parentHost;
        }
        return builder.toString();
    }

   
    
}
