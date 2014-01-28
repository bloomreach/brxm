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

import java.util.List;

public interface HstSiteMenuConfiguration {
    /**
     * Returns the name of this SiteMenuConfiguration. For example, you could have a "topmenu", "leftmenu" and "footermenu" on your site,
     * where these names might be appropriate . It must be unique within a <code>{@link HstSiteMenusConfiguration}</code> container.
     * @return the name of this SiteMenuConfiguration
     */
    String getName();
    
    /**
     * @return returns all direct child {@link HstSiteMenuItemConfiguration}'s of this SiteMenuConfiguration
     */
    List<HstSiteMenuItemConfiguration> getSiteMenuConfigurationItems();
    
    /**
     * 
     * @return the <code>{@link HstSiteMenusConfiguration}</code> which is the container for this <code>HstSiteMenuConfiguration</code>
     */
    HstSiteMenusConfiguration getSiteMenusConfiguration();

}
