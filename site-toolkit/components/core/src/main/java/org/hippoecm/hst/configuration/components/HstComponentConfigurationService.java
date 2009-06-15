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
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.Configuration;
import org.hippoecm.hst.core.component.GenericHstComponent;
import org.hippoecm.hst.provider.PropertyMap;
import org.hippoecm.hst.service.AbstractJCRService;
import org.hippoecm.hst.service.Service;
import org.hippoecm.hst.service.ServiceException;
import org.slf4j.LoggerFactory;

public class HstComponentConfigurationService extends AbstractJCRService implements HstComponentConfiguration, Service{
    
    private static final long serialVersionUID = 1L;

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(HstComponentConfigurationService.class);
    
    private SortedMap<String, HstComponentConfiguration> componentConfigurations = new TreeMap<String, HstComponentConfiguration>();
    
    private Map<String, HstComponentConfigurationService> childConfByName = new HashMap<String, HstComponentConfigurationService>();
    
    private List<HstComponentConfigurationService> orderedListConfigs = new ArrayList<HstComponentConfigurationService>();
    
    private HstComponentConfiguration parent;
    
    private String id;
    
    private String name;

    private String componentClassName;
    
    private boolean hasClassNameConfigured;

    private String renderPath;
    
    private String hstTemplate;
    
    private String serveResourcePath;
    
    private String referenceName;
    
    private String referenceComponent;
    
    private PropertyMap propertyMap;
    
    private String configurationRootNodePath;
    
    private List<String> usedChildReferenceNames = new ArrayList<String>();
    private int autocreatedCounter = 0;
    
    private Map<String,String> parameters = new HashMap<String,String>();

    // constructor for copy purpose only
    private HstComponentConfigurationService(String id){
        super(null);
        this.id = id;
    }

    
    public HstComponentConfigurationService(Node jcrNode, HstComponentConfiguration parent,  String configurationRootNodePath) throws ServiceException {
        super(jcrNode);
        if(!getValueProvider().getPath().startsWith(configurationRootNodePath)) {
            throw new ServiceException("Node path of the component cannot start without the global components path. Skip Component");
        }
        
        this.parent = parent;
        
        this.configurationRootNodePath = configurationRootNodePath;
        // id is the relative path wrt configuration components path
        this.id = getValueProvider().getPath().substring(configurationRootNodePath.length()+1);
        
        if (getValueProvider().isNodeType(Configuration.NODETYPE_HST_COMPONENT)) {
            this.name = getValueProvider().getName();
            this.referenceName = getValueProvider().getString(Configuration.COMPONENT_PROPERTY_REFERECENCENAME);
            this.componentClassName = getValueProvider().getString(Configuration.COMPONENT_PROPERTY_COMPONENT_CLASSNAME);
            if(componentClassName == null) {
                this.componentClassName = GenericHstComponent.class.getName();
            } else {
                this.hasClassNameConfigured = true;
            }
            
            this.referenceComponent = getValueProvider().getString(Configuration.COMPONENT_PROPERTY_REFERECENCECOMPONENT);
            this.hstTemplate = getValueProvider().getString(Configuration.COMPONENT_PROPERTY_TEMPLATE_);
            this.serveResourcePath = getValueProvider().getString(Configuration.COMPONENT_PROPERTY_SERVE_RESOURCE_PATH);
            this.propertyMap = getValueProvider().getPropertyMap();
            String[] parameterNames = getValueProvider().getStrings(Configuration.COMPONENT_PROPERTY_PARAMETER_NAMES);
            String[] parameterValues = getValueProvider().getStrings(Configuration.COMPONENT_PROPERTY_PARAMETER_VALUES);
            
            if(parameterNames != null && parameterValues != null){
               if(parameterNames.length != parameterValues.length) {
                   log.warn("Skipping parameters for component because they only make sense if there are equal number of names and values");
               }  else {
                   for(int i = 0; i < parameterNames.length ; i++) {
                       this.parameters.put(parameterNames[i], parameterValues[i]);
                   }
               }
            }
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
                        usedChildReferenceNames.add(child.getProperty(Configuration.COMPONENT_PROPERTY_REFERECENCENAME).getString());
                    }
                    try {
                        HstComponentConfigurationService componentConfiguration = new HstComponentConfigurationService(child, this, configurationRootNodePath);
                        componentConfigurations.put(componentConfiguration.getId(), componentConfiguration);
                        
                        // we also need an ordered list
                        orderedListConfigs.add(componentConfiguration);
                        childConfByName.put(child.getName(),componentConfiguration); 
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
    
    public String getHstTemplate(){
        return this.hstTemplate;
    }
    
    public String getServeResourcePath() {
        return this.serveResourcePath;
    }

    public String getParameter(String name) {
        return this.parameters.get(name);
    }
    
    public Map<String, String> getParameters(){
        return Collections.unmodifiableMap(this.parameters);
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

    public void setReferenceName(String referenceName) {
         this.referenceName = referenceName;
    }

    public String getReferenceComponent(){
        return referenceComponent;
    }
    
    public SortedMap<String, HstComponentConfiguration> getChildren() {
       return Collections.unmodifiableSortedMap(this.componentConfigurations);
    }


    private HstComponentConfigurationService deepCopy(HstComponentConfigurationService parent, String newId, HstComponentConfigurationService child, List<HstComponentConfiguration> populated, Map<String, HstComponentConfiguration> rootComponentConfigurations){
        if(child.getReferenceComponent() != null) {
            // populate child component if not yet happened
            child.populateComponentReferences(rootComponentConfigurations, populated);
        }
        HstComponentConfigurationService copy = new HstComponentConfigurationService(newId);
        copy.parent = parent;
        copy.componentClassName = child.componentClassName;
        copy.configurationRootNodePath = child.configurationRootNodePath;
        copy.hstTemplate = child.hstTemplate;
        copy.name = child.name;
        copy.propertyMap = child.propertyMap;
        copy.referenceName = child.referenceName;
        copy.renderPath = child.renderPath;
        copy.referenceComponent = child.referenceComponent;
        copy.serveResourcePath = child.serveResourcePath;
        copy.parameters = child.parameters;
        List<String> copyToList = new ArrayList<String>();
        Collections.copy(copyToList, child.usedChildReferenceNames);
        copy.usedChildReferenceNames = copyToList;
        for(HstComponentConfigurationService descendant : child.orderedListConfigs) {
            String descId = copy.id + descendant.id;
            HstComponentConfigurationService copyDescendant = deepCopy(copy,descId,descendant, populated, rootComponentConfigurations);
            copy.componentConfigurations.put(copyDescendant.id, copyDescendant);
            copy.orderedListConfigs.add(copyDescendant);
            // do not need them by name for copies
        }
        return copy;
    }
    
    protected void populateComponentReferences(Map<String, HstComponentConfiguration> rootComponentConfigurations, List<HstComponentConfiguration> populated) {
        if(populated.contains(this)) {
            return;
        }
        
        populated.add(this);
        
        if(this.getReferenceComponent() != null){
            HstComponentConfigurationService referencedComp = (HstComponentConfigurationService)rootComponentConfigurations.get(this.getReferenceComponent());
            if(referencedComp != null) {
                 if(referencedComp.getReferenceComponent() != null) {
                     // populate referenced comp first:
                     referencedComp.populateComponentReferences(rootComponentConfigurations, populated);
                 }
                 // get all properties that are null from the referenced component:
                 if(!this.hasClassNameConfigured) this.componentClassName = referencedComp.componentClassName;
                 if(this.configurationRootNodePath == null) this.configurationRootNodePath = referencedComp.configurationRootNodePath;
                 if(this.hstTemplate == null) this.hstTemplate = referencedComp.hstTemplate;
                 if(this.name == null) this.name = referencedComp.name;
                 if(this.propertyMap == null) this.propertyMap = referencedComp.propertyMap;
                 if(this.referenceName == null) this.referenceName = referencedComp.referenceName;
                 if(this.renderPath == null) this.renderPath = referencedComp.renderPath;
                 if(this.referenceComponent == null) this.referenceComponent = referencedComp.referenceComponent;
                 if(this.serveResourcePath == null) this.serveResourcePath = referencedComp.serveResourcePath;
                 
                 if(this.parameters == null) {
                     this.parameters = referencedComp.parameters;
                 } else if(referencedComp.parameters != null){
                     // as we already have parameters, add only the once we do not yet have
                     for(Entry<String,String> entry : referencedComp.parameters.entrySet()){
                         if(this.parameters.containsKey(entry.getKey())) { 
                             // skip: we already have this parameter set ourselves
                         } else {
                           this.parameters.put(entry.getKey(), entry.getValue());  
                         }
                     }
                 }
                
                 List<String> copyToList = new ArrayList<String>();
                 Collections.copy(copyToList, referencedComp.usedChildReferenceNames);
                 this.usedChildReferenceNames.addAll(copyToList);
                
                 // now we need to merge all the descendant components from the referenced component with this component.
                 
                 for(HstComponentConfigurationService childToMerge : referencedComp.orderedListConfigs){
                     if(childToMerge.getReferenceComponent() != null) {
                         // populate child component if not yet happened
                         childToMerge.populateComponentReferences(rootComponentConfigurations, populated);
                         // after population, add it
                         addDeepCopy(childToMerge, populated, rootComponentConfigurations);
                     }
                     if(this.childConfByName.get(childToMerge.name) != null){
                         // we have an overlay again because we have a component with the same name
                         // first populate it
                         HstComponentConfigurationService existingChild = this.childConfByName.get(childToMerge.name);
                         existingChild.populateComponentReferences(rootComponentConfigurations, populated);
                         // merge the childToMerge with existingChild
                         existingChild.combine(childToMerge, rootComponentConfigurations, populated);
                     } else {
                         // make a copy of the child
                         addDeepCopy(childToMerge, populated,rootComponentConfigurations);
                     } 
                 }
                  
            } else {
                log.warn("Cannot lookup referenced component '{}' for this component ['{}']", this.getReferenceComponent(), this.getId());
            }
        }
    }
    
    private void combine(HstComponentConfigurationService childToMerge, Map<String, HstComponentConfiguration> rootComponentConfigurations, List<HstComponentConfiguration> populated) {
        if(!this.hasClassNameConfigured) this.componentClassName = childToMerge.componentClassName;
        if(this.configurationRootNodePath == null) this.configurationRootNodePath = childToMerge.configurationRootNodePath;
        if(this.hstTemplate == null) this.hstTemplate = childToMerge.hstTemplate;
        if(this.name == null) this.name = childToMerge.name;
        if(this.propertyMap == null) this.propertyMap = childToMerge.propertyMap;
        if(this.referenceName == null) this.referenceName = childToMerge.referenceName;
        if(this.renderPath == null) this.renderPath = childToMerge.renderPath;
        if(this.referenceComponent == null) this.referenceComponent = childToMerge.referenceComponent;
        if(this.serveResourcePath == null) this.serveResourcePath = childToMerge.serveResourcePath;
        if(this.parameters == null) {
            this.parameters = childToMerge.parameters;
        } else if(childToMerge.parameters != null){
            // as we already have parameters, add only the once we do not yet have
            for(Entry<String,String> entry : childToMerge.parameters.entrySet()){
                if(this.parameters.containsKey(entry.getKey())) { 
                    // skip: we already have this parameter set ourselves
                } else {
                  this.parameters.put(entry.getKey(), entry.getValue());  
                }
            }
        }
        for(HstComponentConfigurationService toMerge :  childToMerge.orderedListConfigs) {
            if(this.childConfByName.get(toMerge.name) != null){
                this.childConfByName.get(toMerge.name).combine(toMerge, rootComponentConfigurations, populated);
            } else {
              //  String newId = this.id + "-" + toMerge.id;
              //  this.deepCopy(this, newId, toMerge, populated, rootComponentConfigurations);
                this.addDeepCopy(toMerge, populated, rootComponentConfigurations );
            }
        }
        
    }
    
    private void addDeepCopy(HstComponentConfigurationService childToMerge, List<HstComponentConfiguration> populated, Map<String, HstComponentConfiguration> rootComponentConfigurations) {
        
        String newId = this.id + "-" + childToMerge.id;
        HstComponentConfigurationService copy = this.deepCopy(this, newId, childToMerge, populated, rootComponentConfigurations);
        this.componentConfigurations.put(copy.getId(), copy);
        this.orderedListConfigs.add(copy);
        
        
    }

    
    protected void setRenderPath(Map<String, String> templateRenderMap) {
        String templateRenderPath = templateRenderMap.get(this.getHstTemplate());
        if(templateRenderPath == null) {
            log.debug("Cannot find renderpath for component '{}'. This component can only be used to be extended", this.getId());
        }
        this.renderPath = templateRenderPath;
        for(HstComponentConfigurationService child :  orderedListConfigs) {
            child.setRenderPath(templateRenderMap);
        }
    }
    
    protected void inheritParameters(){
        // before traversing child components add the parameters from the parent, and if already present, override them
        if(this.parent != null && this.parent.getParameters() != null) {
            this.parameters.putAll(this.parent.getParameters());
        }
        for(HstComponentConfigurationService child :  orderedListConfigs) {
            child.inheritParameters();
        }
    }
    
    protected void autocreateReferenceNames() {
        
        for(HstComponentConfigurationService child :  orderedListConfigs) {
            child.autocreateReferenceNames();
            if(child.getReferenceName() == null) {
                String autoRefName = "r" + (++autocreatedCounter);
                while(usedChildReferenceNames.contains(autoRefName)){
                    autoRefName = "r" + (++autocreatedCounter);
                }
                child.setReferenceName(autoRefName);
            }
        }
    }



   
}
