package org.hippoecm.hst.configuration;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.components.HstComponentsConfiguration;
import org.hippoecm.hst.configuration.components.HstComponentsConfigurationService;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapService;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.service.AbstractJCRService;
import org.hippoecm.hst.service.Service;
import org.hippoecm.hst.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstSiteService extends AbstractJCRService implements HstSite, Service{

    private HstSiteMapService siteMapService;
    private HstComponentsConfigurationService componentsService;
    private String name;
    private String contentPath;
    private HstSites hstSites;
    
    private static final Logger log = LoggerFactory.getLogger(HstSite.class);
    
    
    public HstSiteService(Node site, HstSites hstSites) throws ServiceException, RepositoryException{
        super(site);
        this.name = site.getName();
        this.hstSites = hstSites;
        if(hstSites.getSite(name) != null) {
            throw new ServiceException("Duplicate subsite with same name for '"+name+"'. Skipping this one");
        } else {
            init();
            /*
             * After initialization, all needed jcr properties and nodes have to be loaded. The underlying jcr nodes in 
             * the value providers now will all be closed.
             */
            this.closeValueProvider(true);
        }
    }

    public Service[] getChildServices() {
       Service[] services = {siteMapService,componentsService};
       return services;
    }
    
    private void init() throws PathNotFoundException, RepositoryException, ServiceException {
       Node componentsNode = getValueProvider().getJcrNode().getNode(Configuration.NODEPATH_HST_COMPONENTS); 
       this.componentsService = new HstComponentsConfigurationService(componentsNode); 
       
       if(log.isDebugEnabled()){
           StringBuffer buf = new StringBuffer();
           componentsService.dump(buf, "");
           log.debug(buf.toString());
       }
       
       Node siteMapNode = getValueProvider().getJcrNode().getNode(Configuration.NODEPATH_HST_SITEMAP);
       this.siteMapService = new HstSiteMapService(siteMapNode, componentsService);
       
       if(log.isDebugEnabled()){
           StringBuffer buf = new StringBuffer();
           siteMapService.dump(buf, "");
           log.debug(buf.toString());
       }
    }

    public HstComponentsConfiguration getComponents() {
        return this.componentsService;
    }

    public HstSiteMap getSiteMap() {
       return this.siteMapService;
    }

    public String getContentPath() {
        return contentPath;
    }

    public String getName() {
        return name;
    }

    public HstSites getHstSites(){
        return this.hstSites;
    }


}
