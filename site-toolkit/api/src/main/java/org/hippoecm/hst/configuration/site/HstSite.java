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
package org.hippoecm.hst.configuration.site;

import org.hippoecm.hst.configuration.components.HstComponentsConfiguration;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemapitemhandlers.HstSiteMapItemHandlersConfiguration;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenusConfiguration;
import org.hippoecm.hst.core.linking.LocationMapTree;

/**
 * The <code>HstSite</code> object is the object representing a site. It contains a reference to the site's components configuration returned by
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
     * Returns the identifier of the backing canonical configuration node of the  HstSite.
     * 
     * @return the identifier of the backing HstSite
     */
    String getCanonicalIdentifier();
    
    /**
     * @return the siteMapItemHandlersConfiguration for this <code>HstSite</code>
     */
    HstSiteMapItemHandlersConfiguration getSiteMapItemHandlersConfiguration();
    
    /**
     * @return the componentsConfiguration for this <code>HstSite</code>
     */
    HstComponentsConfiguration getComponentsConfiguration();

    /**
     * @return the hstSiteMap for this <code>HstSite</code>
     */
    HstSiteMap getSiteMap();
    

    /**
     * The location map is an inverted version from the {@link HstSiteMap}: Instead of mapping for <code>URL</code>s to 
     * <code>relativecontentpath</code>'s, the location map is a mapping from  <code>relativecontentpath</code>s to <code>URL</code>s
     * @return the location map for this <code>HstSite</code>
     */
    LocationMapTree getLocationMapTree();
    
    /**
     * Returns the configured {@link HstSiteMenusConfiguration} for this HstSite or <code>null</code> if this <code>HstSite</code> does
     * not make use of a HstSiteMenusConfiguration
     * @return the {@link HstSiteMenusConfiguration} for this HstSite or <code>null</code> 
     */
    HstSiteMenusConfiguration getSiteMenusConfiguration();

    /**
     * @return the absolute location where the configuration for this {@link HstSite} is stored
     */
    String getConfigurationPath();

    /**
     * @return The version of hst configuration this {@link HstSite} uses. When no explicit version present, <code>-1</code>
     * is returned
     */
    long getVersion();

    /**
     * @return <code>true</code> when this {@link HstSite} has a different configuration than the live.
     */
    boolean hasPreviewConfiguration();
}
