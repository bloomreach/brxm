/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import org.hippoecm.hst.configuration.ConfigurationUtils;
import org.hippoecm.hst.configuration.internal.CanonicalInfo;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.cache.CompositeConfigurationNodes;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.configuration.model.ModelLoadingException;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.site.MountSiteMapConfiguration;
import org.hippoecm.hst.configuration.sitemapitemhandlers.HstSiteMapItemHandlersConfiguration;
import org.hippoecm.hst.service.Service;
import org.hippoecm.hst.util.DuplicateKeyNotAllowedHashMap;
import org.slf4j.LoggerFactory;

public class HstSiteMapService implements HstSiteMap, CanonicalInfo {
    
    private static final long serialVersionUID = 1L;


    private static final org.slf4j.Logger log = LoggerFactory.getLogger(HstSiteMapService.class);

    private final String canonicalIdentifier;
    private final String canonicalPath;

    private final boolean workspaceConfiguration;
    
    private HstSite hstSite;
    
    private Map<String, HstSiteMapItem> rootSiteMapItems = new LinkedHashMap<String, HstSiteMapItem>();
   
    /*
     * The map of HstSiteMapItem where the key is HstSiteMapItem#getId()
     */
    private Map<String, HstSiteMapItem> siteMapDescendants = new DuplicateKeyNotAllowedHashMap<String, HstSiteMapItem>();
    
    /*
     * The map of HstSiteMapItem where the key is HstSiteMapItem#getRefId(). Only HstSiteMapItem that have a refId are added to this 
     * map. When duplicate key's are tried to be put, an error is logged
     */
    private Map<String, HstSiteMapItem> siteMapDescendantsByRefId = new HashMap<String, HstSiteMapItem>();
    
    public HstSiteMapService(final HstSite hstSite,
                             final CompositeConfigurationNodes.CompositeConfigurationNode siteMapNode,
                             final MountSiteMapConfiguration mountSiteMapConfiguration,
                             final HstSiteMapItemHandlersConfiguration siteMapItemHandlersConfiguration) throws ModelLoadingException {
        this.hstSite = hstSite;

        canonicalIdentifier = siteMapNode.getMainConfigNode().getValueProvider().getIdentifier();
        canonicalPath = siteMapNode.getMainConfigNode().getValueProvider().getPath();

        workspaceConfiguration = ConfigurationUtils.isWorkspaceConfig(siteMapNode.getMainConfigNode());

        // initialize all sitemap items
        for(HstNode child : siteMapNode.getCompositeChildren().values()) {
            if ("deleted".equals(child.getValueProvider().getString(HstNodeTypes.EDITABLE_PROPERTY_STATE))) {
                log.debug("SKipping marked deleted node {}", child.getValueProvider().getPath());
                continue;
            }
            if(HstNodeTypes.NODETYPE_HST_SITEMAPITEM.equals(child.getNodeTypeName())) {
                try {
                    HstSiteMapItemService siteMapItemService = new HstSiteMapItemService(child, mountSiteMapConfiguration, siteMapItemHandlersConfiguration , null, this, 1);
                    rootSiteMapItems.put(siteMapItemService.getValue(), siteMapItemService);
                } catch (ModelLoadingException e) {
                    if (log.isDebugEnabled()) {
                        log.warn("Skipping root sitemap '{}'", child.getValueProvider().getPath(), e);
                    } else if (log.isWarnEnabled()) {
                        log.warn("Skipping root sitemap '{}' : '{}'", child.getValueProvider().getPath(), e.getMessage());
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

        for(HstSiteMapItem child : this.rootSiteMapItems.values()) {
            ((HstSiteMapItemService)child).optimize();
        }
        
    }

    private void populateDescendants(HstSiteMapItem hstSiteMapItem)  throws ModelLoadingException {
        try {
            siteMapDescendants.put(hstSiteMapItem.getId(), hstSiteMapItem);
        } catch (IllegalArgumentException e) {
           throw new ModelLoadingException("HstSiteMapItem with already existing id encountered. Not allowed to have duplicate id's within one HstSiteMap. Duplicate id = '"+hstSiteMapItem.getId()+"'" , e);
        }
        if(hstSiteMapItem.getRefId() != null) {
            HstSiteMapItem prevValue =  siteMapDescendantsByRefId.put(hstSiteMapItem.getRefId(), hstSiteMapItem);
            if(prevValue != null) {
                log.warn("HstSiteMapItem with already existing refId encountered. Not allowed to have duplicate refId's within one HstSiteMap. Duplicate refId = '{}' for HstSiteMapItem with id='{}'. Previous HstSiteMapItem with same refId is replaced.",hstSiteMapItem.getRefId(), hstSiteMapItem.getId());
            }
        }
        for(HstSiteMapItem child : hstSiteMapItem.getChildren()) {
            populateDescendants(child);
        }
    }

    @Override
    public String getCanonicalIdentifier() {
        return canonicalIdentifier;
    }

    @Override
    public String getCanonicalPath() {
        return canonicalPath;
    }

    @Override
    public boolean isWorkspaceConfiguration() {
        return workspaceConfiguration;
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

    public HstSiteMapItem getSiteMapItemByRefId(String refId) {
        return siteMapDescendantsByRefId.get(refId);
    }


    public List<HstSiteMapItem> getSiteMapItems() {
        return Collections.unmodifiableList(new ArrayList<HstSiteMapItem>(rootSiteMapItems.values()));
    }

    public HstSite getSite() {
        return this.hstSite;
    }

}
