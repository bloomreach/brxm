/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst.configuration.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.common.base.Optional;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.components.HstComponentConfigurationService;
import org.hippoecm.hst.configuration.components.HstComponentsConfiguration;
import org.hippoecm.hst.configuration.components.HstComponentsConfigurationService;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.configuration.model.ModelLoadingException;
import org.hippoecm.hst.configuration.sitemapitemhandler.HstSiteMapItemHandlersConfigurationService;
import org.hippoecm.hst.configuration.sitemapitemhandlers.HstSiteMapItemHandlerConfiguration;
import org.hippoecm.hst.configuration.sitemapitemhandlers.HstSiteMapItemHandlersConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 *   Note that this class is <strong>not</strong> thread-safe : It should not be accessed by concurrent threads
 * </p>
 */
public class HstConfigurationLoadingCache implements HstEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(HstConfigurationLoadingCache.class);

    private EventCache<List<UUID>, HstComponentsConfiguration, String> componentsConfigurationCache = new EventCache<>();
    private EventCache<List<UUID>, HstSiteMapItemHandlersConfiguration, String> siteMapItemHandlerConfigurationCache = new EventCache<>();

    private HstNodeLoadingCache hstNodeLoadingCache;
    private String rootConfigurationsPrefix;
    private String commonCatalogPath;

    /*
     * The List of all common catalog items. These have a fixed location at rootConfigurationsPrefix + "/hst:catalog"
     */
    private Optional<List<HstComponentConfiguration>> commonCatalogItems = null;


    public void setHstNodeLoadingCache(final HstNodeLoadingCache hstNodeLoadingCache) {
        this.hstNodeLoadingCache = hstNodeLoadingCache;
    }

    public void setRootConfigurationsPrefix(final String rootConfigurationsPrefix) {
        this.rootConfigurationsPrefix = rootConfigurationsPrefix;
        commonCatalogPath = rootConfigurationsPrefix + HstNodeTypes.NODENAME_HST_CATALOG;
    }

    @Override
    public void handleEvents(final Set<HstEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        Set<String> eventPaths = new HashSet<>();
        for (HstEvent event : events) {
            log.debug("Processing event {}", event);
            // get event for root config
            final String eventPath = getMainConfigOrRootConfigNodePath(event);
            if (eventPath != null) {
                if (eventPath.startsWith(commonCatalogPath)) {
                    // change in common catalog. No need to ever reload any item in
                    // componentsConfigurationCache or siteMapItemHandlerConfigurationCache for that
                    // just set commonCatalogItems to null : will be reloaded then
                    commonCatalogItems = null;
                } else {
                    eventPaths.add(eventPath);
                }
            }
        }
        for (String eventPath : eventPaths) {
            componentsConfigurationCache.handleEvent(eventPath);
            siteMapItemHandlerConfigurationCache.handleEvent(eventPath);
        }
    }

    /**
     *
     * @return main node path for event or <code>null</code> if event is not for hst configurations
     */
    String getMainConfigOrRootConfigNodePath(final HstEvent event) throws IllegalArgumentException {
        if (!isHstConfigurationsEvent(event)) {
            return null;
        }
        String eventPath = event.getNodePath();
        String pathFromMainConfig = eventPath.substring(rootConfigurationsPrefix.length());
        if (pathFromMainConfig.isEmpty()) {
            return null;
        }
        String[] elems = pathFromMainConfig.split("/");
        if (elems.length == 1) {
            return rootConfigurationsPrefix + elems[0];
        }
        return rootConfigurationsPrefix + elems[0] + "/" + elems[1];
    }

    private boolean isHstConfigurationsEvent(final HstEvent event) {
        return event.getNodePath().startsWith(rootConfigurationsPrefix);
    }


    /**
     * check wether we already a an instance that would result in the very same HstComponentsConfiguration instance. If so, set that value
     * the cachekey is the set of all HstNode identifiers that make a HstComponentsConfigurationService unique: thus, pages, components, catalog and templates.
     *
     * @param configurationPath
     *@param createIfNotInCache when <code>true</code> a {@link org.hippoecm.hst.configuration.components.HstComponentsConfigurationService} will be created when missing. When <code>false</code>
     *                        only a {@link org.hippoecm.hst.configuration.components.HstComponentsConfigurationService} will be returned if present in cache  @return a {@link HstComponentsConfigurationService} instance or <code>null</code> when no found for <code>configurationPath</code>
     */
    public HstComponentsConfiguration getComponentsConfiguration(final String configurationPath,
                                                                 final boolean createIfNotInCache) throws ModelLoadingException {

        final CompositeConfigurationNodes ccn = getCompositeConfigurationNodes(configurationPath,
                HstNodeTypes.NODENAME_HST_PAGES,
                HstNodeTypes.NODENAME_HST_COMPONENTS,
                HstNodeTypes.NODENAME_HST_TEMPLATES,
                HstNodeTypes.NODENAME_HST_WORKSPACE,
                HstNodeTypes.NODENAME_HST_CATALOG);

        List<UUID> cachekey = ccn.getCacheKey();

        HstComponentsConfiguration hstComponentsConfiguration = componentsConfigurationCache.get(cachekey);
        if (hstComponentsConfiguration != null ) {
            log.debug("Return cached HstComponentsConfiguration because exact same configuration. We do not build HstComponentsConfiguration for '{}' but use existing version.", ccn.getConfigurationRootNode().getValueProvider().getPath());
            return hstComponentsConfiguration;
        }
        if (!createIfNotInCache) {
            // no cached instance found
            return null;
        }

        if (commonCatalogItems == null) {
            HstNode commonCatalogNode = hstNodeLoadingCache.getNode(commonCatalogPath);
            if (commonCatalogNode == null) {
                commonCatalogItems = Optional.absent();
            } else {
                commonCatalogItems = Optional.of(getCommonCatalog(commonCatalogNode));
            }
        }


        hstComponentsConfiguration = new HstComponentsConfigurationService(ccn, commonCatalogItems.orNull());
        final List<String> events = ccn.getCompositeConfigurationDependenyPaths();
        componentsConfigurationCache.put(cachekey, hstComponentsConfiguration, events.toArray(new String[events.size()]));

        return hstComponentsConfiguration;
    }

    public HstSiteMapItemHandlersConfiguration getSiteMapItemHandlersConfiguration(final String configurationPath,
                                                                                          final boolean createIfNotInCache) throws ModelLoadingException {
        final CompositeConfigurationNodes ccn = getCompositeConfigurationNodes(configurationPath,
                HstNodeTypes.NODENAME_HST_SITEMAPITEMHANDLERS);

        // TODO cache ccn also !!!
        final CompositeConfigurationNodes.CompositeConfigurationNode compositeSiteMapItemHandlersNode = ccn.getCompositeConfigurationNodes().get(HstNodeTypes.NODENAME_HST_SITEMAPITEMHANDLERS);
        if (compositeSiteMapItemHandlersNode != null) {
            return new HstSiteMapItemHandlersConfiguration() {
                @Override
                public Map<String, HstSiteMapItemHandlerConfiguration> getSiteMapItemHandlerConfigurations() {
                    return Collections.EMPTY_MAP;
                }
                @Override
                public HstSiteMapItemHandlerConfiguration getSiteMapItemHandlerConfiguration(final String id) {
                    return null;
                }
            };
        }
        List<UUID> cachekey = ccn.getCacheKey();
        HstSiteMapItemHandlersConfiguration siteMapItemHandlerConfiguration = siteMapItemHandlerConfigurationCache.get(cachekey);
        if (siteMapItemHandlerConfiguration != null ) {
            log.debug("Return cached HstSiteMapItemHandlersConfigurationService because exact same configuration. We do not " +
                    "(re)build HstSiteMapItemHandlersConfigurationService for '{}' but use existing version.", ccn.getConfigurationRootNode().getValueProvider().getPath());
            return siteMapItemHandlerConfiguration;
        }
        if (!createIfNotInCache) {
            // no cached instance found
            return null;
        }
        siteMapItemHandlerConfiguration = new HstSiteMapItemHandlersConfigurationService(compositeSiteMapItemHandlersNode);
        final List<String> events = ccn.getCompositeConfigurationDependenyPaths();
        siteMapItemHandlerConfigurationCache.put(cachekey, siteMapItemHandlerConfiguration, events.toArray(new String[events.size()]));

        return siteMapItemHandlerConfiguration;
    }


    public CompositeConfigurationNodes getCompositeConfigurationNodes(final String configurationPath,
                                                                      final String... nodeNames) {
        final HstNode rootConfigNode = hstNodeLoadingCache.getNode(configurationPath);
        if (rootConfigNode == null) {
            throw new ModelLoadingException("No configuration node found at '"+configurationPath+"'. Cannot load model for it.");
        }
        if(!HstNodeTypes.NODETYPE_HST_CONFIGURATION.equals(rootConfigNode.getNodeTypeName())) {
            throw new ModelLoadingException("Configuration node for '"+configurationPath+"' must be of type '"+
                    HstNodeTypes.NODETYPE_HST_CONFIGURATION+"'");
        }
        return new CompositeConfigurationNodes(rootConfigNode,nodeNames);
    }


    private List<HstComponentConfiguration> getCommonCatalog(final HstNode commonCatalog) {

        List<HstComponentConfiguration> commonCatalogItemsList = new ArrayList<>();
        for(HstNode itemPackage :commonCatalog.getNodes()){
            if(HstNodeTypes.NODETYPE_HST_CONTAINERITEM_PACKAGE.equals(itemPackage.getNodeTypeName())) {
                for(HstNode containerItem : itemPackage.getNodes()) {
                    if(HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT.equals(containerItem.getNodeTypeName()))
                    {
                        try {
                            // create a HstComponentConfigurationService that does not traverse to descendant components: this is not needed for the catalog. Hence, the argument 'false'
                            HstComponentConfiguration componentConfiguration = new HstComponentConfigurationService(containerItem,
                                    null, HstNodeTypes.NODENAME_HST_COMPONENTS , false, null, true, null);
                            commonCatalogItemsList.add(componentConfiguration);
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
        return commonCatalogItemsList;
    }

}
