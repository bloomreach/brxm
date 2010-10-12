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

import java.util.HashMap;
import java.util.Map;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.components.HstComponentsConfiguration;
import org.hippoecm.hst.configuration.components.HstComponentsConfigurationService;
import org.hippoecm.hst.configuration.hosting.SiteMount;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.configuration.model.HstSiteRootNode;
import org.hippoecm.hst.configuration.model.HstManagerImpl;
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

    private static final long serialVersionUID = 1L;

    private HstSiteMapService siteMapService;
    private HstSiteMapItemHandlersConfigurationService siteMapItemHandlersConfigurationService;
    private HstComponentsConfigurationService componentsConfigurationService;
    private HstSiteMenusConfiguration siteMenusConfigurations;
    private String name;
    private String contentPath;
    private String canonicalcontentPath;
    private String configurationPath;
    private LocationMapTree locationMapTree;
    
    private SiteMount siteMount;
    
    private static final Logger log = LoggerFactory.getLogger(HstSiteService.class);
    
    
    public HstSiteService(HstSiteRootNode site, SiteMount siteMount, HstManagerImpl hstManager) throws ServiceException{
        this.name = site.getValueProvider().getName();
        this.siteMount = siteMount;
        contentPath = site.getContentPath();
        canonicalcontentPath = site.getCanonicalcontentPath();
        configurationPath = site.getConfigurationPath();
        
        HstNode configurationNode = hstManager.getConfigurationRootNodes().get(configurationPath);
        
        if(configurationNode == null) {
            throw new ServiceException("Cannot find configuration at '"+configurationPath+"' for site '"+getName()+"'" );
        }
        
        init(configurationNode, hstManager.getRootPath() + "/hst:configurations");
    }

    
    private void init(HstNode configurationNode, String hstConfigurationsRootPath) throws ServiceException {
       Map<String, String> templateRenderMap = new HashMap<String,String>();
       
       // templates
       HstNode hstTemplates = configurationNode.getNode(HstNodeTypes.NODENAME_HST_TEMPLATES);
       if(hstTemplates == null) {
           throw new ServiceException("There are no '"+HstNodeTypes.NODENAME_HST_TEMPLATES+"' present for the configuration at '"+configurationNode.getValueProvider().getPath()+"'");
       }
       for(HstNode template : hstTemplates.getNodes()) {
           if(!template.getValueProvider().hasProperty(HstNodeTypes.TEMPLATE_PROPERTY_RENDERPATH) ) {
               log.warn("Skipping template '{}' because missing '{}' property", template.getValueProvider().getPath(), HstNodeTypes.TEMPLATE_PROPERTY_RENDERPATH);
               continue;
           }
           String renderpath = template.getValueProvider().getString(HstNodeTypes.TEMPLATE_PROPERTY_RENDERPATH);
           if(renderpath != null && ! "".equals(renderpath)) {
               templateRenderMap.put(template.getValueProvider().getName(), renderpath.trim());
           }
       }
       
       // component configuration
       this.componentsConfigurationService = new HstComponentsConfigurationService(configurationNode, hstConfigurationsRootPath , templateRenderMap); 
       
       // sitemapitem handlers
       
       HstNode sitemapItemHandlersNode = configurationNode.getNode(HstNodeTypes.NODENAME_HST_SITEMAPITEMHANDLERS);
       
       if(sitemapItemHandlersNode != null) {
           log.info("Found a '{}' configuration. Initialize sitemap item handlers service now", HstNodeTypes.NODENAME_HST_SITEMAPITEMHANDLERS);
           try {
               this.siteMapItemHandlersConfigurationService = new HstSiteMapItemHandlersConfigurationService(sitemapItemHandlersNode);
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
       this.siteMapService = new HstSiteMapService(this, siteMapNode, siteMapItemHandlersConfigurationService); 
       
       checkAndLogAccessibleRootComponents();
       
       this.locationMapTree = new LocationMapTreeImpl(this.getSiteMap().getSiteMapItems());
       
       HstNode siteMenusNode = configurationNode.getNode(HstNodeTypes.NODENAME_HST_SITEMENUS);
       if(siteMenusNode != null) {
           try {
               this.siteMenusConfigurations = new HstSiteMenusConfigurationService(this, siteMenusNode);
           } catch (ServiceException e) {
               log.error("ServiceException: Skipping SiteMenusConfiguration", e);
           }
       } else {
           log.info("There is no configuration for 'hst:sitemenus' for this HstSite. The clien cannot use the HstSiteMenusConfiguration");
       }
       
    }
   
    public SiteMount getSiteMount(){
        return this.siteMount;
    }

    /*
     * meant to check all accessible root components from the sitemap space, and check whether every component has at least a template 
     * (jsp/freemarker/etc) configured. If not, we log a warning about this
     */
    private void checkAndLogAccessibleRootComponents() {
        for(HstSiteMapItem hstSiteMapItem :this.siteMapService.getSiteMapItems()){
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
       return this.siteMapService;
    }
    
    public HstSiteMapItemHandlersConfiguration getSiteMapItemHandlersConfiguration(){
        return this.siteMapItemHandlersConfigurationService;
    }
    
    public String getContentPath() {
        return contentPath;
    }
    
    public String getCanonicalContentPath() {
        return canonicalcontentPath;
    }
    
    public String getConfigurationPath() {
        return this.configurationPath;
    }

    public String getName() {
        return name;
    }
    
    public LocationMapTree getLocationMapTree() {
        return this.locationMapTree;
    }

    public HstSiteMenusConfiguration getSiteMenusConfiguration() {
        return this.siteMenusConfigurations;
    }



}
