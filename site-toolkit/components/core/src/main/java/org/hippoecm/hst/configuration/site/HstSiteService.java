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
package org.hippoecm.hst.configuration.site;

import com.google.common.base.Optional;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.cache.CompositeConfigurationNodes;
import org.hippoecm.hst.configuration.cache.HstConfigurationLoadingCache;
import org.hippoecm.hst.configuration.cache.HstNodeLoadingCache;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.components.HstComponentsConfiguration;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.configuration.model.ModelLoadingException;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapService;
import org.hippoecm.hst.configuration.sitemapitemhandlers.HstSiteMapItemHandlersConfiguration;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenusConfiguration;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenusConfigurationService;
import org.hippoecm.hst.core.linking.LocationMapTree;
import org.hippoecm.hst.core.linking.LocationMapTreeImpl;
import org.hippoecm.hst.site.HstServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstSiteService implements HstSite {

    private static final Logger log = LoggerFactory.getLogger(HstSiteService.class);
    volatile Optional<HstSiteMap> siteMap;
    volatile LocationMapTree locationMapTree;
    volatile Optional<HstSiteMapItemHandlersConfiguration> siteMapItemHandlersConfigurationService;
    volatile Optional<HstComponentsConfiguration> componentsConfiguration;
    volatile Optional<HstSiteMenusConfiguration> siteMenusConfigurations;
    private String name;
    private boolean hasPreviewConfiguration;
    private String canonicalIdentifier;
    private String configurationPath;
    private MountSiteMapConfiguration mountSiteMapConfiguration;
    private HstConfigurationLoadingCache configLoadingCache;
    private final Object hstModelMutex;


    public static HstSiteService createLiveSiteService(final HstNode site,
                                                       final MountSiteMapConfiguration mountSiteMapConfiguration,
                                                       final HstNodeLoadingCache hstNodeLoadingCache) throws ModelLoadingException {
        return new HstSiteService(site, mountSiteMapConfiguration, hstNodeLoadingCache, false);
    }

    public static HstSiteService createPreviewSiteService(final HstNode site,
                                                       final MountSiteMapConfiguration mountSiteMapConfiguration,
                                                       final HstNodeLoadingCache hstNodeLoadingCache) throws ModelLoadingException {
        return new HstSiteService(site, mountSiteMapConfiguration, hstNodeLoadingCache, true);
    }

    private HstSiteService(final HstNode site,
                           final MountSiteMapConfiguration mountSiteMapConfiguration,
                           final HstNodeLoadingCache hstNodeLoadingCache,
                           final boolean isPreviewSite) throws ModelLoadingException {
        hstModelMutex = HstServices.getComponentManager().getComponent("hstModelMutex");
        configLoadingCache = HstServices.getComponentManager().getComponent(HstConfigurationLoadingCache.class.getName());
        name = site.getValueProvider().getName();
        canonicalIdentifier = site.getValueProvider().getIdentifier();
        this.mountSiteMapConfiguration = mountSiteMapConfiguration;
        findAndSetConfigurationPath(site, hstNodeLoadingCache, isPreviewSite);
        init();
    }

    private void findAndSetConfigurationPath(final HstNode site,
                                             final HstNodeLoadingCache hstNodeLoadingCache,
                                             final boolean isPreviewSite
                                             ) {
        if (site.getValueProvider().hasProperty(HstNodeTypes.SITE_CONFIGURATIONPATH)) {
            configurationPath = site.getValueProvider().getString(HstNodeTypes.SITE_CONFIGURATIONPATH);
        } else {
            configurationPath = hstNodeLoadingCache.getRootPath() + "/" +
                    HstNodeTypes.NODENAME_HST_CONFIGURATIONS + "/" +site.getValueProvider().getName();
        }
        if (isPreviewSite) {
            String previewConfigurationPath = configurationPath + "-preview";
            HstNode previewConfig = hstNodeLoadingCache.getNode(previewConfigurationPath);
            if (previewConfig != null) {
                hasPreviewConfiguration = true;
                configurationPath = previewConfigurationPath;
            }
        }
    }

    private void init() {

        HstComponentsConfiguration ccs = configLoadingCache.getComponentsConfiguration(configurationPath, false);
        if (ccs != null) {
            log.debug("Reusing cached HstComponentsConfiguration for '{}'", configurationPath);
            componentsConfiguration = Optional.of(ccs);
        } else {
            log.debug("No cached HstComponentsConfiguration for '{}' present. On first access it will " +
                    "be loaded lazily", configurationPath);
        }

        HstSiteMapItemHandlersConfiguration hsihcs = configLoadingCache.getSiteMapItemHandlersConfiguration(configurationPath, false);
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


    public HstComponentsConfiguration getComponentsConfiguration() {
        if (componentsConfiguration != null) {
            if (componentsConfiguration.isPresent()) {
                return componentsConfiguration.get();
            }
            throw new ModelLoadingException("HstComponentsConfiguration for '"+configurationPath+"' could not be correctly" +
                    " loaded earlier.");
        }
        log.debug("Loading HstComponentsConfiguration for '{}'", configurationPath);

        synchronized (hstModelMutex) {
            if (componentsConfiguration != null) {
                if (componentsConfiguration.isPresent()) {
                    return componentsConfiguration.get();
                }
                throw new ModelLoadingException("HstComponentsConfiguration for '"+configurationPath+"' could not be correctly" +
                        " loaded earlier.");
            }
            try {
                long start = System.currentTimeMillis();
                HstComponentsConfiguration ccs = configLoadingCache.getComponentsConfiguration(configurationPath, true);
                componentsConfiguration = Optional.of(ccs);
                if (siteMap != null && siteMap.isPresent()) {
                    checkAndLogAccessibleRootComponents(ccs, siteMap.get());
                }
                log.info("Loading HstComponentsConfiguration for '{}' took '{}' ms.", configurationPath,
                        String.valueOf(System.currentTimeMillis() - start));
                return ccs;
            } catch (ModelLoadingException e) {
                // avoid same model being incorrectly loaded over and over
                componentsConfiguration = Optional.absent();
                throw new ModelLoadingException("Could not load HstComponentsConfiguration for '"+configurationPath+"'.", e);
            }
        }
    }

    public HstSiteMap getSiteMap() {
        if (siteMap != null) {
            if (siteMap.isPresent()) {
                return siteMap.get();
            }
            throw new ModelLoadingException("HstSiteMap for '"+configurationPath+"' could not be correctly" +
                    " loaded earlier.");
        }
        synchronized (hstModelMutex) {
            if (siteMap != null) {
                if (siteMap.isPresent()) {
                    return siteMap.get();
                }
                throw new ModelLoadingException("HstSiteMap for '"+configurationPath+"' could not be correctly" +
                        " loaded earlier.");
            }
            try {
                long start = System.currentTimeMillis();
                final CompositeConfigurationNodes ccn = configLoadingCache.getCompositeConfigurationNodes(configurationPath, HstNodeTypes.NODENAME_HST_SITEMAP);
                final CompositeConfigurationNodes.CompositeConfigurationNode siteMapNode = ccn.getCompositeConfigurationNodes().get(HstNodeTypes.NODENAME_HST_SITEMAP);
                if (siteMapNode == null) {
                    siteMap = Optional.absent();
                    throw new ModelLoadingException("There is no sitemap configured");
                }
                HstSiteMap sm = new HstSiteMapService(this, siteMapNode, mountSiteMapConfiguration, getSiteMapItemHandlersConfiguration());
                siteMap = Optional.of(sm);
                if (componentsConfiguration != null && componentsConfiguration.isPresent()) {
                    checkAndLogAccessibleRootComponents(componentsConfiguration.get(), sm);
                }
                log.info("Loading HstSiteMap for '{}' took '{}' ms.", configurationPath,
                        String.valueOf(System.currentTimeMillis() - start));
                return sm;
            } catch (ModelLoadingException e) {
                // avoid same model being incorrectly loaded over and over
                siteMap = Optional.absent();
                throw new ModelLoadingException("Could not load HstSiteMap for '" + configurationPath + "'.", e);
            }
        }
    }

    public HstSiteMapItemHandlersConfiguration getSiteMapItemHandlersConfiguration() {
        if (siteMapItemHandlersConfigurationService != null) {
            if (siteMapItemHandlersConfigurationService.isPresent()) {
                return siteMapItemHandlersConfigurationService.get();
            }
            throw new ModelLoadingException("HstSiteMapItemHandlersConfiguration for '"+configurationPath+"' could not be correctly" +
                    " loaded earlier.");
        }
        log.debug("Loading HstComponentsConfiguration for '{}'", configurationPath);

        synchronized (hstModelMutex) {
            if (siteMapItemHandlersConfigurationService != null) {
                if (siteMapItemHandlersConfigurationService.isPresent()) {
                    return siteMapItemHandlersConfigurationService.get();
                }
                throw new ModelLoadingException("HstSiteMapItemHandlersConfiguration for '"+configurationPath+"' could not be correctly" +
                        " loaded earlier.");
            }
            try {
                long start = System.currentTimeMillis();

                HstSiteMapItemHandlersConfiguration hsihcs = configLoadingCache.getSiteMapItemHandlersConfiguration(configurationPath, true);
                siteMapItemHandlersConfigurationService = Optional.of(hsihcs);
                log.info("Loading HstSiteMapItemHandlersConfiguration for '{}' took '{}' ms.", configurationPath,
                        String.valueOf(System.currentTimeMillis() - start));
                return hsihcs;
            } catch (ModelLoadingException e) {
                // avoid same model being incorrectly loaded over and over
                siteMapItemHandlersConfigurationService = Optional.absent();
                throw new ModelLoadingException("Could not load HstComponentsConfigurationService for '"+configurationPath+"'.", e);
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
        synchronized (hstModelMutex) {
            if (locationMapTree != null) {
                return locationMapTree;
            }
            locationMapTree = new LocationMapTreeImpl(getSiteMap().getSiteMapItems());
            return locationMapTree;
        }
    }

    public HstSiteMenusConfiguration getSiteMenusConfiguration() {
        if (siteMenusConfigurations != null) {
            return siteMenusConfigurations.orNull();
        }
        synchronized (hstModelMutex) {
            if (siteMenusConfigurations != null) {
                return siteMenusConfigurations.orNull();
            }
            final CompositeConfigurationNodes ccn = configLoadingCache.getCompositeConfigurationNodes(configurationPath, HstNodeTypes.NODENAME_HST_SITEMENUS);
            final CompositeConfigurationNodes.CompositeConfigurationNode siteMenusNode = ccn.getCompositeConfigurationNodes().get(HstNodeTypes.NODENAME_HST_SITEMENUS);
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

}
