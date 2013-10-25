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
package org.hippoecm.hst.configuration.sitemenu;

import java.util.Map;

import org.hippoecm.hst.configuration.site.HstSite;

/**
 * Implementations should return an unmodifiable map for {@link #getSiteMenuConfigurations()} because clients should not
 * be able to modify the configuration
 *
 */
public interface HstSiteMenusConfiguration {
    
    /**
     * Return the {@link HstSite} this <code>HstSiteMenusConfiguration</code> belongs to.
     * @return the site this <code>HstSiteMenusConfiguration</code> belongs to
     */
    HstSite getSite();
    
    /**
     * Returns the map containing all <code>HstSiteMenuConfiguration</code>'s and an empty map if there are no <code>HstSiteMenuConfiguration</code>'s.
     * <p/>
     * Note: implementation should better return an unmodifiable map to make sure clients cannot modify the map 
     * @return map containing all <code>HstSiteMenuConfiguration</code>'s and an empty map if there are no <code>HstSiteMenuConfiguration</code>'s 
     */
    Map<String, HstSiteMenuConfiguration> getSiteMenuConfigurations();
    
    /**
     * 
     * @param name the name of the {@link HstSiteMenuConfiguration}
     * @return the {@link HstSiteMenuConfiguration} with this name and <code>null</code> if does not exist
     */
    HstSiteMenuConfiguration getSiteMenuConfiguration(String name);
 
}
