package org.hippoecm.hst.configuration;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.components.HstComponents;
import org.hippoecm.hst.configuration.components.HstComponentsService;
import org.hippoecm.hst.configuration.sitemap.JCRSiteMapService;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.service.AbstractJCRService;
import org.hippoecm.hst.service.Service;
import org.hippoecm.hst.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstSiteService extends AbstractJCRService implements HstSite{

    private HstSiteMap siteMapService;
    private HstComponents componentsService;
    private String name;
    private String contentPath;
    
    private static final Logger log = LoggerFactory.getLogger(HstSite.class);
    
    
    public HstSiteService(String name, Node confNode, Node contentNode) throws ServiceException, RepositoryException{
        super(confNode);
        if(name == null) {
            throw new ServiceException("HstSite's name is not allowed to be null.");
        }
        this.name = name;
        this.contentPath = contentNode.getPath();
        
        if(!confNode.isNodeType(Configuration.NODETYPE_HST_CONFIGURATION)) {
            throw new ServiceException("Cannot create HstConfigurationServiceImpl: Expected nodeType '"+Configuration.NODETYPE_HST_CONFIGURATION+"' but found '"+confNode.getPrimaryNodeType().getName()+"'");
        }
        init();
        
        /*
         * After initialization, all needed jcr properties and nodes have to be loaded. The underlying jcr nodes in 
         * the value providers now will all be closed.
         */
        this.closeValueProvider(true);
    }

    public Service[] getChildServices() {
       Service[] services = {siteMapService,componentsService};
       return services;
    }
    
    private void init() throws PathNotFoundException, RepositoryException, ServiceException {
       Node componentsNode = getValueProvider().getJcrNode().getNode(Configuration.NODEPATH_HST_PAGEMAPPING); 
       this.componentsService = new HstComponentsService(componentsNode); 
       
       if(log.isDebugEnabled()){
           StringBuffer buf = new StringBuffer();
           componentsService.dump(buf, "");
           log.debug(buf.toString());
       }
       
       Node siteMapNode = getValueProvider().getJcrNode().getNode(Configuration.NODEPATH_HST_SITEMAP);
       this.siteMapService = new JCRSiteMapService(siteMapNode, componentsService);
       
       if(log.isDebugEnabled()){
           StringBuffer buf = new StringBuffer();
           siteMapService.dump(buf, "");
           log.debug(buf.toString());
       }
    }

    public HstComponents getComponents() {
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



}
