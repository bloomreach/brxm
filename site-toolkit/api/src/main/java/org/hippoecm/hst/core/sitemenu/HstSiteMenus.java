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
package org.hippoecm.hst.core.sitemenu;

import java.io.Serializable;
import java.util.Map;

/**
 * Implementation of this interface is the container of all the <code>{@link HstSiteMenu}</code>'s that are needed to build site menus.
 * The implementations of this class, and of <code>{@link HstSiteMenu}</code> and <code>{@link HstSiteMenuItem}</code>, are the request context based instances of their
 * configuration equivalences, <code>{@link SiteMenusConfiguration}</code>'s, <code>{@link SiteMenuConfiguration}</code>'s and <code>{@link SiteMenuItemConfiguration}</code>'s
 * <p/>
 * The configuration parts are the request independent objects, while this package contains the request dependent instances, which typically
 * have the configuration as their template from which these instances are created. 
 */
public interface HstSiteMenus extends Serializable{

    /**
     * @return the available {@link HstSiteMenu}'s as a (recommended unmodifiable) map in this SiteMenus impl
     */
    Map<String, HstSiteMenu> getSiteMenus();
    
    /**
     * 
     * @param name the name of the {@link HstSiteMenu}
     * @return the {@link HstSiteMenu} having the corresponding name and <code>null</code> if none matches
     */
    HstSiteMenu getSiteMenu(String name);
    
}
