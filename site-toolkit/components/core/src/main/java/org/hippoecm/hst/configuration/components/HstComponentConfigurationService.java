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
package org.hippoecm.hst.configuration.components;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.Configuration;
import org.hippoecm.hst.provider.PropertyMap;
import org.hippoecm.hst.service.AbstractJCRService;
import org.hippoecm.hst.service.Service;
import org.hippoecm.hst.service.ServiceException;
import org.slf4j.LoggerFactory;

public class HstComponentConfigurationService extends AbstractJCRService implements HstComponentConfiguration, Service{
    
    private static final long serialVersionUID = 1L;

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(HstComponentConfiguration.class);
    
    private SortedMap<String, HstComponentConfiguration> componentConfigurations = new TreeMap<String, HstComponentConfiguration>();
    
    private String id;
    
    private String name;

    private String componentClassName;
    
    private String renderPath;
    
    private String serveResourcePath;
    
    private String referenceName;
    
    private PropertyMap propertyMap;
    
    private String configurationRootNodePath;
    
    public HstComponentConfigurationService(Node jcrNode, String configurationRootNodePath) throws ServiceException {
        super(jcrNode);
        if(!getValueProvider().getPath().startsWith(configurationRootNodePath)) {
            throw new ServiceException("Node path of the component cannot start without the global components path. Skip Component");
        }
        this.configurationRootNodePath = configurationRootNodePath;
        // id is the relative path wrt configuration components path
        this.id = getValueProvider().getPath().substring(configurationRootNodePath.length()+1);
       
        if (getValueProvider().isNodeType(Configuration.NODETYPE_HST_COMPONENT)) {
            this.name = getValueProvider().getName();
            this.referenceName = getValueProvider().getString(Configuration.COMPONENT_PROPERTY_REFERECENCENAME);
            this.componentClassName = getValueProvider().getString(Configuration.COMPONENT_PROPERTY_COMPONENT_CLASSNAME);
            this.renderPath = getValueProvider().getString(Configuration.COMPONENT_PROPERTY_RENDER_PATH);
            this.serveResourcePath = getValueProvider().getString(Configuration.COMPONENT_PROPERTY_SERVE_RESOURCE_PATH);
            this.propertyMap = getValueProvider().getPropertyMap();
        } 
        
        init(jcrNode);
       
    }

    public void init(Node jcrNode) {
        try {
            for(NodeIterator nodeIt = jcrNode.getNodes(); nodeIt.hasNext();) {
                Node child = nodeIt.nextNode();
                if(child == null) {
                    log.warn("skipping null node");
                    continue;
                }
                if(child.isNodeType(Configuration.NODETYPE_HST_COMPONENT)) {
                    if(child.hasProperty(Configuration.COMPONENT_PROPERTY_REFERECENCENAME)) {
                        try {
                            HstComponentConfiguration componentConfiguration = new HstComponentConfigurationService(child, configurationRootNodePath);
                            componentConfigurations.put(componentConfiguration.getId(), componentConfiguration);
                            log.debug("Added component service with key '{}'",componentConfiguration.getId());
                        } catch (ServiceException e) {
                            if (log.isDebugEnabled()) {
                                log.warn("Skipping component '{}'", child.getPath(), e);
                            } else if (log.isWarnEnabled()) {
                                log.warn("Skipping component '{}'", child.getPath());
                            }
                        }
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("Skipping '{}' hst:component + child components because it does not contain the mandatory property '{}'",Configuration.COMPONENT_PROPERTY_REFERECENCENAME, child.getPath());
                        }
                    }
                } else {
                    if (log.isWarnEnabled()) {
                        log.warn("Skipping node '{}' because is not of type '{}'", child.getPath(), (Configuration.NODETYPE_HST_COMPONENT));
                    }
                }
            }
        } catch (RepositoryException e) {
            log.warn("Skipping Component due to Repository Exception ", e);
        }
        
    }
    
    public Service[] getChildServices() {
        return componentConfigurations.values().toArray(new Service[componentConfigurations.size()]);
    } 

    public String getComponentClassName(){
        return this.componentClassName;
    }
    
    public String getRenderPath(){
        return this.renderPath;
    }
    
    public String getServeResourcePath() {
        return this.serveResourcePath;
    }

    public Map<String, Object> getProperties() {
        return propertyMap.getAllMapsCombined();
    }
    
    public String getId() {
        return this.id;
    }
    
    public String getName() {
        return this.name;
    }

    public String getReferenceName() {
        return this.referenceName;
    }

    public SortedMap<String, HstComponentConfiguration> getChildren() {
       return this.componentConfigurations;
    }

   
}
