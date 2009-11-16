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
package org.hippoecm.hst.configuration.sitemap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.HstSite;
import org.hippoecm.hst.service.AbstractJCRService;
import org.hippoecm.hst.service.Service;
import org.hippoecm.hst.service.ServiceException;
import org.slf4j.LoggerFactory;

public class HstSiteMapService extends AbstractJCRService implements HstSiteMap, Service{
    
    private static final long serialVersionUID = 1L;


    private static final org.slf4j.Logger log = LoggerFactory.getLogger(HstSiteMapService.class);
    
    
    private HstSite hstSite;
    
    private String siteMapRootNodePath;
    
    private Map<String, HstSiteMapItem> rootSiteMapItems = new LinkedHashMap<String, HstSiteMapItem>();
   
    private Map<String, HstSiteMapItem> siteMapDescendants = new HashMap<String, HstSiteMapItem>();
    
    public HstSiteMapService(HstSite hstSite, Node siteMapNode) throws RepositoryException, ServiceException {
        super(siteMapNode);
        this.hstSite = hstSite;
        this.siteMapRootNodePath = siteMapNode.getPath();
        
        if(!siteMapNode.isNodeType(HstNodeTypes.NODETYPE_HST_SITEMAP)) {
            throw new ServiceException("Cannot create SitemapServiceImpl: Expected nodeType '"+HstNodeTypes.NODETYPE_HST_SITEMAP+"' but was '"+siteMapNode.getPrimaryNodeType().getName()+"'");
        }
        
        // initialize all sitemap items
        init(siteMapNode);
        
        // add lookups to any descendant sitemap item
        for(HstSiteMapItem child : this.rootSiteMapItems.values()) {
            populateDescendants(child);
        }
        
    }
    
    private void init(Node siteMapNode) throws RepositoryException {
        for(NodeIterator nodeIt = siteMapNode.getNodes(); nodeIt.hasNext();) {
            Node child = nodeIt.nextNode();
            if(child == null) {
                log.warn("skipping null node");
                continue;
            }
            if(child.isNodeType(HstNodeTypes.NODETYPE_HST_SITEMAPITEM)) {
                try {
                    HstSiteMapItemService siteMapItemService = new HstSiteMapItemService(child, siteMapRootNodePath, null, this, 1);
                    rootSiteMapItems.put(siteMapItemService.getValue(), siteMapItemService);
                } catch (ServiceException e) {
                    if (log.isDebugEnabled()) {
                        log.warn("Skipping root sitemap '{}'", child.getPath(), e);
                    } else if (log.isWarnEnabled()) {
                        log.warn("Skipping root sitemap '{}'", child.getPath());
                    }
                }
                
            } else {
                if (log.isWarnEnabled()) {
                    log.warn("Skipping node '{}' because is not of type {}", child.getPath(), HstNodeTypes.NODETYPE_HST_SITEMAPITEM);
                }
            }
        }
    }

    
    private void populateDescendants(HstSiteMapItem hstSiteMapItem) {
        siteMapDescendants.put(hstSiteMapItem.getId(), hstSiteMapItem);
        for(HstSiteMapItem child : hstSiteMapItem.getChildren()) {
            populateDescendants(child);
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
        return Collections.unmodifiableList(new ArrayList<HstSiteMapItem>(rootSiteMapItems.values()));
    }

    public HstSite getSite() {
        return this.hstSite;
    }


}
