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
package org.hippoecm.hst.core.sitemenu;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuConfiguration;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuItemConfiguration;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenusConfiguration;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;

public class HstSiteMenusImpl implements HstSiteMenus{

    private static final long serialVersionUID = 1L;
    private Map<String, HstSiteMenu> siteMenus = new HashMap<String, HstSiteMenu>(); 
   
    
    public HstSiteMenusImpl(ResolvedSiteMapItem resolvedSiteMapItem, HstRequestContext hstRequestContext) {
    
        // find currently selected hstSiteMenuItemConfiguration's
        HstSiteMapItem selectedSiteMapItem = resolvedSiteMapItem.getHstSiteMapItem();
        HstSiteMenusConfiguration siteMenusConfiguration = selectedSiteMapItem.getHstSiteMap().getSite().getSiteMenusConfiguration();
        if(siteMenusConfiguration != null) {
            List<HstSiteMenuItemConfiguration> selectedSiteMenuItemConfigurations =  siteMenusConfiguration.getItemsBySiteMapItemId(selectedSiteMapItem.getId());
        
            /*
             * if no selected SiteMenuItems can be found for the selectedSiteMapItem, look for the first parent sitemap item that has an
             * assiociated HstSiteMenuItemConfiguration: this is then the SiteMenuItem that must be set to selected
             */ 
            
            while(selectedSiteMenuItemConfigurations.isEmpty() && selectedSiteMapItem.getParentItem() != null) {
                selectedSiteMapItem = selectedSiteMapItem.getParentItem();
                selectedSiteMenuItemConfigurations =  siteMenusConfiguration.getItemsBySiteMapItemId(selectedSiteMapItem.getId());
            }
            
            for(HstSiteMenuConfiguration siteMenuConfiguration : siteMenusConfiguration.getSiteMenuConfigurations().values()) {
                HstSiteMenuImpl siteMenu = new HstSiteMenuImpl(this, siteMenuConfiguration, selectedSiteMenuItemConfigurations, hstRequestContext);
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
