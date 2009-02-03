package org.hippoecm.hst.configuration.sitemap;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.Configuration;
import org.hippoecm.hst.configuration.components.HstComponent;
import org.hippoecm.hst.configuration.components.HstComponents;
import org.hippoecm.hst.core.mapping.UrlUtilities;
import org.hippoecm.hst.service.AbstractJCRService;
import org.hippoecm.hst.service.Service;
import org.hippoecm.hst.service.ServiceException;
import org.slf4j.LoggerFactory;

public class JCRSiteMapService extends AbstractJCRService implements HstSiteMap{
    
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(HstSiteMap.class);
    
    private DataSourceTree dataSourceTree;
    
    private HstComponents pageMappingService;
    
    private HstSiteMapItem rootSiteMapItemService; 
    
    public JCRSiteMapService(Node siteMapNode, HstComponents pageMappingService) throws RepositoryException, ServiceException {
        super(siteMapNode);
        this.pageMappingService = pageMappingService;
        if(!siteMapNode.isNodeType(Configuration.NODETYPE_HST_SITEMAP)) {
            throw new ServiceException("Cannot create SitemapServiceImpl: Expected nodeType '"+Configuration.NODETYPE_HST_SITEMAP+"' but was '"+siteMapNode.getPrimaryNodeType().getName()+"'");
        }
        rootSiteMapItemService = new JCRSiteMapItemService(siteMapNode,null);
        populate(siteMapNode, (JCRSiteMapItemService)rootSiteMapItemService);
        
    }


    public Service[] getChildServices() {
        return rootSiteMapItemService.getChildServices();
    }

    private void populate(Node siteMapNode, JCRSiteMapItemService parentSiteMapItemService) throws RepositoryException {
        for(NodeIterator nodeIt = siteMapNode.getNodes(); nodeIt.hasNext();) {
            Node child = nodeIt.nextNode();
            if(child == null) {
                log.warn("skipping null node");
                continue;
            }
            if(child.isNodeType(Configuration.NODETYPE_HST_SITEMAPITEM)) {
                JCRSiteMapItemService siteMapItemService = new JCRSiteMapItemService(child,parentSiteMapItemService);
                String componentLocation = siteMapItemService.getComponentLocation();
                if(componentLocation != null) {
                    HstComponent componentService = pageMappingService.getComponent(componentLocation);
                    if(componentService != null) {
                        log.debug("Adding componentService for component location '{}'", componentLocation);
                        siteMapItemService.setComponentService(componentService);
                    } else {
                        log.warn("No ComponentService found for component location '{}' for node '{}'", componentLocation, siteMapItemService.getValueProvider().getPath());
                    }
                }
                parentSiteMapItemService.addChild(siteMapItemService);
                populate(child, siteMapItemService);
            } else {
                log.warn("Skipping node '{}' because is not of type {}", child.getPath(), Configuration.NODETYPE_HST_SITEMAPITEM);
            }
        }
    }

    public HstMatchingSiteMapItem match(String url) {
        url = UrlUtilities.normalizeUrl(url);
        String[] tokens  = url.split("/");
        HstSiteMapItem currentSiteMapItemService = rootSiteMapItemService;
        for(String token : tokens) {
            HstSiteMapItem match = currentSiteMapItemService.getChild(token);
            if(match != null) {
                currentSiteMapItemService = match;
            } else {
                // token is not part of the sitemap structure anymore, but part of the repository source
                break;
            }
        }
        if(currentSiteMapItemService == rootSiteMapItemService) {
            log.warn("Did not find matching child node SiteMapItem for '{}'. Return null.", url);
            return null;
        }
        
        String siteMapItemUrl = UrlUtilities.normalizeUrl(currentSiteMapItemService.getUrl());
        
        if(!url.startsWith(siteMapItemUrl)){
            log.error("Impossible found SiteMapItemService as its url does not start with the url to match. Return null");
            return null;
        }
        return new JCRMatchingSiteMapItemService(currentSiteMapItemService,url.substring(siteMapItemUrl.length()) );
    }
    
    
    
    public class DataSourceTree {
        
    }



    public void dump(StringBuffer buf, String indent) {
        
        buf.append("\n\n------ SiteMapService ------ \n\n");
        
        for(Service child : rootSiteMapItemService.getChildServices()) {
            if(child instanceof HstSiteMapItem)
            appendChild(buf, (HstSiteMapItem)child, "");
        }
        buf.append("\n\n------ End SiteMapService ------");
        
    }

    private void appendChild(StringBuffer buf, HstSiteMapItem child, String indent) {
        child.dump(buf, indent);
        for(Service s : child.getChildServices()) {
            if(s instanceof HstSiteMapItem)
            appendChild(buf, (HstSiteMapItem)s, indent + "\t");
        }
    }


}
