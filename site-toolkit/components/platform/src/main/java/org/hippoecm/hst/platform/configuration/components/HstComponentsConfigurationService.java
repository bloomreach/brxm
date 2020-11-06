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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.components.DynamicFieldGroup;
import org.hippoecm.hst.configuration.components.DynamicParameter;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.components.HstComponentsConfiguration;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.core.internal.StringPool;
import org.hippoecm.hst.platform.configuration.cache.CompositeConfigurationNodes;
import org.hippoecm.hst.platform.configuration.model.ModelLoadingException;
import org.hippoecm.hst.provider.ValueProvider;
import org.slf4j.LoggerFactory;

import static java.util.Collections.unmodifiableMap;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_CONTAINERITEM_PACKAGE;
import static org.hippoecm.hst.configuration.HstNodeTypes.TEMPLATE_PROPERTY_IS_NAMED;
import static org.hippoecm.hst.configuration.HstNodeTypes.TEMPLATE_PROPERTY_RENDERPATH;
import static org.hippoecm.hst.configuration.HstNodeTypes.TEMPLATE_PROPERTY_SCRIPT;
import static org.hippoecm.hst.core.container.ContainerConstants.FREEMARKER_JCR_TEMPLATE_PROTOCOL;
import static org.hippoecm.hst.platform.configuration.cache.HstConfigurationLoadingCache.createCatalogItemId;
import static org.hippoecm.hst.platform.configuration.cache.HstConfigurationLoadingCache.isCatalogItem;

public class HstComponentsConfigurationService implements HstComponentsConfiguration {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(HstComponentsConfigurationService.class);

    private final String id;

    /*
     * canonicalComponentConfigurations are component configurations that are retrievable through getComponentConfiguration(String id),
     * They are the HstComponentConfiguration items that are not the result of enhancing but present without enhancing
     */
    private final Map<String, HstComponentConfiguration> canonicalComponentConfigurations;

    /*
     * prototypePages are component configurations that are retrievable through getComponentConfiguration(String id) and are directly
     * configured below 'hst:prototypepages'
     */
    private Map<String, HstComponentConfiguration> prototypePages = new HashMap<>();


    private Map<String, HstComponentConfiguration> xPages;

    /*
     * The Map of all containter items. These are the hst:containeritemcomponent's that are configured as child of hst:containeritemcomponent's
     */
    private List<HstComponentConfiguration> availableContainerItems = new ArrayList<>();

    private final Set<String> usedReferenceNames = new HashSet<>();
    private AtomicInteger autoCreatedCounter = new AtomicInteger(0);

    /**
     * Map from template node name to Template
     */
    private final Map<String, Template> templateResourceMap;

    public HstComponentsConfigurationService(final CompositeConfigurationNodes ccn,
                                             final List<HstComponentConfiguration> commonCatalogItem,
                                             final ClassLoader websiteClassLoader) throws ModelLoadingException {

        id = ccn.getConfigurationRootNode().getValueProvider().getPath();

        final CompositeConfigurationNodes.CompositeConfigurationNode referableContainersCCN = ccn.getCompositeConfigurationNodes().get(
                HstNodeTypes.NODENAME_HST_WORKSPACE + "/" + HstNodeTypes.NODENAME_HST_CONTAINERS);

        final Map<String, HstNode> referableContainers;
        if (referableContainersCCN != null) {
            referableContainers = referableContainersCCN.getCompositeChildren();
        } else {
            referableContainers = Collections.emptyMap();
        }

        String[] mainComponentNodeNames = {HstNodeTypes.NODENAME_HST_COMPONENTS,
                HstNodeTypes.NODENAME_HST_ABSTRACTPAGES,
                HstNodeTypes.NODENAME_HST_PAGES,
                HstNodeTypes.NODENAME_HST_PROTOTYPEPAGES,
                HstNodeTypes.NODENAME_HST_XPAGES};

        final String rootConfigurationPathPrefix = ccn.getConfigurationRootNode().getValueProvider().getPath() + "/";

        List<HstComponentConfiguration> nonPrototypeRootComponents  = new ArrayList<>();
        for (String mainComponentNodeName : mainComponentNodeNames) {
            log.debug("Initializing the {}", mainComponentNodeName);
            final CompositeConfigurationNodes.CompositeConfigurationNode componentNodes = ccn.getCompositeConfigurationNodes().get(mainComponentNodeName);
            if (componentNodes == null) {
                log.debug("No configuration nodes present for {}", mainComponentNodeName);
                continue;
            }
            init(componentNodes, mainComponentNodeName, rootConfigurationPathPrefix, referableContainers, nonPrototypeRootComponents);
        }

        prototypePages = unmodifiableMap(prototypePages);

        // from the nonPrototypeRootComponents, take the root components which are an XPage component and put them in a
        // map where the key is the (unique within 1 hst:configuration) xpage name
        xPages = unmodifiableMap(
                nonPrototypeRootComponents.stream()
                        .filter(hcc -> ((HstComponentConfigurationService)hcc).isXPage())
                        .collect(Collectors.toMap(hcc -> hcc.getName(), hcc -> hcc)));

        // populate all the available containeritems that are part of hst:catalog. These container items do *not* need to be enhanced as they
        // are *never* used directly. They are only to be used by the page composer that can drop these containeritems into containers
        final CompositeConfigurationNodes.CompositeConfigurationNode catalog = ccn.getCompositeConfigurationNodes().get(HstNodeTypes.NODENAME_HST_CATALOG);

        if (catalog != null) {
            log.debug("Initializing the catalog");
            initCatalog(catalog, rootConfigurationPathPrefix, websiteClassLoader);
        }

        if (commonCatalogItem != null) {
            availableContainerItems.addAll(commonCatalogItem);
        }

        if (availableContainerItems.isEmpty()) {
            availableContainerItems = Collections.emptyList();
        } else {
            availableContainerItems = Collections.unmodifiableList(availableContainerItems);
        }

        if (nonPrototypeRootComponents.isEmpty()) {
            canonicalComponentConfigurations = Collections.emptyMap();
        } else {
            canonicalComponentConfigurations = unmodifiableMap(
                    flattened(nonPrototypeRootComponents)
                            .collect(Collectors
                                    .toMap(hstComponentConfiguration -> hstComponentConfiguration.getId(),
                                            hstComponentConfiguration -> hstComponentConfiguration))
            );
        }

        /*
         * The component tree needs to be enhanced, for
         * 1: merging referenced components,
         * 2: autocreating missing referenceNames
         * 3: setting renderpaths for each component
         * 4: Adding parameters from parent components to child components and override them when they already are present
         */

        templateResourceMap = unmodifiableMap(getTemplateResourceMap(ccn.getCompositeConfigurationNodes().get(HstNodeTypes.NODENAME_HST_TEMPLATES)));

        populateComponentReferences(canonicalComponentConfigurations.values(), websiteClassLoader);

        enhanceComponentTree(nonPrototypeRootComponents, true);

    }

    public void populateComponentReferences(final Collection<HstComponentConfiguration> populate,
                                            final ClassLoader websiteClassLoader) {

        final Map<String, List<DynamicParameter>> legacyComponentParametersCache = new HashMap<>();
        final Map<String, List<DynamicFieldGroup>> legacyComponentFieldGroupsCache = new HashMap<>();

        for (HstComponentConfiguration child : populate) {
            if (isNotEmpty(child.getComponentDefinition())) {
                ((HstComponentConfigurationService) child).populateCatalogItemReference(availableContainerItems);
            } else {
                // In case the component is a container item, this is legacy component instances support. For components
                // which are not container items, this is not legacy!
                // If component instance does not have a component definition reference, explicitly populate component parameters.
                ((HstComponentConfigurationService) child).populateLegacyComponentParameters(websiteClassLoader,legacyComponentParametersCache);
                ((HstComponentConfigurationService) child).populateLegacyFieldGroups(websiteClassLoader, legacyComponentFieldGroupsCache);
            }
        }

        for (HstComponentConfiguration child : populate) {
            if (isEmpty(child.getComponentDefinition())) {
                // Only AFTER #populateCatalogItemReference, #populateLegacyComponentParameters and #populateLegacyFieldGroups
                // have been done it is allowed to invoke #populateComponentReferences : If we do it while not all
                // components have the 'component parameters' set, the result can be that referenced component items are
                // already merged/deep copied into another component while not having their 'dynamic component parameters' set, and
                // thus are not fully functional!
                ((HstComponentConfigurationService) child).populateComponentReferences(canonicalComponentConfigurations);
            }
        }

    }

    public void enhanceComponentTree(final Collection<HstComponentConfiguration> childComponents, final boolean hstConfigModel) {


        if (hstConfigModel) {
            //  autocreating missing referenceNames : never needed for request based XPage config since for XPage we
            // reset the entire namespaces since cloned (detached) from HST Model altogether
            for (HstComponentConfiguration child : childComponents) {
                autocreateReferenceNames(child);
            }
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

        if (hstConfigModel) {
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
        }


        for (HstComponentConfiguration child : childComponents) {
            ((HstComponentConfigurationService) child).populateIsCompositeCacheable();
        }

        if (hstConfigModel) {
            // for request based XPages, do not bother to optimize memory usage since xpage config GC-ed at the end
            // of request
            for (HstComponentConfiguration child : childComponents) {
                ((HstComponentConfigurationService) child).makeCollectionsImmutableAndOptimize();
            }
        }

    }

    public HstComponentConfiguration getComponentConfiguration(String id) {
        return this.canonicalComponentConfigurations.get(id);
    }

    public Map<String, HstComponentConfiguration> getComponentConfigurations() {
        return canonicalComponentConfigurations;
    }


    public List<HstComponentConfiguration> getAvailableContainerItems() {
        return availableContainerItems;
    }

    @Override
    public Map<String, HstComponentConfiguration> getPrototypePages() {
        return prototypePages;
    }

    @Override
    public Map<String, HstComponentConfiguration> getXPages() {
        return xPages;
    }


    public Map<String, Template> getTemplates() {
        return templateResourceMap;
    }

    private void autocreateReferenceNames(final HstComponentConfiguration componentConfiguration) {

        final String referenceName = componentConfiguration.getReferenceName();
        if (StringUtils.isBlank(referenceName)) {

            setAutocreatedReference((HstComponentConfigurationService) componentConfiguration, usedReferenceNames, autoCreatedCounter);
        } else if (usedReferenceNames.contains(referenceName)){
            log.error("componentConfiguration '{}' contains invalid explicit reference '{}' since already in use. " +
                    "Autocreating a new one now.", componentConfiguration.getCanonicalStoredLocation(), referenceName);
            setAutocreatedReference((HstComponentConfigurationService) componentConfiguration, usedReferenceNames, autoCreatedCounter);
        } else {
            usedReferenceNames.add(referenceName);
        }

        ((HstComponentConfigurationService) componentConfiguration).autocreateReferenceNames();
    }

    public static void setAutocreatedReference(final HstComponentConfigurationService componentConfiguration,
                                               Set<String> usedReferenceNames,
                                               final AtomicInteger autoCreatedCounter) {
        String autoRefName = "r" + (autoCreatedCounter.incrementAndGet());
        while (usedReferenceNames.contains(autoRefName)) {
            autoRefName = "r" + (autoCreatedCounter.incrementAndGet());
        }
        componentConfiguration.setReferenceName(StringPool.get(autoRefName));
    }

    /*
     * rootNodeName is either hst:components, hst:pages, hst:abstractpages or hst:prototypepages.
     */
    private void init(final CompositeConfigurationNodes.CompositeConfigurationNode node,
                      final String rootNodeName,
                      final String rootConfigurationPathPrefix,
                      final Map<String, HstNode> referableContainers,
                      final List<HstComponentConfiguration> nonPrototypeRootComponents) {

        for (HstNode child : node.getCompositeChildren().values()) {
            if (isHstComponentType(child)) {
                try {
                    HstComponentConfiguration componentConfiguration = new HstComponentConfigurationService(child,
                            null, rootNodeName, referableContainers, rootConfigurationPathPrefix);

                    if (rootNodeName.equals(HstNodeTypes.NODENAME_HST_PROTOTYPEPAGES)) {
                        prototypePages.put(componentConfiguration.getId(), componentConfiguration);
                    } else {
                        nonPrototypeRootComponents.add(componentConfiguration);
                    }
                    log.debug("Added component service with key '{}'", componentConfiguration.getId());
                } catch (ModelLoadingException e) {
                    if (log.isDebugEnabled()) {
                        log.warn("Skipping component '"+child.getValueProvider().getPath()+"'" , e);
                    } else if (log.isWarnEnabled()) {
                        log.warn("Skipping component '{}' : '{}'", child.getValueProvider().getPath(), e.toString());
                    }
                }
            } else {
                log.warn("Skipping node '{}' because is not of type '{}'", child.getValueProvider().getPath(),
                        (HstNodeTypes.NODETYPE_HST_COMPONENT));
            }
        }
    }

    private boolean isHstComponentType(final HstNode node) {
        return HstNodeTypes.NODETYPE_HST_COMPONENT.equals(node.getNodeTypeName())
                || HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT.equals(node.getNodeTypeName())
                || HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT.equals(node.getNodeTypeName())
                || HstNodeTypes.NODETYPE_HST_XPAGE.equals(node.getNodeTypeName());
    }

    private void initCatalog(final CompositeConfigurationNodes.CompositeConfigurationNode catalog,
                             final String rootConfigurationPathPrefix, ClassLoader websiteClassLoader) {

        for(HstNode itemPackage :catalog.getCompositeChildren().values()){
            if(NODETYPE_HST_CONTAINERITEM_PACKAGE.equals(itemPackage.getNodeTypeName())) {
                for(HstNode containerItem : itemPackage.getNodes()) {
                    if(isCatalogItem(containerItem)) {
                        try {
                            // create a HstComponentConfigurationService that does not traverse to descendant components: this is not needed for the catalog. Hence, the argument 'false'
                            final String componentId = createCatalogItemId(containerItem);
                            final HstComponentConfigurationService componentConfiguration = new HstComponentConfigurationService(containerItem,
                                    null, HstNodeTypes.NODENAME_HST_COMPONENTS , true, null, rootConfigurationPathPrefix, componentId);
                            componentConfiguration.populateAnnotationComponentParameters(websiteClassLoader);
                            componentConfiguration.populateFieldGroups(websiteClassLoader);
                            availableContainerItems.add(componentConfiguration);
                            log.debug("Added catalog component to availableContainerItems with key '{}'", componentConfiguration.getId());
                        } catch (ModelLoadingException e) {
                            if (log.isDebugEnabled()) {
                                log.warn("Skipping catalog component '"+containerItem.getValueProvider().getPath()+"'", e);
                            } else if (log.isWarnEnabled()) {
                                log.warn("Skipping catalog component '{}' : '{}'", containerItem.getValueProvider().getPath(), e.toString());
                            }
                        }
                    } else {
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

    private Map<String, Template> getTemplateResourceMap(CompositeConfigurationNodes.CompositeConfigurationNode templateNodes) throws ModelLoadingException {
        if(templateNodes == null) {
            log.info("Configuration for '{}' does not have hst:templates. Model will be loaded without templates", id);
            return Collections.emptyMap();
        }
        Map<String, Template> templateResourceMap = new HashMap<>();

        for (HstNode templateNode : templateNodes.getCompositeChildren().values()) {
            Template template = new Template(templateNode);
            if (template.isValid()) {
                templateResourceMap.put(template.getName(), template);
            }
        }
        return templateResourceMap;
    }

    @Override
    public String toString() {
        return "HstComponentsConfigurationService [id='"+id+"', hashcode = '"+hashCode()+"']";
    }


    public static class Template {

        private final String name;
        private final String uuid;
        private final String path;
        private final String configuredRenderPath;
        private final String effectiveRenderPath;
        private final String script;
        private final boolean named;

        public Template(HstNode templateNode){
            final ValueProvider valueProvider = templateNode.getValueProvider();
            name = valueProvider.getName();
            path = valueProvider.getPath();
            uuid = valueProvider.getIdentifier();
            configuredRenderPath = valueProvider.getString(TEMPLATE_PROPERTY_RENDERPATH);
            script = valueProvider.getString(TEMPLATE_PROPERTY_SCRIPT);
            named = valueProvider.getBoolean(TEMPLATE_PROPERTY_IS_NAMED);

            if (StringUtils.isNotBlank(configuredRenderPath)) {
                effectiveRenderPath = configuredRenderPath;
            } else if (StringUtils.isNotBlank(script)) {
                effectiveRenderPath = FREEMARKER_JCR_TEMPLATE_PROTOCOL + path;
            } else {
                log.warn("Template '{}' is invalid, supply a hst:renderpath or hst:script.", getPath());
                effectiveRenderPath = null;
            }
        }

        public String getPath() {
            return path;
        }

        public String getName() {
            return name;
        }

        public String getUuid() {
            return uuid;
        }

        public String getEffectiveRenderPath() {
            return effectiveRenderPath;
        }

        public boolean isNamed() {
            return named;
        }

        public boolean isValid() {
            return getEffectiveRenderPath() != null;
        }
    }

}
