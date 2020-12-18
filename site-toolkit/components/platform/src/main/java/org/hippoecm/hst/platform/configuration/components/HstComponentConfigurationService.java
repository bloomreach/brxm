/*
 *  Copyright 2008-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.configuration.components;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.builtin.components.StandardContainerComponent;
import org.hippoecm.hst.configuration.ConfigurationUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.components.DynamicFieldGroup;
import org.hippoecm.hst.configuration.components.DynamicParameter;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.components.HstComponentsConfiguration;
import org.hippoecm.hst.configuration.experiencepage.ExperiencePageLoadingException;
import org.hippoecm.hst.configuration.internal.ConfigurationLockInfo;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.core.component.HstURL;
import org.hippoecm.hst.core.internal.StringPool;
import org.hippoecm.hst.core.parameters.FieldGroup;
import org.hippoecm.hst.core.parameters.FieldGroupList;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.platform.configuration.model.ModelLoadingException;
import org.hippoecm.hst.provider.ValueProvider;
import org.hippoecm.hst.util.ParametersInfoAnnotationUtils;
import org.hippoecm.repository.standardworkflow.JcrTemplateNode;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static org.apache.commons.lang3.ArrayUtils.nullToEmpty;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.jackrabbit.JcrConstants.JCR_FROZENPRIMARYTYPE;
import static org.apache.jackrabbit.JcrConstants.NT_FROZENNODE;
import static org.hippoecm.hst.configuration.HstNodeTypes.COMPONENT_PROPERTY_COMPONENTDEFINITION;
import static org.hippoecm.hst.configuration.HstNodeTypes.COMPONENT_PROPERTY_FIELD_GROUPS;
import static org.hippoecm.hst.configuration.HstNodeTypes.COMPONENT_PROPERTY_SUPPRESS_WASTE_MESSAGE;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_XPAGE;
import static org.hippoecm.hst.configuration.components.HstComponentConfiguration.Type.CONTAINER_COMPONENT;
import static org.hippoecm.hst.platform.configuration.components.HstComponentsConfigurationService.setAutocreatedReference;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_IDENTIFIER;

public class HstComponentConfigurationService implements HstComponentConfiguration, ConfigurationLockInfo {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(HstComponentConfigurationService.class);
    public static final String OLD_MOVED_BUILT_IN_STANDARD_CONTAINER_COMPONENT_CLASS = "org.hippoecm.hst.pagecomposer.builtin.components.StandardContainerComponent";

    private Map<String, HstComponentConfiguration> componentConfigurations = new LinkedHashMap<String, HstComponentConfiguration>();

    private Map<String, HstComponentConfigurationService> childConfByName = new HashMap<String, HstComponentConfigurationService>();

    private List<HstComponentConfigurationService> orderedListConfigs = new ArrayList<HstComponentConfigurationService>();

    private HstComponentConfiguration parent;

    protected String id;

    private String name;

    protected String componentClassName;

    private String parametersInfoClassName;

    private String hstTemplate;

    private String hstResourceTemplate;

    private boolean isNamedRenderer;

    private boolean isNamedResourceServer;

    private String renderPath;

    private String serveResourcePath;

    private String xtype;

    private String ctype;

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

    private String componentDefinition;

    private String pageErrorHandlerClassName;

    private Set<String> usedChildReferenceNames = new HashSet<>();
    private AtomicInteger autocreatedCounter = new AtomicInteger(0);

    private Map<String, String> parameters = new LinkedHashMap<String, String>();

    //named and residual component parameters
    protected List<DynamicParameter> hstDynamicComponentParameters = new LinkedList<DynamicParameter>();

    // the set of parameter prefixes
    private Set<String> parameterNamePrefixSet = new HashSet<String>();

    private Map<String, String> localParameters = new LinkedHashMap<String, String>();

    protected String canonicalStoredLocation;

    private String canonicalIdentifier;

    /**
     * <code>true</code> when the backing {@link HstNode} of this {@link HstComponentConfiguration} is inherited
     */
    private boolean inherited;

    /**
     * {@code true} if this {@link HstComponentConfiguration} is shared. Note that if
     * {@link HstComponentConfiguration#isInherited()} is true, then {@code shared} will also be always true. Note that
     * containers referenced via a 'hst:containercomponentreference' can in general be shared, but this is not the
     * purpose of 'hst:containercomponentreference' : it it used to enable a container to 'live' below the hst:workspace
     * and in general is never meant to support 'sharing', hence a container referenced via
     * hst:containercomponentreference will only have 'shared = true' *IF* the 'hst:containercomponentreference' node
     * is already shared
     */
    private boolean shared;

    /**
     * whether this {@link HstComponentConfigurationService} can serve as prototype.
     */
    private boolean prototype;

    /**
     * true if this hst component configuration root is an hst:xpage node
     */
    private boolean xpage;

    /**
     * if xpage == true, the xPageLayoutAsJcrTemplate contains the HstNode as JcrTemplateNode, where the JcrTemplateNode
     * is a structure that can be used in folder workflow to add subnodes / mixins to newly created document variant
     */
    private JcrTemplateNode xPageLayoutAsJcrTemplate;

    // true if this is an explicit xpage layout component (aka not inherited component but directly below the hst:xpage
    private boolean xpageLayoutComponent;

    /**
     * if true, this component is build from a jcr node of type 'hst:containercomponentreference'
     */
    private boolean containerComponentReference;

    // in case the config is transformed to an XPage but the XPage document does not have a container for the
    // container from the XPAge Layout
    private boolean unresolvedXpageLayoutContainer;

    /**
     * See {@link #isExperiencePageComponent()}
     */
    private boolean experiencePageComponent;

    /**
     * hst:component of type 'xpage' or 'containercomponent' are expected to have a hippo:identifier
     * (auto created property). Can be null
     */
    private String hippoIdentifier;

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

    private Boolean suppressWasteMessage = null;

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
    private Boolean markedDeleted;

    /**
     * detached is true for components returned from {@link #copy(String, String, boolean)} implying that the
     * specific {@link HstComponentConfigurationService} is detached from the HST in memory model
     */
    private boolean detached;

    private boolean hidden;

    private AtomicBoolean logWasteMessageProcessed = new AtomicBoolean(false);

    protected List<DynamicFieldGroup> fieldGroups = new ArrayList<>();

    // constructor for copy purpose only
    private HstComponentConfigurationService(String id) {
        this.id = StringPool.get(id);
    }

    //Test constructor
    protected HstComponentConfigurationService() {
    }

    /**
     * rootNodeName is either hst:components or hst:pages.
     *
     * @param referableContainers : can be null
     */
    public HstComponentConfigurationService(final HstNode node,
                                            final HstComponentConfiguration parent,
                                            final String rootNodeName,
                                            final Map<String, HstNode> referableContainers,
                                            final String rootConfigurationPathPrefix) {
        this(node, parent, rootNodeName, false, referableContainers, rootConfigurationPathPrefix, null);
    }

    /**
     * rootNodeName is either hst:components or hst:pages.
     *
     * @param referableContainers : can be null
     */
    public HstComponentConfigurationService(final HstNode node,
                                            final HstComponentConfiguration parent,
                                            final String rootNodeName,
                                            final boolean isCatalogItem,
                                            final Map<String, HstNode> referableContainers,
                                            String rootConfigurationPathPrefix,
                                            final String explicitName) {
        this(node, parent, rootNodeName, isCatalogItem, referableContainers, rootConfigurationPathPrefix, explicitName, false);
    }

    /**
     * if loadInIsolation = true, it means that for example a container item is loaded out of its parent context
     */
    public HstComponentConfigurationService(final HstNode node,
                                            final HstComponentConfiguration parent,
                                            final String rootNodeName,
                                            final boolean isCatalogItem,
                                            final Map<String, HstNode> referableContainers,
                                            String rootConfigurationPathPrefix,
                                            final String explicitName,
                                            final boolean loadInIsolation) {


        this.canonicalStoredLocation = StringPool.get(node.getValueProvider().getCanonicalPath());
        this.canonicalIdentifier = StringPool.get(node.getValueProvider().getIdentifier());

        inherited = !canonicalStoredLocation.startsWith(rootConfigurationPathPrefix);

        if (inherited) {
            shared = true;
        }

        this.parent = parent;
        this.prototype = HstNodeTypes.NODENAME_HST_PROTOTYPEPAGES.equals(rootNodeName);

        if (explicitName == null) {
            this.name = StringPool.get(node.getValueProvider().getName());
        } else {
            // in case of a hst:componentcontainerreference, we only need to keep the name of the 'hst:componentcontainerreference' node
            // and for the rest everything is inherited from the referenced HstNode
            this.name = explicitName;
        }

        if (parent == null) {
            this.id = StringPool.get(rootNodeName + "/" + name);
        } else {
            this.id = StringPool.get(parent.getId() + "/" + name);
        }

        this.componentClassName = StringPool.get(node.getValueProvider().getString(HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_CLASSNAME));

        String nodeTypeName = node.getNodeTypeName();
        if (NT_FROZENNODE.equals(nodeTypeName)) {
            // loading an experience page component from version history, take the frozen nodetype
            nodeTypeName = node.getValueProvider().getString(JCR_FROZENPRIMARYTYPE);
        }

        xpage = NODETYPE_HST_XPAGE.equals(nodeTypeName);

        if (xpage) {
            // load JcrTemplatePage, the xPageLayoutAsJcrTemplate should not get any referenced components included, just
            // only follow the HstNode tree
            xPageLayoutAsJcrTemplate = JcrTemplateNodeConverter.getXPageLaoutAsJcrTemplate(node);
        }

        if (isAncestorXPage(this)) {
            xpageLayoutComponent = true;
        }

        if (HstNodeTypes.NODETYPE_HST_COMPONENT.equals(nodeTypeName) || NODETYPE_HST_XPAGE.equals(nodeTypeName)) {
            type = Type.COMPONENT;
        } else if (NODETYPE_HST_CONTAINERCOMPONENT.equals(nodeTypeName)) {
            type = Type.CONTAINER_COMPONENT;
            if (componentClassName == null) {
                log.debug("Setting componentClassName to '{}' for a component of type '{}' because there is no explicit componentClassName configured on component '{}'",
                        new String[]{StandardContainerComponent.class.getName(), NODETYPE_HST_CONTAINERCOMPONENT, id});
                componentClassName = StandardContainerComponent.class.getName();
            } else if (OLD_MOVED_BUILT_IN_STANDARD_CONTAINER_COMPONENT_CLASS.equals(componentClassName)) {
                log.warn("For Component '{}' the configured property '{}' points to old location '{}'. Remove the " +
                                "property completely because it is the default container component class anyway. Moved class '{}' " +
                                "will be used instead", id, NODETYPE_HST_CONTAINERCOMPONENT,
                        "org.hippoecm.hst.pagecomposer.builtin.components.StandardContainerComponent", StandardContainerComponent.class.getName());
                componentClassName = StandardContainerComponent.class.getName();
            }
        } else if (HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT.equals(nodeTypeName)
                || HstNodeTypes.NODETYPE_HST_COMPONENTDEFINITION.equals(nodeTypeName)) {
            type = Type.CONTAINER_ITEM_COMPONENT;
            componentFilterTag = node.getValueProvider().getString(HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_FILTER_TAG);
            if (!isCatalogItem && !loadInIsolation && (parent == null || !(Type.CONTAINER_COMPONENT.equals(parent.getComponentType())))) {
                log.warn("Component of type '{}' at '{}' is not configured below a '{}' node. This is not allowed. " +
                                "Either change the primary nodetype to '{}' or '{}' or move the node below a node of type '{}'.",
                        new String[]{NODETYPE_HST_CONTAINERITEMCOMPONENT, canonicalStoredLocation,
                                NODETYPE_HST_CONTAINERCOMPONENT, NODETYPE_HST_CONTAINERCOMPONENT,
                                HstNodeTypes.NODETYPE_HST_COMPONENT, NODETYPE_HST_CONTAINERCOMPONENT});
            }
        } else {
            throw new ModelLoadingException("Unknown componentType '" + nodeTypeName + "' for '" + canonicalStoredLocation + "'. Cannot build configuration.");
        }

        this.componentDefinition = StringPool.get(node.getValueProvider().getString(COMPONENT_PROPERTY_COMPONENTDEFINITION));

        this.parametersInfoClassName = StringPool.get(node.getValueProvider().getString(HstNodeTypes.COMPONENT_PROPERTY_PARAMETERSINFO_CLASSNAME));

        this.referenceName = StringPool.get(node.getValueProvider().getString(HstNodeTypes.COMPONENT_PROPERTY_REFERECENCENAME));

        this.referenceComponent = StringPool.get(node.getValueProvider().getString(HstNodeTypes.COMPONENT_PROPERTY_REFERECENCECOMPONENT));

        if (referenceComponent != null) {
            if (type == Type.CONTAINER_COMPONENT) {
                throw new ModelLoadingException("ContainerComponents are not allowed to have a reference. Pls fix the" +
                        "configuration for '" + canonicalStoredLocation + "'");
            } else if (type == Type.CONTAINER_ITEM_COMPONENT) {
                log.warn("Component '{}' is not allowed to have a '{}' property as this is not supported for " +
                        "components of type '{}'. Setting reference to null.", new String[]{canonicalStoredLocation, HstNodeTypes.COMPONENT_PROPERTY_REFERECENCECOMPONENT,
                        NODETYPE_HST_CONTAINERITEMCOMPONENT});
                this.referenceComponent = null;
            }
        }

        this.hstTemplate = StringPool.get(node.getValueProvider().getString(HstNodeTypes.COMPONENT_PROPERTY_TEMPLATE));
        this.hstResourceTemplate = StringPool.get(node.getValueProvider().getString(HstNodeTypes.COMPONENT_PROPERTY_RESOURCE_TEMPLATE));
        this.pageErrorHandlerClassName = StringPool.get(node.getValueProvider().getString(HstNodeTypes.COMPONENT_PROPERTY_PAGE_ERROR_HANDLER_CLASSNAME));

        this.label = StringPool.get(node.getValueProvider().getString(HstNodeTypes.COMPONENT_PROPERTY_LABEL));
        this.hippoIdentifier = StringPool.get(node.getValueProvider().getString(HIPPO_IDENTIFIER));
        this.iconPath = StringPool.get(node.getValueProvider().getString(HstNodeTypes.COMPONENT_PROPERTY_ICON_PATH));

        if (type == Type.CONTAINER_COMPONENT || type == Type.CONTAINER_ITEM_COMPONENT) {
            this.xtype = StringPool.get(node.getValueProvider().getString(HstNodeTypes.COMPONENT_PROPERTY_XTYPE));
            if (xtype == null && type == Type.CONTAINER_COMPONENT) {
                // set default ot HST.bBox for container
                xtype = "HST.vBox";
            }
        }
        String[] parameterNames = node.getValueProvider().getStrings(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES);
        String[] parameterValues = node.getValueProvider().getStrings(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES);
        String[] parameterNamePrefixes = node.getValueProvider().getStrings(HstNodeTypes.COMPONENT_PROPERTY_PARAMETER_NAME_PREFIXES);

        if (parameterNames.length != parameterValues.length) {
            log.warn("Skipping parameters for component '{}' because they only make sense if there are equal number of names and values", id);
        } else {
            if (parameterNamePrefixes.length > 0) {
                if (parameterNamePrefixes.length != parameterNames.length) {
                    log.warn("Skipping parameters for component '{}' because there are hst:parameternameprefixes configured, but if " +
                            "it is configured it MUST be of equal length as the hst:parameternames", id);
                } else {
                    // if there is a non empty parameterNamePrefix, we prefix the parameter name with this value + the
                    // HstComponentConfiguration#PARAMETER_PREFIX_NAME_DELIMITER
                    for (int i = 0; i < parameterNames.length; i++) {
                        if (StringUtils.isEmpty(parameterNamePrefixes[i])) {
                            this.parameters.put(StringPool.get(parameterNames[i]), StringPool.get(parameterValues[i]));
                            this.localParameters.put(StringPool.get(parameterNames[i]), StringPool.get(parameterValues[i]));
                        } else {
                            if (!parameterNamePrefixSet.contains(parameterNamePrefixes[i])) {
                                parameterNamePrefixSet.add(parameterNamePrefixes[i]);
                            }
                            final String prefixedParameterName = ConfigurationUtils.createPrefixedParameterName(parameterNamePrefixes[i], parameterNames[i]);
                            this.parameters.put(StringPool.get(prefixedParameterName), StringPool.get(parameterValues[i]));
                            this.localParameters.put(StringPool.get(prefixedParameterName), StringPool.get(parameterValues[i]));
                        }
                    }
                }
            } else {
                for (int i = 0; i < parameterNames.length; i++) {
                    this.parameters.put(StringPool.get(parameterNames[i]), StringPool.get(parameterValues[i]));
                    this.localParameters.put(StringPool.get(parameterNames[i]), StringPool.get(parameterValues[i]));
                }
            }
        }

        final List<HstNode> componentParameterNodes = node.getNodes(HstNodeTypes.NODETYPE_HST_DYNAMIC_PARAMETER);
        for (final HstNode componentParameterNode : componentParameterNodes) {
            final DynamicParameter hstComponentParameter = new DynamicComponentParameter(componentParameterNode);
            hstDynamicComponentParameters.add(hstComponentParameter);
        }

        if (isCatalogItem) {
            readJcrFieldGroups(node);
        }

        if (node.getValueProvider().hasProperty(HstNodeTypes.COMPONENT_PROPERTY_STANDALONE)) {
            this.standalone = node.getValueProvider().getBoolean(HstNodeTypes.COMPONENT_PROPERTY_STANDALONE);
        }

        if (node.getValueProvider().hasProperty(HstNodeTypes.COMPONENT_PROPERTY_ASYNC)) {
            this.async = node.getValueProvider().getBoolean(HstNodeTypes.COMPONENT_PROPERTY_ASYNC);
        }

        if (node.getValueProvider().hasProperty(HstNodeTypes.COMPONENT_PROPERTY_ASYNC_MODE)) {
            this.asyncMode = node.getValueProvider().getString(HstNodeTypes.COMPONENT_PROPERTY_ASYNC_MODE);
        }

        if (node.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROPERTY_CACHEABLE)) {
            this.cacheable = node.getValueProvider().getBoolean(HstNodeTypes.GENERAL_PROPERTY_CACHEABLE);
        }

        if (node.getValueProvider().hasProperty(COMPONENT_PROPERTY_SUPPRESS_WASTE_MESSAGE)) {
            this.suppressWasteMessage = node.getValueProvider().getBoolean(COMPONENT_PROPERTY_SUPPRESS_WASTE_MESSAGE);
        }

        if (type == Type.CONTAINER_COMPONENT) {
            lockedBy = node.getValueProvider().getString(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY);
            lockedOn = node.getValueProvider().getDate(HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON);
            lastModified = node.getValueProvider().getDate(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED);
        }
        // regardless merging/referencing of components, we directly inherit lock props: They are normally
        // only stored on hst container items and those don't support merging any way
        if (parent != null) {
            lockedBy = (lockedBy == null) ? ((ConfigurationLockInfo) parent).getLockedBy() : lockedBy;
            lockedOn = (lockedOn == null) ? ((ConfigurationLockInfo) parent).getLockedOn() : lockedOn;
            lastModified = (lastModified == null) ? parent.getLastModified() : lastModified;
        }

        if (node.getValueProvider().hasProperty(HstNodeTypes.EDITABLE_PROPERTY_STATE)) {
            if ("deleted".equals(node.getValueProvider().getString(HstNodeTypes.EDITABLE_PROPERTY_STATE))) {
                markedDeleted = true;
            }
        } else if (parent != null) {
            markedDeleted = parent.isMarkedDeleted();
        }

        if (isCatalogItem) {
            this.ctype = StringPool.get(node.getValueProvider().getString(HstNodeTypes.COMPONENT_PROPERTY_CTYPE));
            this.hidden = isCatalogItemHidden(node);
            // do not load children
            return;
        }
        for (HstNode child : node.getNodes()) {
            HstComponentConfigurationService childComponent = loadChildComponent(child, rootNodeName,
                    rootConfigurationPathPrefix, referableContainers);
            if (childComponent == null) {
                continue;
            }
            componentConfigurations.put(StringPool.get(childComponent.getId()), childComponent);
            orderedListConfigs.add(childComponent);
            childConfByName.put(StringPool.get(childComponent.getName()), childComponent);
            log.debug("Added component service with key '{}'", id);
        }
    }

    private Boolean isCatalogItemHidden(final HstNode catalogItem) {
        Boolean hidden = catalogItem.getValueProvider().getBoolean(
            HstNodeTypes.COMPONENT_PROPERTY_HIDDEN_IN_CHANNEL_MANAGER);
        HstNode containerItemPackage = catalogItem.getParent();
        return  hidden || containerItemPackage.getValueProvider().getBoolean(
                HstNodeTypes.CONTAINERITEM_PACKAGE_PROPERTY_HIDDEN_IN_CHANNEL_MANAGER);
    }

    private boolean isAncestorXPage(final HstComponentConfigurationService comp) {
        if (comp == null){
            return false;
        }
        if (comp.isXPage()) {
            return true;
        }

        return isAncestorXPage((HstComponentConfigurationService) comp.getParent());
    }

    private HstComponentConfigurationService loadChildComponent(final HstNode child,
                                                                final String rootNodeName,
                                                                final String rootConfigurationPathPrefix,
                                                                final Map<String, HstNode> referenceableContainers) {
        if (isHstComponentOrReferenceType(child)) {
            if (isPrototype() && isNotAllowedInPrototype(child)) {
                log.warn("Component child of type '{}' are not allowed in a prototype page. Skipping component '{}'.",
                        child.getNodeTypeName(), child.getValueProvider().getPath());
                return null;
            }
            try {
                if (HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENTREFERENCE.equals(child.getNodeTypeName())) {
                    HstNode referencedContainerNode = getReferencedContainer(child, referenceableContainers, rootConfigurationPathPrefix);
                    if (referencedContainerNode == null) {
                        return null;
                    }
                    // use the referencedContainerNode to build the hst component but use current child nodename as
                    // the name of the component node
                    String explicitName = child.getValueProvider().getName();
                    HstComponentConfigurationService childComponent = new HstComponentConfigurationService(referencedContainerNode,
                            this, rootNodeName, false, referenceableContainers, rootConfigurationPathPrefix, explicitName);
                    // keep track of that the child component was loaded via a container component reference: these
                    // need special handling in case of an XPage Layout since are not like XPage Layout container but
                    // more like inherited components
                    childComponent.containerComponentReference = true;
                    return childComponent;
                } else {
                    return new HstComponentConfigurationService(child, this, rootNodeName, false, referenceableContainers, rootConfigurationPathPrefix, null);
                }
            } catch (ModelLoadingException e) {
                if (log.isDebugEnabled()) {
                    log.warn("Skipping component '" + child.getValueProvider().getPath() + "'", e);
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
        final String nodeTypeName = node.getNodeTypeName();
        return HstNodeTypes.NODETYPE_HST_COMPONENT.equals(nodeTypeName)
                || NODETYPE_HST_CONTAINERCOMPONENT.equals(nodeTypeName)
                || NODETYPE_HST_CONTAINERITEMCOMPONENT.equals(nodeTypeName)
                || HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENTREFERENCE.equals(nodeTypeName);
    }

    private boolean isNotAllowedInPrototype(final HstNode node) {
        return HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENTREFERENCE.equals(node.getNodeTypeName());
    }


    private HstNode getReferencedContainer(final HstNode child, final Map<String, HstNode> referenceableContainers, final String rootConfigurationPathPrefix) {
        if (referenceableContainers == null || referenceableContainers.isEmpty()) {
            log.warn("Component '{}' is of type '{}' but there are no referenceable containers at '{}' for configuration at '{}'. " +
                            "Component '{}' will be ignored.",
                    new String[]{child.getValueProvider().getPath(), HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENTREFERENCE,
                            HstNodeTypes.RELPATH_HST_WORKSPACE_CONTAINERS, rootConfigurationPathPrefix, child.getValueProvider().getPath()});
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
                log.warn("Component '{}' contains an unresolvable reference '{}' for configuration '{}'. It should be a location relative to '{}'. " +
                                "Component '{}' will be ignored.",
                        new String[]{child.getValueProvider().getPath(), reference, rootConfigurationPathPrefix,
                                HstNodeTypes.RELPATH_HST_WORKSPACE_CONTAINERS, child.getValueProvider().getPath()});
                return null;
            }
            if (!NODETYPE_HST_CONTAINERCOMPONENT.equals(refNode.getNodeTypeName())) {
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

    @Override
    public HstComponentConfiguration getParent() {
        return parent;
    }

    @Override
    public String getComponentClassName() {
        return this.componentClassName;
    }

    @Override
    public String getParametersInfoClassName() {
        return parametersInfoClassName;
    }

    @Override
    public String getXType() {
        return this.xtype;
    }

    @Override
    public String getCType() {
        return this.ctype;
    }

    @Override
    public Type getComponentType() {
        return this.type;
    }


    public String getHstTemplate() {
        return this.hstTemplate;
    }

    public String getRenderPath() {
        if (isNamedRenderer) {
            return null;
        }
        return this.renderPath;
    }

    public String getNamedRenderer() {
        if (!isNamedRenderer) {
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
    public List<DynamicParameter> getDynamicComponentParameters() {
        return hstDynamicComponentParameters;
    }

    @Override
    public Optional<DynamicParameter> getDynamicComponentParameter(String name) {
        return hstDynamicComponentParameters.stream()
                .filter(hstComponentParameter -> hstComponentParameter.getName().equals(name)).findAny();
    }

    public List<DynamicFieldGroup> getFieldGroups() {
        return fieldGroups;
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

    public String getComponentDefinition() {
        return componentDefinition;
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
    public boolean isShared() {
        return shared;
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
    public boolean isSuppressWasteMessage() {
        return suppressWasteMessage == null ? false : suppressWasteMessage;
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

    @Override
    public boolean isMarkedDeleted() {
        return markedDeleted == null ? false : markedDeleted;
    }

    /**
     * @return {@core true} if this {@link HstComponentConfiguration} is an XPage: Note *ONLY* root hst components can
     * be an 'xpage HstComponentConfiguration' and that this is different than {@link #isExperiencePageComponent} : the
     * {@link #isExperiencePageComponent} indicates whether the component is stored below an experience page document
     * variant, while this {@link #isXPage()} indicates whether the hst component stored in HST CONFIG (!!) is an XPage
     * (layout)
     */
    public boolean isXPage() {
        return xpage;
    }

    /**
     * in case {@link #isXPage()} returns {@code true}, then {@link #getJcrTemplateNode()} returns the HstNode hierarchy
     * for an XPage Layout as a JcrTemplateNode, otherwise it is null
     * @return the JcrTemplateNode in case {@link #isXPage()} is {@code true}, otherwise {@code null}
      */
    public JcrTemplateNode getJcrTemplateNode() {
        return xPageLayoutAsJcrTemplate;
    }

    /**
     * @return {@code true} if this component configuration is stored canonically below an XPage Layout (config)
     */
    public boolean isXpageLayoutComponent() {
        return xpageLayoutComponent;
    }

    public boolean isContainerComponentReference() {
        return containerComponentReference;
    }

    @Override
    public String getHippoIdentifier() {
        return hippoIdentifier;
    }

    @Override
    public boolean isExperiencePageComponent() {
        return experiencePageComponent;
    }

    public void setExperiencePageComponent(final boolean experiencePageComponent) {
        this.experiencePageComponent = experiencePageComponent;
    }

    @Override
    public boolean isHidden() {
        return hidden;
    }

    @Override
    public boolean isUnresolvedXpageLayoutContainer() {
        return unresolvedXpageLayoutContainer;
    }

    /**
     * <p>
     *     The returned copy is *DETACHED* from the HST Model and a complete independent
     *     HstComponentConfigurationService (tree). Therefor all the components in this returned deep copy will have
     *     {@link HstComponentConfigurationService#detached equal to true}.
     *
     * </p>
     * @return A deep copy of {@code source} with parent = null and root component having {@link #getId()}
     * equal to {@code canonicalIdentifier}, {@link #getCanonicalIdentifier()} equal to {@code canonicalIdentifier}
     * and {@link #getCanonicalStoredLocation()} equal to {@code canonicalStoredLocation}
     */
    public HstComponentConfigurationService copy(final String canonicalIdentifier, final String canonicalStoredLocation,
                                                 final boolean includeContainerItems) {
        final HstComponentConfigurationService hstComponentConfigurationService =
                deepCopy(null, canonicalIdentifier, this, null, includeContainerItems);
        hstComponentConfigurationService.canonicalIdentifier = canonicalIdentifier;
        hstComponentConfigurationService.canonicalStoredLocation = canonicalStoredLocation;
        hstComponentConfigurationService.flattened().forEach(config -> ((HstComponentConfigurationService)config).detached = true);
        return hstComponentConfigurationService;
    }

    private HstComponentConfigurationService deepCopy(final HstComponentConfigurationService parent, String newId,
                                                      final HstComponentConfigurationService child,
                                                      final Map<String, HstComponentConfiguration> rootComponentConfigurations,
                                                      final boolean includeContainerItems) {
        if (rootComponentConfigurations == null) {
            if (isNotBlank(child.getReferenceComponent()) && !child.referencesPopulated) {
                throw new IllegalStateException("If 'rootComponentConfigurations' is null, all components references " +
                        "are expected to be resolved already");
            }
        } else {
            if (child.getReferenceComponent() != null) {
                // populate child component if not yet happened
                child.populateComponentReferences(rootComponentConfigurations);
            }
        }
        HstComponentConfigurationService copy = new HstComponentConfigurationService(newId);
        copy.parent = parent;
        copy.componentClassName = child.componentClassName;
        copy.parametersInfoClassName = child.parametersInfoClassName;
        copy.name = child.name;
        copy.referenceName = child.referenceName;
        copy.hstTemplate = child.hstTemplate;
        copy.label = child.label;
        copy.hippoIdentifier = child.hippoIdentifier;
        copy.xpage = child.xpage;
        copy.xpageLayoutComponent = child.xpageLayoutComponent;
        copy.iconPath = child.iconPath;
        copy.renderPath = child.renderPath;
        copy.isNamedRenderer = child.isNamedRenderer;
        copy.hstResourceTemplate = child.hstResourceTemplate;
        copy.serveResourcePath = child.serveResourcePath;
        copy.isNamedResourceServer = child.isNamedResourceServer;
        copy.referenceComponent = child.referenceComponent;
        copy.pageErrorHandlerClassName = child.pageErrorHandlerClassName;
        copy.xtype = child.xtype;
        copy.ctype = child.ctype;
        copy.type = child.type;
        copy.canonicalStoredLocation = child.canonicalStoredLocation;
        copy.canonicalIdentifier = child.canonicalIdentifier;
        copy.componentFilterTag = child.componentFilterTag;
        copy.inherited = child.inherited;
        // a copy is always shared unless the child IS an XPage Document Component: in that case it is never shared
        if (child.isExperiencePageComponent()) {
            copy.shared = false;
        } else {
            copy.shared = true;
        }
        copy.standalone = child.standalone;
        copy.async = child.async;
        copy.asyncMode = child.asyncMode;
        copy.cacheable = child.cacheable;
        copy.suppressWasteMessage = child.suppressWasteMessage;
        copy.parameters = new LinkedHashMap<String, String>(child.parameters);
        copy.parameterNamePrefixSet = new HashSet<String>(child.parameterNamePrefixSet);
        // localParameters have no merging, but for copy, the localParameters are copied
        copy.localParameters = new LinkedHashMap<String, String>(child.localParameters);
        copy.usedChildReferenceNames = new HashSet<>(child.usedChildReferenceNames);
        copy.variants = new ArrayList<>(variants);
        copy.mountVariants = new ArrayList<>(mountVariants);
        copy.lockedBy = child.lockedBy;
        copy.lockedOn = child.lockedOn;
        copy.lastModified = child.lastModified;
        copy.markedDeleted = child.markedDeleted;
        copy.fieldGroups = child.fieldGroups;
        copy.hstDynamicComponentParameters = child.hstDynamicComponentParameters;
        copy.xpageLayoutComponent = child.xpageLayoutComponent;
        copy.experiencePageComponent = child.experiencePageComponent;
        copy.containerComponentReference = child.containerComponentReference;

        if (type != Type.CONTAINER_COMPONENT || includeContainerItems) {
            for (HstComponentConfigurationService descendant : child.orderedListConfigs) {
                String descId = StringPool.get(copy.id + descendant.id);
                HstComponentConfigurationService copyDescendant = deepCopy(copy, descId, descendant,
                        rootComponentConfigurations, includeContainerItems);
                copy.componentConfigurations.put(copyDescendant.id, copyDescendant);
                copy.orderedListConfigs.add(copyDescendant);
                copy.childConfByName.put(StringPool.get(copyDescendant.getName()), copyDescendant);
                // do not need them by name for copies
            }
        }

        // the copy is populated
        //populated.add(copy);
        copy.referencesPopulated = true;
        return copy;
    }


    /**
     * Populates Legacy component parameters
     * @param websiteClassLoader Classloader of website application components belong to
     * @param paramsCache Parameters cache per component class
     */
    public void populateLegacyComponentParameters(final ClassLoader websiteClassLoader, @Nonnull Map<String, List<DynamicParameter>> paramsCache) {
        if (isEmpty(this.getComponentClassName())) {
            return;
        }

        if (paramsCache.containsKey(this.getComponentClassName())) {
            hstDynamicComponentParameters = paramsCache.get(this.getComponentClassName());
        } else {
            final ParametersInfo parametersInfo = ParametersInfoAnnotationUtils.getParametersInfoAnnotation(this, websiteClassLoader);
            List<DynamicParameter> dynamicParameters = new ArrayList<>();
            if (parametersInfo != null) {
                for (final Method method : parametersInfo.type().getMethods()) {
                    if (method.isAnnotationPresent(Parameter.class)) {
                        final Parameter parameter = method.getAnnotation(Parameter.class);
                        dynamicParameters.add(new DynamicComponentParameter(parameter, method));
                    }
                }
            }

            paramsCache.put(this.getComponentClassName(), dynamicParameters);
            hstDynamicComponentParameters = dynamicParameters;
        }
    }

    /**
     * Adds annotation based component parameter definitions
     * @param websiteClassLoader Classloader of website application components belong to
     */
    public void populateAnnotationComponentParameters(final ClassLoader websiteClassLoader) {
        if (isEmpty(this.getComponentClassName())) {
            return;
        }
        final ParametersInfo parametersInfo = ParametersInfoAnnotationUtils.getParametersInfoAnnotation(this, websiteClassLoader);
        if (parametersInfo != null) {
            for (final Method method : parametersInfo.type().getMethods()) {
                if (method.isAnnotationPresent(Parameter.class)) {
                    final Parameter parameter = method.getAnnotation(Parameter.class);
                    final Optional<DynamicParameter> dynamicParameter = hstDynamicComponentParameters.stream().filter(
                            dynamicComponentParameter -> (parameter.name().equals(dynamicComponentParameter.getName())))
                            .findFirst();
                    if (dynamicParameter.isPresent()) {
                        if (!dynamicParameter.get().getValueType().supportsReturnType(method.getReturnType())) {
                            //Remove jcr based parameter if an annotation parameter with the same name but with the different type exists
                            log.warn(
                                    "Jcr and annotation based parameters are defined with the same name but with different type: {}",
                                    parameter.name());
                            hstDynamicComponentParameters.remove(dynamicParameter.get());
                        } else {
                            //don't add annotation based parameter to the list, which means jcr based parameter 
                            //overrides annotation based parameter
                            continue;
                        }
                    }
                    hstDynamicComponentParameters.add(new DynamicComponentParameter(parameter, method));
                }
            }
        }
    }

    /**
     * Read Field Groups from JCR
     * @param node hst:containeritemcomponent node which might contain field group configuration
     */
    void readJcrFieldGroups(final HstNode node) {
        final Set<String> uniqueParameterNames = new HashSet<>();
        final ValueProvider valueProvider = node.getValueProvider();
        stream(nullToEmpty(valueProvider.getStrings(COMPONENT_PROPERTY_FIELD_GROUPS)))
                .filter(StringUtils::isNotEmpty).distinct().map(StringPool::get)
                .forEach(groupName -> {
                    final List<String> groupParameterNames = stream(nullToEmpty(
                            valueProvider.getStrings(COMPONENT_PROPERTY_FIELD_GROUPS + "." + groupName)))
                            .filter(StringUtils::isNotEmpty).filter(x -> !uniqueParameterNames.contains(x)).distinct().map(StringPool::get)
                            .collect(Collectors.toList());
                    uniqueParameterNames.addAll(groupParameterNames);
                    final DynamicFieldGroup fieldGroup = new DynamicFieldGroup(groupName, groupParameterNames);
                    this.fieldGroups.add(fieldGroup);
                });
    }


    /**
     * Populate Field Groups from Annotation model for legacy components
     * @param websiteClassLoader Website classloader
     * @param fieldGroupsCache Field Groups cache per component class
     */
    protected void populateLegacyFieldGroups(ClassLoader websiteClassLoader, @Nonnull Map<String, List<DynamicFieldGroup>> fieldGroupsCache) {
        if (isEmpty(this.getComponentClassName())) {
            return;
        }

        if (fieldGroupsCache.containsKey(this.getComponentClassName())) {
            this.fieldGroups = fieldGroupsCache.get(this.getComponentClassName());
        } else {
            final ParametersInfo parametersInfo = ParametersInfoAnnotationUtils.getParametersInfoAnnotation(this, websiteClassLoader);
            final Collection<DynamicFieldGroup> annotatedFieldGroups = populateAnnotatedFieldGroups(parametersInfo, new HashMap<>());
            final ArrayList<DynamicFieldGroup> fieldGroups = new ArrayList<>(annotatedFieldGroups);
            fieldGroupsCache.put(this.getComponentClassName(), fieldGroups);
            this.fieldGroups = fieldGroups;
        }
    }

    /**
     * Populate Field Groups from JCR & Annotation models
     * @param websiteClassLoader Website classloader
     */
    public void populateFieldGroups(ClassLoader websiteClassLoader) {
        if (isEmpty(this.getComponentClassName())) {
            return;
        }
        final ParametersInfo parametersInfo = ParametersInfoAnnotationUtils.getParametersInfoAnnotation(this, websiteClassLoader);
        populateFieldGroups(parametersInfo);
    }


    /**
     * Populate Field Groups from JCR & Annotation models
     * @param parametersInfo Parameters Info of a component class.
     */
    void populateFieldGroups(final ParametersInfo parametersInfo) {
        final Map<String, DynamicFieldGroup> jcrGroupMap = this.fieldGroups.stream()
                .collect(Collectors.toMap(DynamicFieldGroup::getTitleKey, Function.identity()));
        final Collection<DynamicFieldGroup> annotatedFieldGroups = populateAnnotatedFieldGroups(parametersInfo, jcrGroupMap);
        final Collection<DynamicFieldGroup> combinedGroups = mergeFieldGroups(annotatedFieldGroups, this.fieldGroups);
        cleanDuplicateParameters(combinedGroups);
        this.fieldGroups = new ArrayList<>(combinedGroups);
    }

    /**
     * Populate field groups from annotations
     */
    @SuppressWarnings("unchecked")
    private Collection<DynamicFieldGroup> populateAnnotatedFieldGroups(final ParametersInfo parametersInfo,
                                                                       final Map<String, DynamicFieldGroup> jcrGroups) {
        if (parametersInfo ==  null) {
            return Collections.EMPTY_LIST;
        }

        final List<DynamicFieldGroup> annotatedFieldGroups = new ArrayList<>();
        final Set<String> uniqueParametersList = new HashSet<>();
        final Stream<FieldGroup> fieldGroupStream = getAnnotatedFieldGroups(parametersInfo.type());

        fieldGroupStream.filter(fg -> !jcrGroups.containsKey(fg.titleKey())).forEach(fieldGroup -> {
            final List<String> uniqueParams = stream(fieldGroup.value()).distinct()
                    .filter(fg -> !uniqueParametersList.contains(fg)).collect(Collectors.toList());
            uniqueParametersList.addAll(uniqueParams);
            final DynamicFieldGroup dynamicFieldGroup = new DynamicFieldGroup(fieldGroup.titleKey(), uniqueParams);
            if (!annotatedFieldGroups.contains(dynamicFieldGroup)) {
                annotatedFieldGroups.add(dynamicFieldGroup);
            } else {
                //If group already exists, populate extra parameters. check if parameter is unknown and ignore it.
                final DynamicFieldGroup existingGroup = annotatedFieldGroups.get(annotatedFieldGroups.indexOf(dynamicFieldGroup));
                final List<String> parameters = existingGroup.getParameters();
                stream(fieldGroup.value()).filter(parameterName -> !parameters.contains(parameterName))
                        .distinct().forEach(parameters::add);
            }
        });
        return annotatedFieldGroups;
    }

    /**
     * Return flattened stream of Field Groups in order they're defined on a class level
     * @param componentClass ParametersInfo type.
     */
    private Stream<FieldGroup> getAnnotatedFieldGroups(final Class<?> componentClass) {
        return getBreadthFirstInterfaceHierarchy(componentClass).stream()
                .filter(interfaceClass -> interfaceClass.getAnnotation(FieldGroupList.class) != null)
                .map(interfaceClass -> interfaceClass.getAnnotation(FieldGroupList.class))
                .flatMap(fgl -> stream(fgl.value()));
    }


    /**
     * Remove duplicated group parameters
     */
    private void cleanDuplicateParameters(final Collection<DynamicFieldGroup> groups) {
        final Set<String> uniqueParamNames = new HashSet<>();
        groups.forEach(group -> group.getParameters().removeIf(parameter -> !uniqueParamNames.add(parameter)));
    }

    /**
     * Merge annotated and jcr defined field groups. If a field group is defined both
     * through annotation & JCR definition, then JCR definition wins, i.e. annotation based definition
     * is replaced by the one from JCR
     * @param annotatedFieldGroups Annotation based definitions
     * @param jcrFieldGroups JCR based definitions
     * @return A LinkedMaop
     */
    @NotNull
    private Collection<DynamicFieldGroup> mergeFieldGroups(final Collection<DynamicFieldGroup> annotatedFieldGroups,
                                                           final Collection<DynamicFieldGroup> jcrFieldGroups) {
        return Stream.concat(annotatedFieldGroups.stream(), jcrFieldGroups.stream())
                .collect(Collectors.toMap(DynamicFieldGroup::getTitleKey, Function.identity(), (left, right) -> right, LinkedHashMap::new)).values();
    }

    static List<Class<?>> getBreadthFirstInterfaceHierarchy(final Class<?> clazz) {
        final List<Class<?>> interfaceHierarchyList = new ArrayList<>();
        interfaceHierarchyList.add(clazz);
        populateBreadthFirstSuperInterfaces(clazz.getInterfaces(), interfaceHierarchyList);
        return interfaceHierarchyList;
    }

    private static void populateBreadthFirstSuperInterfaces(final Class<?>[] interfaces,
                                                            final List<Class<?>> populatedSuperInterfaces) {

        populatedSuperInterfaces.addAll(Arrays.asList(interfaces));
        final List<Class<?>> superInterfaces = new ArrayList<>();
        for (final Class<?> clazz : interfaces) {
            superInterfaces.addAll(Arrays.asList(clazz.getInterfaces()));
        }
        if (superInterfaces.size() == 0) {
            return;
        }
        populateBreadthFirstSuperInterfaces(superInterfaces.toArray(new Class[superInterfaces.size()]),
                populatedSuperInterfaces);
    }


    protected void populateCatalogItemReference(final List<HstComponentConfiguration> availableContainerItems) {
        final Optional<HstComponentConfiguration> catalogItem = availableContainerItems.stream()
                .filter(c -> c.getId().equals(this.getComponentDefinition())).findFirst();

        final HstComponentConfiguration catalogItemRef = catalogItem.orElse(null);

        if (catalogItemRef == null) {
            log.warn("Invalid component '{}' since no catalog item found for '{} = {}'", getCanonicalStoredLocation(),
                    COMPONENT_PROPERTY_COMPONENTDEFINITION, this.getComponentDefinition());
        } else {
            if (this.componentClassName == null) {
                this.componentClassName = catalogItemRef.getComponentClassName();
            }

            if (this.xtype == null) {
                this.xtype = catalogItemRef.getXType();
            }

            if (this.ctype == null) {
                this.ctype = catalogItemRef.getCType();
            }

            if (this.hstTemplate == null) {
                this.hstTemplate = catalogItemRef.getHstTemplate();
            }

            if (this.iconPath == null) {
                this.iconPath = catalogItemRef.getIconPath();
            }

            this.fieldGroups = catalogItemRef.getFieldGroups();

            if (this.label == null) {
                this.label = catalogItemRef.getLabel();
            }

            this.hstDynamicComponentParameters = catalogItemRef.getDynamicComponentParameters();
        }
    }

    // marker if this instance already has been populated
    private boolean referencesPopulated = false;

    protected void populateComponentReferences(Map<String, HstComponentConfiguration> rootComponentConfigurations) {
        if (referencesPopulated || rootComponentConfigurations == null) {
            return;
        }
        referencesPopulated = true;

        if (this.getReferenceComponent() != null) {
            HstComponentConfigurationService referencedComp = (HstComponentConfigurationService) rootComponentConfigurations
                    .get(this.getReferenceComponent());
            if (referencedComp != null && !referencedComp.isMarkedDeleted()) {
                if (referencedComp == this) {
                    throw new ModelLoadingException("There is a component referencing itself: this is not allowed. The site configuration cannot be loaded. Incorrect ComponentId = " + this.getId());
                }
                if (referencedComp.getReferenceComponent() != null) {
                    // populate referenced comp first:
                    referencedComp.populateComponentReferences(rootComponentConfigurations);
                }
                // get all properties that are null from the referenced component:
                if (this.componentClassName == null) {
                    this.componentClassName = referencedComp.componentClassName;
                }
                if (this.parametersInfoClassName == null) {
                    this.parametersInfoClassName = referencedComp.parametersInfoClassName;
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
                if (this.hippoIdentifier == null) {
                    this.hippoIdentifier = referencedComp.hippoIdentifier;
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
                if (this.ctype == null) {
                    this.ctype = referencedComp.ctype;
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
                if (suppressWasteMessage == null) {
                    this.suppressWasteMessage = referencedComp.suppressWasteMessage;
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

                if (this.fieldGroups.isEmpty()) {
                    this.fieldGroups = referencedComp.fieldGroups;
                } else {
                    mergeFieldGroups(referencedComp.fieldGroups, this.fieldGroups);
                }

                if (this.hstDynamicComponentParameters.isEmpty()) {
                    this.hstDynamicComponentParameters = referencedComp.hstDynamicComponentParameters;
                } else {
                    mergeResidualDynamicParameters(referencedComp.hstDynamicComponentParameters, this.hstDynamicComponentParameters);
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
                        childToMerge.populateComponentReferences(rootComponentConfigurations);
                    }

                    if (this.childConfByName.get(childToMerge.name) != null) {
                        // we have an overlay again because we have a component with the same name
                        // first populate it
                        HstComponentConfigurationService existingChild = this.childConfByName.get(childToMerge.name);
                        existingChild.populateComponentReferences(rootComponentConfigurations);
                        childToMerge.populateComponentReferences(rootComponentConfigurations);
                        // merge the childToMerge with existingChild
                        existingChild.combine(childToMerge, rootComponentConfigurations);
                    } else {
                        // make a copy of the child
                        addDeepCopy(childToMerge, rootComponentConfigurations);
                    }
                }

            } else {
                log.warn("Cannot lookup referenced component '{}' for this component ['{}']. We skip this reference", this
                        .getReferenceComponent(), this.getId());
            }
        }
    }

    private void mergeFieldGroups(final List<DynamicFieldGroup> source,
                                  final List<DynamicFieldGroup> target) {
        if (source == target) {
            return;
        }
        source.forEach(dynamicFieldGroup -> {
            // dynamicFieldGroup has an equals and hashcode impl
            if (!target.contains(dynamicFieldGroup)) {
                target.add(dynamicFieldGroup);
            }
        });
    }

    // only merge residual parameters, not the one from the ParametersInfo since a component class can
    // have only one single ParametersInfo : merging residual params means that there is a component which
    // has a ParametersInfo class and inherits from another component which is a new style component
    // which has dynamic parameters configured
    // Note that merging residual parameters in effect for now will never happen since residual parameters are currently
    // only supported on container items which do not support inheritance
    private void mergeResidualDynamicParameters(final List<DynamicParameter> source,
                                                final List<DynamicParameter> target) {
        if (source == target) {
            return;
        }
        source.forEach(dynamicParameter -> {
            if (!dynamicParameter.isResidual()) {
                // only residual parameters should be merged since a class can have only one ParametersInfo class
                return;
            }

            final String name = dynamicParameter.getName();

            // DynamicParameter instances do not have hashcode/equals hence we need to go through all the target params
            if (target.stream().noneMatch(targetParam -> name.equals(targetParam.getName()))) {
                // found a residual parameter which was not yet present in the target component its dynamic parametr
                target.add(dynamicParameter);
            }
        });
    }



    private void combine(HstComponentConfigurationService childToMerge,
                         Map<String, HstComponentConfiguration> rootComponentConfigurations) {

        if (this.type == Type.CONTAINER_COMPONENT || childToMerge.type == Type.CONTAINER_COMPONENT) {
            log.warn("Incorrect component configuration: *Container* Components are not allowed to be merged with other " +
                    "components. Cannot merge '{}' and '{}' because at least one of them is a Container component. Fix configuration.", childToMerge.getId(), this.getId());
            return;
        }
        if (this.type == Type.CONTAINER_ITEM_COMPONENT || childToMerge.type == Type.CONTAINER_ITEM_COMPONENT) {
            log.warn("Incorrect component configuration: *ContainerItem* Components are not allowed to be merged with other " +
                    "components. Cannot merge '{}' and '{}' because at least one of them is a ContainerItemComponent. Fix configuration.", childToMerge.getId(), this.getId());
            return;
        }

        if (this.componentClassName == null) {
            this.componentClassName = childToMerge.componentClassName;
        }
        if (this.parametersInfoClassName == null) {
            this.parametersInfoClassName = childToMerge.parametersInfoClassName;
        }
        if (this.hstTemplate == null) {
            this.hstTemplate = childToMerge.hstTemplate;
        }
        if (this.label == null) {
            this.label = childToMerge.label;
        }
        if (this.hippoIdentifier == null) {
            this.hippoIdentifier = childToMerge.hippoIdentifier;
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
        if (this.ctype == null) {
            this.ctype = childToMerge.ctype;
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
        if (this.suppressWasteMessage == null) {
            this.suppressWasteMessage = childToMerge.suppressWasteMessage;
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

        if (this.fieldGroups.isEmpty()) {
            this.fieldGroups = childToMerge.fieldGroups;
        } else {
            mergeFieldGroups(childToMerge.fieldGroups, this.fieldGroups);
        }

        if (this.hstDynamicComponentParameters.isEmpty()) {
            this.hstDynamicComponentParameters = childToMerge.hstDynamicComponentParameters;
        } else {
            mergeResidualDynamicParameters(childToMerge.hstDynamicComponentParameters, this.hstDynamicComponentParameters);
        }

        if (!this.containerComponentReference) {
            this.containerComponentReference = childToMerge.containerComponentReference;
        }

        // Note we do NOT set this.shared = childToMerge.shared  : For XPages namely, an HstComponent can defined on the
        // XPage doc as well as on, say, the inherited abstract page: in that case, the abstract inherited page has
        // precedence

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
                existingChild.populateComponentReferences(rootComponentConfigurations);
                toMerge.populateComponentReferences(rootComponentConfigurations);
                this.childConfByName.get(toMerge.name).combine(toMerge, rootComponentConfigurations);
            } else {
                //  String newId = this.id + "-" + toMerge.id;
                //  this.deepCopy(this, newId, toMerge, populated, rootComponentConfigurations);
                // deepCopy also does the populateComponentReferences for child 'toMerge'
                this.addDeepCopy(toMerge, rootComponentConfigurations);
            }
        }

    }

    private void addDeepCopy(HstComponentConfigurationService childToMerge,
                             Map<String, HstComponentConfiguration> rootComponentConfigurations) {

        String newId = StringPool.get(this.id + "-" + childToMerge.id);

        HstComponentConfigurationService copy = deepCopy(this, newId, childToMerge,
                rootComponentConfigurations, true);

        this.componentConfigurations.put(copy.getId(), copy);
        this.childConfByName.put(copy.getName(), copy);
        this.orderedListConfigs.add(copy);

    }

    protected void setRenderPath(Map<String, HstComponentsConfigurationService.Template> templateResourceMap) {
        if (StringUtils.isNotEmpty(hstTemplate)) {
            HstComponentsConfigurationService.Template template = templateResourceMap.get(hstTemplate);
            if (template != null) {
                renderPath = StringPool.get(template.getEffectiveRenderPath());
                isNamedRenderer = template.isNamed();
            } else {
                log.warn("Cannot find hst:template '{}' for hst component '{}'.", hstTemplate, this.toString());
            }
            if (renderPath == null) {
                log.info("renderer '{}' for component '{}' can not be found. This component will not have a renderer " +
                        "by default. It can be set runtime or this component is used without renderer.", getHstTemplate(), id);
            }
        }
        for (HstComponentConfigurationService child : orderedListConfigs) {
            child.setRenderPath(templateResourceMap);
        }
    }

    protected void setServeResourcePath(Map<String, HstComponentsConfigurationService.Template> templateResourceMap) {
        HstComponentsConfigurationService.Template template = templateResourceMap.get(getHstResourceTemplate());

        if (template != null) {
            this.serveResourcePath = StringPool.get(template.getEffectiveRenderPath());
            isNamedResourceServer = template.isNamed();
        }
        for (HstComponentConfigurationService child : orderedListConfigs) {
            child.setServeResourcePath(templateResourceMap);
        }
    }

    protected void inheritParameters() {
        if (parent != null && parent.getParameters() != null) {
            for (Entry<String, String> entry : parent.getParameters().entrySet()) {
                if (parameters.containsKey(entry.getKey())) {
                    log.debug("Skip adding parameter '{}' = '{}' to component {} from ancestor {} because parameter '{}' is" +
                            " already present ", entry.getKey(), entry.getValue(), this, parent, entry.getKey());
                    continue;
                }
                String parameterName = entry.getKey();
                log.debug("Adding inherited parameter '{}' = '{}' to component {} from ancestor {}", parameterName,
                        entry.getValue(), this, parent);
                parameters.put(parameterName, entry.getValue());
                // if the parameter has a prefix that is not yet in parameterNamePrefixSet, add it as well
                if (parameterName.indexOf(PARAMETER_PREFIX_NAME_DELIMITER) > -1) {
                    String prefix = parameterName.substring(0, parameterName.indexOf(PARAMETER_PREFIX_NAME_DELIMITER));
                    if (!parameterNamePrefixSet.contains(prefix)) {
                        parameterNamePrefixSet.add(prefix);
                    }
                }
            }
        }
        for (HstComponentConfigurationService child : orderedListConfigs) {
            child.inheritParameters();
        }
    }

    /**
     * get all the unique variants for this component + its descendants and set this to variants instance variable if
     * not empty
     */
    public void populateVariants() {
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
     * Since the HST supports async only on one level (thus async descendants of an async component are rendered with
     * the async ancestor), we only include the variants of an async component if the current or an ancestor component
     * is already async
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
                HstComponentConfigurationService parent = (HstComponentConfigurationService) getParent();
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
            usedChildReferenceNames = Collections.emptySet();
        }
    }

    void autocreateReferenceNames() {

        for (HstComponentConfigurationService child : orderedListConfigs) {
            child.autocreateReferenceNames();
            final String referenceName = child.getReferenceName();
            if (StringUtils.isBlank(referenceName)) {
                setAutocreatedReference(child, usedChildReferenceNames, autocreatedCounter);
            } else if (usedChildReferenceNames.contains(referenceName)){
                log.error("componentConfiguration '{}' contains invalid explicit reference '{}' since already in use. " +
                        "Autocreating a new one now.", child.getCanonicalStoredLocation(), referenceName);
                setAutocreatedReference(child, usedChildReferenceNames, autocreatedCounter);
            } else {
                usedChildReferenceNames.add(referenceName);
            }
        }
    }

    public void removeChild(HstComponentConfiguration child) {
        componentConfigurations.remove(child.getId());
        orderedListConfigs.remove(child);
        childConfByName.remove(child.getName());
    }


    public void transformXpageLayoutContainer(final HstComponentConfigurationService xPageDocumentContainer) {
        if (!detached) {
            throw new ExperiencePageLoadingException("Not allowed to transform a 'non-hst-model-detached' " +
                    "HstComponentConfigurationService into an XPage for a Document");
        }

        if (getComponentType() != CONTAINER_COMPONENT || xPageDocumentContainer.getComponentType() !=  CONTAINER_COMPONENT) {
            throw new ExperiencePageLoadingException(format("Not allowed to merge XpageDocument container '%s' with " +
                    "non-container component '%s'", xPageDocumentContainer.getCanonicalStoredLocation(), getCanonicalStoredLocation()));
        }

        // assert the root component of the container to merge with is an 'xpage' component
        HstComponentConfigurationService root = this;
        while (root.getParent() != null) {
            root = (HstComponentConfigurationService)root.getParent();
        }
        if (!root.isXPage()) {
            throw new ExperiencePageLoadingException(format("Not allowed to merge XpageDocument container '%s' with " +
                            "container component '%s' which is not part of an XPage Layout",
                    xPageDocumentContainer.getCanonicalStoredLocation(), getCanonicalStoredLocation()));
        }

        // for the Channel Mgr interactions, make sure to *ONLY* replace the identifier and canonical stored location of
        // the MERGED HST config contariner
        canonicalIdentifier = xPageDocumentContainer.getCanonicalIdentifier();
        canonicalStoredLocation = xPageDocumentContainer.getCanonicalStoredLocation();

        lastModified = xPageDocumentContainer.getLastModified();

        // mark the component to be an exp page container
        experiencePageComponent = true;
        // since the XPage document contains the container, we mark it shared
        shared = false;

        // even when the container comes from inherited common configuration, an XPage Document can hijack it via the
        // hippo:identifier making it effectively a non-inherited and non-shared container!
        inherited = false;

        // replace all the existing children with those from 'xPageDocumentContainer'
        orderedListConfigs = xPageDocumentContainer.orderedListConfigs;
        componentConfigurations = xPageDocumentContainer.componentConfigurations;
        childConfByName = xPageDocumentContainer.childConfByName;

        // set the parent of the xPageDocumentContainer to the parent of the XPage Layout container such that
        // if you request the getParent#getParent on a container item, you get the parent of the XPage Layout container
        // since the parent of a container item will give the container of the XPage Document
        xPageDocumentContainer.parent = parent;
    }


    /**
     * This is a very specific transformation: Although the container really is still from the XPage layout config, it
     * is a container that is not present in the current request based XPage document variant, hence we have to 'fake' it
     * in such that it becomes visible in the Channel Manager: Only we fake it in without potential component items from
     * XPage Layout : When in the CM someone adds a container item to it, we make sure the correct container gets added
     * to the Xpage in the Document!
     */
    public void transformUnresolvedXpageLayoutContainer() {

        // mark the component to be an exp page container (even though this is a copy config from Xpage Config!)
        // the reason: in the CM UI, an author should be able to add a container item to this container still!
        experiencePageComponent = true;

        // shared FALSE because the CM needs to be able to interact with the component as PART OF an Xpage Document!
        shared = false;
        // even when the container comes from inherited common configuration, an XPage Document can hijack it via the
        // hippo:identifier making it effectively a non-inherited and non-shared container!
        inherited = false;

        if (xpageLayoutComponent) {
            unresolvedXpageLayoutContainer = true;
        }

        // remove any child items present in Xpage Layout
        orderedListConfigs = Collections.emptyList();
        componentConfigurations = Collections.emptyMap();
        childConfByName = Collections.emptyMap();
    }


    public void addXPageDocChild(final HstComponentConfigurationService child) {
        child.parent = this;
        // xpage doc never contains 'xpageLayoutComponent' : to be sure, set it explicitly to false
        child.xpageLayoutComponent = false;
        componentConfigurations.put(child.getId(), child);
        orderedListConfigs.add(child);
        childConfByName.put(child.getName(), child);
    }


    public void merge(final HstComponentConfigurationService xpageDocComponent) {
        HstComponentConfigurationService component = (HstComponentConfigurationService)getChildByName(xpageDocComponent.getName());
        if (component ==null) {
            throw new ExperiencePageLoadingException(String.format("Cannot merge XPage Doc component '%s' since does not exist for" +
                    "'%s'", xpageDocComponent.getName(), this.getCanonicalStoredLocation()));
        }
        component.combine(xpageDocComponent, null);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("HstComponentConfiguration [id=");
        builder.append(id).append(", stored jcr location=").append(canonicalStoredLocation)
                .append(", className=").append(this.componentClassName)
                .append(", parametersInfoClassName=").append(this.parametersInfoClassName)
                .append(", template=").append(this.hstTemplate).append("]");
        return builder.toString();
    }

    @Override
    public boolean getAndSetLogWasteMessageProcessed() {
        return logWasteMessageProcessed.getAndSet(true);
    }
}
