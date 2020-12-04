/*
 * Copyright 2013-2020 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.platform.configuration.cache;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.google.common.base.Optional;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.onehippo.cms7.services.hst.Channel;
import org.hippoecm.hst.platform.configuration.channel.ChannelPropertyMapper;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.platform.configuration.components.HstComponentConfigurationService;
import org.hippoecm.hst.configuration.components.HstComponentsConfiguration;
import org.hippoecm.hst.platform.configuration.components.HstComponentsConfigurationService;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.platform.configuration.model.ModelLoadingException;
import org.hippoecm.hst.platform.configuration.sitemapitemhandler.HstSiteMapItemHandlersConfigurationService;
import org.hippoecm.hst.configuration.sitemapitemhandlers.HstSiteMapItemHandlersConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_CHANNEL;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_COMPONENTDEFINITION;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT;

/**
 * <p>
 *   Note that this class is <strong>thread-safe</strong> and all public methods can be invoked concurrently
 * </p>
 */
public class HstConfigurationLoadingCache implements HstEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(HstConfigurationLoadingCache.class);

    private WeakTaggedCache<List<UUID>, Channel, String> liveChannelsCache = new WeakTaggedCache<>();
    private WeakTaggedCache<List<UUID>, Channel, String> previewChannelsCache = new WeakTaggedCache<>();
    private WeakTaggedCache<List<UUID>, HstComponentsConfiguration, String> componentsConfigurationCache = new WeakTaggedCache<>();
    private WeakTaggedCache<List<UUID>, HstSiteMapItemHandlersConfiguration, String> siteMapItemHandlerConfigurationCache = new WeakTaggedCache<>();

    private final HstNodeLoadingCache hstNodeLoadingCache;
    private final String rootConfigurationsPrefix;
    private final String commonCatalogPath;

    /*
     * The List of all common catalog items. These have a fixed location at rootConfigurationsPrefix + "/hst:catalog"
     */
    private Optional<List<HstComponentConfiguration>> commonCatalogItems = null;

    public HstConfigurationLoadingCache(final HstNodeLoadingCache hstNodeLoadingCache, final String rootConfigurationsPrefix) {
        this.hstNodeLoadingCache = hstNodeLoadingCache;
        this.rootConfigurationsPrefix = rootConfigurationsPrefix;
        commonCatalogPath = rootConfigurationsPrefix + HstNodeTypes.NODENAME_HST_CATALOG;
    }

    @Override
    public synchronized void handleEvents(final Set<HstEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        Set<String> eventPaths = new HashSet<>();
        for (HstEvent event : events) {
            try {
                log.debug("Processing event {}", event);
                // get event for root config
                final String eventPath = getMainConfigOrRootConfigNodePath(event);
                if (eventPath != null) {
                    if (eventPath.startsWith(commonCatalogPath)) {
                        commonCatalogItems = null;
                    }
                    eventPaths.add(eventPath);
                }
            } catch (Exception e) {
                log.warn("Exception during processing event '"+event.toString()+"'. Skip event.", e);
            }
        }
        for (String eventPath : eventPaths) {
            liveChannelsCache.evictKeysByTag(eventPath);
            previewChannelsCache.evictKeysByTag(eventPath);
            componentsConfigurationCache.evictKeysByTag(eventPath);
            siteMapItemHandlerConfigurationCache.evictKeysByTag(eventPath);
        }
    }

    /**
     *
     * @return main node path for event or <code>null</code> if event is not for hst configurations
     */
    synchronized String getMainConfigOrRootConfigNodePath(final HstEvent event) throws IllegalArgumentException {
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
        if (elems.length > 2 && elems[1].equals(HstNodeTypes.NODENAME_HST_WORKSPACE)) {
            // for workspace the main nodes are directly below the workspace
            return  rootConfigurationsPrefix + elems[0] + "/" + elems[1] + "/" + elems[2];
        }
        return rootConfigurationsPrefix + elems[0] + "/" + elems[1];
    }

    private boolean isHstConfigurationsEvent(final HstEvent event) {
        return event.getNodePath().startsWith(rootConfigurationsPrefix);
    }


    public synchronized Channel loadChannel(final String configurationPath, final boolean isPreviewSite, final String mountIdentifier,
                               final String contextPath) {
        final CompositeConfigurationNodes ccn = getCompositeConfigurationNodes(configurationPath, NODENAME_HST_CHANNEL);
        List<UUID> cachekey = ccn.getCacheKey();
        // mount needs to be part of the cache key for channel objects
        cachekey.add(UUID.fromString(mountIdentifier));
        // configuration node needs to be part of the cache key for channel objects otherwise for for example branches the same
        // channel object will be returned
        final HstNode rootConfigNode = hstNodeLoadingCache.getNode(configurationPath);
        cachekey.add(UUID.fromString(rootConfigNode.getValueProvider().getIdentifier()));

        final WeakTaggedCache<List<UUID>, Channel, String> channelsCache;
        if (isPreviewSite) {
            channelsCache = previewChannelsCache;
        } else {
            channelsCache = liveChannelsCache;
        }

        Channel channel = channelsCache.get(cachekey);
        if (channel != null ) {
            return clone(channel);
        }

        CompositeConfigurationNodes.CompositeConfigurationNode channelCompositeNode = ccn.getCompositeConfigurationNodes().get(NODENAME_HST_CHANNEL);
        if (channelCompositeNode == null) {
            log.debug("No channel node present for '{}'. Return null", configurationPath);
            return null;
        }
        HstNode channelNode = channelCompositeNode.getMainConfigNode();
        if (channelNode == null) {
            log.debug("No channel node present for '{}'. Return null", configurationPath);
            return null;
        }
        channel = ChannelPropertyMapper.readChannel(channelNode, rootConfigNode, contextPath);
        channel.setChannelPath(channelNode.getValueProvider().getPath());
        List<String> events = ccn.getCompositeConfigurationDependencyPaths();
        if (isPreviewSite && !configurationPath.endsWith("-preview")) {
            // we need to add event paths for preview to make sure the preview channel gets reloaded in case
            // a preview is created
            events.add(configurationPath + "-preview");
            events.add(configurationPath + "-preview/hst:channel");
            events.add(configurationPath + "-preview/hst:workspace/hst:channel");
        }
        channelsCache.put(cachekey, channel, events.toArray(new String[events.size()]));
        return clone(channel);
    }

    // since org.hippoecm.hst.configuration.site.HstSiteService.init() can change the Channel object with
    // setters, we return a cloned Channel object to avoid an existing Channel object to be changed
    private Channel clone(final Channel channel) {
        return new Channel(channel);
    }

    /**
     * check wether we already a an instance that would result in the very same HstComponentsConfiguration instance. If
     * so, set that value the cachekey is the set of all HstNode identifiers that make a
     * HstComponentsConfigurationService unique: thus, pages, components, catalog and templates.
     *
     * @param configurationPath  the configurationPath for which the {@link HstComponentsConfiguration} is requested
     * @param createIfNotInCache when <code>true</code> a {@link HstComponentsConfigurationService}
     *                           will be created when missing. When <code>false</code> only a {@link
     *                           HstComponentsConfigurationService} will be
     *                           returned if present in cache  @return a {@link HstComponentsConfigurationService}
     *                           instance or <code>null</code> when no found for <code>configurationPath</code>
     * @param websiteClassloader Classloader of the website for which components configuration is initialized
     */
    public synchronized HstComponentsConfiguration getComponentsConfiguration(final String configurationPath,
                                                                 final boolean createIfNotInCache, final ClassLoader websiteClassloader) throws ModelLoadingException {

        final CompositeConfigurationNodes ccn = getCompositeConfigurationNodes(configurationPath,
                HstNodeTypes.NODENAME_HST_ABSTRACTPAGES,
                HstNodeTypes.NODENAME_HST_PAGES,
                HstNodeTypes.NODENAME_HST_XPAGES,
                HstNodeTypes.NODENAME_HST_PROTOTYPEPAGES,
                HstNodeTypes.NODENAME_HST_COMPONENTS,
                HstNodeTypes.NODENAME_HST_TEMPLATES,
                HstNodeTypes.NODENAME_HST_WORKSPACE + "/" + HstNodeTypes.NODENAME_HST_CONTAINERS,
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
                commonCatalogItems = Optional.of(getCommonCatalog(commonCatalogNode, websiteClassloader));
            }
        }


        hstComponentsConfiguration = new HstComponentsConfigurationService(ccn, commonCatalogItems.orNull(), websiteClassloader);
        final List<String> events = ccn.getCompositeConfigurationDependencyPaths();

        // the commmon catalog, default at /hst:hst/hst:configurations/hst:catalog is a special node that is always included
        // as all the hstComponentsConfiguration need a reload after a change in there
        events.add(commonCatalogPath);

        componentsConfigurationCache.put(cachekey, hstComponentsConfiguration, events.toArray(new String[events.size()]));

        return hstComponentsConfiguration;
    }

    public synchronized HstSiteMapItemHandlersConfiguration getSiteMapItemHandlersConfiguration(final String configurationPath,
                                                                                          final boolean createIfNotInCache) throws ModelLoadingException {
        final CompositeConfigurationNodes ccn = getCompositeConfigurationNodes(configurationPath,
                HstNodeTypes.NODENAME_HST_SITEMAPITEMHANDLERS);

        final CompositeConfigurationNodes.CompositeConfigurationNode compositeSiteMapItemHandlersNode = ccn.getCompositeConfigurationNodes().get(HstNodeTypes.NODENAME_HST_SITEMAPITEMHANDLERS);
        // if the compositeSiteMapItemHandlersNode is null, we do not even need to check the cache or register the NOOP with
        // tags: Namely, if later on a sitemap item handler node is added, we will have a compositeSiteMapItemHandlersNode that is
        // not null. It is more efficient to directly return the NOOP
        if (compositeSiteMapItemHandlersNode == null) {
            return HstSiteMapItemHandlersConfiguration.NOOP;
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
        final List<String> events = ccn.getCompositeConfigurationDependencyPaths();
        siteMapItemHandlerConfigurationCache.put(cachekey, siteMapItemHandlerConfiguration, events.toArray(new String[events.size()]));

        return siteMapItemHandlerConfiguration;
    }


    public synchronized CompositeConfigurationNodes getCompositeConfigurationNodes(final String configurationPath,
                                                                      final String... relPaths) {
        final HstNode rootConfigNode = hstNodeLoadingCache.getNode(configurationPath);
        if (rootConfigNode == null) {
            throw new ModelLoadingException("No configuration node found at '"+configurationPath+"'. Cannot load model for it.");
        }
        if(!HstNodeTypes.NODETYPE_HST_CONFIGURATION.equals(rootConfigNode.getNodeTypeName())) {
            throw new ModelLoadingException("Configuration node for '"+configurationPath+"' must be of type '"+
                    HstNodeTypes.NODETYPE_HST_CONFIGURATION+"'");
        }
        return new CompositeConfigurationNodes(rootConfigNode,relPaths);
    }


    private List<HstComponentConfiguration> getCommonCatalog(final HstNode commonCatalog, final ClassLoader websiteClassLoader) {

        List<HstComponentConfiguration> commonCatalogItemsList = new ArrayList<>();
        for(HstNode itemPackage :commonCatalog.getNodes()){
            if(HstNodeTypes.NODETYPE_HST_CONTAINERITEM_PACKAGE.equals(itemPackage.getNodeTypeName())) {
                for(HstNode containerItem : itemPackage.getNodes()) {
                    if(isCatalogItem(containerItem)) {
                        try {
                            // create a HstComponentConfigurationService that does not traverse to descendant components: this is not needed for the catalog. Hence, the argument 'false'
                            final String componentId = createCatalogItemId(containerItem);
                            HstComponentConfigurationService componentConfiguration = new HstComponentConfigurationService(containerItem,
                                    null, HstNodeTypes.NODENAME_HST_COMPONENTS , true, null, commonCatalogPath, componentId);
                            componentConfiguration.populateAnnotationComponentParameters(websiteClassLoader);
                            componentConfiguration.populateFieldGroups(websiteClassLoader);
                            commonCatalogItemsList.add(componentConfiguration);
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
        return commonCatalogItemsList;
    }

    public static String createCatalogItemId(final HstNode containerItem) {
        return containerItem.getParent().getName() + "/" + containerItem.getName();
    }

    public static boolean isCatalogItem(final HstNode containerItem) {
        return NODETYPE_HST_CONTAINERITEMCOMPONENT.equals(containerItem.getNodeTypeName()) ||
                NODETYPE_HST_COMPONENTDEFINITION.equals(containerItem.getNodeTypeName());
    }

    public void clear() {
        liveChannelsCache = new WeakTaggedCache<>();
        previewChannelsCache = new WeakTaggedCache<>();
        componentsConfigurationCache = new WeakTaggedCache<>();
        siteMapItemHandlerConfigurationCache = new WeakTaggedCache<>();
    }

}
