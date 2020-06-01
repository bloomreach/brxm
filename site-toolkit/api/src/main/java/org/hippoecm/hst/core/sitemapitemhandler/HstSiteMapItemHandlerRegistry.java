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
package org.hippoecm.hst.core.sitemapitemhandler;

import org.hippoecm.hst.core.container.HstContainerConfig;

/**
 * The HstSiteMapItemHandlerRegistry registry interface
 * 
 */
public interface HstSiteMapItemHandlerRegistry {

    /**
     * Registers the HstSiteMapItemHandler. The key is the pair of container configuration and handle ID.
     * 
     * @param requestContainerConfig the container configuration
     * @param siteMapItemHandlerId the siteMapItemHandler ID
     * @param siteMapItemHandler
     */
    void registerSiteMapItemHandler(HstContainerConfig requestContainerConfig, String siteMapItemHandlerId, HstSiteMapItemHandler siteMapItemHandler);
    
    /**
     * Unregister the HstSiteMapItemHandler. The key is the pair of container configuration and handle ID.
     * 
     * @param requestContainerConfig the container configuration
     * @param siteMapItemHandlerId the handle ID
     */
    void unregisterSiteMapItemHandler(HstContainerConfig requestContainerConfig, String siteMapItemHandlerId);

    /**
     * Returns the registered HstSiteMapItemHandler. The key is the pair of container configuration and siteMapItemHandler ID.
     * 
     * @param requestContainerConfig the container configuration
     * @param siteMapItemHandlerId the siteMapItemHandler ID
     * @return the SiteMapItemHandler registered with the key pair.
     */
    HstSiteMapItemHandler getSiteMapItemHandler(HstContainerConfig requestContainerConfig, String siteMapItemHandlerId);
    
    /**
     * Unregisters all the SiteMapItemHandlers.
     */
    void unregisterAllSiteMapItemHandlers();
    
}
