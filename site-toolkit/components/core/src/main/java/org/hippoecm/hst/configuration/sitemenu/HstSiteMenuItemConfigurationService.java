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
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.Configuration;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
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
    private String siteMapItemPath;
    private int depth;
    private boolean repositoryBased;
    
    public HstSiteMenuItemConfigurationService(Node siteMenuItem, HstSiteMenuItemConfiguration parent, HstSiteMenuConfiguration hstSiteMenuConfiguration) throws ServiceException {
        this.parent = parent;
        this.hstSiteMenuConfiguration = hstSiteMenuConfiguration;
        HstSiteMap hstSiteMap = hstSiteMenuConfiguration.getSiteMenusConfiguration().getSite().getSiteMap();
        try {
            this.name = siteMenuItem.getName();
            init(siteMenuItem, hstSiteMap);
        } catch (RepositoryException e) {
            throw new ServiceException("Repository Exception occured '" + e.getMessage() + "'");
        }
    }

    private void init(Node siteMenuItem, HstSiteMap hstSiteMap) throws ServiceException{
        try {
            if(siteMenuItem.hasProperty(Configuration.SITEMENUITEM_PROPERTY_REFERENCESITEMAPITEM)) {
               // siteMapItemPath can be an exact path to a sitemap item, but can also be a path to a sitemap item containing wildcards.
               this.siteMapItemPath = siteMenuItem.getProperty(Configuration.SITEMENUITEM_PROPERTY_REFERENCESITEMAPITEM).getString();
               NodeIterator siteMenuIt = siteMenuItem.getNodes();
               while(siteMenuIt.hasNext()){
                   Node childSiteMenuItem = siteMenuIt.nextNode();
                   if(childSiteMenuItem == null) {
                       continue;
                   }
                   
                   HstSiteMenuItemConfiguration child = new HstSiteMenuItemConfigurationService(childSiteMenuItem, this, this.hstSiteMenuConfiguration);
                   childItems.add(child);
               }
            } else {
               log.info("HstSiteMenuItemConfiguration cannot be used for linking because no associated HstSiteMapItem present"); 
            }
            
            if(siteMenuItem.hasProperty(Configuration.SITEMENUITEM_PROPERTY_REPOBASED)) {
                this.repositoryBased = siteMenuItem.getProperty(Configuration.SITEMENUITEM_PROPERTY_REPOBASED).getBoolean();
            }
            
            if(siteMenuItem.hasProperty(Configuration.SITEMENUITEM_PROPERTY_DEPTH)) {
               this.depth = (int)siteMenuItem.getProperty(Configuration.SITEMENUITEM_PROPERTY_DEPTH).getLong();
            }
            
            if( (this.repositoryBased && this.depth <= 0) || (!this.repositoryBased && this.depth > 0) ) {
                this.repositoryBased =false;
                this.depth = 0;
                log.warn("Ambiguous configuration for repository based sitemenu: only when both repository based is true AND " +
                		"depth > 0 the configuration is correct for repository based navigation. Skipping repobased and depth setting for this item.");
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

    public String getSiteMapItemPath() {
        return this.siteMapItemPath;
    }

    public int getDepth() {
        return this.depth;
    }

    public boolean isRepositoryBased() {
        return this.repositoryBased;
    }

}
