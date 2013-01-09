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

public class EditableMenuImpl extends AbstractMenu implements EditableMenu {

    private static final long serialVersionUID = 1L;

    private HstSiteMenus hstSiteMenus;
    private List<EditableMenuItem> editableMenuItems = new ArrayList<EditableMenuItem>();
    private EditableMenuItem selectedMenuItem;
    
    public EditableMenuImpl(HstSiteMenu hstSiteMenu) {
       this.name = hstSiteMenu.getName();
       this.hstSiteMenus = hstSiteMenu.getHstSiteMenus();
       this.expanded = hstSiteMenu.isExpanded();
       for(HstSiteMenuItem siteMenuItem : hstSiteMenu.getSiteMenuItems()) {
           editableMenuItems.add(new EditableMenuItemImpl(this, null, siteMenuItem));
       }
    }

    public List<EditableMenuItem> getMenuItems() {
        return this.editableMenuItems;
    }

    public EditableMenuItem getDeepestExpandedItem() {
        if(selectedMenuItem != null) {
            return selectedMenuItem;
        }
        if(!this.expanded) {
            return null;
        }
        
        // traverse to the deepest expanded item
        for(EditableMenuItem item: editableMenuItems) {
            if(item.isExpanded()){
                return traverseToDeepestExpandedItem(item);
            }
        }
        return null;
    }
    
    private EditableMenuItem traverseToDeepestExpandedItem(EditableMenuItem item){
        for(EditableMenuItem child: item.getChildMenuItems()) {
            if(child.isExpanded()){
                return traverseToDeepestExpandedItem(child); 
            }
        }
        return item;
    }

    public HstSiteMenus getHstSiteMenus() {
        return this.hstSiteMenus;
    }

    public EditableMenuItem getSelectMenuItem() {
        return this.selectedMenuItem;
    }
    
    public void setSelectedMenuItem(EditableMenuItem selectedMenuItem){
        this.selectedMenuItem = selectedMenuItem;
    }

}
