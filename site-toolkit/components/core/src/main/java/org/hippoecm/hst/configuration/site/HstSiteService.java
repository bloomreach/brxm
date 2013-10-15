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

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.cache.HstConfigurationLoadingCache;
import org.hippoecm.hst.configuration.cache.HstNodeLoadingCache;
import org.hippoecm.hst.configuration.cache.HstSiteConfigurationRootNodeImpl;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.components.HstComponentsConfiguration;
import org.hippoecm.hst.configuration.components.HstComponentsConfigurationService;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.configuration.model.ModelLoadingException;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapService;
import org.hippoecm.hst.configuration.sitemapitemhandler.HstSiteMapItemHandlersConfigurationService;
import org.hippoecm.hst.configuration.sitemapitemhandlers.HstSiteMapItemHandlersConfiguration;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenusConfiguration;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenusConfigurationService;
import org.hippoecm.hst.core.linking.LocationMapTree;
import org.hippoecm.hst.core.linking.LocationMapTreeImpl;
import org.hippoecm.hst.service.ServiceException;
import org.hippoecm.hst.site.HstServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstSiteService implements HstSite {

    private static final Logger log = LoggerFactory.getLogger(HstSiteService.class);
    private HstSiteMapService siteMapService;
    private HstSiteMapItemHandlersConfigurationService siteMapItemHandlersConfigurationService;
    private HstComponentsConfigurationService componentsConfigurationService;
    private HstSiteMenusConfiguration siteMenusConfigurations;
    private String name;
    private boolean hasPreviewConfiguration;
    private String canonicalIdentifier;
    private String configurationPath;
    private LocationMapTree locationMapTree;

    
    public HstSiteService(HstNode site, MountSiteMapConfiguration mountSiteMapConfiguration, HstNodeLoadingCache hstNodeLoadingCache) throws ServiceException {
        name = site.getValueProvider().getName();
        canonicalIdentifier = site.getValueProvider().getIdentifier();

        findAndSetConfigurationPath(site, hstNodeLoadingCache);

        init(mountSiteMapConfiguration, hstNodeLoadingCache);
    }

    private void findAndSetConfigurationPath(final HstNode site, HstNodeLoadingCache hstNodeLoadingCache) throws ServiceException {
        boolean isPreviewSite = site.getValueProvider().getName().endsWith("-preview");
        if (site.getValueProvider().hasProperty(HstNodeTypes.SITE_CONFIGURATIONPATH)) {
            configurationPath = site.getValueProvider().getString(HstNodeTypes.SITE_CONFIGURATIONPATH);
            if (isPreviewSite) {
                configurationPath = configurationPath + "-preview";
            }
        } else {
            configurationPath = hstNodeLoadingCache.getRootPath() + "/" +
                    HstNodeTypes.NODENAME_HST_CONFIGURATIONS + "/" +site.getValueProvider().getName();
        }
        if (isPreviewSite) {
            HstNode previewConfig = hstNodeLoadingCache.getNode(configurationPath);
            if (previewConfig != null) {
                hasPreviewConfiguration = true;
            } else {
                configurationPath = configurationPath.substring(0, configurationPath.length() - "-preview".length());
            }
        }
    }

    private void init(MountSiteMapConfiguration mountSiteMapConfiguration, HstNodeLoadingCache hstNodeLoadingCache) throws ServiceException {

        HstConfigurationLoadingCache loadingCache = HstServices.getComponentManager().getComponent(HstConfigurationLoadingCache.class.getName());
        HstSiteConfigurationRootNodeImpl node = loadingCache.getInheritanceResolvedNode(configurationPath);
        if (node == null) {
            throw new ModelLoadingException(
                    "There is no configuration found at '"+configurationPath+"'. Cannot load a configuration for it. This can only" +
                            " happen if the jcr model changed during loading.");
        }

       componentsConfigurationService =  loadingCache.get(node);
       // sitemapitem handlers
       HstNode sitemapItemHandlersNode = node.getNode(HstNodeTypes.NODENAME_HST_SITEMAPITEMHANDLERS);
       if(sitemapItemHandlersNode != null) {
           log.info("Found a '{}' configuration. Initialize sitemap item handlers service now", HstNodeTypes.NODENAME_HST_SITEMAPITEMHANDLERS);
           try {
               siteMapItemHandlersConfigurationService = new HstSiteMapItemHandlersConfigurationService(sitemapItemHandlersNode);
           } catch (ServiceException e) {
               log.error("ServiceException: Skipping handlesConfigurationService", e);
           }
       } else {
           log.debug("No sitemap item handlers configuration present.");
       }
       
       // sitemap
       HstNode siteMapNode = node.getNode(HstNodeTypes.NODENAME_HST_SITEMAP);
       if(siteMapNode == null) {
           throw new ServiceException("There is no sitemap configured");
       }
       siteMapService = new HstSiteMapService(this, siteMapNode, mountSiteMapConfiguration, siteMapItemHandlersConfigurationService);
       
       checkAndLogAccessibleRootComponents();
       
       locationMapTree = new LocationMapTreeImpl(this.getSiteMap().getSiteMapItems());
       
       HstNode siteMenusNode = node.getNode(HstNodeTypes.NODENAME_HST_SITEMENUS);
       if(siteMenusNode != null) {
           try {
               siteMenusConfigurations = new HstSiteMenusConfigurationService(this, siteMenusNode);
           } catch (ServiceException e) {
               log.error("ServiceException: Skipping SiteMenusConfiguration", e);
           }
       } else {
           log.info("There is no configuration for 'hst:sitemenus' for this HstSite. The clien cannot use the HstSiteMenusConfiguration");
       }
       
    }


    /*
     * meant to check all accessible root components from the sitemap space, and check whether every component has at least a template 
     * (jsp/freemarker/etc) configured. If not, we log a warning about this
     */
    private void checkAndLogAccessibleRootComponents() {
        for(HstSiteMapItem hstSiteMapItem : siteMapService.getSiteMapItems()){
            sanitizeSiteMapItem(hstSiteMapItem);
        }
    }

    private void sanitizeSiteMapItem(HstSiteMapItem hstSiteMapItem) {
        HstComponentConfiguration hstComponentConfiguration = this.getComponentsConfiguration().getComponentConfiguration(hstSiteMapItem.getComponentConfigurationId());
        if(hstComponentConfiguration == null) {
            log.info("HST Configuration info: The sitemap item '{}' does not point to a HST Component.", hstSiteMapItem.getId());
        } else {
            sanitizeHstComponentConfiguration(hstComponentConfiguration);
        }
        for(HstSiteMapItem child : hstSiteMapItem.getChildren()) {
            sanitizeSiteMapItem(child);
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

    public HstComponentsConfiguration getComponentsConfiguration() {
        return this.componentsConfigurationService;
    }

    public HstSiteMap getSiteMap() {
       return siteMapService;
    }
    
    public HstSiteMapItemHandlersConfiguration getSiteMapItemHandlersConfiguration(){
        return siteMapItemHandlersConfigurationService;
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
        return locationMapTree;
    }

    public HstSiteMenusConfiguration getSiteMenusConfiguration() {
        return siteMenusConfigurations;
    }

}
