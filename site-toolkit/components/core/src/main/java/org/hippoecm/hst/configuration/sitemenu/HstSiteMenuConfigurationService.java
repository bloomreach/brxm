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

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstSiteMenuConfigurationService implements HstSiteMenuConfiguration {

    private static final long serialVersionUID = 1L;
    
    private static final Logger log = LoggerFactory.getLogger(HstSiteMenuConfigurationService.class);
    private String name;
    private HstSiteMenusConfiguration hstSiteMenusConfiguration;
    private List<HstSiteMenuItemConfiguration> siteMenuItems = new ArrayList<HstSiteMenuItemConfiguration>();

    public HstSiteMenuConfigurationService(HstSiteMenusConfiguration hstSiteMenusConfiguration, Node siteMenu) throws ServiceException{
        this.hstSiteMenusConfiguration = hstSiteMenusConfiguration;
        try {
            this.name = siteMenu.getName();
            init(siteMenu);
        } catch (RepositoryException e) {
            throw new ServiceException("Repository Exception occured '" + e.getMessage() + "'");
        }
        
    }

    private void init(Node siteMenu) throws RepositoryException {
        NodeIterator siteMenuItemsIt = siteMenu.getNodes();
        while(siteMenuItemsIt.hasNext()) {
            Node siteMenuItem = siteMenuItemsIt.nextNode();
            if(siteMenuItem == null) {
                continue;
            }
            if(siteMenuItem.isNodeType(HstNodeTypes.NODETYPE_HST_SITEMENUITEM)) {
                try {
                    HstSiteMenuItemConfiguration siteMenuItemConfiguration = new HstSiteMenuItemConfigurationService(siteMenuItem, null, this);
                    siteMenuItems.add(siteMenuItemConfiguration);
                } catch(ServiceException e) {
                    log.warn("Skipping siteMenuItemConfiguration for '{}' : '{}'", siteMenuItem.getPath(), e.toString());
                }
            } else {
                log.error("Skipping siteMenuItem '{}' because not of type '{}'", siteMenuItem.getPath(), HstNodeTypes.NODETYPE_HST_SITEMENUITEM);
            }
        }
    }

    public String getName() {
        return this.name;
    }

    public List<HstSiteMenuItemConfiguration> getSiteMenuConfigurationItems() {
        return Collections.unmodifiableList(siteMenuItems);
    }

    public HstSiteMenusConfiguration getSiteMenusConfiguration() {
        return hstSiteMenusConfiguration;
    }

}
