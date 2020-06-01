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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.platform.configuration.cache.CompositeConfigurationNodes;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.components.HstComponentsConfiguration;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.platform.configuration.model.ModelLoadingException;
import org.hippoecm.hst.core.internal.StringPool;
import org.hippoecm.hst.provider.ValueProvider;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.configuration.HstNodeTypes.TEMPLATE_PROPERTY_IS_NAMED;
import static org.hippoecm.hst.configuration.HstNodeTypes.TEMPLATE_PROPERTY_RENDERPATH;
import static org.hippoecm.hst.configuration.HstNodeTypes.TEMPLATE_PROPERTY_SCRIPT;
import static org.hippoecm.hst.core.container.ContainerConstants.FREEMARKER_JCR_TEMPLATE_PROTOCOL;

public class HstComponentsConfigurationService implements HstComponentsConfiguration {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(HstComponentsConfigurationService.class);

    private final String id;

    /*
     * canonicalComponentConfigurations are component configurations that are retrievable through getComponentConfiguration(String id),
     * They are the HstComponentConfiguration items that are not the result of enhancing but present without enhancing
     */
    private Map<String, HstComponentConfiguration> canonicalComponentConfigurations;

    /*
     * prototypePages are component configurations that are retrievable through getComponentConfiguration(String id) and are directly
     * configured below 'hst:prototypepages'
     */
    private Map<String, HstComponentConfiguration> prototypePages = new HashMap<>();

    /*
     * The Map of all containter items. These are the hst:containeritemcomponent's that are configured as child of hst:containeritemcomponent's
     */
    private List<HstComponentConfiguration> availableContainerItems = new ArrayList<>();

    private final Set<String> usedReferenceNames = new HashSet<>();
    private int autoCreatedCounter = 0;

    /**
     * Map from template node name to Template
     */
    private final Map<String, Template> templateResourceMap;

    public HstComponentsConfigurationService(final CompositeConfigurationNodes ccn,
                                             final List<HstComponentConfiguration> commonCatalogItem) throws ModelLoadingException {

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
                HstNodeTypes.NODENAME_HST_PROTOTYPEPAGES};

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

        prototypePages = Collections.unmodifiableMap(prototypePages);

        // populate all the available containeritems that are part of hst:catalog. These container items do *not* need to be enhanced as they
        // are *never* used directly. They are only to be used by the page composer that can drop these containeritems into containers
        final CompositeConfigurationNodes.CompositeConfigurationNode catalog = ccn.getCompositeConfigurationNodes().get(HstNodeTypes.NODENAME_HST_CATALOG);

        if (catalog != null) {
            log.debug("Initializing the catalog");
            initCatalog(catalog, rootConfigurationPathPrefix);
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
            canonicalComponentConfigurations = Collections.unmodifiableMap(
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
        
        templateResourceMap = Collections.unmodifiableMap(getTemplateResourceMap(ccn.getCompositeConfigurationNodes().get(HstNodeTypes.NODENAME_HST_TEMPLATES)));

        populateComponentReferences(canonicalComponentConfigurations);

        //  autocreating missing referenceNames
        for (HstComponentConfiguration child : nonPrototypeRootComponents) {
            autocreateReferenceNames(child);
        }

        enhanceComponentTree(nonPrototypeRootComponents);

    }

    public void populateComponentReferences(final Map<String, HstComponentConfiguration> populate) {
        for (HstComponentConfiguration child : populate.values()) {
            ((HstComponentConfigurationService) child).populateComponentReferences(populate);
        }
    }

    public void enhanceComponentTree(final List<HstComponentConfiguration> childComponents) {

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
        return canonicalComponentConfigurations;
    }


    public List<HstComponentConfiguration> getAvailableContainerItems() {
        return availableContainerItems;
    }

    @Override
    public Map<String, HstComponentConfiguration> getPrototypePages() {
        return prototypePages;
    }


    public Map<String, Template> getTemplates() {
        return templateResourceMap;
    }

    private void autocreateReferenceNames(final HstComponentConfiguration componentConfiguration) {

        if (componentConfiguration.getReferenceName() == null || "".equals(componentConfiguration.getReferenceName())) {

            String autoRefName = "r" + (++autoCreatedCounter);
            while (usedReferenceNames.contains(autoRefName)) {
                autoRefName = "r" + (++autoCreatedCounter);
            }
            ((HstComponentConfigurationService) componentConfiguration).setReferenceName(StringPool.get(autoRefName));
        }

        ((HstComponentConfigurationService) componentConfiguration).autocreateReferenceNames(false);
    }

    private void createExperienceComponentRefNames(final HstComponentConfigurationService experiencePageComponentConfig) {
        // this is the root experience page
        experiencePageComponentConfig.setReferenceName("ep");
        experiencePageComponentConfig.autocreateReferenceNames(true);
    }

    /**
     *
     * <p>
     *     Note that this method is invoked concurrently! Therefore, it should never modify non thread-safe instance
     *     variables of objects that are shared between threads, like this {@link HstComponentsConfigurationService} instance!
     * </p>
     * <p>
     *     Although hard to see from the code below, it does not modify ANY HST component instance from the HST in
     *     memory model, but only the 'request based Experience Page Components'
     * </p>
     * @param experiencePageComponentConfig
     */
    public void populateExperiencePage(final HstComponentConfigurationService experiencePageComponentConfig) {
        // we need to populate the components for experiencePageComponentConfig but can only do so if we also have
        // all the populated hstModelComponents from the in memory hst model (for example to resolve hst
        // config inherited components for experience page)

        final Map<String, HstComponentConfiguration> hstModelComponents = getComponentConfigurations();

        // add experiencePageComponentConfig to combined
        final Map<String, HstComponentConfiguration> combined = experiencePageComponentConfig.flattened()
                .collect(Collectors.toMap(HstComponentConfiguration::getId, comp -> comp));

        // populated HstComponentsConfigurationService#getComponentConfigurations() plus the flattened list of
        // components in experiencePageComponentConfig
        combined.putAll(hstModelComponents);

        // below is very delicate wrt concurrency: A HstComponentConfiguration is NOT thread-safe. However, the
        // method below won't populate (or modify) *any* component from hstComponentsConfigurationService.getComponentConfigurations()
        // since all these components already have
        //
        // HstComponentConfigurationService#referencesPopulated = true
        //
        // therefor, only the HstComponents from 'experiencePageComponentConfig' will be modified, and those components
        // are only for the current request and thus there won't be concurrency on those objects. Therefor the
        // below method is allowed even though there can be concurrency on the HstComponent objects from the
        // in memory HST model!
        populateComponentReferences(combined);

        // As a result of 'populateComponentReferences', any HST configuration model referenced hst components from
        // the experience page hst:page, will be *cloned* into the experiencePageComponentConfig. Therefor at this
        // point, experiencePageComponentConfig will contain all the required HstComponentService instances and these
        // are *not* shared with the Hst in memory model.


        createExperienceComponentRefNames(experiencePageComponentConfig);

        // resolve inherited components : note only the experiencePageComponentConfig is enhanced, not the
        // HstComponentService objects from the shared in memory model, so no concurrency involved
        enhanceComponentTree(Collections.singletonList(experiencePageComponentConfig));

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
                if (child.getValueProvider().hasProperty(HstNodeTypes.COMPONENT_PROPERTY_REFERECENCENAME)) {
                    // add to the used referencenames set
                    usedReferenceNames.add(StringPool.get(child.getValueProvider().getString(HstNodeTypes.COMPONENT_PROPERTY_REFERECENCENAME)));
                }
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
                || HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT.equals(node.getNodeTypeName());
    }
    
    private void initCatalog(final CompositeConfigurationNodes.CompositeConfigurationNode catalog,
                             final String rootConfigurationPathPrefix) {
        
        for(HstNode itemPackage :catalog.getCompositeChildren().values()){
            if(HstNodeTypes.NODETYPE_HST_CONTAINERITEM_PACKAGE.equals(itemPackage.getNodeTypeName())) {
                for(HstNode containerItem : itemPackage.getNodes()) {
                    if(HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT.equals(containerItem.getNodeTypeName()))
                    {
                        try {
                            // create a HstComponentConfigurationService that does not traverse to descendant components: this is not needed for the catalog. Hence, the argument 'false'
                            HstComponentConfiguration componentConfiguration = new HstComponentConfigurationService(containerItem,
                                    null, HstNodeTypes.NODENAME_HST_COMPONENTS , true, null, rootConfigurationPathPrefix, null);
                            availableContainerItems.add(componentConfiguration);
                            log.debug("Added catalog component to availableContainerItems with key '{}'", componentConfiguration.getId());
                        } catch (ModelLoadingException e) {
                            if (log.isDebugEnabled()) {
                                log.warn("Skipping catalog component '"+containerItem.getValueProvider().getPath()+"'", e);
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
