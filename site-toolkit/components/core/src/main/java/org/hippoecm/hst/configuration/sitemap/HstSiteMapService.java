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

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemapitemhandlers.HstSiteMapItemHandlersConfiguration;
import org.hippoecm.hst.service.Service;
import org.hippoecm.hst.service.ServiceException;
import org.slf4j.LoggerFactory;

public class HstSiteMapService implements HstSiteMap {
    
    private static final long serialVersionUID = 1L;


    private static final org.slf4j.Logger log = LoggerFactory.getLogger(HstSiteMapService.class);
    
    
    private HstSite hstSite;
    
    private String siteMapRootNodePath;
    
    private Map<String, HstSiteMapItem> rootSiteMapItems = new LinkedHashMap<String, HstSiteMapItem>();
   
    private Map<String, HstSiteMapItem> siteMapDescendants = new HashMap<String, HstSiteMapItem>();
    
    public HstSiteMapService(HstSite hstSite, HstNode siteMapNode, HstSiteMapItemHandlersConfiguration siteMapItemHandlersConfiguration) throws ServiceException {
        this.hstSite = hstSite;
        this.siteMapRootNodePath = siteMapNode.getValueProvider().getPath();
       
        if(!HstNodeTypes.NODETYPE_HST_SITEMAP.equals(siteMapNode.getNodeTypeName())) {
            throw new ServiceException("Cannot create SitemapServiceImpl: Expected nodeType '"+HstNodeTypes.NODETYPE_HST_SITEMAP+"' but was '"+siteMapNode.getNodeTypeName()+"'");
        }
        
        // initialize all sitemap items
        for(HstNode child : siteMapNode.getNodes()) {
            if(HstNodeTypes.NODETYPE_HST_SITEMAPITEM.equals(child.getNodeTypeName())) {
                try {
                    HstSiteMapItemService siteMapItemService = new HstSiteMapItemService(child, siteMapRootNodePath, siteMapItemHandlersConfiguration , null, this, 1);
                    rootSiteMapItems.put(siteMapItemService.getValue(), siteMapItemService);
                } catch (ServiceException e) {
                    if (log.isDebugEnabled()) {
                        log.warn("Skipping root sitemap '{}'", child.getValueProvider().getPath(), e);
                    } else if (log.isWarnEnabled()) {
                        log.warn("Skipping root sitemap '{}'", child.getValueProvider().getPath());
                    }
                }
            } else {
                log.warn("Skipping node '{}' because is not of type {}", child.getValueProvider().getPath(), HstNodeTypes.NODETYPE_HST_SITEMAPITEM);
            }
        }
        
        // add lookups to any descendant sitemap item
        for(HstSiteMapItem child : this.rootSiteMapItems.values()) {
            populateDescendants(child);
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
