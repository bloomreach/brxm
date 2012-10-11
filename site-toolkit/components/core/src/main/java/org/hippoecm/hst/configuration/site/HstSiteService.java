/*
 *  Copyright 2008 Hippo.
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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.components.HstComponentsConfiguration;
import org.hippoecm.hst.configuration.components.HstComponentsConfigurationService;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.internal.ContextualizableMount;
import org.hippoecm.hst.configuration.model.HstManagerImpl;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.configuration.model.HstSiteRootNode;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstSiteService implements HstSite {

    private static final String FQCN = HstSiteService.class.getName();

    private HstSiteMapService siteMapService;
    private HstSiteMapItemHandlersConfigurationService siteMapItemHandlersConfigurationService;
    private HstComponentsConfigurationService componentsConfigurationService;
    private HstSiteMenusConfiguration siteMenusConfigurations;
    private String name;
    private String canonicalIdentifier;
    private String configurationPath;
    private LocationMapTree locationMapTree;

    private static final Logger log = LoggerFactory.getLogger(HstSiteService.class);

    
    public HstSiteService(HstSiteRootNode site, ContextualizableMount mount, HstManagerImpl hstManager) throws ServiceException{
        name = site.getValueProvider().getName();
        canonicalIdentifier = site.getValueProvider().getIdentifier();
        configurationPath = findConfigurationPath(site, hstManager, mount);
        HstNode configurationNode = hstManager.getEnhancedConfigurationRootNodes().get(configurationPath);

        if(configurationNode == null) {
            throw new ServiceException("Cannot find configuration at '"+configurationPath+"' for site '"+getName()+"'" );
        }

        mount.setLockedBy(configurationNode.getValueProvider().getString(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY));
        mount.setLockedOn(configurationNode.getValueProvider().getDate(HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON));

        init(configurationNode, mount, hstManager);
    }

    /**
     * if {@link HstSiteRootNode#getConfigurationPath()} returns <code>null</code> or empty, return a configuration path by convention, 
     * namely <code>hst:rootPath + /hst:configurations/ + siteNode.name</code>
     * @param site
     * @param hstManager
     * @return the <code>configurationPath</code> 
     */
    private String findConfigurationPath(HstSiteRootNode site, HstManagerImpl hstManager, ContextualizableMount mount) throws ServiceException {
        String path = site.getConfigurationPath(); 
        // when there is an explicit path configured, return directly:
        if(!StringUtils.isEmpty(path)) {
            // when there is an explicit configuration path, and the current site name ends with '-preview' but the 
            // path does not, then, when there is a configuration at 'path' + -preview , we'll pick this one. Otherwise, just path is 
            // used
            if(site.getValueProvider().getName().endsWith("-" + Mount.PREVIEW_NAME)) {
                // it is a preview site. Check whether the path ends with -preview. 
                if(path.endsWith("-" + Mount.PREVIEW_NAME)) {
                    return path;
                } else {
                    // check whether there is a preview config: If so, use this preview config
                    String previewPath = path + "-" + Mount.PREVIEW_NAME;
                    HstNode configurationNode = hstManager.getEnhancedConfigurationRootNodes().get(previewPath);
                    if (configurationNode != null) {
                        // preview config found : use this config
                        return previewPath;
                    }
                    // no previuew config, just use live config as well for preview
                    return path;
                }
            }
            return path;
        }
           
        // when HstSiteRootNode#getValueProvider()#getPath() equals ContextualizableMount#getMountPoint() we need to return live
        // otherwise, we try if a preview version exists. 
        if (site.getValueProvider().getPath().equals(mount.getMountPoint())) {
            // this mount can still be a preview mount: Namely, when the preview Mount is configured explicitly to be a preview
            // if this is the case, and the preview config does not exist, we fallback to the live config. 
            path = hstManager.getRootPath() + "/hst:configurations/" + site.getValueProvider().getName();
            log.debug("There is no hst:configurationpath configured for '{}'. Set it to '{}' by convention.", site
                    .getValueProvider().getPath(), path);
            HstNode configurationNode = hstManager.getEnhancedConfigurationRootNodes().get(path);
            if (configurationNode != null) {
                return path;
            } else {
                if(path.endsWith("-"+Mount.PREVIEW_NAME)) {
                    String livePath = path.substring(0, path.length() - (Mount.PREVIEW_NAME.length() + 1) ); 
                    log.debug("There is no preview configuration available for '{}'. Try to use live '{}' if present.",
                            path, livePath);
                    configurationNode = hstManager.getEnhancedConfigurationRootNodes().get(livePath);
                    if (configurationNode != null) {
                        return livePath;
                    } 
                }
                throw new ServiceException(
                        "There is no preview configuration available for '"+path+"'. Also it is not ending with the convention -preview. Cannot load a configuration for it");
            }
        } else if (site.getValueProvider().getPath().equals(mount.getPreviewMountPoint())) {
            path = hstManager.getRootPath() + "/hst:configurations/" + site.getValueProvider().getName();
            log.debug("There is no hst:configurationpath configured for preview '{}'. We'll try  if '{}' exists by convention.",
                          site.getValueProvider().getPath(), path);
            HstNode configurationNode = hstManager.getEnhancedConfigurationRootNodes().get(path);
            if (configurationNode != null) {
                return path;
            } else {
                if(path.endsWith("-"+Mount.PREVIEW_NAME)) {
                    String livePath = path.substring(0, path.length() - (Mount.PREVIEW_NAME.length() + 1));
                    log.debug("There is no preview configuration available for '{}'. Try to use live '{}' if present.",
                            path, livePath);
                    return livePath;
                } else {
                    throw new ServiceException("There is not preview configuration for '"+path+"' and cannot test live because '"+path+"' does not end with -preview");
                }
            }
        } else {
            throw new ServiceException(
                    "The HstSiteRootNode must have a value provider with path either equal to the live mountpoint or to the preview");
        }
    }
    
    private void init(HstNode configurationNode, Mount mount, HstManagerImpl hstManager) throws ServiceException {
       // check wether we already a an instance that would reulst in the very same HstComponentsConfiguration instance. If so, set that value
      
       // the cachekey is the set of all HstNode identifiers that make a HstComponentsConfigurationService unique: thus, pages, components, catalog and templates.
       Set<String> cachekey = computeCacheKey(configurationNode);
       Map<Set<String>, HstComponentsConfigurationService> hstComponentsConfigurationInstanceCache = hstManager.getHstComponentsConfigurationInstanceCache();
       
       HstComponentsConfigurationService prevLoaded =  hstComponentsConfigurationInstanceCache.get(cachekey);
       if(prevLoaded == null) {
           componentsConfigurationService = new HstComponentsConfigurationService(configurationNode, hstManager); 
           hstComponentsConfigurationInstanceCache.put(cachekey, componentsConfigurationService);
       } else {
           log.debug("Reusing existing HstComponentsConfiguration because exact same configuration. We do not build HstComponentsConfiguration for '{}' but use existing version.", configurationNode.getValueProvider().getPath());
           componentsConfigurationService = prevLoaded; 
       }
       
       // sitemapitem handlers
       
       HstNode sitemapItemHandlersNode = configurationNode.getNode(HstNodeTypes.NODENAME_HST_SITEMAPITEMHANDLERS);
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
       HstNode siteMapNode = configurationNode.getNode(HstNodeTypes.NODENAME_HST_SITEMAP);
       if(siteMapNode == null) {
           throw new ServiceException("There is no sitemap configured");
       }
       siteMapService = new HstSiteMapService(this, siteMapNode, mount, siteMapItemHandlersConfigurationService); 
       
       checkAndLogAccessibleRootComponents();
       
       locationMapTree = new LocationMapTreeImpl(this.getSiteMap().getSiteMapItems());
       
       HstNode siteMenusNode = configurationNode.getNode(HstNodeTypes.NODENAME_HST_SITEMENUS);
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

    public String getName() {
        return name;
    }
    
    public LocationMapTree getLocationMapTree() {
        return locationMapTree;
    }

    public HstSiteMenusConfiguration getSiteMenusConfiguration() {
        return siteMenusConfigurations;
    }
    
    private Set<String> computeCacheKey(HstNode configurationNode) {
        Set<String> key = new HashSet<String>();
        augmentKey(key,configurationNode.getNode(HstNodeTypes.NODENAME_HST_COMPONENTS));
        augmentKey(key,configurationNode.getNode(HstNodeTypes.NODENAME_HST_PAGES));
        augmentKey(key,configurationNode.getNode(HstNodeTypes.NODENAME_HST_CATALOG));
        augmentKey(key,configurationNode.getNode(HstNodeTypes.NODENAME_HST_TEMPLATES));
        return key;
    }


    private void augmentKey(Set<String> key, HstNode node) {
        if(node != null) {
            for(HstNode n :node.getNodes()) {
                key.add(n.getValueProvider().getIdentifier());
            }
        }
    }

}
