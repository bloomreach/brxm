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
import java.util.Map;

import org.hippoecm.hst.configuration.HstSite;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuItemConfiguration;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.util.PathUtils;

public class HstSiteMenuItemImpl extends AbstractMenuItem implements HstSiteMenuItem {

    private HstSiteMenu hstSiteMenu;
    private List<HstSiteMenuItem> hstSiteMenuItems = new ArrayList<HstSiteMenuItem>();
    private HstSiteMenuItem parent;
    private HstLinkCreator linkCreator;
    private HstSite hstSite;
    private String hstSiteMapItemPath;
    
    public HstSiteMenuItemImpl(HstSiteMenu hstSiteMenu, HstSiteMenuItem parent, HstSiteMenuItemConfiguration hstSiteMenuItemConfiguration, HstRequestContext hstRequestContext) {
        this.hstSiteMenu = hstSiteMenu;
        this.parent = parent;
        this.hstSite = hstSiteMenuItemConfiguration.getHstSiteMenuConfiguration().getSiteMenusConfiguration().getSite();
        this.hstSiteMapItemPath = hstSiteMenuItemConfiguration.getSiteMapItemPath();
        this.linkCreator = hstRequestContext.getHstLinkCreator();
        this.name = hstSiteMenuItemConfiguration.getName();
        this.depth = hstSiteMenuItemConfiguration.getDepth();
        this.repositoryBased = hstSiteMenuItemConfiguration.isRepositoryBased();
        this.properties = hstSiteMenuItemConfiguration.getProperties();
        for(HstSiteMenuItemConfiguration childItemConfiguration : hstSiteMenuItemConfiguration.getChildItemConfigurations()) {
            hstSiteMenuItems.add(new HstSiteMenuItemImpl(hstSiteMenu, this, childItemConfiguration, hstRequestContext));
        }
        
        String currentPathInfo = hstRequestContext.getResolvedSiteMapItem().getPathInfo();
        String siteMenuItemToMapPath = PathUtils.normalizePath(hstSiteMenuItemConfiguration.getSiteMapItemPath());
       
         if (siteMenuItemToMapPath != null && currentPathInfo != null) {
            
             if (siteMenuItemToMapPath.equals(currentPathInfo)) {
                // the current HstSiteMenuItem is selected. Set it to selected, and also set all the ancestors selected
                this.selected = true;
                ((HstSiteMenuImpl)hstSiteMenu).setSelectedSiteMenuItem(this);
             }
             
             if(currentPathInfo.startsWith(siteMenuItemToMapPath)) {
                // check if the match was until a slash, otherwise it is not a sitemenu item we want to expand
                String sub = currentPathInfo.substring(siteMenuItemToMapPath.length());
                if("".equals(sub) || sub.startsWith("/")) {
                    // not selected but expand all ancestors
                    this.expanded = true;
                    if (this.parent == null) {
                        // we are a root HstSiteMenuItem. Set selected to the HstSiteMenu container of this item
                        ((HstSiteMenuImpl) this.hstSiteMenu).setExpanded();
                    } else {
                        ((HstSiteMenuItemImpl) this.parent).setExpanded();
                    }
                }
            }
        }
    }

    public List<HstSiteMenuItem> getChildMenuItems() {
        return hstSiteMenuItems;
    }

    // laze loaded
    public HstLink getHstLink() {
        return linkCreator.create(this.hstSiteMapItemPath, this.hstSite);
    }

    public HstSiteMenuItem getParentItem() {
        return this.parent;
    }
    
    public void setExpanded(){
       this.expanded = true;
       if (this.parent == null) {
           // we are a root HstSiteMenuItem. Set selected to the HstSiteMenu container of this item
           ((HstSiteMenuImpl) this.hstSiteMenu).setExpanded();
       } else {
           ((HstSiteMenuItemImpl) this.parent).setExpanded();
       }
    }
    
    public HstSiteMenu getHstSiteMenu() {
        return this.hstSiteMenu;
    }

    public int getDepth() {
        return this.depth;
    }

    public boolean isRepositoryBased() {
        return this.repositoryBased;
    }
    
    public HstSiteMenuItem getDeepestExpandedItem(){
        for(HstSiteMenuItem item : hstSiteMenuItems) {
            if(item.isExpanded()) {
                return ((HstSiteMenuItemImpl)item).getDeepestExpandedItem();
            }
        }
        return this;
    }

    public Map<String, Object> getProperties() {
        return this.properties;
    }

    
}
