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

import java.util.HashMap;
import java.util.Map;

import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuConfiguration;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenusConfiguration;
import org.hippoecm.hst.core.request.HstRequestContext;

public class HstSiteMenusImpl implements HstSiteMenus {

    private static final long serialVersionUID = 1L;
    private Map<String, HstSiteMenu> siteMenus = new HashMap<String, HstSiteMenu>(); 
   
    
    public HstSiteMenusImpl(HstRequestContext hstRequestContext) {
    
        // find currently selected hstSiteMenuItemConfiguration's
        HstSiteMapItem selectedSiteMapItem = hstRequestContext.getResolvedSiteMapItem().getHstSiteMapItem();
        HstSiteMenusConfiguration siteMenusConfiguration = selectedSiteMapItem.getHstSiteMap().getSite().getSiteMenusConfiguration();
        if(siteMenusConfiguration != null) {
          
            for(HstSiteMenuConfiguration siteMenuConfiguration : siteMenusConfiguration.getSiteMenuConfigurations().values()) {
                HstSiteMenuImpl siteMenu = new HstSiteMenuImpl(this, siteMenuConfiguration, hstRequestContext);
                siteMenus.put(siteMenu.getName(), siteMenu);
            }
        }
    }

    public HstSiteMenu getSiteMenu(String name) {
        return siteMenus.get(name);
    }

    public Map<String, HstSiteMenu> getSiteMenus() {
        return siteMenus;
    }

}
