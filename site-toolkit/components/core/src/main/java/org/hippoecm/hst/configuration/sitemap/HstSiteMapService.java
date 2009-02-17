package org.hippoecm.hst.configuration.sitemap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.Configuration;
import org.hippoecm.hst.configuration.HstSite;
import org.hippoecm.hst.service.AbstractJCRService;
import org.hippoecm.hst.service.Service;
import org.hippoecm.hst.service.ServiceException;
import org.slf4j.LoggerFactory;

public class HstSiteMapService extends AbstractJCRService implements HstSiteMap, Service{
    
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(HstSiteMap.class);
    
    
    private HstSite hstSite;
    
    private String siteMapRootNodePath;
    
    private Map<String, HstSiteMapItem> rootSiteMapItems = new HashMap<String, HstSiteMapItem>();
    
    private Map<String, HstSiteMapItem> siteMapDescendants = new HashMap<String, HstSiteMapItem>();
    
    public HstSiteMapService(HstSite hstSite, Node siteMapNode) throws RepositoryException, ServiceException {
        super(siteMapNode);
        this.hstSite = hstSite;
        this.siteMapRootNodePath = siteMapNode.getPath();
        
        if(!siteMapNode.isNodeType(Configuration.NODETYPE_HST_SITEMAP)) {
            throw new ServiceException("Cannot create SitemapServiceImpl: Expected nodeType '"+Configuration.NODETYPE_HST_SITEMAP+"' but was '"+siteMapNode.getPrimaryNodeType().getName()+"'");
        }
        
        // initialize all sitemap items
        init(siteMapNode);
        
        // add lookups to any descendant sitemap item
        for(Iterator<HstSiteMapItem> childsIt = this.rootSiteMapItems.values().iterator(); childsIt.hasNext();) {
            populateDescendants(childsIt.next());
        }
        
    }
    
    private void init(Node siteMapNode) throws RepositoryException {
        for(NodeIterator nodeIt = siteMapNode.getNodes(); nodeIt.hasNext();) {
            Node child = nodeIt.nextNode();
            if(child == null) {
                log.warn("skipping null node");
                continue;
            }
            if(child.isNodeType(Configuration.NODETYPE_HST_SITEMAPITEM)) {
                try {
                    HstSiteMapItemService siteMapItemService = new HstSiteMapItemService(child, siteMapRootNodePath, null, this);
                    rootSiteMapItems.put(siteMapItemService.getValue(), siteMapItemService);
                } catch (ServiceException e) {
                    log.warn("Skipping root sitemap '{}'", child.getPath(), e);
                }
                
            } else {
                log.warn("Skipping node '{}' because is not of type {}", child.getPath(), Configuration.NODETYPE_HST_SITEMAPITEM);
            }
        }
    }

    
    private void populateDescendants(HstSiteMapItem hstSiteMapItem) {
        siteMapDescendants.put(hstSiteMapItem.getId(), hstSiteMapItem);
        for(Iterator<HstSiteMapItem> childsIt = hstSiteMapItem.getChildren().iterator(); childsIt.hasNext();) {
            populateDescendants(childsIt.next());
        }
    }
    
    public Service[] getChildServices() {
        return rootSiteMapItems.values().toArray(new Service[rootSiteMapItems.size()]);
    }

    public HstSiteMapItem getSiteMapItem(String value) {
        return rootSiteMapItems.get(value);
    }
    

    public HstSiteMapItem getSiteMapItemById(String id) {
        return siteMapDescendants.get(id);
    }


    public List<HstSiteMapItem> getSiteMapItems() {
        return new ArrayList<HstSiteMapItem>(rootSiteMapItems.values());
    }

    public HstSite getSite() {
        return this.hstSite;
    }
    
    
    


}
