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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.Configuration;
import org.hippoecm.hst.provider.PropertyMap;
import org.hippoecm.hst.service.AbstractJCRService;
import org.hippoecm.hst.service.Service;
import org.hippoecm.hst.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstSiteMapItemService extends AbstractJCRService implements HstSiteMapItem, Service{

    private static final Logger log = LoggerFactory.getLogger(HstSiteMapItem.class);
    
    private static final String WILDCARD = "_default_";
    
    private Map<String, HstSiteMapItem> childSiteMapItems = new HashMap<String, HstSiteMapItem>();
   
    private String siteMapRootNodePath;
    
    private String id;
    
    private String value;
    
    private String path;
    
    private String relativeContentPath;
    
    private String componentConfigurationId;
    
    private List<String> roles;
    
    private boolean isWildCard;
    
    private PropertyMap propertyMap;
    
    private HstSiteMap hstSiteMap;
    
    private HstSiteMapItem parentItem;
    
    public HstSiteMapItemService(Node jcrNode, String siteMapRootNodePath, HstSiteMapItem parentItem, HstSiteMap hstSiteMap) throws ServiceException{
        super(jcrNode);
        this.parentItem = parentItem;
        this.hstSiteMap = hstSiteMap; 
        String nodePath = getValueProvider().getPath();
        if(!getValueProvider().getPath().startsWith(siteMapRootNodePath)) {
            throw new ServiceException("Node path of the sitemap cannot start without the global sitemap root path. Skip SiteMapItem");
        }
        this.siteMapRootNodePath = siteMapRootNodePath;
        // path & id are the same
        this.id = this.path = nodePath.substring(siteMapRootNodePath.length()+1);
        // currently, the value is always the nodename
        this.value = getValueProvider().getName();
        if(WILDCARD.equals(value)) {
            this.isWildCard = true;
        }
        this.relativeContentPath = getValueProvider().getString(Configuration.SITEMAPITEM_PROPERTY_RELATIVECONTENTPATH);
        this.componentConfigurationId = getValueProvider().getString(Configuration.SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID);
        String[] rolesProp = getValueProvider().getStrings(Configuration.SITEMAPITEM_PROPERTY_ROLES);
        if(rolesProp!=null) {
            this.roles = Arrays.asList(rolesProp);
        }
        
        this.propertyMap = getValueProvider().getPropertyMap();
        
        init(jcrNode);
    }

    private void init(Node node) {
        try{
            for(NodeIterator nodeIt = node.getNodes(); nodeIt.hasNext();) {
                Node child = nodeIt.nextNode();
                if(child == null) {
                    log.warn("skipping null node");
                    continue;
                }
                if(child.isNodeType(Configuration.NODETYPE_HST_SITEMAPITEM)) {
                    try {
                        HstSiteMapItemService siteMapItemService = new HstSiteMapItemService(child, siteMapRootNodePath, this, this.hstSiteMap);
                        childSiteMapItems.put(siteMapItemService.getValue(), siteMapItemService);
                    } catch (ServiceException e) {
                        log.warn("Skipping root sitemap '{}'", child.getPath(), e);
                    }
                }
                else {
                    log.warn("Skipping node '{}' because is not of type '{}'", child.getPath(), Configuration.NODETYPE_HST_SITEMAPITEM);
                }
            } 
        } catch (RepositoryException e) {
            log.warn("Skipping SiteMap structure due to Repository Exception ", e);
        }
    }
    
    public Service[] getChildServices() {
        return childSiteMapItems.values().toArray(new Service[childSiteMapItems.size()]);
    }

    public HstSiteMapItem getChild(String value) {
        return this.childSiteMapItems.get(value);
    }

    public List<HstSiteMapItem> getChildren() {
        return new ArrayList<HstSiteMapItem>(this.childSiteMapItems.values());
    }

    public String getComponentConfigurationId() {
        return this.componentConfigurationId;
    }

    public String getId() {
        return this.id;
    }

    public String getPath() {
        return this.path;
    }

    public Map<String, Object> getProperties() {
        return this.propertyMap.getAllMapsCombined();
    }

    public String getRelativeContentPath() {
        return this.relativeContentPath;
    }

    public List<String> getRoles() {
        return this.roles;
    }

    public String getValue() {
        return this.value;
    }

    public boolean isWildCard() {
        return this.isWildCard;
    }

    public HstSiteMap getHstSiteMap() {
        return this.hstSiteMap;
    }

    public HstSiteMapItem getParentItem() {
        return this.parentItem;
    }

}
