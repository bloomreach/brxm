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
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.core.component.HstURL;
import org.hippoecm.hst.provider.ValueProvider;
import org.hippoecm.hst.service.ServiceException;
import org.onehippo.cms7.utilities.pools.StringPool;
import org.slf4j.LoggerFactory;

public class HstComponentConfigurationService implements HstComponentConfiguration {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(HstComponentConfigurationService.class);

    private Map<String, HstComponentConfiguration> componentConfigurations = new LinkedHashMap<String, HstComponentConfiguration>();

    private Map<String, HstComponentConfigurationService> childConfByName = new HashMap<String, HstComponentConfigurationService>();

    private Map<String, HstComponentConfiguration> derivedChildrenByName = null;

    private List<HstComponentConfigurationService> orderedListConfigs = new ArrayList<HstComponentConfigurationService>();

    private HstComponentConfiguration parent;

    private String id;

    private String name;

    private String componentClassName;

    private String hstTemplate;
    
    private String hstResourceTemplate;

    private boolean isNamedRenderer;
    
    private boolean isNamedResourceServer;
    
    private String renderPath;
    
    private String serveResourcePath;
    
    private String xtype;

    /**
     * Components of type {@link Type#CONTAINER_ITEM_COMPONENT} can have a filter tag to trigger their rendering.
     */
    private String componentFilterTag;

    /**
     * the type of this {@link HstComponentConfiguration}. 
     */
    private Type type;

    private String referenceName;

    private String referenceComponent;

    private String pageErrorHandlerClassName;

    private List<String> usedChildReferenceNames = new ArrayList<String>();
    private int autocreatedCounter = 0;

    private Map<String, String> parameters = new LinkedHashMap<String, String>();
    
    // the set of parameter prefixes
    private Set<String> parameterNamePrefixSet = new HashSet<String>(); 
    
    private Map<String, String> localParameters = new LinkedHashMap<String, String>();
    
    private String canonicalStoredLocation;
    
    private String canonicalIdentifier;
    
    /**
     * <code>true</code> when the backing {@link HstNode} of this {@link HstComponentConfiguration} is inherited
     */
    private boolean inherited;
    
    /**
     * <code>true</code> when this {@link HstComponentConfiguration} is configured to render standalone in case of {@link HstURL#COMPONENT_RENDERING_TYPE}
     * The default value is <code>true</code> when the property {@link HstNodeTypes#COMPONENT_PROPERTY_STANDALONE} is not configured.
     * The value for standalone is *not* inherited from ancestor components.
     * When standalone = null, it means it is not configured. Then, we return true for {@link #isStandalone()}. It is easier to work with
     * object Boolean here to support the copying and merging etc.
     */
    private Boolean standalone = null;

    /**
     * <code>true</code> when this {@link HstComponentConfiguration} is configured to render async : Thus, with a asynchronous ajax call. The
     * default value is <code>false</code> when the property {@link HstNodeTypes#COMPONENT_PROPERTY_ASYNC} is not configured.
     * The value for standalone is *not* inherited from ancestor components.
     * When async = null, it means it is not configured. Then, we return true for {@link #isAsync()}. It is easier to work with
     * object Boolean here to support the copying and merging etc.
     */
    private Boolean async = null;
    
    /**
     * Optional iconPath relative to webapp for sites. If not configured, it is <code>null</code>. It does not inherit 
     * from ancestor components
     */
    private String iconPath;
    
    /**
     * Optional label of this component. if not configured, it is just <code>null</code>. It does not inherit 
     * from ancestor components
     */
    private String label;

    /**
     * containing all the variants of this {@link HstComponentConfiguration} : This is including the variants of all 
     * descendant {@link HstComponentConfiguration}s. This member can be null if no variants configured
     * Default empty List.
     */
    private List<String> variants = Collections.emptyList();
    
    // constructor for copy purpose only
    private HstComponentConfigurationService(String id) {
        this.id = StringPool.get(id);
    } 

    

    public HstComponentConfigurationService(HstNode node, HstComponentConfiguration parent,
            String rootNodeName) throws ServiceException {
        this(node, parent, rootNodeName, true);
    }

    /*
     * rootNodeName is either hst:components or hst:pages.
     */
    public HstComponentConfigurationService(HstNode node, HstComponentConfiguration parent,
            String rootNodeName, boolean traverseDescendants) throws ServiceException {
    
        this.canonicalStoredLocation = StringPool.get(node.getValueProvider().getCanonicalPath());
        this.canonicalIdentifier = StringPool.get(node.getValueProvider().getIdentifier());
        this.inherited = node.isInherited();

        this.parent = parent;

        if(parent == null) {
            this.id = StringPool.get(rootNodeName + "/" + node.getValueProvider().getName());   
        } else {
            this.id = StringPool.get(parent.getId() + "/" + node.getValueProvider().getName());   
        }
        
        this.componentClassName = StringPool.get(node.getValueProvider().getString(HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_CLASSNAME));
        
        if(HstNodeTypes.NODETYPE_HST_COMPONENT.equals(node.getNodeTypeName())) {
          type = Type.COMPONENT;
        } else if(HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT.equals(node.getNodeTypeName())) {
          type = Type.CONTAINER_COMPONENT;
          if(componentClassName == null) {
              // TODO do not depend on hardcoded location 'org.hippoecm.hst.pagecomposer.builtin.components.StandardContainerComponent'
              log.debug("Setting componentClassName to '{}' for a component of type '{}' because there is no explicit componentClassName configured on component '{}'", new String[]{"org.hippoecm.hst.pagecomposer.builtin.components.StandardContainerComponent",HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT, id});
              componentClassName = "org.hippoecm.hst.pagecomposer.builtin.components.StandardContainerComponent";
          }
        } else if(HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT.equals(node.getNodeTypeName())) {
            type = Type.CONTAINER_ITEM_COMPONENT;
            componentFilterTag = node.getValueProvider().getString(HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_FILTER_TAG);
        } else {
            throw new ServiceException("Unknown componentType '"+node.getNodeTypeName()+"' for '"+canonicalStoredLocation+"'. Cannot build configuration.");
        }

        this.name = StringPool.get(node.getValueProvider().getName());
        this.referenceName = StringPool.get(node.getValueProvider().getString(HstNodeTypes.COMPONENT_PROPERTY_REFERECENCENAME));
        
        this.referenceComponent = StringPool.get(node.getValueProvider().getString(HstNodeTypes.COMPONENT_PROPERTY_REFERECENCECOMPONENT));
        
        if(referenceComponent != null) {
            if(type == Type.CONTAINER_COMPONENT) {
                throw new ServiceException("ContainerComponents are not allowed to have a reference. Pls fix the" +
                        "configuration for '"+canonicalStoredLocation+"'");
            }
        }
        
        this.hstTemplate = StringPool.get(node.getValueProvider().getString(HstNodeTypes.COMPONENT_PROPERTY_TEMPLATE));
        this.hstResourceTemplate = StringPool.get(node.getValueProvider().getString(HstNodeTypes.COMPONENT_PROPERTY_RESOURCE_TEMPLATE));
        this.pageErrorHandlerClassName = StringPool.get(node.getValueProvider().getString(HstNodeTypes.COMPONENT_PROPERTY_PAGE_ERROR_HANDLER_CLASSNAME));
        
        this.label = StringPool.get(node.getValueProvider().getString(HstNodeTypes.COMPONENT_PROPERTY_LABEL));
        this.iconPath = StringPool.get(node.getValueProvider().getString(HstNodeTypes.COMPONENT_PROPERTY_ICON_PATH));
        
        if(type == Type.CONTAINER_COMPONENT || type == Type.CONTAINER_ITEM_COMPONENT) {
            this.xtype = StringPool.get(node.getValueProvider().getString(HstNodeTypes.COMPONENT_PROPERTY_XTYPE));
        } 
        String[] parameterNames = node.getValueProvider().getStrings(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES);
        String[] parameterValues = node.getValueProvider().getStrings(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES);
        String[] parameterNamePrefixes = node.getValueProvider().getStrings(HstNodeTypes.COMPONENT_PROPERTY_PARAMETER_NAME_PREFIXES);

        if (parameterNames.length != parameterValues.length) {
            log.warn("Skipping parameters for component '{}' because they only make sense if there are equal number of names and values", id);
        } else {
            if(parameterNamePrefixes.length > 0 ) {
                if(parameterNamePrefixes.length != parameterNames.length) {
                    log.warn("Skipping parameters for component '{}' because there are hst:parameternameprefixes configured, but if " +
                    		"it is configured it MUST be of equal length as the hst:parameternames", id);
                } else {
                    // if there is a non empty parameterNamePrefix, we prefix the parameter name with this value + the 
                    // HstComponentConfiguration#PARAMETER_PREFIX_NAME_DELIMITER
                    for (int i = 0; i < parameterNames.length; i++) {
                        StringBuilder parameterNameBuilder = new StringBuilder(parameterNames[i]);
                        if(!StringUtils.isEmpty(parameterNamePrefixes[i])) {
                            parameterNameBuilder.insert(0, HstComponentConfiguration.PARAMETER_PREFIX_NAME_DELIMITER);
                            parameterNameBuilder.insert(0, parameterNamePrefixes[i]);
                            if (!parameterNamePrefixSet.contains(parameterNamePrefixes[i])) {
                                parameterNamePrefixSet.add(parameterNamePrefixes[i]);
                            }
                        }
                        this.parameters.put(StringPool.get(parameterNameBuilder.toString()), StringPool.get(parameterValues[i]));
                        this.localParameters.put(StringPool.get(parameterNames[i]), StringPool.get(parameterValues[i]));
                    } 
                }
            } else {
                for (int i = 0; i < parameterNames.length; i++) {
                    this.parameters.put(StringPool.get(parameterNames[i]), StringPool.get(parameterValues[i]));
                    this.localParameters.put(StringPool.get(parameterNames[i]), StringPool.get(parameterValues[i]));
                } 
            } 
        }



        if(node.getValueProvider().hasProperty(HstNodeTypes.COMPONENT_PROPERTY_STANDALONE)) {
            this.standalone = node.getValueProvider().getBoolean(HstNodeTypes.COMPONENT_PROPERTY_STANDALONE);
        }

        if(node.getValueProvider().hasProperty(HstNodeTypes.COMPONENT_PROPERTY_ASYNC)) {
            this.async = node.getValueProvider().getBoolean(HstNodeTypes.COMPONENT_PROPERTY_ASYNC);
        }

        if(!traverseDescendants) {
            // do not load children 
            return;
        }
        for(HstNode child : node.getNodes()) {
            if(HstNodeTypes.NODETYPE_HST_COMPONENT.equals(node.getNodeTypeName())
                    || HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT.equals(node.getNodeTypeName())
                    || HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT.equals(node.getNodeTypeName())
                  )  {
                if (child.getValueProvider().hasProperty(HstNodeTypes.COMPONENT_PROPERTY_REFERECENCENAME)) {
                    usedChildReferenceNames.add(StringPool.get(child.getValueProvider().getString(HstNodeTypes.COMPONENT_PROPERTY_REFERECENCENAME)));
                }
                try {
                    HstComponentConfigurationService componentConfiguration = new HstComponentConfigurationService(
                            child, this, rootNodeName, true);
                    componentConfigurations.put(StringPool.get(componentConfiguration.getId()), componentConfiguration);

                    // we also need an ordered list
                    orderedListConfigs.add(componentConfiguration);
                    childConfByName.put(StringPool.get(child.getValueProvider().getName()), componentConfiguration);
                    log.debug("Added component service with key '{}'", id);
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

    public String getXType() {
        return this.xtype;
    }

    public Type getComponentType() {
        return this.type;
    }

    public String getHstTemplate() {
        return this.hstTemplate;
    }

    public String getRenderPath() {
        if(isNamedRenderer) {
            return null;
        }
        return this.renderPath;
    }

    public String getNamedRenderer() {
        if(!isNamedRenderer) {
            return null;
        }
        return this.renderPath;
    }
    
    public String getHstResourceTemplate() {
        return this.hstResourceTemplate;
    }
    
    public String getServeResourcePath() {
        if (isNamedResourceServer) {
            return null;
        }
        return this.serveResourcePath;
    }
    
    public String getNamedResourceServer() {
        if (!isNamedResourceServer) {
            return null;
        }
        return this.serveResourcePath;
    }

    public String getParameter(String name) {
        return this.parameters.get(name);
    }

    public Map<String, String> getParameters() {
        return parameters;
    }
    
    @Override
    public Set<String> getParameterPrefixes() {
        return parameterNamePrefixSet;
    }

    @Override
    public List<String> getVariants() {
        return variants;
    }

 
	public String getLocalParameter(String name) {
		return localParameters.get(name);
	}

	public Map<String, String> getLocalParameters() {
		return localParameters;
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

    @Override
    public String getComponentFilterTag() {
        return componentFilterTag;
    }

    public Map<String, HstComponentConfiguration> getChildren() {
        return componentConfigurations;
    }

    public HstComponentConfiguration getChildByName(String name) {
        if (derivedChildrenByName == null) {
            HashMap<String, HstComponentConfiguration> children = new HashMap<String, HstComponentConfiguration>();
            for (HstComponentConfiguration config : orderedListConfigs) {
                children.put(StringPool.get(config.getName()), config);
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
     
    @Override
    public boolean isInherited() {
        return inherited;
    }

    @Override
    public boolean isStandalone() {
        // when Boolean standalone is null, we return true by default
        return standalone == null ? true : standalone;
    }

    @Override
    public boolean isAsync() {
        // when Boolean asyn is null, we return false by default
        return async == null ? false : async;
    }
    
    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getIconPath() {
        return iconPath;
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
        copy.name = child.name;
        copy.referenceName = child.referenceName;
        copy.hstTemplate = child.hstTemplate;
        copy.label = child.label;
        copy.iconPath = child.iconPath;
        copy.renderPath = child.renderPath;
        copy.isNamedRenderer = child.isNamedRenderer;
        copy.hstResourceTemplate = child.hstResourceTemplate;
        copy.serveResourcePath = child.serveResourcePath;
        copy.isNamedResourceServer = child.isNamedResourceServer;
        copy.referenceComponent = child.referenceComponent;
        copy.pageErrorHandlerClassName = child.pageErrorHandlerClassName;
        copy.xtype = child.xtype;
        copy.type = child.type;
        copy.canonicalStoredLocation = child.canonicalStoredLocation;
        copy.canonicalIdentifier = child.canonicalIdentifier;
        copy.componentFilterTag = child.componentFilterTag;
        copy.inherited = child.inherited;
        copy.standalone = child.standalone;
        copy.async = child.async;
        copy.parameters = new LinkedHashMap<String, String>(child.parameters);
        copy.parameterNamePrefixSet = new HashSet<String>(child.parameterNamePrefixSet);
        // localParameters have no merging, but for copy, the localParameters are copied 
        copy.localParameters = new LinkedHashMap<String, String>(child.localParameters);
        copy.usedChildReferenceNames = new ArrayList<String>(child.usedChildReferenceNames);
        for (HstComponentConfigurationService descendant : child.orderedListConfigs) {
            String descId = StringPool.get(copy.id + descendant.id);
            HstComponentConfigurationService copyDescendant = deepCopy(copy, descId, descendant, populated,
                    rootComponentConfigurations);
            copy.componentConfigurations.put(copyDescendant.id, copyDescendant);
            copy.orderedListConfigs.add(copyDescendant);
            copy.childConfByName.put(StringPool.get(copyDescendant.getName()), copyDescendant);
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
                    throw new ServiceException("There is a component referencing itself: this is not allowed. The site configuration cannot be loaded. Incorrect ComponentId = "+this.getId());
                }
                if (referencedComp.getReferenceComponent() != null) {
                    // populate referenced comp first:
                    referencedComp.populateComponentReferences(rootComponentConfigurations, populated);
                }
                // get all properties that are null from the referenced component:
                if (this.componentClassName == null) {
                    this.componentClassName = referencedComp.componentClassName;
                }
                if (this.name == null) {
                    this.name = referencedComp.name;
                }
                if (this.referenceName == null) {
                    this.referenceName = referencedComp.referenceName;
                }
                if (this.referenceComponent == null) {
                    this.referenceComponent = referencedComp.referenceComponent;
                }
                if (this.hstTemplate == null) {
                    this.hstTemplate = referencedComp.hstTemplate;
                }
                if (this.label == null) {
                    this.label = referencedComp.label;
                }
                if (this.iconPath == null) {
                    this.iconPath = referencedComp.iconPath;
                }
                if (this.renderPath == null) {
                    this.renderPath = referencedComp.renderPath;
                    this.isNamedRenderer = referencedComp.isNamedRenderer;
                }
                if (this.hstResourceTemplate == null) {
                    this.hstResourceTemplate = referencedComp.hstResourceTemplate;
                }
                if (this.serveResourcePath == null) {
                    this.serveResourcePath = referencedComp.serveResourcePath;
                    this.isNamedResourceServer = referencedComp.isNamedResourceServer;
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
                if (this.xtype == null) {
                    this.xtype = referencedComp.xtype;
                }
                if (this.componentFilterTag == null) {
                    this.componentFilterTag = referencedComp.componentFilterTag;
                }
                if (this.standalone == null) {
                    this.standalone = referencedComp.standalone;
                }
                if (this.async == null) {
                    this.async = referencedComp.async;
                }

                // inherited variable flag not needed to take from the referencedComp so no check here for that variable!
                
                if (!referencedComp.parameters.isEmpty()) {
                    // as we already have parameters, add only the once we do not yet have
                    for (Entry<String, String> entry : referencedComp.parameters.entrySet()) {
                        if (!parameters.containsKey(entry.getKey())) {
                            parameters.put(entry.getKey(), entry.getValue());
                        }
                    }
                }
                
                if (!referencedComp.parameterNamePrefixSet.isEmpty()) {
                    // as we already have parameters, add only the once we do not yet have
                    for (String prefix : referencedComp.parameterNamePrefixSet) {
                        if (!parameterNamePrefixSet.contains(prefix)) {
                            parameterNamePrefixSet.add(prefix); 
                        }
                    }
                }

                this.usedChildReferenceNames.addAll(referencedComp.usedChildReferenceNames);

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
                        childToMerge.populateComponentReferences(rootComponentConfigurations, populated);
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
            log.warn("Incorrect component configuration: *Container* Components are not allowed to be merged with other " +
                    "components. Cannot merge '{}' and '{}' because at least one of them is a Container component. Fix configuration.", childToMerge.getId(), this.getId());
            return;
        }
        if(this.type == Type.CONTAINER_ITEM_COMPONENT || childToMerge.type == Type.CONTAINER_ITEM_COMPONENT) {
            log.warn("Incorrect component configuration: *ContainerItem* Components are not allowed to be merged with other " +
                    "components. Cannot merge '{}' and '{}' because at least one of them is a ContainerItemComponent. Fix configuration.", childToMerge.getId(), this.getId());
            return;
        }
        
        if (this.componentClassName == null) {
            this.componentClassName = childToMerge.componentClassName;
        }
        if (this.hstTemplate == null) {
            this.hstTemplate = childToMerge.hstTemplate;
        }
        if (this.label == null) {
            this.label = childToMerge.label;
        }
        if (this.iconPath == null) {
            this.iconPath = childToMerge.iconPath;
        }
        if (this.hstResourceTemplate == null) {
            this.hstResourceTemplate = childToMerge.hstResourceTemplate;
        }
        if (this.name == null) {
            this.name = childToMerge.name;
        }
        if (this.referenceName == null) {
            this.referenceName = childToMerge.referenceName;
        }
        if (this.renderPath == null) {
            this.renderPath = childToMerge.renderPath;
            this.isNamedRenderer = childToMerge.isNamedRenderer;
        }
        if (this.referenceComponent == null) {
            this.referenceComponent = childToMerge.referenceComponent;
        }
        if (this.serveResourcePath == null) {
            this.serveResourcePath = childToMerge.serveResourcePath;
            this.isNamedResourceServer = childToMerge.isNamedResourceServer;
        }
        if (this.pageErrorHandlerClassName == null) {
            this.pageErrorHandlerClassName = childToMerge.pageErrorHandlerClassName;
        }
        if (this.xtype == null) {
            this.xtype = childToMerge.xtype;
        }
        if (this.componentFilterTag == null) {
            this.componentFilterTag = childToMerge.componentFilterTag;
        }
        if (this.standalone == null) {
            this.standalone = childToMerge.standalone;
        }
        if (this.async == null) {
            this.async = childToMerge.async;
        }
        
        // inherited flag not needed to merge
        
        if (!childToMerge.parameters.isEmpty()) {
            // as we already have parameters, add only the once we do not yet have
            for (Entry<String, String> entry : childToMerge.parameters.entrySet()) {
                if (!parameters.containsKey(entry.getKey())) {
                    parameters.put(entry.getKey(), entry.getValue());
                }
            }
        }
        
        if (!childToMerge.parameterNamePrefixSet.isEmpty()) {
            // as we already have parameters, add only the once we do not yet have
            for (String prefix : childToMerge.parameterNamePrefixSet) {
                if (!parameterNamePrefixSet.contains(prefix)) {
                    parameterNamePrefixSet.add(prefix);
                }
            }
        }
        
        for (HstComponentConfigurationService toMerge : childToMerge.orderedListConfigs) {
            HstComponentConfigurationService existingChild = this.childConfByName.get(toMerge.name);
            if (existingChild != null) {
                // check whether the child of its own has a referencecomponent: This referencecomponent then needs
                // to be first populated before merging
                existingChild.populateComponentReferences(rootComponentConfigurations, populated);
                toMerge.populateComponentReferences(rootComponentConfigurations, populated);
                this.childConfByName.get(toMerge.name).combine(toMerge, rootComponentConfigurations, populated);
            } else {
                //  String newId = this.id + "-" + toMerge.id;
                //  this.deepCopy(this, newId, toMerge, populated, rootComponentConfigurations);
                // deepCopy also does the populateComponentReferences for child 'toMerge'
                this.addDeepCopy(toMerge, populated, rootComponentConfigurations);
            }
        }

    }

    private void addDeepCopy(HstComponentConfigurationService childToMerge, List<HstComponentConfiguration> populated,
            Map<String, HstComponentConfiguration> rootComponentConfigurations) throws ServiceException {

        String newId = StringPool.get(this.id + "-" + childToMerge.id);
        
        HstComponentConfigurationService copy = deepCopy(this, newId, childToMerge, populated,
                rootComponentConfigurations);
        this.componentConfigurations.put(copy.getId(), copy);
        this.childConfByName.put(copy.getName(), copy);
        this.orderedListConfigs.add(copy);

    }

    protected void setRenderPath(Map<String, HstNode> templateResourceMap) {
        if(getHstTemplate()  != null) {
            String templateRenderPath = null;
            HstNode template = templateResourceMap.get(getHstTemplate());
            if (template != null) {
                ValueProvider valueProvider = template.getValueProvider();
                
                if (valueProvider.hasProperty(HstNodeTypes.TEMPLATE_PROPERTY_RENDERPATH)) {
                    templateRenderPath = valueProvider.getString(HstNodeTypes.TEMPLATE_PROPERTY_RENDERPATH);
                }
                
                if (StringUtils.isBlank(templateRenderPath) && valueProvider.hasProperty(HstNodeTypes.TEMPLATE_PROPERTY_SCRIPT)) {
                    templateRenderPath = "jcr:" + valueProvider.getPath();
                }
                
                this.isNamedRenderer = valueProvider.getBoolean(HstNodeTypes.TEMPLATE_PROPERTY_IS_NAMED);
            }
            renderPath = StringPool.get(templateRenderPath);
            if(renderPath == null) {
                log.warn("renderer '{}' for component '{}' can not be found. This component will not have a renderer.", getHstTemplate(), id);
            }
        }
        for (HstComponentConfigurationService child : orderedListConfigs) {
            child.setRenderPath(templateResourceMap);
        }
    }
    
    protected void setServeResourcePath(Map<String, HstNode> templateResourceMap) {
        String templateServeResourcePath = null;
        HstNode template = templateResourceMap.get(getHstResourceTemplate());
        
        if (template != null) {
            ValueProvider valueProvider = template.getValueProvider();
            
            if (valueProvider.hasProperty(HstNodeTypes.TEMPLATE_PROPERTY_RENDERPATH)) {
                templateServeResourcePath = valueProvider.getString(HstNodeTypes.TEMPLATE_PROPERTY_RENDERPATH);
            }
            
            if (StringUtils.isBlank(templateServeResourcePath) && valueProvider.hasProperty(HstNodeTypes.TEMPLATE_PROPERTY_SCRIPT)) {
                templateServeResourcePath = "jcr:" + valueProvider.getPath();
            }
            
            this.isNamedResourceServer = template.getValueProvider().getBoolean(HstNodeTypes.TEMPLATE_PROPERTY_IS_NAMED);
        }
        
        this.serveResourcePath = StringPool.get(templateServeResourcePath);
        
        for (HstComponentConfigurationService child : orderedListConfigs) {
            child.setServeResourcePath(templateResourceMap);
        }
    }
    
    protected void inheritParameters() {
        // before traversing child components add the parameters from the parent, and if already present, override them
        // this overriding however is *not* done for containeritemcomponents: They are self contained and do never get their
        // parametervalues overridden by ancestors: They only get what they don't already have themselves
        if (type == Type.CONTAINER_ITEM_COMPONENT) {
            if (parent != null && parent.getParameters() != null) {
                for(Entry<String, String> entry : parent.getParameters().entrySet()) {
                    if(parameters.containsKey(entry.getKey())) {
                        // we already have the parameter, skip
                        continue;
                    }
                    String parameterName = entry.getKey();
                    parameters.put(parameterName, entry.getValue());
                    // if the parameter has a prefix that is not yet in parameterNamePrefixSet, add it as well
                    if(parameterName.indexOf(PARAMETER_PREFIX_NAME_DELIMITER) > -1) {
                        String prefix = parameterName.substring(0, parameterName.indexOf(PARAMETER_PREFIX_NAME_DELIMITER));
                        if (!parameterNamePrefixSet.contains(prefix)) {
                            parameterNamePrefixSet.add(prefix);
                        }
                    }
                }
            }
        } else {
            if (parent != null && parent.getParameters() != null) {
                parameters.putAll(parent.getParameters());
                // also add the parameter name prefixes from parents
                parameterNamePrefixSet.addAll(parent.getParameterPrefixes());
            }
        }
        for (HstComponentConfigurationService child : orderedListConfigs) {
            child.inheritParameters();
        }
    }

    /**
     * get all the unique variants for this component + its descendants and set this to
     * variants instance variable if not empty
     */
    protected void populateVariants() {
        // first traverse the children
        Set<String> variantsSet = new HashSet<String>();
        for (HstComponentConfigurationService child : orderedListConfigs) {
            child.populateVariants();
            variantsSet.addAll(child.getParameterPrefixes());
            variantsSet.addAll(child.getVariants());
        }
        // add parameter prefixes of component itself (not child)
        if (!getParameterPrefixes().isEmpty()) {
            variantsSet.addAll(getParameterPrefixes());
        }
        if (!variantsSet.isEmpty()) {
            // set variants to unmodifiable list
            this.variants = Collections.unmodifiableList(new ArrayList<String>(variantsSet));
        }
        
    }

    protected void makeCollectionsImmutableAndOptimize() {

        for (HstComponentConfigurationService child : orderedListConfigs) {
            // optimize the entire tree of components
            child.makeCollectionsImmutableAndOptimize();
        }

        if (parameters.isEmpty()) {
            parameters = Collections.emptyMap();
        } else {
            parameters = Collections.unmodifiableMap(parameters);
        }
        if (localParameters.isEmpty()) {
            localParameters = Collections.emptyMap();
        } else {
            localParameters = Collections.unmodifiableMap(localParameters);
        }
        if (componentConfigurations.isEmpty()) {
            componentConfigurations = Collections.emptyMap();
        } else {
            componentConfigurations = Collections.unmodifiableMap(componentConfigurations);
        }
        if (parameterNamePrefixSet.isEmpty()) {
            parameterNamePrefixSet = Collections.emptySet();
        } else {
            parameterNamePrefixSet = Collections.unmodifiableSet(parameterNamePrefixSet);
        }

        if (childConfByName.isEmpty()) {
            childConfByName = Collections.emptyMap();
        }
        if (orderedListConfigs.isEmpty()) {
            orderedListConfigs = Collections.emptyList();
        }

        if (usedChildReferenceNames.isEmpty()) {
            usedChildReferenceNames = Collections.emptyList();
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
                child.setReferenceName(StringPool.get(autoRefName));
            }
        }
    }

}