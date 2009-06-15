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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.Configuration;
import org.hippoecm.hst.configuration.ConfigurationViewUtilities;
import org.hippoecm.hst.service.AbstractJCRService;
import org.hippoecm.hst.service.Service;
import org.hippoecm.hst.service.ServiceException;
import org.slf4j.LoggerFactory;

public class HstComponentsConfigurationService extends AbstractJCRService implements HstComponentsConfiguration, Service{

    private static final long serialVersionUID = 1L;

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(HstComponentsConfigurationService.class);
    
    /*
     * rootComponentConfigurations are component configurations that are directly retrievable through getComponentConfiguration(String id),
     * in other words, every HstComponent that has a non null id.
     * 
     */
    private Map<String, HstComponentConfiguration> rootComponentConfigurations;
    
    /*
     * Components that are direct childs of the hst:components node. A child component only is a root component when it has a non null
     * id.
     */
    private List<HstComponentConfiguration> childComponents;
    
    private Set<String> usedReferenceNames = new HashSet<String>();
    private int autocreatedCounter = 0;
    
    public HstComponentsConfigurationService(Node configurationNode, Map<String, String> templateRenderMap) throws RepositoryException {
        super(null);
        this.rootComponentConfigurations = new LinkedHashMap<String, HstComponentConfiguration>();
        this.childComponents = new ArrayList<HstComponentConfiguration>();
        
        if(configurationNode.hasNode(Configuration.NODENAME_HST_COMPONENTS)) {
            log.debug("Initializing the components for '{}'", Configuration.NODENAME_HST_COMPONENTS);
            Node fragments = configurationNode.getNode(Configuration.NODENAME_HST_COMPONENTS);
            init(fragments, configurationNode.getPath());
        }
        
        if(configurationNode.hasNode(Configuration.NODENAME_HST_PAGES)) {
            log.debug("Initializing the components for '{}'", Configuration.NODENAME_HST_PAGES);
            Node pages = configurationNode.getNode(Configuration.NODENAME_HST_PAGES);
            init(pages, configurationNode.getPath());
        }
        
        
        for(HstComponentConfiguration child: childComponents) {
            populateRootComponentConfigurations(child);
        }
        
        /*
         * The component tree needs to be enhanced, for 
         * 1: merging referenced components,
         * 2: autocreating missing referenceNames
         * 3: setting renderpaths for each component
         * 4: Adding parameters from parent components to child components and override them when they already are present
         */
        enhanceComponentTree(templateRenderMap);
        
        StringBuffer buf = new StringBuffer();
        ConfigurationViewUtilities.view(buf, rootComponentConfigurations.get("hst:pages/home"));
        System.out.println(buf);
        System.out.println("!!!");
    }
     
    
    private void enhanceComponentTree(Map<String, String> templateRenderMap) {
        // merging referenced components:  to avoid circular population, hold a list of already populated configs
        List<HstComponentConfiguration> populated = new ArrayList<HstComponentConfiguration>();
        for(HstComponentConfiguration child: rootComponentConfigurations.values()){
            if(!populated.contains(child)) {
                ((HstComponentConfigurationService)child).populateComponentReferences(rootComponentConfigurations, populated);
            }
        }
        
        //  autocreating missing referenceNames
        for(HstComponentConfiguration child: childComponents) {
            autocreateReferenceNames(child);
        }
      
        // setting renderpaths for each component
        for(HstComponentConfiguration child: childComponents){
            ((HstComponentConfigurationService)child).setRenderPath(templateRenderMap);
        }
        
        // adding parameters from parent components to child components and override them in a child when they already are present
        for(HstComponentConfiguration child: childComponents){
            ((HstComponentConfigurationService)child).inheritParameters();
        }
    }

    public Service[] getChildServices() {
        return childComponents.toArray(new Service[rootComponentConfigurations.size()]);
    }

    
    public HstComponentConfiguration getComponentConfiguration(String id) {
        return this.rootComponentConfigurations.get(id);
    }
    

    public Map<String, HstComponentConfiguration> getComponentConfigurations() {
        return Collections.unmodifiableMap(this.rootComponentConfigurations);
    }

     
    private void autocreateReferenceNames(HstComponentConfiguration componentConfiguration){
        if(componentConfiguration.getReferenceName() == null) {
            String autoRefName = "r" + (++autocreatedCounter);
            while(usedReferenceNames.contains(autoRefName)){
                autoRefName = "r" + (++autocreatedCounter);
            }
            ((HstComponentConfigurationService)componentConfiguration).setReferenceName(autoRefName);
        }
        ((HstComponentConfigurationService)componentConfiguration).autocreateReferenceNames();
    }
    
    private void populateRootComponentConfigurations(HstComponentConfiguration componentConfiguration){
        if(componentConfiguration.getId() != null) {
            rootComponentConfigurations.put(componentConfiguration.getId(), componentConfiguration);
        }
        for(HstComponentConfiguration child : componentConfiguration.getChildren().values()){
            populateRootComponentConfigurations(child);
        }
    }
    
    private void init(Node node, String configurationNodePath) throws RepositoryException {
        
        for(NodeIterator nodeIt = node.getNodes(); nodeIt.hasNext();) {
            Node child = nodeIt.nextNode();
            if(child == null) {
                log.warn("skipping null node");
                continue;
            }
            if(child.isNodeType(Configuration.NODETYPE_HST_COMPONENT)) {
                if(child.hasProperty(Configuration.COMPONENT_PROPERTY_REFERECENCENAME)) {
                   // add to the used referencenames set 
                    usedReferenceNames.add(child.getProperty(Configuration.COMPONENT_PROPERTY_REFERECENCENAME).getString());
                }
                try {
                    HstComponentConfiguration componentConfiguration = new HstComponentConfigurationService(child, null ,configurationNodePath);
                    childComponents.add(componentConfiguration);
                    log.debug("Added component service with key '{}'",componentConfiguration.getId());
                } catch (ServiceException e) {
                    if (log.isDebugEnabled()) {
                        log.warn("Skipping component '{}'", child.getPath(), e);
                    } else if (log.isWarnEnabled()) {
                        log.warn("Skipping component '{}'", child.getPath());
                    }
                }
                
            } else {
                if (log.isWarnEnabled()) {
                    log.warn("Skipping node '{}' because is not of type '{}'", child.getPath(), (Configuration.NODETYPE_HST_COMPONENT));
                }
            }
        }
    }

}
