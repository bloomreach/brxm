package org.hippoecm.hst.configuration;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.pagemapping.JCRPageMappingService;
import org.hippoecm.hst.configuration.pagemapping.PageMapping;
import org.hippoecm.hst.configuration.sitemap.JCRSiteMapService;
import org.hippoecm.hst.configuration.sitemap.SiteMapService;
import org.hippoecm.hst.service.AbstractJCRService;
import org.hippoecm.hst.service.Service;
import org.hippoecm.hst.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JCRHstConfigurationService extends AbstractJCRService implements HstConfigurationService{

    private SiteMapService siteMapService;
    private PageMapping pageMappingService;
    
    private static final Logger log = LoggerFactory.getLogger(HstConfigurationService.class);
    
    
    public JCRHstConfigurationService(Node jcrNode) throws ServiceException, RepositoryException{
        super(jcrNode);
        if(!jcrNode.isNodeType(Configuration.NODETYPE_HST_CONFIGURATION)) {
            throw new ServiceException("Cannot create HstConfigurationServiceImpl: Expected nodeType '"+Configuration.NODETYPE_HST_CONFIGURATION+"' but found '"+jcrNode.getPrimaryNodeType().getName()+"'");
        }
        init();
        
        /*
         * After initialization, all needed jcr properties and nodes have to be loaded. The underlying jcr nodes in 
         * the value providers now will all be closed.
         */
        this.closeValueProvider(true);
    }

    public Service[] getChildServices() {
       Service[] services = {siteMapService,pageMappingService};
       return services;
    }
    
    private void init() throws PathNotFoundException, RepositoryException, ServiceException {
       Node pageMappingNode = getValueProvider().getJcrNode().getNode(Configuration.NODEPATH_HST_PAGEMAPPING); 
       this.pageMappingService = new JCRPageMappingService(pageMappingNode); 
       
       if(log.isDebugEnabled()){
           StringBuffer buf = new StringBuffer();
           pageMappingService.dump(buf, "");
           log.debug(buf.toString());
       }
       
       Node siteMapNode = getValueProvider().getJcrNode().getNode(Configuration.NODEPATH_HST_SITEMAP);
       this.siteMapService = new JCRSiteMapService(siteMapNode, pageMappingService);
       
       if(log.isDebugEnabled()){
           StringBuffer buf = new StringBuffer();
           siteMapService.dump(buf, "");
           log.debug(buf.toString());
       }
    }

    public PageMapping getPageMappingService() {
        return this.pageMappingService;
    }

    public SiteMapService getSiteMapService() {
       return this.siteMapService;
    }



}
