/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.core.internal.StringPool;
import org.hippoecm.hst.configuration.model.HstManagerImpl;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.provider.ValueProvider;
import org.hippoecm.hst.service.ServiceException;
import org.slf4j.LoggerFactory;

public class HstComponentsConfigurationService implements HstComponentsConfiguration {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(HstComponentsConfigurationService.class);

    /*
     * canonicalComponentConfigurations are component configurations that are retrievable through getComponentConfiguration(String id),
     * They are the HstComponentConfiguration items that are not the result of enhancing but present without enhancing
     */
    private Map<String, HstComponentConfiguration> canonicalComponentConfigurations = new LinkedHashMap<String, HstComponentConfiguration>();

    /*
     * Components that are direct childs of the hst:components node. A child component only is a root component when it has a non null
     * id.
     */
    private List<HstComponentConfiguration> childComponents  = new ArrayList<HstComponentConfiguration>();
    
    /*
     * The Map of all containter items. These are the hst:containeritemcomponent's that are configured as child of hst:containeritemcomponent's
     */
    private List<HstComponentConfiguration> availableContainerItems  = new ArrayList<HstComponentConfiguration>();

    private Set<String> usedReferenceNames = new HashSet<String>();
    private int autocreatedCounter = 0;

    public HstComponentsConfigurationService(HstNode configurationNode, HstManagerImpl hstManager) throws ServiceException {
 
        HstNode components = configurationNode.getNode(HstNodeTypes.NODENAME_HST_COMPONENTS);
        
        if (components != null) {
            log.debug("Initializing the components");
            init(components, HstNodeTypes.NODENAME_HST_COMPONENTS);
        }

        HstNode pages =  configurationNode.getNode(HstNodeTypes.NODENAME_HST_PAGES);
        if (pages != null) {
            log.debug("Initializing the pages");
            init(pages, HstNodeTypes.NODENAME_HST_PAGES);
        }

        // populate all the available containeritems that are part of hst:catalog. These container items do *not* need to be enhanced as they
        // are *never* used directly. They are only to be used by the page composer that can drop these containeritems into containers
        
        HstNode catalog =  configurationNode.getNode(HstNodeTypes.NODENAME_HST_CATALOG);
        if (catalog != null) {
            log.debug("Initializing the catalog");
            initCatalog(catalog);
        }
        
        if(hstManager.getCommonCatalog() != null) {
            // now also load the COMMON catalog
            log.debug("Initializing the common catalog");
            initCatalog(hstManager.getCommonCatalog());
        }
     
        for (HstComponentConfiguration child : childComponents) {
            populateCanonicalComponentConfigurations(child);
        }

        /*
         * The component tree needs to be enhanced, for 
         * 1: merging referenced components,
         * 2: autocreating missing referenceNames
         * 3: setting renderpaths for each component
         * 4: Adding parameters from parent components to child components and override them when they already are present
         */
        
        Map<String, HstNode> templateResourceMap = getTemplateResourceMap(configurationNode);
        enhanceComponentTree(templateResourceMap);

    }

    private void enhanceComponentTree(Map<String, HstNode> templateResourceMap) throws ServiceException{
        // merging referenced components:  to avoid circular population, hold a list of already populated configs
        List<HstComponentConfiguration> populated = new ArrayList<HstComponentConfiguration>();
        for (HstComponentConfiguration child : canonicalComponentConfigurations.values()) {
            if (!populated.contains(child)) {
                ((HstComponentConfigurationService) child).populateComponentReferences(canonicalComponentConfigurations,
                        populated);
            }
        }

        //  autocreating missing referenceNames
        for (HstComponentConfiguration child : childComponents) {
            autocreateReferenceNames(child);
        }

        // setting renderpaths for each component
        for (HstComponentConfiguration child : childComponents) {
            ((HstComponentConfigurationService) child).setRenderPath(templateResourceMap);
            ((HstComponentConfigurationService) child).setServeResourcePath(templateResourceMap);
        }

        // adding parameters from parent components to child components and override them in a child when they already are present
        for (HstComponentConfiguration child : childComponents) {
            ((HstComponentConfigurationService) child).inheritParameters();
        }

        for (HstComponentConfiguration child : childComponents) {
            ((HstComponentConfigurationService) child).populateVariants();
        }

        Set<String> allMountVariants = new HashSet<String>();
        for (HstComponentConfiguration child : childComponents) {
            allMountVariants.addAll(child.getVariants());
        }

        for (HstComponentConfiguration child : childComponents) {
            List<String> allVariants = new ArrayList<String>(allMountVariants);
            ((HstComponentConfigurationService) child).setMountVariants(Collections.unmodifiableList(allVariants));
        }


        for (HstComponentConfiguration child : childComponents) {
            ((HstComponentConfigurationService) child).populateIsCompositeCacheable();
        }

        for (HstComponentConfiguration child : childComponents) {
            ((HstComponentConfigurationService) child).makeCollectionsImmutableAndOptimize();
        }

    }

    public HstComponentConfiguration getComponentConfiguration(String id) {
        return this.canonicalComponentConfigurations.get(id);
    }

    public Map<String, HstComponentConfiguration> getComponentConfigurations() {
        return Collections.unmodifiableMap(this.canonicalComponentConfigurations);
    }


    public List<HstComponentConfiguration> getAvailableContainerItems() {
        return availableContainerItems;
    }
    
    private void autocreateReferenceNames(HstComponentConfiguration componentConfiguration) {
        if (componentConfiguration.getReferenceName() == null || "".equals(componentConfiguration.getReferenceName())) {
            String autoRefName = "r" + (++autocreatedCounter);
            while (usedReferenceNames.contains(autoRefName)) {
                autoRefName = "r" + (++autocreatedCounter);
            }
            ((HstComponentConfigurationService) componentConfiguration).setReferenceName(StringPool.get(autoRefName));
        }
        ((HstComponentConfigurationService) componentConfiguration).autocreateReferenceNames();
    }

    private void populateCanonicalComponentConfigurations(HstComponentConfiguration componentConfiguration) {
        if (componentConfiguration.getId() != null) {
            canonicalComponentConfigurations.put(componentConfiguration.getId(), componentConfiguration);
        }
        for (HstComponentConfiguration child : componentConfiguration.getChildren().values()) {
            populateCanonicalComponentConfigurations(child);
        }
    }
    
    /*
     * rootNodeName is either hst:components or hst:pages.
     */
    private void init(HstNode node, String rootNodeName) {
        for(HstNode child : node.getNodes()) {
            if(HstNodeTypes.NODETYPE_HST_COMPONENT.equals(child.getNodeTypeName())
              || HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT.equals(child.getNodeTypeName())
              || HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT.equals(child.getNodeTypeName())
            )
            {
                if(child.getValueProvider().hasProperty(HstNodeTypes.COMPONENT_PROPERTY_REFERECENCENAME)) {
                 // add to the used referencenames set 
                    usedReferenceNames.add(StringPool.get(child.getValueProvider().getString(HstNodeTypes.COMPONENT_PROPERTY_REFERECENCENAME)));
                }
                try {
                    HstComponentConfiguration componentConfiguration = new HstComponentConfigurationService(child,
                            null, rootNodeName);
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
    
    private void initCatalog(HstNode catalog) {
        
        for(HstNode itemPackage :catalog.getNodes()){
            if(HstNodeTypes.NODETYPE_HST_CONTAINERITEM_PACKAGE.equals(itemPackage.getNodeTypeName())) {
                for(HstNode containerItem : itemPackage.getNodes()) {
                    if(HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT.equals(containerItem.getNodeTypeName()))
                    {
                        try {
                            // create a HstComponentConfigurationService that does not traverse to descendant components: this is not needed for the catalog. Hence, the argument 'false'
                            HstComponentConfiguration componentConfiguration = new HstComponentConfigurationService(containerItem,null, HstNodeTypes.NODENAME_HST_COMPONENTS , false);
                            availableContainerItems.add(componentConfiguration);
                            log.debug("Added catalog component to availableContainerItems with key '{}'", componentConfiguration.getId());
                        } catch (ServiceException e) {
                            if (log.isDebugEnabled()) {
                                log.warn("Skipping catalog component '{}'", containerItem.getValueProvider().getPath(), e);
                            } else if (log.isWarnEnabled()) {
                                log.warn("Skipping catalog component '{}' : '{}'", containerItem.getValueProvider().getPath(), e.toString());
                            }
                        }
                    }
                    else {
                        log.warn("Skipping catalog component '{}' because is not of type '{}'", containerItem.getValueProvider().getPath(),
                                (HstNodeTypes.NODETYPE_HST_COMPONENT));
                    }
                }
            } else {
                log.warn("Skipping node '{}' because is not of type '{}'", itemPackage.getValueProvider().getPath(),
                        (HstNodeTypes.NODETYPE_HST_CONTAINERITEM_PACKAGE));
            }
        }
    }

    private Map<String, HstNode> getTemplateResourceMap(HstNode configurationNode) throws ServiceException {
        Map<String, HstNode> templateResourceMap = new HashMap<String, HstNode>();
           
           // templates
           HstNode hstTemplates = configurationNode.getNode(HstNodeTypes.NODENAME_HST_TEMPLATES);
           
           if(hstTemplates == null) {
               throw new ServiceException("There are no '"+HstNodeTypes.NODENAME_HST_TEMPLATES+"' present for the configuration at '"+configurationNode.getValueProvider().getPath()+"'");
           }
           
           for(HstNode template : hstTemplates.getNodes()) {
               ValueProvider valueProvider = template.getValueProvider();
               boolean renderPathExisting = valueProvider.hasProperty(HstNodeTypes.TEMPLATE_PROPERTY_RENDERPATH);
               boolean scriptExisting = valueProvider.hasProperty(HstNodeTypes.TEMPLATE_PROPERTY_SCRIPT);
               
               if (!renderPathExisting && !scriptExisting) {
                   log.warn("Skipping template '{}' because missing property, either hst:renderpath or hst:script.", valueProvider.getPath());
                   continue;
               }
               
               if (renderPathExisting && !scriptExisting) {
                   String resourcePath = valueProvider.getString(HstNodeTypes.TEMPLATE_PROPERTY_RENDERPATH);
                   
                   if (StringUtils.isBlank(resourcePath)) {
                       log.warn("Skipping template '{}' because of invalid hst:renderpath value.", valueProvider.getPath());
                       continue;
                   }
               }
               
               templateResourceMap.put(valueProvider.getName(), template);
           }
        return templateResourceMap;
    }

}
