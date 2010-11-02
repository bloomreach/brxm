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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration.Type;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.service.ServiceException;
import org.slf4j.LoggerFactory;

public class HstComponentsConfigurationService implements HstComponentsConfiguration {

    private static final long serialVersionUID = 1L;

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(HstComponentsConfigurationService.class);

    /*
     * rootComponentConfigurations are component configurations that are directly retrievable through getComponentConfiguration(String id),
     * in other words, every HstComponent that has a non null id.
     * 
     */
    private Map<String, HstComponentConfiguration> rootComponentConfigurations = new LinkedHashMap<String, HstComponentConfiguration>();

    /*
     * Components that are direct childs of the hst:components node. A child component only is a root component when it has a non null
     * id.
     */
    private List<HstComponentConfiguration> childComponents  = new ArrayList<HstComponentConfiguration>();
    
    /*
     * The Map of all containter items that are unique: A container item can be used multiple times with a single HstSite. This Map contains only the unique container items.
     * The key is composed of the component class name & template value
     * TODO Currently, in uniqueness calculation, we do not take into account possible different child components.
     */
    private Map<String, HstComponentConfiguration> uniqueContainerItemsMap  = new HashMap<String, HstComponentConfiguration>();

    private Set<String> usedReferenceNames = new HashSet<String>();
    private int autocreatedCounter = 0;

    public HstComponentsConfigurationService(HstNode configurationNode, String hstConfigurationsRootPath, Map<String, String> templateRenderMap) throws ServiceException {

        HstNode components = configurationNode.getNode(HstNodeTypes.NODENAME_HST_COMPONENTS);
        
        if (components != null) {
            log.debug("Initializing the components for '{}'", HstNodeTypes.NODENAME_HST_COMPONENTS);
            init(components, hstConfigurationsRootPath);
        }

        HstNode pages =  configurationNode.getNode(HstNodeTypes.NODENAME_HST_PAGES);
        if (pages != null) {
            log.debug("Initializing the pages for '{}'", HstNodeTypes.NODENAME_HST_PAGES);
            init(pages, hstConfigurationsRootPath);
        }

        // we rather compute all the unique container items before enhancing.
        for (HstComponentConfiguration child : childComponents) {
            populateUniqueContainerItems(child);
        }
        
        for (HstComponentConfiguration child : childComponents) {
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

    }

    private void enhanceComponentTree(Map<String, String> templateRenderMap) throws ServiceException{
        // merging referenced components:  to avoid circular population, hold a list of already populated configs
        List<HstComponentConfiguration> populated = new ArrayList<HstComponentConfiguration>();
        for (HstComponentConfiguration child : rootComponentConfigurations.values()) {
            if (!populated.contains(child)) {
                ((HstComponentConfigurationService) child).populateComponentReferences(rootComponentConfigurations,
                        populated);
            }
        }

        //  autocreating missing referenceNames
        for (HstComponentConfiguration child : childComponents) {
            autocreateReferenceNames(child);
        }

        // setting renderpaths for each component
        for (HstComponentConfiguration child : childComponents) {
            ((HstComponentConfigurationService) child).setRenderPath(templateRenderMap);
        }

        // adding parameters from parent components to child components and override them in a child when they already are present
        for (HstComponentConfiguration child : childComponents) {
            ((HstComponentConfigurationService) child).inheritParameters();
        }
    }

    public HstComponentConfiguration getComponentConfiguration(String id) {
        return this.rootComponentConfigurations.get(id);
    }

    public Map<String, HstComponentConfiguration> getComponentConfigurations() {
        return Collections.unmodifiableMap(this.rootComponentConfigurations);
    }


    public List<HstComponentConfiguration> getUniqueContainerItems() {
        return Collections.unmodifiableList(new ArrayList<HstComponentConfiguration>(uniqueContainerItemsMap.values()));
    }
    
    private void autocreateReferenceNames(HstComponentConfiguration componentConfiguration) {
        if (componentConfiguration.getReferenceName() == null || "".equals(componentConfiguration.getReferenceName())) {
            String autoRefName = "r" + (++autocreatedCounter);
            while (usedReferenceNames.contains(autoRefName)) {
                autoRefName = "r" + (++autocreatedCounter);
            }
            ((HstComponentConfigurationService) componentConfiguration).setReferenceName(autoRefName);
        }
        ((HstComponentConfigurationService) componentConfiguration).autocreateReferenceNames();
    }

    private void populateRootComponentConfigurations(HstComponentConfiguration componentConfiguration) {
        if (componentConfiguration.getId() != null) {
            rootComponentConfigurations.put(componentConfiguration.getId(), componentConfiguration);
        }
        for (HstComponentConfiguration child : componentConfiguration.getChildren().values()) {
            populateRootComponentConfigurations(child);
        }
    }

    private void populateUniqueContainerItems(HstComponentConfiguration candidateConfiguration) {
        if(candidateConfiguration.getComponentType() == Type.CONTAINER_ITEM_COMPONENT) {
            // check whether we already have this container item. If not,  we add it
            String key = candidateConfiguration.getComponentClassName() + '\uFFFF' + ((HstComponentConfigurationService)candidateConfiguration).getHstTemplate();
            if(!uniqueContainerItemsMap.containsKey(key)) {
                uniqueContainerItemsMap.put(key, candidateConfiguration);
            }
        }
        for (HstComponentConfiguration child : candidateConfiguration.getChildren().values()) {
            populateUniqueContainerItems(child);
        }
    }
    
    private void init(HstNode node, String hstConfigurationsRootPath) {
        for(HstNode child : node.getNodes()) {
            if(HstNodeTypes.NODETYPE_HST_COMPONENT.equals(child.getNodeTypeName())
              || HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT.equals(child.getNodeTypeName())
              || HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT.equals(child.getNodeTypeName())
            )
            {
                if(child.getValueProvider().hasProperty(HstNodeTypes.COMPONENT_PROPERTY_REFERECENCENAME)) {
                 // add to the used referencenames set 
                    usedReferenceNames.add(child.getValueProvider().getString(HstNodeTypes.COMPONENT_PROPERTY_REFERECENCENAME));
                }
                try {
                    HstComponentConfiguration componentConfiguration = new HstComponentConfigurationService(child,
                            null, hstConfigurationsRootPath);
                    childComponents.add(componentConfiguration);
                    log.debug("Added component service with key '{}'", componentConfiguration.getId());
                } catch (ServiceException e) {
                    if (log.isDebugEnabled()) {
                        log.warn("Skipping component '{}'", child.getValueProvider().getPath(), e);
                    } else if (log.isWarnEnabled()) {
                        log.warn("Skipping component '{}' : '{}'", child.getValueProvider().getPath(), e.toString());
                    }
                }
            }
            else {
                log.warn("Skipping node '{}' because is not of type '{}'", child.getValueProvider().getPath(),
                        (HstNodeTypes.NODETYPE_HST_COMPONENT));
            }
        }
    }


}
