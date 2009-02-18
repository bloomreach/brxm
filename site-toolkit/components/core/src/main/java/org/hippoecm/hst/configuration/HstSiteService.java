package org.hippoecm.hst.configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.components.HstComponentsConfiguration;
import org.hippoecm.hst.configuration.components.HstComponentsConfigurationService;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapService;
import org.hippoecm.hst.core.util.PathUtils;
import org.hippoecm.hst.service.AbstractJCRService;
import org.hippoecm.hst.service.Service;
import org.hippoecm.hst.service.ServiceException;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstSiteService extends AbstractJCRService implements HstSite, Service{

    private HstSiteMapService siteMapService;
    private HstComponentsConfigurationService componentsConfigurationService;
    private String name;
    private String contentPath;
    private String canonicalcontentPath;
    private String configurationPath;
    private LocationMapTree locationMapTree;
    
    private HstSites hstSites;
    
    private static final Logger log = LoggerFactory.getLogger(HstSite.class);
    
    
    public HstSiteService(Node site, HstSites hstSites) throws ServiceException{
        super(site);
        try {
            this.name = site.getName();
            this.hstSites = hstSites;
            if(site.hasNode(Configuration.NODENAME_HST_CONTENTNODE) && site.hasNode(Configuration.NODEPATH_HST_CONFIGURATION)) {
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
       Node componentsNode = configurationNode.getNode(Configuration.NODENAME_HST_COMPONENTS); 
       this.componentsConfigurationService = new HstComponentsConfigurationService(componentsNode); 
      
       Node siteMapNode = configurationNode.getNode(Configuration.NODENAME_HST_SITEMAP);
       this.siteMapService = new HstSiteMapService(this, siteMapNode);
   
       this.locationMapTree = this.createLocationMap();
       
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
            locMap.add(new ArrayList<String>(Arrays.asList(normPath.split("/"))), siteMapItem);
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

    public LocationMapTree getLocationMap() {
        return this.locationMapTree;
    }


}
