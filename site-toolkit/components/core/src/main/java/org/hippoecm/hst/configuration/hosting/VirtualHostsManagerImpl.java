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
package org.hippoecm.hst.configuration.hosting;

import org.hippoecm.hst.configuration.model.HstWebSitesManager;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.RepositoryNotAvailableException;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandlerFactory;
import org.hippoecm.hst.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VirtualHostsManagerImpl implements VirtualHostsManager {

    private static final Logger log = LoggerFactory.getLogger(VirtualHostsManagerImpl.class);
    
    
    private VirtualHosts virtualHosts;
    private HstURLFactory urlFactory;
    private HstSiteMapMatcher siteMapMatcher;
    private HstSiteMapItemHandlerFactory siteMapItemHandlerFactory;
    private HstWebSitesManager hstWebSitesManager;
    
    
    public HstURLFactory getUrlFactory() {
        return this.urlFactory;
    }
    
    public void setHstWebSitesManager(HstWebSitesManager hstWebSitesManager) {
        this.hstWebSitesManager = hstWebSitesManager;
    }
    
    public void setUrlFactory(HstURLFactory urlFactory) {
        this.urlFactory = urlFactory;
    }
    
    public void setSiteMapMatcher(HstSiteMapMatcher siteMapMatcher) {
        this.siteMapMatcher = siteMapMatcher;
    }
    
    public HstSiteMapMatcher getSiteMapMatcher() {
        return siteMapMatcher;
    }
    
    public void setSiteMapItemHandlerFactory(HstSiteMapItemHandlerFactory siteMapItemHandlerFactory) {
        this.siteMapItemHandlerFactory = siteMapItemHandlerFactory;
    }
    
    public HstSiteMapItemHandlerFactory getSiteMapItemHandlerFactory() {
        return siteMapItemHandlerFactory;
    }
    
    public VirtualHosts getVirtualHosts() throws RepositoryNotAvailableException{
        VirtualHosts vHosts = this.virtualHosts;
        
        if (vHosts == null) {
            synchronized(this) {
                buildVirtualHosts();
                vHosts = this.virtualHosts;
            }
        }
        
        return vHosts;
    }

    protected synchronized void buildVirtualHosts() throws RepositoryNotAvailableException{
        if (this.virtualHosts != null) {
            return;
        }
        
        hstWebSitesManager.populate();

        try {
            this.virtualHosts = new VirtualHostsService(hstWebSitesManager.getVirtualHostsNode(), this, hstWebSitesManager);

        } catch (ServiceException e) {
            // TODO!!!!
            System.out.println(e);
            e.printStackTrace();
        }
    }
    
    public synchronized void invalidate(String path) {
        this.virtualHosts = null;
    }

}
