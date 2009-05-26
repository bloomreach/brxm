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
package org.hippoecm.hst.configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.components.HstComponentsConfiguration;
import org.hippoecm.hst.configuration.components.HstComponentsConfigurationService;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapService;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenusConfiguration;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenusConfigurationService;
import org.hippoecm.hst.core.linking.BasicLocationMapTree;
import org.hippoecm.hst.core.linking.LocationMapTree;
import org.hippoecm.hst.service.AbstractJCRService;
import org.hippoecm.hst.service.Service;
import org.hippoecm.hst.service.ServiceException;
import org.hippoecm.hst.util.PathUtils;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstSiteService extends AbstractJCRService implements HstSite, Service{

    private static final long serialVersionUID = 1L;
    
    private HstSiteMapService siteMapService;
    private HstComponentsConfigurationService componentsConfigurationService;
    private HstSiteMenusConfiguration siteMenusConfigurations;
    private String name;
    private String contentPath;
    private String canonicalcontentPath;
    private String configurationPath;
    private LocationMapTree locationMapTree;
    
    private HstSites hstSites;
    
    private static final Logger log = LoggerFactory.getLogger(HstSiteService.class);
    
    
    public HstSiteService(Node site, HstSites hstSites) throws ServiceException{
        super(site);
        try {
            this.name = site.getName();
            this.hstSites = hstSites;
            if(site.hasNode(Configuration.NODENAME_HST_CONTENTNODE) && site.hasNode(Configuration.NODEPATH_HST_CONFIGURATION) ) {
                if(hstSites.getSites().get(name) != null) {
                    throw new ServiceException("Duplicate subsite with same name for '"+name+"'. Skipping this one");
                } else {
                    Node contentNode = site.getNode(Configuration.NODENAME_HST_CONTENTNODE);
                    contentPath = contentNode.getPath();
                    
                    // fetch the mandatory hippo:docbase property to retrieve the canonical node
                    if(contentNode.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                        String docbaseUuid = contentNode.getProperty(HippoNodeType.HIPPO_DOCBASE).getString();
                        
                        try {
                                // test whether docbaseUuid is valid uuid. If UUID.fromString fails, an IllegalArgumentException is thrown
                                
                                UUID.fromString(docbaseUuid);
                                Item item =  contentNode.getSession().getNodeByUUID(docbaseUuid); 
                                if(item instanceof Node) {
                                    // set the canonical content path
                                    this.canonicalcontentPath = ((Node)item).getPath();
                                } else {
                                    log.warn("Docbase from '{}' does contain a uuid that points to a property instead of a 'root content node'. Content mirror is broken", contentNode.getPath());
                                }
                        } catch (IllegalArgumentException e) {
                            log.warn("Docbase from '{}' does not contain a valid uuid. Content mirror is broken", contentNode.getPath());
                        } catch (ItemNotFoundException e) {
                            log.warn("ItemNotFoundException: Content mirror is broken. ", e.getMessage());
                        } catch (RepositoryException e) {
                            log.error("RepositoryException: Content mirror is broken. ", e);
                        }
                    } else {
                        // contentNode is not a mirror. Take the canonical path to be the same
                        this.canonicalcontentPath = this.contentPath;
                    }
                    
                    Node configurationNode = site.getNode(Configuration.NODEPATH_HST_CONFIGURATION);
                    configurationPath = configurationNode.getPath();
                    
                    init(configurationNode);
                    
                }
            } else {
                throw new ServiceException("Subsite '"+name+"' cannot be instantiated because it does not contain the mandatory nodes. Skipping this one");
            } 
        } catch (RepositoryException e) {
            throw new ServiceException("Repository Exception during instantiating '"+name+"'. Skipping subsite.");
        }
        
    }

    public Service[] getChildServices() {
       Service[] services = {siteMapService,componentsConfigurationService};
       return services;
    }
    
    private void init(Node configurationNode) throws  RepositoryException, ServiceException {
       Map<String, String> templateRenderMap = new HashMap<String,String>();
       Node hstTemplates = configurationNode.getNode(Configuration.NODENAME_HST_TEMPLATES);
       NodeIterator nodeIt = hstTemplates.getNodes();
       while(nodeIt.hasNext()){
           Node template = nodeIt.nextNode();
           if(template == null) {
               continue;
           }
           if(!template.hasProperty(Configuration.TEMPLATE_PROPERTY_RENDERPATH) ) {
               log.warn("Skipping template '{}' because missing '{}' property", template.getPath(), Configuration.TEMPLATE_PROPERTY_RENDERPATH);
               continue;
           }
           String renderpath = template.getProperty(Configuration.TEMPLATE_PROPERTY_RENDERPATH).getString();
           if(renderpath != null && ! "".equals(renderpath)) {
               templateRenderMap.put(template.getName(), renderpath.trim());
           }
       }
        
       this.componentsConfigurationService = new HstComponentsConfigurationService(configurationNode, templateRenderMap); 
      
       Node siteMapNode = configurationNode.getNode(Configuration.NODENAME_HST_SITEMAP);
       this.siteMapService = new HstSiteMapService(this, siteMapNode);   
       this.locationMapTree = this.createLocationMap();
       
       if(configurationNode.hasNode(Configuration.NODENAME_HST_SITEMENUS)) {
           Node siteMenusNode = configurationNode.getNode(Configuration.NODENAME_HST_SITEMENUS);
           try {
           this.siteMenusConfigurations = new HstSiteMenusConfigurationService(this, siteMenusNode);
           } catch (ServiceException e) {
               log.error("ServiceException: Skipping SiteMenusConfiguration '{}'", e);
           }
       } else {
           log.info("There is no configuration for 'hst:sitemenus' for this HstSite. The clien cannot use the HstSiteMenusConfiguration");
       }
    }

    private LocationMapTree createLocationMap() {

        BasicLocationMapTree locMap  = new BasicLocationMapTree(canonicalcontentPath);
        for(HstSiteMapItem siteMapItem : this.getSiteMap().getSiteMapItems()){
            add2LocationMap(locMap,siteMapItem);
        }
        return locMap;
    }

    private void add2LocationMap(BasicLocationMapTree locMap, HstSiteMapItem siteMapItem) {
        String normPath = PathUtils.normalizePath(siteMapItem.getRelativeContentPath());
        if( !(normPath == null || "".equals(normPath))) {
            locMap.add(normPath, siteMapItem);
        }
        for(HstSiteMapItem child : siteMapItem.getChildren()) {
           add2LocationMap(locMap, child);
        }
    }

    public HstComponentsConfiguration getComponentsConfiguration() {
        return this.componentsConfigurationService;
    }

    public HstSiteMap getSiteMap() {
       return this.siteMapService;
    }

    public String getContentPath() {
        return contentPath;
    }
    
    public String getConfigurationPath() {
        return this.configurationPath;
    }

    public String getName() {
        return name;
    }

    public HstSites getHstSites(){
        return this.hstSites;
    }
    
    public LocationMapTree getLocationMapTree() {
        return this.locationMapTree;
    }

    public HstSiteMenusConfiguration getSiteMenusConfiguration() {
        return this.siteMenusConfigurations;
    }


}
