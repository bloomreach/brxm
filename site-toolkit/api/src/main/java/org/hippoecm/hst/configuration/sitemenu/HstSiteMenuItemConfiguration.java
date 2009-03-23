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

import java.io.Serializable;
import java.util.List;

import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.service.ServiceException;

/**
 * Implementations should return an unmodifiable map for {@link #getSiteMenuItemConfigurations()} because clients should not
 * be able to modify the configuration
 *
 */
public interface HstSiteMenuItemConfiguration extends Serializable{
    /**
     * 
     * @return the name of this SiteMenuItem
     */
    String getName();
    
    /**
     * @return the container <code>HstSiteMenuConfiguration</code> of this <code>HstSiteMenuItemConfiguration</code>
     */
    HstSiteMenuConfiguration getHstSiteMenuConfiguration();
    
    /**
     * 
     * @return all direct child <code>SiteMenuItemConfiguration</code>'s of this item
     */
    List<HstSiteMenuItemConfiguration> getChildItemConfigurations();
    
    
    /**
     * 
     * @return the parent <code>HstSiteMenuItemConfiguration</code> and <code>null</code> is none exists (ie, it is a root)
     */
    HstSiteMenuItemConfiguration getParentItemConfiguration();
    
    /**
     * If there is a HstSiteMapItem associated with this HstSiteMenuItemConfiguration, it can be accessed by this method. 
     * If no HstSiteMapItem is associated, there is no possible link to this HstSiteMenuItemConfiguration. This might be for 
     * non-linkeable HstSiteMapItem's only meant for holding child HstSiteMapItem's which are linkeable
     * @return the <code>{@link HstSiteMapItem}</code> this SiteMenuItemConfiguration points to or <code>null</code>. 
     */
    HstSiteMapItem getHstSiteMapItem();
    
}
