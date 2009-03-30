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

import java.util.ArrayList;
import java.util.List;

import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuItemConfiguration;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstSiteMenuItemImpl implements HstSiteMenuItem {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(HstSiteMenuItemImpl.class);
    
    private HstSiteMenu hstSiteMenu;
    private String name;
    private boolean selected;
    private List<HstSiteMenuItem> hstSiteMenuItems = new ArrayList<HstSiteMenuItem>();
    private List<HstSiteMenuItem> selectedHstSiteMenuItems = new ArrayList<HstSiteMenuItem>();
    private HstSiteMenuItem parent;
    private HstLinkCreator linkCreator;
    private HstSiteMapItem siteMapItem;
    
    public HstSiteMenuItemImpl(HstSiteMenu hstSiteMenu, HstSiteMenuItem parent, HstSiteMenuItemConfiguration hstSiteMenuItemConfiguration, List<HstSiteMenuItemConfiguration> selectedSiteMenuItemConfigurations, HstLinkCreator hstLinkCreator) {
        this.hstSiteMenu = hstSiteMenu;
        this.parent = parent;
        this.siteMapItem = hstSiteMenuItemConfiguration.getHstSiteMapItem();
        this.linkCreator = hstLinkCreator;
        this.name = hstSiteMenuItemConfiguration.getName();
        for(HstSiteMenuItemConfiguration childItemConfiguration : hstSiteMenuItemConfiguration.getChildItemConfigurations()) {
            hstSiteMenuItems.add(new HstSiteMenuItemImpl(hstSiteMenu, this, childItemConfiguration, selectedSiteMenuItemConfigurations, hstLinkCreator));
        }
        
        if(selectedSiteMenuItemConfigurations.contains(hstSiteMenuItemConfiguration)) {
            // the current HstSiteMenuItem is selected. Set it to selected, and also set all the ancestors selected
            this.selected = true;
            if(this.parent == null) {
                ((HstSiteMenuImpl)this.hstSiteMenu).addSelectedSiteMenuItem(this);
            } else {
                // we are a root HstSiteMenuItem. Set selected to the HstSiteMenu container of this item
                ((HstSiteMenuItemImpl)this.parent).addSelectedSiteMenuItem(this);
            }
        }
    }

    public List<HstSiteMenuItem> getChildMenuItems() {
        return hstSiteMenuItems;
    }

    public HstLink getHstLink() {
        if(siteMapItem == null) {
            log.warn("Cannot create a link for HstSiteMenuItem because no valid hstSiteMapItem is referenced");
            return null;
        }
        return linkCreator.create(siteMapItem);
    }

    public String getName() {
        return name;
    }

    public boolean isSelected() {
        return selected;
    }


    public HstSiteMenuItem getParentItem() {
        return this.parent;
    }

    public List<HstSiteMenuItem> getSelectedSiteMenuItems() {
        return selectedHstSiteMenuItems;
    }
    
    public void addSelectedSiteMenuItem(HstSiteMenuItem hstSiteMenuItem){
        this.selected = true;
        // only add it when it is not already added
        if(!this.selectedHstSiteMenuItems.contains(hstSiteMenuItem)) {
            this.selectedHstSiteMenuItems.add(hstSiteMenuItem);
            // set this HstSiteMenuItem also selected in the parent, and when parent is null, set in HstSiteMenu container the selected item 
            if(this.parent == null) {
                ((HstSiteMenuImpl)this.hstSiteMenu).addSelectedSiteMenuItem(hstSiteMenuItem);
            } else {
                // we are a root HstSiteMenuItem. Set selected to the HstSiteMenu container of this item
                ((HstSiteMenuItemImpl)this.parent).addSelectedSiteMenuItem(this);
            }
        }
    }

    public HstSiteMenu getHstSiteMenu() {
        return this.hstSiteMenu;
    }

}
