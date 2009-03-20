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
package org.hippoecm.hst.sitemenu;

import java.io.Serializable;
import java.util.Map;

import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * Implementation of this interface is the container of all the <code>{@link SiteMenu}</code>'s that are needed in the frontend.
 * 
 * As the implementation will be available (at least, if configured to be so) on the {@link HstRequestContext}, the Map returned by 
 * {@link #getSiteMenus()} would best be an unmodifiable map, as the client, for instance a {@link HstComponent} instance should not be 
 * able to change the SiteMenus, though, this is up to implementation
 */
public interface SiteMenus extends Serializable{

    /**
     * @return the available {@link SiteMenu}'s as a (recommended unmodifiable) map in this SiteMenus impl
     */
    Map<String, SiteMenu> getSiteMenus();
    
    /**
     * 
     * @param name the name of the {@link SiteMenu}
     * @return the {@link SiteMenu} having the correct name
     */
    SiteMenu getSiteMenu(String name);
    
}
