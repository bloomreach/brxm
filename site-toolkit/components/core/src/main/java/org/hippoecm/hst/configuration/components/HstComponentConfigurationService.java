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
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.internal.ConfigurationLockInfo;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.configuration.model.ModelLoadingException;
import org.hippoecm.hst.core.component.HstURL;
import org.hippoecm.hst.core.internal.StringPool;
import org.hippoecm.hst.provider.ValueProvider;
import org.slf4j.LoggerFactory;

public class HstComponentConfigurationService implements HstComponentConfiguration, ConfigurationLockInfo {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(HstComponentConfigurationService.class);

    private Map<String, HstComponentConfiguration> componentConfigurations = new LinkedHashMap<String, HstComponentConfiguration>();

    private Map<String, HstComponentConfigurationService> childConfByName = new HashMap<String, HstComponentConfigurationService>();

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
     * whether this {@link HstComponentConfigurationService} can serve as prototype.
     */
    private boolean prototype;

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
     * Optional mode parameter to determine which available rendering/aggregating technology should be used
     * for the async component. e.g., 'ajax', 'esi', etc.
     */
    private String asyncMode = null;

    /**
     * @return <code>true</code> if rendering / resource requests can have their entire page http responses cached. Note that 
     * A {@link HstComponentConfiguration} is only cacheable if and only if <b>none</b> of its descendant {@link HstComponentConfiguration}s for the request
     * are marked as uncacheable : <b>Note</b>  explicitly for 'the request', thus {@link HstComponentConfiguration} that are {@link HstComponentConfiguration#isAsync()}
     * and its descendants can be uncacheable while an ancestor of the async {@link HstComponentConfiguration} can still be cacheable
     */
    private boolean compositeCacheable = true;

    /**
     * @return <code>true</code> this hst component configuration if configured to be cacheable
     */
    private Boolean cacheable = null;

    
    
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
     * Contains all the variants of this {@link HstComponentConfiguration}. This includes the variants of all
     * descendant {@link HstComponentConfiguration}s. This member can be null if no variants configured
     * Default empty List.
     */
    private List<String> variants = Collections.emptyList();

    /**
     * Contains all the variants for all the {@link HstComponentConfiguration} for the {@link HstComponentsConfiguration}
     */
    private List<String> mountVariants = Collections.emptyList();

    private String lockedBy;
    private Calendar lockedOn;
    private Calendar lastModified;
    
    // constructor for copy purpose only
    private HstComponentConfigurationService(String id) {
        this.id = StringPool.get(id);
    }

    /**
     * rootNodeName is either hst:components or hst:pages.
     * @param referenceableContainers : can be null
     */
    public HstComponentConfigurationService(final HstNode node,
                                            final HstComponentConfiguration parent,
                                            final String rootNodeName,
                                            final Map<String, HstNode> referenceableContainers,
                                            final boolean inherited) {
        this(node, parent, rootNodeName, false, referenceableContainers, inherited, null);
    }

    /**
     * rootNodeName is either hst:components or hst:pages.
     * @param referenceableContainers : can be null
     */
    public HstComponentConfigurationService(final HstNode node,
                                            final HstComponentConfiguration parent,
                                            final String rootNodeName,
                                            final boolean isCatalogItem,
                                            final Map<String, HstNode> referenceableContainers,
                                            final boolean inherited,
                                            final String explicitName) {


        this.canonicalStoredLocation = StringPool.get(node.getValueProvider().getCanonicalPath());
        this.canonicalIdentifier = StringPool.get(node.getValueProvider().getIdentifier());

        this.inherited =  inherited;
        this.parent = parent;
        this.prototype = HstNodeTypes.NODENAME_HST_PROTOTYPEPAGES.equals(rootNodeName);

        if (explicitName == null) {
            this.name = StringPool.get(node.getValueProvider().getName());
        } else {
            // in case of a hst:componentcontainerreference, we only need to keep the name of the 'hst:componentcontainerreference' node
            // and for the rest everything is inherited from the referenced HstNode
            this.name = explicitName;
        }

        if(parent == null) {
            this.id = StringPool.get(rootNodeName + "/" + name);
        } else {
            this.id = StringPool.get(parent.getId() + "/" + name);
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
            if (!isCatalogItem && (parent == null || !(Type.CONTAINER_COMPONENT.equals(parent.getComponentType())))) {
                log.warn("Component of type '{}' at '{}' is not configured below a '{}' node. This is not allowed. " +
                        "Either change the primary nodetype to '{}' or '{}' or move the node below a node of type '{}'.",
                        new String[]{HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT, canonicalStoredLocation,
                                HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT, HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT,
                                HstNodeTypes.NODETYPE_HST_COMPONENT, HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT});
            }
        } else {
            throw new ModelLoadingException("Unknown componentType '" + node.getNodeTypeName() + "' for '" + canonicalStoredLocation + "'. Cannot build configuration.");
        }
        this.referenceName = StringPool.get(node.getValueProvider().getString(HstNodeTypes.COMPONENT_PROPERTY_REFERECENCENAME));
        
        this.referenceComponent = StringPool.get(node.getValueProvider().getString(HstNodeTypes.COMPONENT_PROPERTY_REFERECENCECOMPONENT));
        
        if(referenceComponent != null) {
            if (type == Type.CONTAINER_COMPONENT) {
                throw new ModelLoadingException("ContainerComponents are not allowed to have a reference. Pls fix the" +
                        "configuration for '"+canonicalStoredLocation+"'");
            } else if (type == Type.CONTAINER_ITEM_COMPONENT) {
                log.error("Component '{}' is not allowed to have a '{}' property as this is not supported for " +
                        "components of type '{}'. Setting reference to null.", new String[]{canonicalStoredLocation, HstNodeTypes.COMPONENT_PROPERTY_REFERECENCECOMPONENT,
                        HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT});
                this.referenceComponent = null;
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

        if(node.getValueProvider().hasProperty(HstNodeTypes.COMPONENT_PROPERTY_ASYNC_MODE)) {
            this.asyncMode = node.getValueProvider().getString(HstNodeTypes.COMPONENT_PROPERTY_ASYNC_MODE);
        }

        if(node.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROPERTY_CACHEABLE)) {
            this.cacheable = node.getValueProvider().getBoolean(HstNodeTypes.GENERAL_PROPERTY_CACHEABLE);
        }

        if (type == Type.CONTAINER_COMPONENT) {
            lockedBy = node.getValueProvider().getString(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY);
            lockedOn = node.getValueProvider().getDate(HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON);
            lastModified = node.getValueProvider().getDate(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED);
        }
        // regardless merging/referencing of components, we directly inherit lock props: They are normally
        // only stored on hst container items and those don't support merging any way
        if (parent != null) {
            lockedBy = (lockedBy == null) ?  ((ConfigurationLockInfo)parent).getLockedBy() : lockedBy;
            lockedOn = (lockedOn == null) ?  ((ConfigurationLockInfo)parent).getLockedOn() : lockedOn;
            lastModified = (lastModified == null) ?  parent.getLastModified() : lastModified;
        }

        if(isCatalogItem) {
            // do not load children 
            return;
        }
        for (HstNode child : node.getNodes()) {
            if ("deleted".equals(child.getValueProvider().getString(HstNodeTypes.EDITABLE_PROPERTY_STATE))) {
                log.debug("SKipping marked deleted node {}", child.getValueProvider().getPath());
                continue;
            }
            HstComponentConfigurationService childComponent = loadChildComponent(child, rootNodeName, referenceableContainers);
            if (childComponent == null) {
                continue;
            }
            componentConfigurations.put(StringPool.get(childComponent.getId()), childComponent);
            orderedListConfigs.add(childComponent);
            childConfByName.put(StringPool.get(childComponent.getName()), childComponent);
            log.debug("Added component service with key '{}'", id);
        }
    }

    private HstComponentConfigurationService loadChildComponent(final HstNode child,
                                                                final String rootNodeName,
                                                                final Map<String, HstNode> referenceableContainers) {
        if (isHstComponentOrReferenceType(child)) {
            if (isPrototype() && isNotAllowedInPrototype(child)) {
                log.warn("Component child of type '{}' are not allowed in a prototype page. Skipping component '{}'.",
                        child.getNodeTypeName(), child.getValueProvider().getPath());
                return null;
            }
            if (child.getValueProvider().hasProperty(HstNodeTypes.COMPONENT_PROPERTY_REFERECENCENAME)) {
                usedChildReferenceNames.add(StringPool.get(child.getValueProvider().getString(HstNodeTypes.COMPONENT_PROPERTY_REFERECENCENAME)));
            }
            try {
                if (HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENTREFERENCE.equals(child.getNodeTypeName())) {
                    HstNode referencedContainerNode = getReferencedContainer(child, referenceableContainers);
                    if (referencedContainerNode == null) {
                        return null;
                    }
                    // use the referencedContainerNode to build the hst component but use current child nodename as
                    // the name of the component node
                    String explicitName = child.getValueProvider().getName();
                    return new HstComponentConfigurationService(referencedContainerNode,
                            this, rootNodeName, false, referenceableContainers, inherited, explicitName);
                } else {
                    return new HstComponentConfigurationService(child, this, rootNodeName, false, referenceableContainers, inherited, null);
                }
            } catch (ModelLoadingException e) {
                if (log.isDebugEnabled()) {
                    log.warn("Skipping component '"+child.getValueProvider().getPath()+"'", e);
                } else if (log.isWarnEnabled()) {
                    log.warn("Skipping component '{}' : '{}'", child.getValueProvider().getPath(), e.toString());
                }
                return null;
            }
        } else {
            log.warn("Skipping node '{}' because is not of type '{}'", child.getValueProvider().getPath(),
                    (HstNodeTypes.NODETYPE_HST_COMPONENT));
            return null;
        }
    }

    private boolean isHstComponentOrReferenceType(final HstNode node) {
        return HstNodeTypes.NODETYPE_HST_COMPONENT.equals(node.getNodeTypeName())
                || HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT.equals(node.getNodeTypeName())
                || HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT.equals(node.getNodeTypeName())
                || HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENTREFERENCE.equals(node.getNodeTypeName());
    }

    private boolean isNotAllowedInPrototype(final HstNode node) {
        return HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENTREFERENCE.equals(node.getNodeTypeName());
    }


    private HstNode getReferencedContainer(final HstNode child, final Map<String, HstNode> referenceableContainers) {
        if (referenceableContainers == null || referenceableContainers.isEmpty()) {
            log.warn("Component '{}' is of type '{}' but there are no referenceable containers at '{}'. Component '{}' will be ignored.",
                    new String[]{child.getValueProvider().getPath(), HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENTREFERENCE,
                            HstNodeTypes.RELPATH_HST_WORKSPACE_CONTAINERS, child.getValueProvider().getPath()});
            return null;
        }
        // reference is mandatory so can't be null
        String reference = child.getValueProvider().getString(HstNodeTypes.COMPONENT_PROPERTY_REFERECENCECOMPONENT);
        if (reference.startsWith("/")) {
            log.warn("Component '{}' has reference '{}' that starts with a '/'. Reference should not start with a slash and " +
                    "must be relative to '{}'. Removing leading slash");
            reference = reference.substring(1);
        }
        try {
            final String[] elements = reference.split("/");
            HstNode refNode = null;
            final HstNode hstNode = referenceableContainers.get(elements[0]);
            if (hstNode != null) {
                if (elements.length == 1) {
                    refNode = hstNode;
                } else {
                    String subPath = reference.substring(elements[0].length() + 1);
                    refNode = hstNode.getNode(subPath);
                }
            }
            if (refNode == null) {
                log.warn("Component '{}' contains an unresolvable reference '{}'. It should be a location relative to '{}'. " +
                        "Component '{}' will be ignored.",
                        new String[]{child.getValueProvider().getPath(), reference,
                                HstNodeTypes.RELPATH_HST_WORKSPACE_CONTAINERS, child.getValueProvider().getPath()});
                return null;
            }
            if (!HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT.equals(refNode.getNodeTypeName())) {
                log.warn("Component '{}' contains an reference '{}' that does not point to a node of type '{}'. That is not allowed. " +
                        "Component '{}' will be ignored.",
                        new String[]{child.getValueProvider().getPath(), reference,
                                HstNodeTypes.RELPATH_HST_WORKSPACE_CONTAINERS, child.getValueProvider().getPath()});
                return null;
            }
            log.debug("Succesfully found referenced containercomponent node '{}' for '{}'.", refNode.getValueProvider().getPath(),
                    child.getValueProvider().getPath());
            return refNode;
        } catch (IllegalArgumentException e) {
            log.warn("Reference '{}' for '{}' is invalid : {}", new String[]{reference, child.getValueProvider().getPath(), e.toString()});
            return null;
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

    public void setMountVariants(List<String> immutableMountVariants) {
        this.mountVariants = immutableMountVariants;
        // also for all descendants set the mountVariants
        for (HstComponentConfigurationService child : orderedListConfigs) {
            child.setMountVariants(immutableMountVariants);
        }
    }

    @Override
    public List<String> getMountVariants() {
        return mountVariants;
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
        return childConfByName.get(name);
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
    public boolean isPrototype() {
        return prototype;
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
    public String getAsyncMode() {
        return asyncMode;
    }

    @Override
    public boolean isCompositeCacheable() {
        return compositeCacheable;
    }
    
    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getIconPath() {
        return iconPath;
    }

    @Override
    public String getLockedBy() {
        return lockedBy;
    }

    @Override
    public Calendar getLockedOn() {
        return lockedOn;
    }

    @Override
    public Calendar getLastModified() {
        return lastModified;
    }


    private HstComponentConfigurationService deepCopy(HstComponentConfigurationService parent, String newId,
            HstComponentConfigurationService child, List<HstComponentConfiguration> populated,
            Map<String, HstComponentConfiguration> rootComponentConfigurations) {
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
        copy.asyncMode = child.asyncMode;
        copy.cacheable = child.cacheable;
        copy.parameters = new LinkedHashMap<String, String>(child.parameters);
        copy.parameterNamePrefixSet = new HashSet<String>(child.parameterNamePrefixSet);
        // localParameters have no merging, but for copy, the localParameters are copied 
        copy.localParameters = new LinkedHashMap<String, String>(child.localParameters);
        copy.usedChildReferenceNames = new ArrayList<String>(child.usedChildReferenceNames);
        copy.lockedBy = child.lockedBy;
        copy.lockedOn = child.lockedOn;
        copy.lastModified = child.lastModified;
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
            List<HstComponentConfiguration> populated) {
        if (populated.contains(this)) {
            return;
        }

        populated.add(this);

        if (this.getReferenceComponent() != null) {
            HstComponentConfigurationService referencedComp = (HstComponentConfigurationService) rootComponentConfigurations
                    .get(this.getReferenceComponent());
            if (referencedComp != null) {
                if(referencedComp == this) {
                    throw new ModelLoadingException("There is a component referencing itself: this is not allowed. The site configuration cannot be loaded. Incorrect ComponentId = "+this.getId());
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
                if (this.asyncMode == null) {
                    this.asyncMode = referencedComp.asyncMode;
                }
                if (this.cacheable == null) {
                    this.cacheable = referencedComp.cacheable;
                }

                if (this.lockedBy == null) {
                    this.lockedBy = referencedComp.lockedBy;
                }
                if (this.lockedOn == null) {
                    this.lockedOn = referencedComp.lockedOn;
                }
                if (this.lastModified == null) {
                    this.lastModified = referencedComp.lastModified;
                }

                // inherited variable flag not needed to take from the referencedComp so no check here for that variable!
                // prototype variable flag not needed to take from the referencedComp so no check here for that variable!
                
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
            List<HstComponentConfiguration> populated) {
        
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
        if (this.asyncMode == null) {
            this.asyncMode = childToMerge.asyncMode;
        }
        if (this.cacheable == null) {
            this.cacheable = childToMerge.cacheable;
        }
        if (this.lockedBy == null) {
            this.lockedBy = childToMerge.lockedBy;
        }
        if (this.lockedOn == null) {
            this.lockedOn = childToMerge.lockedOn;
        }
        if (this.lastModified == null) {
            this.lastModified = childToMerge.lastModified;
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
            Map<String, HstComponentConfiguration> rootComponentConfigurations) {

        String newId = StringPool.get(this.id + "-" + childToMerge.id);
        
        HstComponentConfigurationService copy = deepCopy(this, newId, childToMerge, populated,
                rootComponentConfigurations);
        this.componentConfigurations.put(copy.getId(), copy);
        this.childConfByName.put(copy.getName(), copy);
        this.orderedListConfigs.add(copy);

    }

    protected void setRenderPath(Map<String, HstNode> templateResourceMap) {
        if(StringUtils.isNotEmpty(hstTemplate)) {
            String templateRenderPath = null;
            HstNode template = templateResourceMap.get(hstTemplate);
            if (template != null) {
                ValueProvider valueProvider = template.getValueProvider();
                
                if (valueProvider.hasProperty(HstNodeTypes.TEMPLATE_PROPERTY_RENDERPATH)) {
                    templateRenderPath = valueProvider.getString(HstNodeTypes.TEMPLATE_PROPERTY_RENDERPATH);
                }
                
                if (StringUtils.isBlank(templateRenderPath) && valueProvider.hasProperty(HstNodeTypes.TEMPLATE_PROPERTY_SCRIPT)) {
                    templateRenderPath = "jcr:" + valueProvider.getPath();
                }
                this.isNamedRenderer = valueProvider.getBoolean(HstNodeTypes.TEMPLATE_PROPERTY_IS_NAMED);
            } else {
                log.warn("Cannot find hst:template '{}' for hst component '{}'.", hstTemplate, this.toString());
            }
            renderPath = StringPool.get(templateRenderPath);
            if(renderPath == null) {
                log.info("renderer '{}' for component '{}' can not be found. This component will not have a renderer " +
                        "by default. It can be set runtime or this component is used without renderer.", getHstTemplate(), id);
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
            if (child.isAsync()) {
                onlyAddChildVariantsIfCurrentOrAncestorIsAlreadyAsync(variantsSet, child);
            } else {
                variantsSet.addAll(child.getVariants());
            }
        }
        variantsSet.addAll(getParameterPrefixes());
        if (!variantsSet.isEmpty()) {
            // set variants to unmodifiable list
            this.variants = Collections.unmodifiableList(new ArrayList<String>(variantsSet));
        }

    }

    /**
     * Since the HST supports async only on one level (thus async descendants of an async component are rendered with the
     * async ancestor), we only include the variants of an async component if the current or an ancestor component is already
     * async
     */
    private void onlyAddChildVariantsIfCurrentOrAncestorIsAlreadyAsync(final Set<String> variantsSet, final HstComponentConfigurationService asyncChild) {
        if (isAsync() || hasAsyncAncestor()) {
            // we are already async, thus add variants
            variantsSet.addAll(asyncChild.getVariants());
        }
    }

    private boolean hasAsyncAncestor() {
        HstComponentConfiguration parent = getParent();
        while (parent != null) {
            if (parent.isAsync()) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

    /**
     * an {@link HstComponentConfiguration} is only cacheable if all its none async descendants (trees) are cacheable
     */
    public void populateIsCompositeCacheable() {
        if (cacheable != null && !cacheable.booleanValue()) {
            compositeCacheable = false;
            // mark all ancestors uncacheable unless the ancestor or this item is async (and does not have another async ancestor)
            if (isAsync() && !hasAsyncAncestor()) {
                // do not traverse parents because we are an async item and
                // we do not have async ancestors and we already marked ourselves as compositeCacheable = false;
            } else {
                HstComponentConfigurationService parent = (HstComponentConfigurationService)getParent();
                while (parent != null) {
                    parent.compositeCacheable = false;
                    if (parent.isAsync()) {
                        if (!parent.hasAsyncAncestor()) {
                            // the parent is async and the parent does not have async ancestors in turn
                            // so we can break. If there are async ancestors, we still mark the parent as
                            // cacheable = false since async trees are rendered in one request even if they contain
                            // async components
                            break;
                        }
                    }
                    parent = (HstComponentConfigurationService) parent.getParent();
                }
            }
        }
        for (HstComponentConfigurationService child : orderedListConfigs) {
            child.populateIsCompositeCacheable();
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

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("HstComponentConfiguration [id=");
        builder.append(id).append(", stored jcr location=").append(canonicalStoredLocation)
                .append(", className=").append(this.componentClassName)
                .append(", template=").append(this.hstTemplate).append("]");
        return  builder.toString();
    }
}
