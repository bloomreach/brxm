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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.core.component.GenericHstComponent;
import org.hippoecm.hst.provider.PropertyMap;
import org.hippoecm.hst.service.ServiceException;
import org.slf4j.LoggerFactory;

public class HstComponentConfigurationService implements HstComponentConfiguration {

    private static final long serialVersionUID = 1L;

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(HstComponentConfigurationService.class);

    private Map<String, HstComponentConfiguration> componentConfigurations = new LinkedHashMap<String, HstComponentConfiguration>();

    private Map<String, HstComponentConfigurationService> childConfByName = new HashMap<String, HstComponentConfigurationService>();

    private Map<String, HstComponentConfiguration> derivedChildrenByName = null;

    private List<HstComponentConfigurationService> orderedListConfigs = new ArrayList<HstComponentConfigurationService>();

    private HstComponentConfiguration parent;

    private String id;

    private String name;

    private String componentClassName;

    private boolean hasClassNameConfigured;

    private String renderPath;
    
    private String containerType;

    /**
     * Components of type {@link Type#CONTAINER_ITEM_COMPONENT} can have sample content
     */
    private String sampleContent;
    
    /**
     * the type of this {@link HstComponentConfiguration}. 
     */
    private Type type;

    private String hstTemplate;

    private String serveResourcePath;

    private String referenceName;

    private String referenceComponent;

    private String pageErrorHandlerClassName;

    private PropertyMap propertyMap;

    private String configurationRootNodePath;
    
    private ArrayList<String> usedChildReferenceNames = new ArrayList<String>();
    private int autocreatedCounter = 0;

    private Map<String, String> parameters = new HashMap<String, String>();
    private Map<String, String> localParameters = new HashMap<String, String>();
    
    private String canonicalStoredLocation;
    
    private String canonicalIdentifier;

    // constructor for copy purpose only
    private HstComponentConfigurationService(String id) {
        this.id = id;
    }

    public HstComponentConfigurationService(HstNode node, HstComponentConfiguration parent,
            String hstConfigurationsNodePath) throws ServiceException {
        if (!node.getValueProvider().getPath().startsWith(hstConfigurationsNodePath)) {
            throw new ServiceException(
                    "Node path of the component cannot start without the global components path. Skip Component");
        }

        this.canonicalStoredLocation = node.getValueProvider().getCanonicalPath();
        this.canonicalIdentifier = node.getValueProvider().getIdentifier();
       
        if(HstNodeTypes.NODETYPE_HST_COMPONENT.equals(node.getNodeTypeName())) {
          type = Type.COMPONENT;
        } else if(HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT.equals(node.getNodeTypeName())) {
          type = Type.CONTAINER_COMPONENT;
        } else if(HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT.equals(node.getNodeTypeName())) {
          type = Type.CONTAINER_ITEM_COMPONENT;
          sampleContent = node.getValueProvider().getString(HstNodeTypes.COMPONENT_PROPERTY_SAMPLE_CONTENT);
        } else {
            throw new ServiceException("Unknown componentType '"+node.getNodeTypeName()+"' for '"+canonicalStoredLocation+"'. Cannot build configuration.");
        }
        
        this.parent = parent;

        this.configurationRootNodePath = hstConfigurationsNodePath;
        // id is the relative path wrt configuration components path
        this.id = node.getValueProvider().getPath().substring(hstConfigurationsNodePath.length() + 1);

        this.name = node.getValueProvider().getName();
        this.referenceName = node.getValueProvider().getString(HstNodeTypes.COMPONENT_PROPERTY_REFERECENCENAME);
        this.componentClassName = node.getValueProvider().getString(HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_CLASSNAME);
        if (componentClassName == null) {
            this.componentClassName = GenericHstComponent.class.getName();
        } else {
            this.hasClassNameConfigured = true;
        }

        this.referenceComponent = node.getValueProvider().getString(HstNodeTypes.COMPONENT_PROPERTY_REFERECENCECOMPONENT);
        
        if(referenceComponent != null) {
            if(type == Type.CONTAINER_COMPONENT) {
                throw new ServiceException("ContainerComponents are not allowed to have a reference. Pls fix the" +
                		"configuration for '"+canonicalStoredLocation+"'");
            }
        }
        
        this.hstTemplate = node.getValueProvider().getString(HstNodeTypes.COMPONENT_PROPERTY_TEMPLATE_);
        this.serveResourcePath = node.getValueProvider().getString(HstNodeTypes.COMPONENT_PROPERTY_SERVE_RESOURCE_PATH);
        this.pageErrorHandlerClassName = node.getValueProvider().getString(HstNodeTypes.COMPONENT_PROPERTY_PAGE_ERROR_HANDLER_CLASSNAME);
        this.propertyMap = node.getValueProvider().getPropertyMap();
        
        if(type == Type.CONTAINER_COMPONENT) {
            this.containerType = node.getValueProvider().getString(HstNodeTypes.COMPONENT_PROPERTY_CONTAINERTYPE);
        } else if(node.getValueProvider().hasProperty(HstNodeTypes.COMPONENT_PROPERTY_CONTAINERTYPE)){
            log.warn("Ignoring property '{}' as this property is only allowed on nodes of type '{}'", HstNodeTypes.COMPONENT_PROPERTY_CONTAINERTYPE, HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT);
        }
        String[] parameterNames = node.getValueProvider().getStrings(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES);
        String[] parameterValues = node.getValueProvider().getStrings(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES);

        if (parameterNames != null && parameterValues != null) {
            if (parameterNames.length != parameterValues.length) {
                log.warn("Skipping parameters for component because they only make sense if there are equal number of names and values");
            } else {
                for (int i = 0; i < parameterNames.length; i++) {
                    this.parameters.put(parameterNames[i], parameterValues[i]);
                    this.localParameters.put(parameterNames[i], parameterValues[i]);
                }
            }
        }
      
        for(HstNode child : node.getNodes()) {
            if(HstNodeTypes.NODETYPE_HST_COMPONENT.equals(node.getNodeTypeName())
                    || HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT.equals(node.getNodeTypeName())
                    || HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT.equals(node.getNodeTypeName())
                  )  {
                if (child.getValueProvider().hasProperty(HstNodeTypes.COMPONENT_PROPERTY_REFERECENCENAME)) {
                    usedChildReferenceNames.add(child.getValueProvider().getString(HstNodeTypes.COMPONENT_PROPERTY_REFERECENCENAME));
                }
                try {
                    HstComponentConfigurationService componentConfiguration = new HstComponentConfigurationService(
                            child, this, hstConfigurationsNodePath);
                    componentConfigurations.put(componentConfiguration.getId(), componentConfiguration);

                    // we also need an ordered list
                    orderedListConfigs.add(componentConfiguration);
                    childConfByName.put(child.getValueProvider().getName(), componentConfiguration);
                    log.debug("Added component service with key '{}'", componentConfiguration.getId());
                } catch (ServiceException e) {
                    if (log.isDebugEnabled()) {
                        log.warn("Skipping component '{}'", child.getValueProvider().getPath(), e);
                    } else if (log.isWarnEnabled()) {
                        log.warn("Skipping component '{}'", child.getValueProvider().getPath());
                    }
                }
            } else {
                log.warn("Skipping node '{}' because is not of type '{}'", child.getValueProvider().getPath(),
                        (HstNodeTypes.NODETYPE_HST_COMPONENT));
            }
        }
        
    }

    public HstComponentConfiguration getParent() {
        return parent;
    }

    public String getComponentClassName() {
        return this.componentClassName;
    }

    public String getRenderPath() {
        return this.renderPath;
    }
    
    public String getContainerType() {
        return this.containerType;
    }

    public Type getComponentType() {
        return this.type;
    }


    public String getHstTemplate() {
        return this.hstTemplate;
    }

    public String getServeResourcePath() {
        return this.serveResourcePath;
    }

    public String getParameter(String name) {
        return this.parameters.get(name);
    }

    public Map<String, String> getParameters() {
        return Collections.unmodifiableMap(this.parameters);
    }
    

	public String getLocalParameter(String name) {
		return this.localParameters.get(name);
	}

	public Map<String, String> getLocalParameters() {
		return Collections.unmodifiableMap(this.localParameters);
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

    public String getReferenceComponent() {
        return referenceComponent;
    }


    public String getPageErrorHandlerClassName() {
        return pageErrorHandlerClassName;
    }

    public String getSampleContent(){
        return sampleContent;
    }
    
    public Map<String, HstComponentConfiguration> getChildren() {
        return Collections.unmodifiableMap(this.componentConfigurations);
    }

    public HstComponentConfiguration getChildByName(String name) {
        if (derivedChildrenByName == null) {
            HashMap<String, HstComponentConfiguration> children = new HashMap<String, HstComponentConfiguration>();
            for (HstComponentConfiguration config : orderedListConfigs) {
                children.put(config.getName(), config);
            }
            derivedChildrenByName = children;
        }
        return derivedChildrenByName.get(name);
    }
    
    public String getCanonicalStoredLocation() {
        return canonicalStoredLocation;
    }

    public String getCanonicalIdentifier() {
        return canonicalIdentifier;
    }
    
    private HstComponentConfigurationService deepCopy(HstComponentConfigurationService parent, String newId,
            HstComponentConfigurationService child, List<HstComponentConfiguration> populated,
            Map<String, HstComponentConfiguration> rootComponentConfigurations) throws ServiceException {
        if (child.getReferenceComponent() != null) {
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
        copy.pageErrorHandlerClassName = child.pageErrorHandlerClassName;
        copy.containerType = child.containerType;
        copy.type = child.type;
        copy.serveResourcePath = child.serveResourcePath;
        copy.canonicalStoredLocation = child.canonicalStoredLocation;
        copy.canonicalIdentifier = child.canonicalIdentifier;
        copy.sampleContent = child.sampleContent;
        copy.parameters = new HashMap<String, String>(child.parameters);
        // localParameters have no merging, but for copy, the localParameters are copied 
        copy.localParameters = new HashMap<String, String>(child.localParameters);
        ArrayList<String> copyToList = (ArrayList<String>) child.usedChildReferenceNames.clone();
        copy.usedChildReferenceNames = copyToList;
        for (HstComponentConfigurationService descendant : child.orderedListConfigs) {
            String descId = copy.id + descendant.id;
            HstComponentConfigurationService copyDescendant = deepCopy(copy, descId, descendant, populated,
                    rootComponentConfigurations);
            copy.componentConfigurations.put(copyDescendant.id, copyDescendant);
            copy.orderedListConfigs.add(copyDescendant);
            copy.childConfByName.put(copyDescendant.getName(), copyDescendant);
            // do not need them by name for copies
        }
        // the copy is populated
        populated.add(copy);
        return copy;
    }

    protected void populateComponentReferences(Map<String, HstComponentConfiguration> rootComponentConfigurations,
            List<HstComponentConfiguration> populated) throws ServiceException{
        if (populated.contains(this)) {
            return;
        }

        populated.add(this);

        if (this.getReferenceComponent() != null) {
            HstComponentConfigurationService referencedComp = (HstComponentConfigurationService) rootComponentConfigurations
                    .get(this.getReferenceComponent());
            if (referencedComp != null) {
                if(referencedComp == this) {
                    throw new ServiceException("There is a component referencing itself: this is not allowed. The site configuration cannot be loaded. ComponentId = "+this.getId());
                }
                if (referencedComp.getReferenceComponent() != null) {
                    // populate referenced comp first:
                    referencedComp.populateComponentReferences(rootComponentConfigurations, populated);
                }
                // get all properties that are null from the referenced component:
                if (!this.hasClassNameConfigured) {
                    this.componentClassName = referencedComp.componentClassName;
                }
                if (this.configurationRootNodePath == null) {
                    this.configurationRootNodePath = referencedComp.configurationRootNodePath;
                }
                if (this.hstTemplate == null) {
                    this.hstTemplate = referencedComp.hstTemplate;
                }
                if (this.name == null) {
                    this.name = referencedComp.name;
                }
                if (this.propertyMap == null) {
                    this.propertyMap = referencedComp.propertyMap;
                }
                if (this.referenceName == null) {
                    this.referenceName = referencedComp.referenceName;
                }
                if (this.renderPath == null) {
                    this.renderPath = referencedComp.renderPath;
                }
                if (this.referenceComponent == null) {
                    this.referenceComponent = referencedComp.referenceComponent;
                }
                if (this.serveResourcePath == null) {
                    this.serveResourcePath = referencedComp.serveResourcePath;
                }
                if (this.canonicalStoredLocation == null) {
                    this.canonicalStoredLocation = referencedComp.canonicalStoredLocation;
                }
                if (this.canonicalIdentifier == null) {
                    this.canonicalIdentifier = referencedComp.canonicalIdentifier;
                }
                if (this.pageErrorHandlerClassName == null) {
                    this.pageErrorHandlerClassName = referencedComp.pageErrorHandlerClassName;
                }
                if (this.containerType == null) {
                    this.containerType = referencedComp.containerType;
                }
                if (this.sampleContent == null) {
                    this.sampleContent = referencedComp.sampleContent;
                }
                
                if (this.parameters == null) {
                    this.parameters = new HashMap<String, String>(referencedComp.parameters);
                } else if (referencedComp.parameters != null) {
                    // as we already have parameters, add only the once we do not yet have
                    for (Entry<String, String> entry : referencedComp.parameters.entrySet()) {
                        if (this.parameters.containsKey(entry.getKey())) {
                            // skip: we already have this parameter set ourselves
                        } else {
                            this.parameters.put(entry.getKey(), entry.getValue());
                        }
                    }
                }

                ArrayList<String> copyToList = (ArrayList<String>) referencedComp.usedChildReferenceNames.clone();
                this.usedChildReferenceNames.addAll(copyToList);

                // now we need to merge all the descendant components from the referenced component with this component.

                for (HstComponentConfigurationService childToMerge : referencedComp.orderedListConfigs) {
                  
                    if (childToMerge.getReferenceComponent() != null) {
                        // populate child component if not yet happened
                        childToMerge.populateComponentReferences(rootComponentConfigurations, populated);
                    }
                    
                    if (this.childConfByName.get(childToMerge.name) != null) {
                        // we have an overlay again because we have a component with the same name
                        // first populate it
                        HstComponentConfigurationService existingChild = this.childConfByName.get(childToMerge.name);
                        existingChild.populateComponentReferences(rootComponentConfigurations, populated);
                        // merge the childToMerge with existingChild
                        existingChild.combine(childToMerge, rootComponentConfigurations, populated);
                    } else  {
                        // make a copy of the child
                        addDeepCopy(childToMerge, populated, rootComponentConfigurations);
                    }
                }

            } else {
                log.warn("Cannot lookup referenced component '{}' for this component ['{}']. We skip this reference", this
                        .getReferenceComponent(), this.getId());
            }
        }
    }

    private void combine(HstComponentConfigurationService childToMerge,
            Map<String, HstComponentConfiguration> rootComponentConfigurations,
            List<HstComponentConfiguration> populated) throws ServiceException {
        
        if(this.type == Type.CONTAINER_COMPONENT || childToMerge.type == Type.CONTAINER_COMPONENT) {
            throw new ServiceException("Incorrect component configuration: *Container* Components are not allowed to be merged with other " +
            		"components. Cannot merge '"+childToMerge.getId()+"' and '"+this.getId()+"' because at least one of them is a Container component. Fix configuration.");
        }
        
        if (!this.hasClassNameConfigured) {
            this.componentClassName = childToMerge.componentClassName;
        }
        if (this.configurationRootNodePath == null) {
            this.configurationRootNodePath = childToMerge.configurationRootNodePath;
        }
        if (this.hstTemplate == null) {
            this.hstTemplate = childToMerge.hstTemplate;
        }
        if (this.name == null) {
            this.name = childToMerge.name;
        }
        if (this.propertyMap == null) {
            this.propertyMap = childToMerge.propertyMap;
        }
        if (this.referenceName == null) {
            this.referenceName = childToMerge.referenceName;
        }
        if (this.renderPath == null) {
            this.renderPath = childToMerge.renderPath;
        }
        if (this.referenceComponent == null) {
            this.referenceComponent = childToMerge.referenceComponent;
        }
        if (this.serveResourcePath == null) {
            this.serveResourcePath = childToMerge.serveResourcePath;
        }
        if (this.pageErrorHandlerClassName == null) {
            this.pageErrorHandlerClassName = childToMerge.pageErrorHandlerClassName;
        }
        if (this.containerType == null) {
            this.containerType = childToMerge.containerType;
        }
        if (this.sampleContent == null) {
            this.sampleContent = childToMerge.sampleContent;
        }
        
        if (this.parameters == null) {
            this.parameters = new HashMap<String, String>(childToMerge.parameters);
        } else if (childToMerge.parameters != null) {
            // as we already have parameters, add only the once we do not yet have
            for (Entry<String, String> entry : childToMerge.parameters.entrySet()) {
                if (this.parameters.containsKey(entry.getKey())) {
                    // skip: we already have this parameter set ourselves
                } else {
                    this.parameters.put(entry.getKey(), entry.getValue());
                }
            }
        }
        for (HstComponentConfigurationService toMerge : childToMerge.orderedListConfigs) {
            if (this.childConfByName.get(toMerge.name) != null) {
                this.childConfByName.get(toMerge.name).combine(toMerge, rootComponentConfigurations, populated);
            } else {
                //  String newId = this.id + "-" + toMerge.id;
                //  this.deepCopy(this, newId, toMerge, populated, rootComponentConfigurations);
                
                this.addDeepCopy(toMerge, populated, rootComponentConfigurations);
            }
        }

    }

    private void addDeepCopy(HstComponentConfigurationService childToMerge, List<HstComponentConfiguration> populated,
            Map<String, HstComponentConfiguration> rootComponentConfigurations) throws ServiceException {

        String newId = this.id + "-" + childToMerge.id;
        
        HstComponentConfigurationService copy = deepCopy(this, newId, childToMerge, populated,
                rootComponentConfigurations);
        this.componentConfigurations.put(copy.getId(), copy);
        this.childConfByName.put(copy.getName(), copy);
        this.orderedListConfigs.add(copy);

    }

    protected void setRenderPath(Map<String, String> templateRenderMap) {
        String templateRenderPath = templateRenderMap.get(this.getHstTemplate());
        if (templateRenderPath == null) {
            log.debug("Cannot find renderpath for component '{}'. This component can only be used to be extended", this
                    .getId());
        }
        this.renderPath = templateRenderPath;
        for (HstComponentConfigurationService child : orderedListConfigs) {
            child.setRenderPath(templateRenderMap);
        }
    }

    protected void inheritParameters() {
        // before traversing child components add the parameters from the parent, and if already present, override them
        if (this.parent != null && this.parent.getParameters() != null) {
            this.parameters.putAll(this.parent.getParameters());
        }
        for (HstComponentConfigurationService child : orderedListConfigs) {
            child.inheritParameters();
        }
    }

    protected void autocreateReferenceNames() {

        for (HstComponentConfigurationService child : orderedListConfigs) {
            child.autocreateReferenceNames();
            if (child.getReferenceName() == null || "".equals(child.getReferenceName())) {
                String autoRefName = "r" + (++autocreatedCounter);
                while (usedChildReferenceNames.contains(autoRefName)) {
                    autoRefName = "r" + (++autocreatedCounter);
                }
                child.setReferenceName(autoRefName);
            }
        }
    }


}
