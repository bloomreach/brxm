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

import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.model.HstManagerImpl;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.service.ServiceException;
import org.hippoecm.hst.util.HstRequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VirtualHostService implements VirtualHost {
    
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(VirtualHostService.class);
    
    private Map<String, VirtualHostService> childVirtualHosts = VirtualHostsService.virtualHostHashMap();
   
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
    
    /**
     * The name of the host group this virtualhost belongs to, for example, dev, acct or prod
     */
    private String hostGroupName;
    private VirtualHostService parentHost;
    
    private Map<Integer, PortMount> portMounts = new HashMap<Integer, PortMount>();
    
    private boolean contextPathInUrl;
    private String scheme;

    public VirtualHostService(VirtualHostsService virtualHosts, HstNode virtualHostNode, VirtualHostService parentHost, String hostGroupName, HstManagerImpl hstManager) throws ServiceException {        
       
        this.parentHost = parentHost;
        this.virtualHosts = virtualHosts;
        this.hostGroupName = hostGroupName;
        if(virtualHostNode.getValueProvider().hasProperty(HstNodeTypes.VIRTUALHOST_PROPERTY_SHOWCONTEXTPATH)) {
            this.contextPathInUrl = virtualHostNode.getValueProvider().getBoolean(HstNodeTypes.VIRTUALHOST_PROPERTY_SHOWCONTEXTPATH);
        } else {
            // try to get the one from the parent
            if(parentHost != null) {
                this.contextPathInUrl = parentHost.contextPathInUrl;
            } else {
                this.contextPathInUrl = virtualHosts.isContextPathInUrl();
            }
        }
        
        if(virtualHostNode.getValueProvider().hasProperty(HstNodeTypes.VIRTUALHOST_PROPERTY_SCHEME)) {
            this.scheme = virtualHostNode.getValueProvider().getString(HstNodeTypes.VIRTUALHOST_PROPERTY_SCHEME);
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
        
        if(virtualHostNode.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROPERTY_HOMEPAGE)) {
            this.homepage = virtualHostNode.getValueProvider().getString(HstNodeTypes.GENERAL_PROPERTY_HOMEPAGE);
        } else {
           // try to get the one from the parent
            if(parentHost != null) {
                this.homepage = parentHost.homepage;
            } else {
                this.homepage = virtualHosts.getHomePage();
            }
        }
        
        if(virtualHostNode.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROPERTY_PAGE_NOT_FOUND)) {
            this.pageNotFound = virtualHostNode.getValueProvider().getString(HstNodeTypes.GENERAL_PROPERTY_PAGE_NOT_FOUND);
        } else {
           // try to get the one from the parent
            if(parentHost != null) {
                this.pageNotFound = parentHost.pageNotFound;
            } else {
                this.pageNotFound = virtualHosts.getPageNotFound();
            }
        }
        
        if(virtualHostNode.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROPERTY_VERSION_IN_PREVIEW_HEADER)) {
            this.versionInPreviewHeader = virtualHostNode.getValueProvider().getBoolean(HstNodeTypes.GENERAL_PROPERTY_VERSION_IN_PREVIEW_HEADER);
        } else {
           // try to get the one from the parent
            if(parentHost != null) {
                this.versionInPreviewHeader = parentHost.versionInPreviewHeader;
            } else {
                this.versionInPreviewHeader = virtualHosts.isVersionInPreviewHeader();
            }
        }
        
        
        String fullName = virtualHostNode.getValueProvider().getName();
        String[] nameSegments = fullName.split("\\.");
        
        VirtualHostService attachPortMountToHost = this;
        
        if(nameSegments.length > 1) {
            // if the fullName is for example www.hippoecm.org, then this items name is 'org', its child is hippoecm, and
            // the last child is 'www'
            this.name = nameSegments[nameSegments.length - 1];
            // add child host services
            int depth = nameSegments.length - 2;
            if(depth > -1 ) {
                VirtualHostService childHost = new VirtualHostService(this, nameSegments, depth, hostGroupName,  hstManager);
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
            this.name = virtualHostNode.getValueProvider().getName();
        }
        
        hostName = buildHostName();
        
        HstNode siteMountRoot = virtualHostNode.getNode(HstNodeTypes.SITEMOUNT_HST_ROOTNAME);
        if(siteMountRoot != null) {
            log.info("Host '{}' does have a root SiteMount configured without PortMount. This SiteMount is port agnostic ", this.getHostName());
            // we have a configured root sitemount node without portmount. Let's populate this sitemount. This site mount will be added to 
            // a portmount service with portnumber 0, which means any port
            HstNode siteMountNode = virtualHostNode.getNode(HstNodeTypes.SITEMOUNT_HST_ROOTNAME);
            if(HstNodeTypes.NODETYPE_HST_SITEMOUNT.equals(siteMountNode.getNodeTypeName())) {
                SiteMount siteMount = new SiteMountService(siteMountNode, null, attachPortMountToHost, hstManager, 0);
                 
                PortMount portMount = new PortMountService(siteMount, this);
                attachPortMountToHost.portMounts.put(portMount.getPortNumber(), portMount);
            } else {
                // TODO : log error / throw exeption?
            }
        }
        
        for(HstNode child : virtualHostNode.getNodes()) {
            if(HstNodeTypes.NODETYPE_HST_VIRTUALHOST.equals(child.getNodeTypeName())) {
                VirtualHostService childHost = new VirtualHostService(virtualHosts, child, attachPortMountToHost, hostGroupName, hstManager);
                attachPortMountToHost.childVirtualHosts.put(childHost.name, childHost);
            } else if (HstNodeTypes.NODETYPE_HST_PORTMOUNT.equals(child.getNodeTypeName())){
                PortMount portMount = new PortMountService(child, attachPortMountToHost, hstManager);
                attachPortMountToHost.portMounts.put(portMount.getPortNumber(), portMount);
            }
        }
       
    }

    public VirtualHostService(VirtualHostService parent, String[] nameSegments, int position, String hostGroup, HstManagerImpl hstManager) {
        this.parentHost = parent;
        this.virtualHosts = parent.virtualHosts;
        this.hostGroupName = hostGroup;
        this.scheme = parent.scheme;
        this.homepage = parent.homepage;
        this.pageNotFound = parent.pageNotFound;
        this.versionInPreviewHeader = parent.versionInPreviewHeader;
        this.contextPathInUrl = parent.contextPathInUrl;
        this.name = nameSegments[position];
        // add child host services
        if(--position > -1 ) {
            VirtualHostService childHost = new VirtualHostService(this,nameSegments, position, hostGroup, hstManager);
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
    
    public String getHostGroupName() {
        return hostGroupName;
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
