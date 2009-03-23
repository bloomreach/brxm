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
package org.hippoecm.hst.configuration.sitemenu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.Configuration;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstSiteMenuItemConfigurationService implements HstSiteMenuItemConfiguration {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(HstSiteMenuItemConfigurationService.class);
    
    private HstSiteMenuConfiguration hstSiteMenuConfiguration;
    private HstSiteMenuItemConfiguration parent;
    private String name;
    private List<HstSiteMenuItemConfiguration> childItems = new ArrayList<HstSiteMenuItemConfiguration>();
    private HstSiteMap hstSiteMap;
    private HstSiteMapItem hstSiteMapItem;
    
    public HstSiteMenuItemConfigurationService(Node siteMenuItem, HstSiteMenuItemConfiguration parent, HstSiteMenuConfiguration hstSiteMenuConfiguration) throws ServiceException {
        this.parent = parent;
        this.hstSiteMenuConfiguration = hstSiteMenuConfiguration;
        this.hstSiteMap = hstSiteMenuConfiguration.getSiteMenusConfiguration().getSite().getSiteMap();
        try {
            this.name = siteMenuItem.getName();
            init(siteMenuItem);
        } catch (RepositoryException e) {
            throw new ServiceException("Repository Exception occured '" + e.getMessage() + "'");
        }
        // if there is a hstSiteMapItem found, let's add this HstSiteMenuItemConfiguration to the Map in HstSiteMenusConfiguration
        if(this.hstSiteMapItem != null) {
            HstSiteMenusConfigurationService siteMenusConfiguration =  (HstSiteMenusConfigurationService)hstSiteMenuConfiguration.getSiteMenusConfiguration();
            siteMenusConfiguration.addHstSiteMenuItem(hstSiteMapItem.getId(), this);
        }
    }

    private void init(Node siteMenuItem) throws ServiceException{
        try {
            if(siteMenuItem.hasProperty(Configuration.SITEMENUITEM_PROPERTY_REFERENCESITEMAPITEM)) {
               String siteMapItemId = siteMenuItem.getProperty(Configuration.SITEMENUITEM_PROPERTY_REFERENCESITEMAPITEM).getString();
               HstSiteMapItem siteMapItem = hstSiteMap.getSiteMapItemById(siteMapItemId);
               if(siteMapItem == null) {
                   log.warn("HstSiteMenuItemConfiguration cannot be used for linking because associated HstSiteMapItem '{}' cannot be resolved", siteMapItemId);  
               } else {
                   this.hstSiteMapItem = siteMapItem;
               }
            } else {
               log.info("HstSiteMenuItemConfiguration cannot be used for linking because no associated HstSiteMapItem present"); 
            }
        } catch (RepositoryException e) {
            throw new ServiceException("ServiceException while initializing HstSiteMenuItemConfiguration.", e);
        }
    }

    public List<HstSiteMenuItemConfiguration> getChildItemConfigurations() {
        return Collections.unmodifiableList(childItems);
    }

    public String getName() {
        return this.name;
    }

    public HstSiteMenuItemConfiguration getParentItemConfiguration() {
        return this.parent;
    }

    public HstSiteMenuConfiguration getHstSiteMenuConfiguration() {
        return this.hstSiteMenuConfiguration;
    }

    public HstSiteMapItem getHstSiteMapItem() {
        return this.hstSiteMapItem;
    }

}
