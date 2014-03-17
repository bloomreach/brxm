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

import java.util.ArrayList;
import java.util.List;

import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuConfiguration;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuItemConfiguration;
import org.hippoecm.hst.core.request.HstRequestContext;

public class HstSiteMenuImpl extends AbstractMenu implements HstSiteMenu {

    private static final long serialVersionUID = 1L;

    private HstSiteMenus hstSiteMenus;
    private List<HstSiteMenuItem> hstSiteMenuItems = new ArrayList<HstSiteMenuItem>();
    private HstSiteMenuItem selectedSiteMenuItem;

    public HstSiteMenuImpl(HstSiteMenus hstSiteMenus, HstSiteMenuConfiguration siteMenuConfiguration, HstRequestContext hstRequestContext) {
        this.hstSiteMenus = hstSiteMenus;
        this.name = siteMenuConfiguration.getName();
        final boolean menuVisibleRegardlessRoles = hstRequestContext.isCmsRequest() && hstRequestContext.getResolvedMount().getMount().getVirtualHost().getVirtualHosts().isChannelMngrSiteAuthenticationSkipped();
        for(HstSiteMenuItemConfiguration hstSiteMenuItemConfiguration : siteMenuConfiguration.getSiteMenuConfigurationItems()) {
            if (hstSiteMenuItemConfiguration.getRoles() == null || menuVisibleRegardlessRoles) {
                hstSiteMenuItems.add(new HstSiteMenuItemImpl(this, null, hstSiteMenuItemConfiguration , hstRequestContext));
            } else {
                if (HstSiteMenuUtils.isUserInRole(hstSiteMenuItemConfiguration, hstRequestContext)) {
                    hstSiteMenuItems.add(new HstSiteMenuItemImpl(this, null, hstSiteMenuItemConfiguration , hstRequestContext));
                }
            }
        }
    }

    public List<HstSiteMenuItem> getSiteMenuItems() {
        return hstSiteMenuItems;
    }

    public HstSiteMenus getHstSiteMenus() {
        return this.hstSiteMenus;
    }

    public HstSiteMenuItem getSelectSiteMenuItem(){
        return selectedSiteMenuItem;
    }
    
    public void setSelectedSiteMenuItem(HstSiteMenuItem selectedSiteMenuItem){
        this.selectedSiteMenuItem = selectedSiteMenuItem;
    }
    
    public void setExpanded(){
        this.expanded = true;
    }

    public HstSiteMenuItem getDeepestExpandedItem() {
        if(selectedSiteMenuItem != null) {
            return selectedSiteMenuItem;
        }
        if(!this.expanded) {
            return null;
        }
        
        // traverse to the deepest expanded item
        for(HstSiteMenuItem item: hstSiteMenuItems) {
            if(item.isExpanded()){
                return ((HstSiteMenuItemImpl)item).getDeepestExpandedItem();
            }
        }
        return null;
    }

    public EditableMenu getEditableMenu() {
        return new EditableMenuImpl(this);
    }

}
