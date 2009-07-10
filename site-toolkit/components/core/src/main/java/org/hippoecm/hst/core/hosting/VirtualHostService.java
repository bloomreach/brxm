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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.Configuration;
import org.hippoecm.hst.service.AbstractJCRService;
import org.hippoecm.hst.service.Service;
import org.hippoecm.hst.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VirtualHostService extends AbstractJCRService implements VirtualHost, Service {
    
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(VirtualHostService.class);
    
    
    
    private Map<String, VirtualHostService> childVirtualHosts = new HashMap<String, VirtualHostService>();
   
    private String siteName;
    private String id;
    private String name;
    private VirtualHosts virtualHosts;
    
    private String jcrPath;
    private boolean portVisible;
    private int portNumber;
    private boolean contextPathInUrl;
    private String protocol;
    private String[] hstMappings;
    private Mapping[] mappings;
    

    public VirtualHostService(VirtualHostsService virtualHosts,Node virtualHostNode, VirtualHostService parentHost) throws ServiceException{
        super(virtualHostNode);
        this.virtualHosts = virtualHosts;
        this.id = this.getValueProvider().getPath();
        this.jcrPath = this.id;
        
        if(this.getValueProvider().hasProperty(Configuration.VIRTUALHOST_PROPERTY_SITENAME)) {
            this.siteName = this.getValueProvider().getString(Configuration.VIRTUALHOST_PROPERTY_SITENAME);
        } else {
            if(parentHost != null) {
                this.siteName = parentHost.siteName;
            } 
        }
        if(this.getValueProvider().hasProperty(Configuration.VIRTUALHOST_PROPERTY_PORT)) {
            this.portNumber = this.getValueProvider().getLong(Configuration.VIRTUALHOST_PROPERTY_PORT).intValue();
        } else {
            // try to get the one from the parent
            if(parentHost != null) {
                this.portNumber = parentHost.portNumber;
            } else {
                this.portNumber = virtualHosts.getPortNumber();
            }
        }
        if(this.getValueProvider().hasProperty(Configuration.VIRTUALHOST_PROPERTY_SHOWPORT)) {
            this.portVisible = this.getValueProvider().getBoolean(Configuration.VIRTUALHOST_PROPERTY_SHOWPORT);
        } else {
            // try to get the one from the parent
            if(parentHost != null) {
                this.portVisible = parentHost.portVisible;
            } else {
                this.portVisible = virtualHosts.isPortVisible();
            }
        }
        if(this.getValueProvider().hasProperty(Configuration.VIRTUALHOST_PROPERTY_SHOWCONTEXTPATH)) {
            this.contextPathInUrl = this.getValueProvider().getBoolean(Configuration.VIRTUALHOST_PROPERTY_SHOWCONTEXTPATH);
        } else {
            // try to get the one from the parent
            if(parentHost != null) {
                this.contextPathInUrl = parentHost.contextPathInUrl;
            } else {
                this.contextPathInUrl = virtualHosts.isContextPathInUrl();
            }
        }
        
        if(this.getValueProvider().hasProperty(Configuration.VIRTUALHOST_PROPERTY_MAPPING)) {
            this.hstMappings = this.getValueProvider().getStrings(Configuration.VIRTUALHOST_PROPERTY_MAPPING);
            this.mappings = createMappings();
        } else {
         // try to get the one from the parent
            if(parentHost != null) {
                this.hstMappings = parentHost.hstMappings;
                this.mappings = parentHost.mappings;
            }  
        }
        
        if(this.getValueProvider().hasProperty(Configuration.VIRTUALHOST_PROPERTY_PROTOCOL)) {
            this.protocol = this.getValueProvider().getString(Configuration.VIRTUALHOST_PROPERTY_PROTOCOL);
            if(this.protocol == null || "".equals(this.protocol)) {
                this.protocol = VirtualHostsService.DEFAULT_PROTOCOL;
            }
        } else {
           // try to get the one from the parent
            if(parentHost != null) {
                this.protocol = parentHost.protocol;
            } else {
                this.protocol = virtualHosts.getProtocol();
            }
        }
        
        String fullName = this.getValueProvider().getName();
        String[] nameSegments = fullName.split("\\.");
        if(nameSegments.length > 1) {
            // if the fullName is for example www.hippoecm.org, then this items name is 'org', its child is hippoecm, and
            // the last child is 'www'
            this.name = nameSegments[nameSegments.length - 1];
            // add child host services
            int position = nameSegments.length - 2;
            if(position > -1 ) {
                VirtualHostService childHost = new VirtualHostService(this, nameSegments, position);
                this.childVirtualHosts.put(childHost.getName(), childHost);
            }
        } else {
            this.name = this.getValueProvider().getName();
        }
        
        try {
            NodeIterator childHosts = virtualHostNode.getNodes();
            while(childHosts.hasNext()) {
                Node childHostNode = childHosts.nextNode();
                if(childHostNode == null) {continue;}
                VirtualHostService childHost = new VirtualHostService(virtualHosts, childHostNode, this);
                this.childVirtualHosts.put(childHost.getName(), childHost);
            }
        } catch (RepositoryException e) {
            throw new ServiceException("Error during initializing hosts: {}", e);
        }
        
    }

    
    public VirtualHostService(VirtualHostService virtualHostService, String[] nameSegments, int position) {
        super(null);
        this.virtualHosts = virtualHostService.virtualHosts;
        this.id = virtualHostService.id+"_";
        this.jcrPath = virtualHostService.jcrPath;
        this.siteName = virtualHostService.siteName;
        this.portNumber = virtualHostService.portNumber;
        this.protocol = virtualHostService.protocol;
        this.portVisible = virtualHostService.portVisible;
        this.contextPathInUrl = virtualHostService.contextPathInUrl;
        this.hstMappings = virtualHostService.hstMappings;
        this.mappings = createMappings();
        this.name = nameSegments[position];
        // add child host services
        if(--position > -1 ) {
            VirtualHostService childHost = new VirtualHostService(this,nameSegments, position);
            this.childVirtualHosts.put(childHost.getName(), childHost);
        }
      
    }

    private Mapping[] createMappings() {
        if(hstMappings == null) {
            return new Mapping[0];
        }
        List<Mapping> mappingsList = new ArrayList<Mapping>();
        for(String mapping : hstMappings) {
            try {
                mappingsList.add(new MappingImpl(mapping, this));
            } catch(MappingException e) {
               log.warn("Ignoring mapping {} : {}", mapping, e.toString()); 
            }
        }
        Mapping[] tmpMappings = mappingsList.toArray(new Mapping[mappingsList.size()]);
        Arrays.sort(tmpMappings);
        return tmpMappings;
    }

    public Mapping getMapping(String pathInfo) {
        if(pathInfo == null) {return null;}
        for(Mapping mapping : mappings) {
            if(pathInfo.startsWith(mapping.getUriPrefix())){
                return mapping;
            }
        }
        return null;
    }
    
    public String getId(){
        return this.id;
    }
    
    public String getName(){
        return this.name;
    }
    
    public String getJcrPath(){
        return this.jcrPath;
    }
   
    public String getSiteName() {
        return this.siteName;
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
    
    public String getProtocol(){
        return this.protocol;
    }
    
    public VirtualHosts getVirtualHosts() {
        return this.virtualHosts;
    }

    public Service[] getChildServices() {
        return childVirtualHosts.values().toArray(new Service[childVirtualHosts.values().size()]);
    }


    public VirtualHost getChildHost(String name) {
        return childVirtualHosts.get(name);
    }

}
