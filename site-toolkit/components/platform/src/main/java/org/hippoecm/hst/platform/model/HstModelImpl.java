/*
 *  Copyright 2018-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiPredicate;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletContext;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.cache.HstCache;
import org.hippoecm.hst.configuration.channel.ChannelManager;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.model.HstConfigurationAugmenter;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.container.PlatformRequestContextProvider;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ContainerConfiguration;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.HstComponentRegistry;
import org.hippoecm.hst.core.container.ModuleNotFoundException;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.linking.HstLinkProcessor;
import org.hippoecm.hst.core.linking.HstLinkProcessorChain;
import org.hippoecm.hst.core.linking.LocationResolver;
import org.hippoecm.hst.core.linking.ResourceContainer;
import org.hippoecm.hst.core.linking.RewriteContextResolver;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.platform.api.model.EventPathsInvalidator;
import org.hippoecm.hst.platform.api.model.InternalHstModel;
import org.hippoecm.hst.platform.configuration.RuntimeVirtualHost;
import org.hippoecm.hst.platform.configuration.cache.HstConfigurationLoadingCache;
import org.hippoecm.hst.platform.configuration.cache.HstNodeLoadingCache;
import org.hippoecm.hst.platform.configuration.channel.ChannelManagerImpl;
import org.hippoecm.hst.platform.configuration.hosting.VirtualHostsService;
import org.hippoecm.hst.platform.container.components.HstComponentRegistryImpl;
import org.hippoecm.hst.platform.container.sitemapitemhandler.HstSiteMapItemHandlerFactories;
import org.hippoecm.hst.platform.container.sitemapitemhandler.HstSiteMapItemHandlerFactoryImpl;
import org.hippoecm.hst.platform.container.sitemapitemhandler.HstSiteMapItemHandlerRegistryImpl;
import org.hippoecm.hst.platform.linking.CompositeHstLinkCreatorImpl;
import org.hippoecm.hst.platform.linking.DefaultHstLinkCreator;
import org.hippoecm.hst.platform.linking.DefaultRewriteContextResolver;
import org.hippoecm.hst.platform.linking.containers.DefaultResourceContainer;
import org.hippoecm.hst.platform.linking.containers.HippoGalleryAssetSet;
import org.hippoecm.hst.platform.linking.containers.HippoGalleryImageSetContainer;
import org.hippoecm.hst.platform.linking.resolvers.HippoResourceLocationResolver;
import org.hippoecm.hst.platform.matching.BasicHstSiteMapMatcher;
import org.hippoecm.hst.platform.services.channel.ContentBasedChannelFilter;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.hst.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import static org.hippoecm.hst.platform.utils.ProxyUtils.createProxy;

public class HstModelImpl implements InternalHstModel {

    private static final Logger log = LoggerFactory.getLogger(HstModelImpl.class);
    public static final String HTTPS_SCHEME = "https";
    public static final String HTTP_SCHEME = "http";

    private Session session;
    private ServletContext websiteServletContext;
    private final ContainerConfiguration websiteContainerConfiguration;
    private ClassLoader platformClassloader = this.getClass().getClassLoader();
    private final ComponentManager websiteComponentManager;
    private final HstNodeLoadingCache hstNodeLoadingCache;
    private final HstConfigurationLoadingCache hstConfigurationLoadingCache;
    private final BasicHstSiteMapMatcher hstSiteMapMatcher;
    private final HstLinkCreator hstLinkCreator;
    private final ChannelManager channelManager;
    private final HstCache pageCache;
    private final boolean clearPageCacheAfterModelLoad;

    private volatile VirtualHosts virtualHosts;
    private BiPredicate<Session, Channel> channelFilter;

    private final String[] hstFilterPrefixExclusions;
    private final String[] hstFilterSuffixExclusions;

    private InvalidationMonitor invalidationMonitor;
    private final HstSiteMapItemHandlerRegistryImpl siteMapItemHandlerRegistry = new HstSiteMapItemHandlerRegistryImpl();

    // map of hostName as key and Pair left is the source HostGroupName and right the targetHostGroupName
    private Map<String, RuntimeHostConfiguration> runtimeHosts = new HashMap<>();

    public static class RuntimeHostConfiguration {
        private String hostName;
        private String sourceHostGroupName;
        private String targetHostGroupName;
        private String scheme;
        private boolean isPortInUrl;
        private Integer portNumber;

        public String getHostName() {
            return hostName;
        }

        public void setHostName(String hostName) {
            this.hostName = hostName;
        }

        public String getSourceHostGroupName() {
            return sourceHostGroupName;
        }

        public void setSourceHostGroupName(String sourceHostGroupName) {
            this.sourceHostGroupName = sourceHostGroupName;
        }

        public String getTargetHostGroupName() {
            return targetHostGroupName;
        }

        public void setTargetHostGroupName(String targetHostGroupName) {
            this.targetHostGroupName = targetHostGroupName;
        }

        public String getScheme() {
            return scheme;
        }

        public void setScheme(String scheme) {
            this.scheme = scheme;
        }

        public boolean isPortInUrl() {
            return isPortInUrl;
        }

        public void setPortInUrl(boolean isPortInUrl) {
            this.isPortInUrl = isPortInUrl;
        }

        public Integer getPortNumber() {
            return portNumber;
        }

        public void setPortNumber(Integer portNumber) {
            this.portNumber = portNumber;
        }
    }

    public HstModelImpl(final Session session,
                        final ServletContext servletContext,
                        final ComponentManager websiteComponentManager,
                        final HstNodeLoadingCache hstNodeLoadingCache,
                        final HstConfigurationLoadingCache hstConfigurationLoadingCache) throws RepositoryException {
        this.session = session;
        this.websiteServletContext = servletContext;

        this.websiteComponentManager = websiteComponentManager;
        websiteContainerConfiguration = websiteComponentManager.getContainerConfiguration();
        this.hstNodeLoadingCache = hstNodeLoadingCache;
        this.hstConfigurationLoadingCache = hstConfigurationLoadingCache;

        hstSiteMapMatcher = new BasicHstSiteMapMatcher();
        configureSiteMapMatcher();

        final HstModelRegistry modelRegistry = HippoServiceRegistry.getService(HstModelRegistry.class);

        final DefaultHstLinkCreator defaultHstLinkCreator = new DefaultHstLinkCreator();
        configureHstLinkCreator(defaultHstLinkCreator, websiteComponentManager);

        this.hstLinkCreator = new CompositeHstLinkCreatorImpl(modelRegistry, defaultHstLinkCreator);

        channelFilter = configureChannelFilter(new ContentBasedChannelFilter());

        invalidationMonitor = new InvalidationMonitor(session, hstNodeLoadingCache, hstConfigurationLoadingCache, this);

        final String contentRoot = websiteContainerConfiguration.getString("channel.manager.contentRoot", ChannelManagerImpl.DEFAULT_CONTENT_ROOT);
        channelManager = new ChannelManagerImpl(this, contentRoot);

        final HstManager websiteHstManager = websiteComponentManager.getComponent(HstManager.class);
        hstFilterPrefixExclusions = websiteHstManager.getHstFilterPrefixExclusions();
        hstFilterSuffixExclusions = websiteHstManager.getHstFilterSuffixExclusions();

        final HstSiteMapItemHandlerFactoryImpl hstSiteMapItemHandlerFactory =
                new HstSiteMapItemHandlerFactoryImpl(siteMapItemHandlerRegistry, websiteServletContext);

        final HstSiteMapItemHandlerFactories hstSiteMapItemHandlerFactories = HippoServiceRegistry.getService(HstSiteMapItemHandlerFactories.class);
        hstSiteMapItemHandlerFactories.register(websiteServletContext.getContextPath(), hstSiteMapItemHandlerFactory);

        pageCache = websiteComponentManager.getComponent("pageCache");
        clearPageCacheAfterModelLoad = websiteContainerConfiguration.getBoolean("pageCache.clearOnHstConfigChange", true);

    }
    public void destroy() throws RepositoryException {

        final HstSiteMapItemHandlerFactories hstSiteMapItemHandlerFactories = HippoServiceRegistry.getService(HstSiteMapItemHandlerFactories.class);
        if (hstSiteMapItemHandlerFactories != null) {
            // platform webapp could already have been destroyed (in integration tests for example)
            hstSiteMapItemHandlerFactories.unregister(websiteServletContext.getContextPath());
        }

        invalidationMonitor.destroy();
        session.logout();
    }

    public synchronized void invalidate() {

        if (virtualHosts == null) {
            return;
        }

        final HstComponentRegistryImpl oldComponentRegistry = (HstComponentRegistryImpl)virtualHosts.getComponentRegistry();
        virtualHosts = null;

        // Note there is a time window that there are currently running requests registering again components in the
        // oldComponentRegistry : That is why in the CleanupValve we invoke componentRegistry.unregisterAllComponents()
        // again in case the registry is awaiting termination
        oldComponentRegistry.unregisterAllComponents();
        oldComponentRegistry.setAwaitingTermination(true);
    }


    public void addRuntime(final String hostName, final String sourceHostGroupName,
            final String autoHostTemplateRuntimeURL, final String targetHostGroupName) {
        synchronized (this) {
            if (!runtimeHosts.containsKey(hostName)) {
                runtimeHosts.put(hostName, createRuntimeHostConfiguration(hostName, autoHostTemplateRuntimeURL,
                        sourceHostGroupName, targetHostGroupName));
                invalidate();
            }
        }
    }

    /**
     * A runtime host configuration that contains configuration options for runtime virtual host
     */
    private RuntimeHostConfiguration createRuntimeHostConfiguration(final String hostName,
            final String autoHostTemplateRuntimeURL, final String sourceHostGroupName,
            final String targetHostGroupName) {
        final RuntimeHostConfiguration configuration = new RuntimeHostConfiguration();
        configuration.setHostName(hostName);
        configuration.setSourceHostGroupName(sourceHostGroupName);
        configuration.setTargetHostGroupName(targetHostGroupName);

        final String portNumberValue = StringUtils.substringAfter(StringUtils.substringAfter(autoHostTemplateRuntimeURL, "://"), ":");
        final boolean isPortInUrl = (StringUtils.isNotEmpty(portNumberValue)) ? true : false;
        final Integer portNumber = (isPortInUrl) ? Integer.valueOf(portNumberValue) : null;
        final String scheme = (autoHostTemplateRuntimeURL.startsWith(HTTPS_SCHEME)) ? HTTPS_SCHEME : HTTP_SCHEME;

        configuration.setPortInUrl(isPortInUrl);
        configuration.setPortNumber(portNumber);
        configuration.setScheme(scheme);
        return configuration;
    }

    @Override
    public VirtualHosts getVirtualHosts() {
        VirtualHosts vhosts = virtualHosts;
        if (vhosts != null) {
            return vhosts;
        }

        synchronized (this) {
            vhosts = virtualHosts;
            if (vhosts != null) {
                return vhosts;
            }


            final PlatformRequestContextProvider platformRequestContextProvider = new PlatformRequestContextProvider();
            final HstRequestContext requestContext = RequestContextProvider.get();

            // make sure that the Thread class loader during model loading is the platform classloader
            final ClassLoader currentClassloader = Thread.currentThread().getContextClassLoader();

            try {
                // Model loading must be HstRequestContext-less hence we clear the request context and in finally set it again
                platformRequestContextProvider.clear();

                siteMapItemHandlerRegistry.expungeStaleEntries();

                // dispatch all events
                invalidationMonitor.dispatchHstEvents();


                if (platformClassloader != currentClassloader) {
                    Thread.currentThread().setContextClassLoader(platformClassloader);
                }
                final VirtualHostsService virtualHosts = new VirtualHostsService(websiteServletContext.getContextPath(),
                        websiteContainerConfiguration,
                        hstNodeLoadingCache,
                        hstConfigurationLoadingCache);

                final HstComponentRegistry hstComponentRegistry = new HstComponentRegistryImpl();

                virtualHosts.setHstFilterPrefixExclusions(hstFilterPrefixExclusions);
                virtualHosts.setHstFilterSuffixExclusions(hstFilterSuffixExclusions);
                virtualHosts.setComponentRegistry(hstComponentRegistry);

                augment(virtualHosts);

                final boolean added = addRuntimeHosts(virtualHosts);
                if (added) {
                    // reload the channels map
                    virtualHosts.loadChannelsMap();
                }

                this.virtualHosts = virtualHosts;

                if (clearPageCacheAfterModelLoad) {
                    log.info("Clearing page cache after new model is loaded");
                    pageCache.clear();
                } else {
                    log.debug("Page cache won't be cleared because 'clearPageCacheAfterModelLoad = false'");
                }

                return this.virtualHosts;
            } catch (RuntimeException e) {
                log.error("Exception loading model", e);
                throw e;
            } catch (Exception e) {
                log.error("Exception loading model", e);
                throw new RuntimeException(e);
            } finally {


                if (platformClassloader != currentClassloader) {
                    Thread.currentThread().setContextClassLoader(currentClassloader);
                }
                platformRequestContextProvider.set(requestContext);
            }
        }
    }

    @Override
    public boolean isHstConfigurationNodesLoaded() {
        return hstNodeLoadingCache.isHstNodesLoaded();
    }

    private void augment(final VirtualHostsService virtualHosts) throws ContainerException {

        try {
            List<HstConfigurationAugmenter> configurationAugmenters =
                    websiteComponentManager.getComponent("org.hippoecm.hst.pagecomposer.jaxrs.customMountAndVirtualCmsHostAugmenters",
                            "org.hippoecm.hst.pagecomposer");

            for (HstConfigurationAugmenter configurationAugmenter : configurationAugmenters) {
                configurationAugmenter.augment(virtualHosts);
            }
        } catch (ModuleNotFoundException e) {
            log.debug("Currently loaded model does not have the page composer (host augmenters)");
        }
    }

    // returns true if at least one runtime host was added
    private boolean addRuntimeHosts(final VirtualHostsService virtualHosts) {

        boolean added = false;
        for (Iterator<Map.Entry<String, RuntimeHostConfiguration>> iterator = runtimeHosts.entrySet().iterator(); iterator.hasNext();) {
            final Entry<String, RuntimeHostConfiguration> entry = iterator.next();
            final String hostName = entry.getKey();
            final RuntimeHostConfiguration runtimeHostConfiguration = entry.getValue();

            final ResolvedMount existing = virtualHosts.matchMount(hostName, "");
            if (existing != null) {
                // remove from runtimeHosts
                iterator.remove();
                continue;
            }

            final Map<String, VirtualHost> sourceHostGroupHosts = virtualHosts.getRootVirtualHostsByGroup().get(runtimeHostConfiguration.getSourceHostGroupName());
            if (sourceHostGroupHosts == null || sourceHostGroupHosts.size() != 1) {
                log.warn("Cannot add runtime hosts '{}' for '{}' since the source host group '{}' does not define a single " +
                        "host which is required", hostName, websiteServletContext, runtimeHostConfiguration.getSourceHostGroupName());
                continue;
            }

            if (sourceHostGroupHosts.values().size() != 1) {
                log.warn("Cannot add runtime hosts '{}' for '{}' since the source host group '{}' does not define a single " +
                        "host but non or more than 1", hostName, websiteServletContext, runtimeHostConfiguration.getSourceHostGroupName());
                continue;
            }
            final VirtualHost virtualHost = sourceHostGroupHosts.values().iterator().next();
            if (!virtualHost.getChildHosts().isEmpty()) {
                log.warn("Cannot add runtime hosts '{}' for '{}' since the source host group '{}' does not define a single " +
                        "host without child hosts which is required", hostName, websiteServletContext, runtimeHostConfiguration.getSourceHostGroupName());
                continue;
            }

            if (virtualHost.getPortMount(0) == null || virtualHost.getPortMount(0).getRootMount() == null) {
                log.warn("Cannot add runtime hosts '{}' for '{}' since the source host group '{}' does not have a host " +
                        "with an hst:root mount below it", hostName, websiteServletContext, runtimeHostConfiguration.getSourceHostGroupName());
                continue;
            }

            // create runtime host
            RuntimeVirtualHost runtimeHost = new RuntimeVirtualHost(virtualHost, runtimeHostConfiguration);

            virtualHosts.addVirtualHost(runtimeHost);

            final ResolvedMount resolvedMount = virtualHosts.matchMount(hostName, "");
            if (resolvedMount == null) {
                log.warn("Expected runtime added virtualhost '{}' to have a root mount", hostName);
                continue;
            }

            final Mount rootMount = resolvedMount.getMount();
            addMounts(virtualHosts, rootMount);

            added = true;
        }
        return added;
    }

    private void addMounts(final VirtualHostsService virtualHosts, final Mount mount) {
        virtualHosts.addMount(mount);
        mount.getChildMounts().forEach(child -> addMounts(virtualHosts, child));
    }


    @Override
    public HstSiteMapMatcher getHstSiteMapMatcher() {
        return hstSiteMapMatcher;
    }

    @Override
    public HstLinkCreator getHstLinkCreator() {
        return hstLinkCreator;
    }

    // internal api!
    @Override
    public BiPredicate<Session, Channel> getChannelFilter() {
        return channelFilter;
    }

    // internal api!
    @Override
    public ChannelManager getChannelManager() {
        return channelManager;
    }

    // internal api!
    @Override
    public String getConfigurationRootPath() {
        return hstNodeLoadingCache.getRootPath();
    }

    // internal api!
    @Override
    public EventPathsInvalidator getEventPathsInvalidator() {
        return invalidationMonitor.getEventPathsInvalidator();
    }

    private void configureSiteMapMatcher() {
        hstSiteMapMatcher.setLinkProcessor(getHstLinkProcessor(websiteComponentManager));
    }

    private HstLinkProcessor getHstLinkProcessor(final ComponentManager websiteComponentManager) {
        HstLinkProcessor customLinkProcessor = websiteComponentManager.getComponent(HstLinkProcessor.class.getName());
        if (customLinkProcessor != null) {
            // wrap proxy for classloader
            return createProxy(websiteServletContext.getClassLoader(), HstLinkProcessor.class, customLinkProcessor);
        }
        return new HstLinkProcessorChain();
    }

    private RewriteContextResolver getRewriteContextResolver(final ComponentManager websiteComponentManager) {
        final RewriteContextResolver customRewriteContextResolver = websiteComponentManager.getComponent(RewriteContextResolver.class.getName());
        if (customRewriteContextResolver != null) {
            // wrap proxy for classloader
            return createProxy(websiteServletContext.getClassLoader(), RewriteContextResolver.class, customRewriteContextResolver);
        }
        return new DefaultRewriteContextResolver();
    }

    private void configureHstLinkCreator(final DefaultHstLinkCreator hstLinkCreator, final ComponentManager websiteComponentManager) {
        final List<String> binaryLocations = websiteComponentManager.getComponent(HstLinkCreator.class.getName() + ".binaryLocations");
        hstLinkCreator.setBinaryLocations(binaryLocations.toArray(new String[binaryLocations.size()]));

        hstLinkCreator.setPageNotFoundPath(websiteComponentManager.getContainerConfiguration()
                .getString("linkrewriting.failed.path", DefaultHstLinkCreator.DEFAULT_PAGE_NOT_FOUND_PATH));

        hstLinkCreator.setRewriteContextResolver(getRewriteContextResolver(websiteComponentManager));

        hstLinkCreator.setLinkProcessor(getHstLinkProcessor(websiteComponentManager));

        final List<ResourceContainer> immutableResourceContainers = getImmutableResoureceContainers(websiteComponentManager);

        final List<LocationResolver> immutableLocationResolvers =
                getImmutableResoureceResolvers(websiteComponentManager, immutableResourceContainers, binaryLocations.toArray(new String[binaryLocations.size()]));

        hstLinkCreator.setLocationResolvers(immutableLocationResolvers);
    }

    private List<LocationResolver> getImmutableResoureceResolvers(final ComponentManager websiteComponentManager,
                                                                  final List<ResourceContainer> resourceContainers,
                                                                  final String[] binaryLocations) {

        List<LocationResolver> locationsResolvers = new ArrayList<>();
        // the spring config id is customResourceResolvers instead of customLocationResolvers
        List<LocationResolver> customLocationResolvers = websiteComponentManager.getComponent("customResourceResolvers");

        // wrap proxy for customLocationResolvers for classloader
        final ClassLoader cl = websiteServletContext.getClassLoader();
        customLocationResolvers.stream()
                .map(locationResolver -> createProxy(cl, LocationResolver.class, locationResolver))
                .forEach(proxy -> locationsResolvers.add(proxy));

        final HippoResourceLocationResolver hippoResourceLocationResolver = new HippoResourceLocationResolver();
        hippoResourceLocationResolver.setBinaryLocations(binaryLocations);
        hippoResourceLocationResolver.setResourceContainers(resourceContainers);

        locationsResolvers.add(hippoResourceLocationResolver);

        return locationsResolvers;
    }

    private List<ResourceContainer> getImmutableResoureceContainers(final ComponentManager websiteComponentManager) {

        List<ResourceContainer> resourceContainers = new ArrayList<>();
        // first add the custom resourceContainers, after that the fallback built in resource containers
        List<ResourceContainer> customResourceContainers = websiteComponentManager.getComponent("customResourceContainers");

        // wrap proxy for customResourceContainers for classloader
        final ClassLoader cl = websiteServletContext.getClassLoader();
        customResourceContainers.stream()
                .map(resourceContainer ->
                        createProxy(cl, ResourceContainer.class, resourceContainer))
                .forEach(proxy -> resourceContainers.add(proxy));

        final HippoGalleryImageSetContainer hippoGalleryImageSetContainer = new HippoGalleryImageSetContainer();
        hippoGalleryImageSetContainer.setPrimaryItem("hippogallery:original");
        hippoGalleryImageSetContainer.setMappings(ImmutableMap.of("hippogallery:thumbnail", "thumbnail"));

        resourceContainers.add(hippoGalleryImageSetContainer);

        final HippoGalleryAssetSet hippoGalleryAssetSet = new HippoGalleryAssetSet();
        hippoGalleryAssetSet.setPrimaryItem("hippogallery:asset");
        hippoGalleryAssetSet.setMappings(ImmutableMap.of());

        resourceContainers.add(hippoGalleryAssetSet);
        resourceContainers.add(new DefaultResourceContainer());
        return ImmutableList.copyOf(resourceContainers);
    }

    private BiPredicate<Session, Channel> configureChannelFilter(final BiPredicate<Session, Channel> builtinFilter) {

        BiPredicate<Session, Channel> compositeFilter = builtinFilter;

        List<BiPredicate<Session, Channel>> customChannelFilters = websiteComponentManager.getComponent("customChannelFilters");

        for (BiPredicate<Session, Channel> customChannelFilter : customChannelFilters) {
            // wrap proxy for classloader
            final BiPredicate proxy = createProxy(websiteServletContext.getClassLoader(), BiPredicate.class, customChannelFilter);
            compositeFilter = compositeFilter.and(proxy);
        }

        return compositeFilter;
    }

    @Override
    public String toString() {
        return "HstModelImpl{" +
                "contextPath=" + websiteServletContext.getContextPath() +
                '}';
    }


}
