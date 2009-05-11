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

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class EditableMenuItemImpl implements EditableMenuItem {

    private static final Logger log = LoggerFactory.getLogger(EditableMenuItemImpl.class);
    private static final long serialVersionUID = 1L;

    private EditableMenu editableMenu;
    private int depth;
    private boolean repositoryBased;
    private List<EditableMenuItem> childMenuItems = new ArrayList<EditableMenuItem>();;
    private EditableMenuItem parentItem;
    private Map<String, Object> properties;
    private HstLink hstLink;
    private String name;
    private boolean expanded;
    private boolean selected;
    
    
    public EditableMenuItemImpl(EditableMenu editableMenu, EditableMenuItem parentItem , HstSiteMenuItem siteMenuItem) {
       
       this.editableMenu = editableMenu;
       this.parentItem = parentItem;
       this.name = siteMenuItem.getName();
       this.depth = siteMenuItem.getDepth();
       this.repositoryBased = siteMenuItem.isRepositoryBased();
       this.properties = siteMenuItem.getProperties();
       this.hstLink = siteMenuItem.getHstLink();
       this.expanded = siteMenuItem.isExpanded();
       this.selected = siteMenuItem.isSelected();
       
       for(HstSiteMenuItem childMenuItem : siteMenuItem.getChildMenuItems()) {
           childMenuItems.add(new EditableMenuItemImpl(this.editableMenu, this, childMenuItem));
       }
    }

    public List<EditableMenuItem> getChildMenuItems() {
        return this.childMenuItems;
    }
    
    public void addChildMenuItem(EditableMenuItem childMenuItem) {
        this.childMenuItems.add(childMenuItem);
        if(childMenuItem.isSelected() || childMenuItem.isExpanded()) {
            this.setExpanded(true);
        }
        if(childMenuItem.isSelected()) {
            this.editableMenu.setSelectedMenuItem(childMenuItem);
        }
    }

    public int getDepth() {
        return this.depth;
    }

    public HstLink getHstLink() {
        return this.hstLink;
    }

    public EditableMenu getEditableMenu() {
        return this.editableMenu;
    }

    public String getName() {
        return this.name;
    }

    public EditableMenuItem getParentItem() {
        return this.parentItem;
    }

    public Map<String, Object> getProperties() {
        return this.properties;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
        if(this.parentItem != null) {
            this.parentItem.setExpanded(expanded);
        }
    }
    
    public boolean isExpanded() {
        return this.expanded;
    }

    public boolean isRepositoryBased() {
        return this.repositoryBased;
    }

    public boolean isSelected() {
        return this.selected;
    }
    
    public ResolvedSiteMapItem resolveToSiteMapItem(HstRequest request){
       if(this.hstLink == null || this.hstLink.getPath() == null || "".equals(this.hstLink.getPath())) {
           log.warn("Cannot resolve to sitemap item because HstLink is null or empty. Return null"); 
           return null;
       }
       HstRequestContext ctx = request.getRequestContext();
       return ctx.getSiteMapMatcher().match(this.hstLink.getPath(), ctx.getResolvedSiteMapItem().getHstSiteMapItem().getHstSiteMap().getSite());
    }

}
