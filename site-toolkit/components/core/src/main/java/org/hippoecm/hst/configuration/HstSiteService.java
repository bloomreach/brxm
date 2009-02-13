package org.hippoecm.hst.configuration;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.components.HstComponentsConfiguration;
import org.hippoecm.hst.configuration.components.HstComponentsConfigurationService;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapService;
import org.hippoecm.hst.service.AbstractJCRService;
import org.hippoecm.hst.service.Service;
import org.hippoecm.hst.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstSiteService extends AbstractJCRService implements HstSite, Service{

    private HstSiteMapService siteMapService;
    private HstComponentsConfigurationService componentsConfigurationService;
    private String name;
    private String contentPath;
    private String configurationPath;
    
    private HstSites hstSites;
    
    private static final Logger log = LoggerFactory.getLogger(HstSite.class);
    
    
    public HstSiteService(Node site, HstSites hstSites) throws ServiceException{
        super(site);
        try {
            this.name = site.getName();
            this.hstSites = hstSites;
            if(site.hasNode(Configuration.NODENAME_HST_CONTENTNODE) && site.hasNode(Configuration.NODEPATH_HST_CONFIGURATION)) {
                if(hstSites.getSite(name) != null) {
                    throw new ServiceException("Duplicate subsite with same name for '"+name+"'. Skipping this one");
                } else {
                    contentPath = site.getNode(Configuration.NODENAME_HST_CONTENTNODE).getPath();
                    
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
       
       if(log.isDebugEnabled()){
           StringBuffer buf = new StringBuffer();
           log.debug(buf.toString());
       }
       
       Node siteMapNode = configurationNode.getNode(Configuration.NODENAME_HST_SITEMAP);
       this.siteMapService = new HstSiteMapService(this, siteMapNode);
       
       if(log.isDebugEnabled()){
           StringBuffer buf = new StringBuffer();
           log.debug(buf.toString());
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


}
