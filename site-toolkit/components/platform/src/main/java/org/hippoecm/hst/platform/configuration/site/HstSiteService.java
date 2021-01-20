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
package org.hippoecm.hst.platform.configuration.site;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.platform.configuration.cache.CompositeConfigurationNodes;
import org.hippoecm.hst.platform.configuration.cache.CompositeConfigurationNodes.CompositeConfigurationNode;
import org.hippoecm.hst.platform.configuration.cache.HstConfigurationLoadingCache;
import org.hippoecm.hst.platform.configuration.cache.HstNodeLoadingCache;
import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.platform.configuration.channel.ChannelLazyLoadingChangedBySet;
import org.hippoecm.hst.platform.configuration.channel.ChannelUtils;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.components.HstComponentsConfiguration;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.platform.configuration.model.ModelLoadingException;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.platform.configuration.sitemap.HstNoopSiteMap;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.platform.configuration.sitemap.HstSiteMapService;
import org.hippoecm.hst.configuration.sitemapitemhandlers.HstSiteMapItemHandlersConfiguration;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenusConfiguration;
import org.hippoecm.hst.platform.configuration.sitemenu.HstSiteMenusConfigurationService;
import org.hippoecm.hst.core.linking.LocationMapTree;
import org.hippoecm.hst.platform.linking.LocationMapTreeComponentDocuments;
import org.hippoecm.hst.platform.linking.LocationMapTreeSiteMap;
import org.hippoecm.hst.site.HstServices;
import org.onehippo.cms7.services.context.HippoWebappContextRegistry;
import org.onehippo.cms7.services.hst.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

import static org.hippoecm.hst.configuration.HstNodeTypes.BRANCH_PROPERTY_BRANCH_ID;
import static org.hippoecm.hst.configuration.HstNodeTypes.BRANCH_PROPERTY_BRANCH_OF;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_CONFIGURATIONS;
import static org.hippoecm.hst.configuration.HstNodeTypes.SITE_CONFIGURATIONPATH;
import static org.hippoecm.hst.platform.configuration.channel.ChannelUtils.getChannelInfoClass;
import static org.hippoecm.hst.platform.configuration.channel.ChannelUtils.getChannelInfoMixins;

public class HstSiteService implements HstSite {

    private static final Logger log = LoggerFactory.getLogger(HstSiteService.class);
    volatile Optional<HstSiteMap> siteMap;
    volatile LocationMapTree locationMapTree;
    volatile LocationMapTree locationMapTreeComponentDocuments;
    volatile Optional<HstSiteMapItemHandlersConfiguration> siteMapItemHandlersConfigurationService;
    volatile Optional<Channel> channel;
    volatile Optional<HstComponentsConfiguration> componentsConfiguration;
    volatile Optional<HstSiteMenusConfiguration> siteMenusConfigurations;
    private String name;
    private boolean hasPreviewConfiguration;
    private String canonicalIdentifier;
    private String configurationPath;
    private MountSiteMapConfiguration mountSiteMapConfiguration;
    private final HstConfigurationLoadingCache hstConfigurationLoadingCache;
    private final ClassLoader websiteClassloader;

    HstSiteService(final HstNode site,
                   final Mount mount,
                   final MountSiteMapConfiguration mountSiteMapConfiguration,
                   final HstNodeLoadingCache hstNodeLoadingCache,
                   final HstConfigurationLoadingCache hstConfigurationLoadingCache,
                   final boolean isPreviewSite) throws ModelLoadingException {

        this.hstConfigurationLoadingCache = hstConfigurationLoadingCache;
        name = site.getValueProvider().getName();
        canonicalIdentifier = site.getValueProvider().getIdentifier();
        this.mountSiteMapConfiguration = mountSiteMapConfiguration;
        this.websiteClassloader = HippoWebappContextRegistry.get().getContext(mountSiteMapConfiguration.getContextPath())
                .getServletContext().getClassLoader();
        findAndSetConfigurationPath(site, hstNodeLoadingCache, isPreviewSite);
        init(site, mount, isPreviewSite, hstNodeLoadingCache, null);
    }

    /**
     *
     * @param site
     * @param mount
     * @param mountSiteMapConfiguration
     * @param hstNodeLoadingCache
     * @param configurationPath
     * @param isPreviewSite
     */
    public HstSiteService(final HstNode site, final Mount mount, final MountSiteMapConfiguration mountSiteMapConfiguration,
                          final HstNodeLoadingCache hstNodeLoadingCache,
                          final HstConfigurationLoadingCache hstConfigurationLoadingCache,
                          final String configurationPath,
                          final boolean isPreviewSite,
                          final Channel master) {

        this.hstConfigurationLoadingCache = hstConfigurationLoadingCache;
        name = site.getValueProvider().getName();
        canonicalIdentifier = site.getValueProvider().getIdentifier();
        this.mountSiteMapConfiguration = mountSiteMapConfiguration;
        this.websiteClassloader = HippoWebappContextRegistry.get().getContext(mountSiteMapConfiguration.getContextPath())
                .getServletContext().getClassLoader();
        this.configurationPath = configurationPath;
        if (configurationPath.endsWith("-preview")) {
            hasPreviewConfiguration = true;
        } else {
            hasPreviewConfiguration = hstNodeLoadingCache.getNode(configurationPath + "-preview") != null;
        }

        init(site, mount, isPreviewSite, hstNodeLoadingCache, master);
    }

    private void findAndSetConfigurationPath(final HstNode site,
                                             final HstNodeLoadingCache hstNodeLoadingCache,
                                             final boolean isPreviewSite
                                             ) {
        if (site.getValueProvider().hasProperty(SITE_CONFIGURATIONPATH)) {
            configurationPath = site.getValueProvider().getString(SITE_CONFIGURATIONPATH);
        } else {
            configurationPath = hstNodeLoadingCache.getRootPath() + "/" +
                    NODENAME_HST_CONFIGURATIONS + "/" +site.getValueProvider().getName();
        }
        String previewConfigurationPath = configurationPath + "-preview";
        HstNode previewConfig = hstNodeLoadingCache.getNode(previewConfigurationPath);
        if (previewConfig != null) {
            hasPreviewConfiguration = true;
            if (isPreviewSite) {
                configurationPath = previewConfigurationPath;
            }
        }
    }

    private void init(final HstNode site, final Mount mount, final boolean isPreviewSite,
                      final HstNodeLoadingCache hstNodeLoadingCache, final Channel master) {

        log.debug("Loading channel configuration for '{}'", configurationPath);

        loadChannel(site, mount, isPreviewSite, hstNodeLoadingCache, master);

        HstComponentsConfiguration ccs = hstConfigurationLoadingCache.getComponentsConfiguration(configurationPath,
                false, websiteClassloader);
        if (ccs != null) {
            log.debug("Reusing cached HstComponentsConfiguration for '{}'", configurationPath);
            componentsConfiguration = Optional.of(ccs);
        } else {
            log.debug("No cached HstComponentsConfiguration for '{}' present. On first access it will " +
                    "be loaded lazily", configurationPath);
        }

        HstSiteMapItemHandlersConfiguration hsihcs = hstConfigurationLoadingCache.getSiteMapItemHandlersConfiguration(configurationPath, false);
        if (hsihcs != null) {
            log.debug("Reusing cached HstSiteMapItemHandlersConfigurationService for '{}'", configurationPath);
            siteMapItemHandlersConfigurationService = Optional.of(hsihcs);
        } else {
            log.debug("No cached HstSiteMapItemHandlersConfigurationService for '{}' present. On first access it will " +
                    "be loaded lazily", configurationPath);
        }

        // because sitemap and sitemenus have a reference to this HstSiteService, we can never reuse
        // them from cache. Instead, they will be created lazily the first time on access

    }

    private void loadChannel(final HstNode site, final Mount mount, final boolean isPreviewSite, final HstNodeLoadingCache hstNodeLoadingCache, final Channel master) {
        final Channel ch;
        if (mount.hasNoChannelInfo()) {
            ch = null;
        } else {
            ch = hstConfigurationLoadingCache.loadChannel(configurationPath, isPreviewSite,  mount.getIdentifier(), mount.getContextPath());
        }
        if (ch != null) {
            final HstNode rootConfigNode = hstNodeLoadingCache.getNode(configurationPath);
            if (master != null) {
                if (!rootConfigNode.getValueProvider().hasProperty(BRANCH_PROPERTY_BRANCH_OF) || !rootConfigNode.getValueProvider().hasProperty(BRANCH_PROPERTY_BRANCH_ID)) {
                    throw new ModelLoadingException(String.format("Cannot load branch '%s' since misses mandatory property '%s' or '%s'",
                            hstConfigurationLoadingCache, BRANCH_PROPERTY_BRANCH_OF, BRANCH_PROPERTY_BRANCH_ID));
                }
                ch.setBranchOf(rootConfigNode.getValueProvider().getString(BRANCH_PROPERTY_BRANCH_OF));
                ch.setBranchId(rootConfigNode.getValueProvider().getString(BRANCH_PROPERTY_BRANCH_ID));

            }

            if (rootConfigNode == null) {
                throw new ModelLoadingException("No configuration node found at '"+configurationPath+"'. Cannot load model for it.");
            }

            ch.setHstMountPoint(mount.getMountPoint());
            ch.setContentRoot(mountSiteMapConfiguration.getMountContentPath());
            ch.setHstConfigPath(configurationPath);

            ch.setPreviewHstConfigExists(hasPreviewConfiguration);

            if (rootConfigNode.getNode(HstNodeTypes.NODENAME_HST_WORKSPACE) != null) {
                ch.setWorkspaceExists(true);
            }

            String mountPath = mount.getMountPath();
            ch.setLocale(mountSiteMapConfiguration.getLocale());
            ch.setMountId(mount.getIdentifier());
            ch.setMountPath(mountPath);
            ch.setContextPath(mountSiteMapConfiguration.getContextPath());

            ch.setHasCustomProperties(hasChannelCustomProperties(ch));


            // do not fetch the sitemap id via #getSiteMap() because that part should be invoked lazily
            CompositeConfigurationNode siteMapNode = findSiteMapNode();
            if (siteMapNode == null) {
                log.warn("Missing sitemap below '{}' and also no sitemap is inherited.", configurationPath);
            } else {
                ch.setSiteMapId(siteMapNode.getMainConfigNode().getValueProvider().getIdentifier());
            }

            VirtualHost virtualHost = mount.getVirtualHost();

            // TODO HSTTWO-4355 always get the cms preview prefix via HstManager instead of via VirtualHosts model!!
            ch.setCmsPreviewPrefix(virtualHost.getVirtualHosts().getCmsPreviewPrefix());
            ch.setHostname(virtualHost.getHostName());
            ch.setHostGroup(virtualHost.getHostGroupName());

            StringBuilder url = new StringBuilder();
            url.append(mount.getScheme());
            url.append("://");
            url.append(virtualHost.getHostName());
            if (mount.isPortInUrl()) {
                int port = mount.getPort();
                if (port != 0 && port != 80 && port != 443) {
                    url.append(':');
                    url.append(mount.getPort());
                }
            }
            if (virtualHost.isContextPathInUrl()) {
                url.append(mount.getContextPath());
            }
            if (StringUtils.isNotEmpty(mountPath)) {
                if (!mountPath.startsWith("/")) {
                    url.append('/');
                }
                url.append(mountPath);
            }
            ch.setUrl(url.toString());

            if (isPreviewSite) {
                ch.setPreview(true);
                ch.setChangedBySet(new ChannelLazyLoadingChangedBySet(this, ch, hstNodeLoadingCache));
            }
        }

        channel = Optional.fromNullable(ch);
    }

    private CompositeConfigurationNode findSiteMapNode() {
        final CompositeConfigurationNodes ccn = hstConfigurationLoadingCache.getCompositeConfigurationNodes(configurationPath, HstNodeTypes.NODENAME_HST_SITEMAP);
        return ccn.getCompositeConfigurationNodes().get(HstNodeTypes.NODENAME_HST_SITEMAP);
    }


    private boolean hasChannelCustomProperties(final Channel channel) {
        Class<? extends ChannelInfo> channelInfoClass = getChannelInfoClass(channel);
        if (channelInfoClass == null) {
            return false;
        }
        return channelInfoClass != ChannelInfo.class;
    }


    @SuppressWarnings("unchecked")
    public <T extends ChannelInfo> T getChannelInfo() {
        Channel channel = getChannel();
        if (channel == null) {
            return null;
        }
        Class<? extends ChannelInfo> channelInfoClass = getChannelInfoClass(channel);
        List<Class<? extends ChannelInfo>> channelInfoMixins = getChannelInfoMixins(channel);
        return (T) ChannelUtils.getChannelInfo(channel.getContextPath(), channel.getProperties(), channelInfoClass,
                channelInfoMixins.toArray(new Class[channelInfoMixins.size()]));
    }

    public Channel getChannel() {
        return channel.orNull();
    }

    public HstComponentsConfiguration getComponentsConfiguration() {
        if (componentsConfiguration != null) {
            return componentsConfiguration.get();
        }
        log.debug("Loading HstComponentsConfiguration for '{}'", configurationPath);

        synchronized (this) {
            if (componentsConfiguration != null) {
                return componentsConfiguration.get();
            }
            try {
                long start = System.currentTimeMillis();
                HstComponentsConfiguration ccs = hstConfigurationLoadingCache.getComponentsConfiguration(configurationPath,
                        true, websiteClassloader);
                componentsConfiguration = Optional.of(ccs);
                if (siteMap != null) {
                    checkAndLogAccessibleRootComponents(ccs, siteMap.get());
                }
                log.info("Loading HstComponentsConfiguration for '{}' took '{}' ms.", configurationPath,
                        String.valueOf(System.currentTimeMillis() - start));
                return ccs;
            } catch (Exception e) {
                // avoid same model being incorrectly loaded over and over
                componentsConfiguration = Optional.of(HstComponentsConfiguration.NOOP);
                log.warn("Could not load HstComponentsConfiguration for '{}'. Return a NOOP HstComponentsConfiguration instance", configurationPath, e);
                return HstComponentsConfiguration.NOOP;
            }
        }
    }

    // TODO why doesn't the getSiteMap use configLoadingCache?? See HSTTWO-3966
    public HstSiteMap getSiteMap() {
        if (siteMap != null) {
            return siteMap.get();
        }
        synchronized (this) {
            if (siteMap != null) {
                 return siteMap.get();
            }
            try {
                long start = System.currentTimeMillis();
                final CompositeConfigurationNode siteMapNode = findSiteMapNode();
                if (siteMapNode == null) {
                    HstSiteMap sm = new HstNoopSiteMap(this);
                    siteMap = Optional.of(sm);
                    log.warn("Could not load HstSiteMap for '{}'. Return a NOOP sitemap for this site config", configurationPath);
                    return sm;
                }
                HstSiteMap sm = new HstSiteMapService(this, siteMapNode, mountSiteMapConfiguration, getSiteMapItemHandlersConfiguration());
                siteMap = Optional.of(sm);
                if (componentsConfiguration != null) {
                    checkAndLogAccessibleRootComponents(componentsConfiguration.get(), sm);
                }
                log.info("Loading HstSiteMap for '{}' took '{}' ms.", configurationPath,
                        String.valueOf(System.currentTimeMillis() - start));
                return sm;
            } catch (ModelLoadingException e) {
                // avoid same model being incorrectly loaded over and over
                HstSiteMap sm = new HstNoopSiteMap(this);
                siteMap = Optional.of(sm);
                log.warn("Could not load HstSiteMap for '{}'. Return a NOOP sitemap for this site config", configurationPath, e);
                return sm;
            }
        }
    }

    public HstSiteMapItemHandlersConfiguration getSiteMapItemHandlersConfiguration() {
        if (siteMapItemHandlersConfigurationService != null) {
            return siteMapItemHandlersConfigurationService.get();
        }
        log.debug("Loading HstComponentsConfiguration for '{}'", configurationPath);

        synchronized (this) {
            if (siteMapItemHandlersConfigurationService != null) {
                return siteMapItemHandlersConfigurationService.get();
            }
            try {
                long start = System.currentTimeMillis();
                HstSiteMapItemHandlersConfiguration hsihcs = hstConfigurationLoadingCache.getSiteMapItemHandlersConfiguration(configurationPath, true);
                siteMapItemHandlersConfigurationService = Optional.of(hsihcs);
                log.info("Loading HstSiteMapItemHandlersConfiguration for '{}' took '{}' ms.", configurationPath,
                        String.valueOf(System.currentTimeMillis() - start));
                return hsihcs;
            } catch (ModelLoadingException e) {
                // avoid same model being incorrectly loaded over and over
                siteMapItemHandlersConfigurationService = Optional.of(HstSiteMapItemHandlersConfiguration.NOOP);
                log.warn("Could not load HstComponentsConfigurationService for '{}'. Return HstSiteMapItemHandlersConfiguration NOOP instance", configurationPath, e);
                return HstSiteMapItemHandlersConfiguration.NOOP;
            }
        }
    }
    
    public String getCanonicalIdentifier() {
        return canonicalIdentifier;
    }

    public String getConfigurationPath() {
        return configurationPath;
    }

    public long getVersion() {
        return -1;
    }
    
    public boolean hasPreviewConfiguration() {
        return hasPreviewConfiguration;
    }

    public String getName() {
        return name;
    }
    
    public LocationMapTree getLocationMapTree() {
        if (locationMapTree != null) {
            return locationMapTree;
        }
        synchronized (this) {
            if (locationMapTree != null) {
                return locationMapTree;
            }

            locationMapTree = new LocationMapTreeSiteMap(getSiteMap().getSiteMapItems());
            return locationMapTree;
        }
    }

    @Override
    public LocationMapTree getLocationMapTreeComponentDocuments() {
        if (locationMapTreeComponentDocuments != null) {
            return locationMapTreeComponentDocuments;
        }
        synchronized (this) {
            if (locationMapTreeComponentDocuments != null) {
                return locationMapTreeComponentDocuments;
            }
            locationMapTreeComponentDocuments = new LocationMapTreeComponentDocuments(getSiteMap().getSiteMapItems(),
                    getComponentsConfiguration(), mountSiteMapConfiguration.getMountContentPath(),
                    mountSiteMapConfiguration.getContextPath());
            return locationMapTreeComponentDocuments;
        }
    }

    public HstSiteMenusConfiguration getSiteMenusConfiguration() {
        if (siteMenusConfigurations != null) {
            return siteMenusConfigurations.orNull();
        }
        synchronized (this) {
            if (siteMenusConfigurations != null) {
                return siteMenusConfigurations.orNull();
            }
            final CompositeConfigurationNodes ccn = hstConfigurationLoadingCache.getCompositeConfigurationNodes(configurationPath, HstNodeTypes.NODENAME_HST_SITEMENUS);
            final CompositeConfigurationNode siteMenusNode = ccn.getCompositeConfigurationNodes().get(HstNodeTypes.NODENAME_HST_SITEMENUS);
            if (siteMenusNode == null) {
                log.info("There is no sitemenu configuration for '{}'. Return null", configurationPath);
                siteMenusConfigurations = Optional.absent();
                return null;
            }
            HstSiteMenusConfiguration sc = new HstSiteMenusConfigurationService(this, siteMenusNode);
            siteMenusConfigurations = Optional.of(sc);
            return sc;
        }
    }


    /*
     * meant to check all accessible root components from the sitemap space, and check whether every component has at least a template
     * (jsp/freemarker/etc) configured. If not, we log a warning about this
     */
    private void checkAndLogAccessibleRootComponents(final HstComponentsConfiguration hstComponentsConfiguration,
                                                     final HstSiteMap sm) {
        for(HstSiteMapItem hstSiteMapItem : sm.getSiteMapItems()){
            sanitizeSiteMapItem(hstSiteMapItem, hstComponentsConfiguration);
        }
    }

    private void sanitizeSiteMapItem(final HstSiteMapItem hstSiteMapItem,
                                     final HstComponentsConfiguration hstComponentsConfiguration) {
        HstComponentConfiguration hstComponentConfiguration = hstComponentsConfiguration.getComponentConfiguration(hstSiteMapItem.getComponentConfigurationId());
        if(hstComponentConfiguration == null) {
            log.info("HST Configuration info: The sitemap item '{}' does not point to a HST Component.", hstSiteMapItem.getId());
        } else {
            sanitizeHstComponentConfiguration(hstComponentConfiguration);
        }
        for(HstSiteMapItem child : hstSiteMapItem.getChildren()) {
            sanitizeSiteMapItem(child, hstComponentsConfiguration);
        }
    }

    private void sanitizeHstComponentConfiguration(HstComponentConfiguration hstComponentConfiguration) {
        String renderPath = hstComponentConfiguration.getRenderPath();
        if(renderPath == null) {
            log.info("HST Configuration info: the component '{}' does not have a render path. Component id = '{}'",hstComponentConfiguration.getName(),  hstComponentConfiguration.getId());
        }
        for(HstComponentConfiguration child : hstComponentConfiguration.getChildren().values()) {
            sanitizeHstComponentConfiguration(child);
        }
    }

    @Override
    public String toString() {
        return "HstSiteService{" +
                "name='" + name + '\'' +
                ",configurationPath='" + configurationPath + '\'' +
                '}';
    }
}
