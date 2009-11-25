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
package org.hippoecm.hst.configuration;

import org.hippoecm.hst.configuration.components.HstComponentsConfiguration;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenusConfiguration;

/**
 * The <code>HstSite</code> object is the object representing a site. It contains a reference to the site content base
 * path returned by {@link #getContentPath()}, to the site's components configuration returned by 
 * <code>{@link #getComponentsConfiguration()}</code> and the site's sitemap, returned by <code>{@link #getSiteMap()}</code> and the site's
 * <code>{@link LocationMapTree}</code> return by <code>{@link #getLocationMapTree()}</code>
 * 
 */

public interface HstSite {

    /**
     * Each contained <code>HstSite</code> within its <code>HstSites</code> container has a unique name. <code>getName()</code>
     * returns this unique name. 
     * @return the unique name for this <code>HstSite</code> within its <code>HstSites</code> container. 
     */
    String getName();

    /**
     * Returns the absolute content path for this <code>HstSite</code> 
     * @return The absolute content path for this <code>HstSite</code>
     */
    String getContentPath();
    
    /**
     * Returns the absolute canonical content path for this <code>HstSite</code>
     * 
     * This path differs from {@link #getContentPath()} when the {@link #getContentPath()} points to a mirror node.
     * @return @return The absolute absolute content path for this <code>HstSite</code>
     */
    String getCanonicalContentPath();
    
    /**
     * @return the componentsConfiguration for this <code>HstSite</code>
     */
    HstComponentsConfiguration getComponentsConfiguration();

    /**
     * @return the hstSiteMap for this <code>HstSite</code>
     */
    HstSiteMap getSiteMap();
    
    /**
     * Returns the configured {@link HstSiteMenusConfiguration} for this HstSite or <code>null</code> if this <code>HstSite</code> does
     * not make use of a HstSiteMenusConfiguration
     * @return the {@link HstSiteMenusConfiguration} for this HstSite or <code>null</code> 
     */
    HstSiteMenusConfiguration getSiteMenusConfiguration();

    /**
     * @return the hstSites object that is the container for this <code>HstSite</code>
     */
    HstSites getHstSites();
    
}
