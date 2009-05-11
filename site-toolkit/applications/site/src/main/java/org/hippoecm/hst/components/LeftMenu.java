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
package org.hippoecm.hst.components;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoFolderBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.sitemenu.EditableMenu;
import org.hippoecm.hst.core.sitemenu.EditableMenuItem;
import org.hippoecm.hst.core.sitemenu.HstSiteMenu;

public class LeftMenu extends BaseHstComponent {

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);

        HstSiteMenu menu = request.getRequestContext().getHstSiteMenus().getSiteMenu("main");
        
        EditableMenu editable = menu.extractEditableMenu();
        EditableMenuItem item = editable.getDeepestExpandedItem();

        if (item.isRepositoryBased() && item.getDepth() > 0) {
            HippoBean deepestMenuBean = this.getBeanForResolvedSiteMapItem(request, item.resolveToSiteMapItem(request));

            if (deepestMenuBean.isHippoFolderBean()) {
                for (HippoFolderBean repoItem : ((HippoFolderBean) deepestMenuBean).getFolders()) {
                    EditableMenuItem repoMenuItem = new MyRepoBasedMenuItem(repoItem, item, request, this.getContentBean(request));
                    item.addChildMenuItem(repoMenuItem);
                }
            }
        }
        request.setAttribute("menu", editable);

    }

    public class MyRepoBasedMenuItem implements EditableMenuItem {

        private String name;
        private int depth;
        private boolean repositoryBased;
        private EditableMenu editableMenu;
        private HstLink hstLink;
        private EditableMenuItem parentItem;
        private Map<String, Object> properties;
        private boolean expanded;
        private boolean selected;
        private List<EditableMenuItem> childMenuItems = new ArrayList<EditableMenuItem>();

        public MyRepoBasedMenuItem(HippoFolderBean repoItem, EditableMenuItem parentItem, HstRequest request,
                HippoBean currentContentBean) {
            this.name = repoItem.getName();
            this.depth = parentItem.getDepth() - 1;
            this.repositoryBased = true;
            this.editableMenu = parentItem.getEditableMenu();
            this.hstLink = request.getRequestContext().getHstLinkCreator()
                    .create(repoItem, request.getRequestContext());
            this.parentItem = parentItem;

            if (repoItem.isAncestor(currentContentBean)) {
                this.expanded = true;
            }
            if (repoItem.isSelf(currentContentBean)) {
                this.expanded = true;
                this.selected = true;
                this.editableMenu.setSelectedMenuItem(this);
            }

            if (this.depth > 0 && this.expanded) {
                for (HippoFolderBean childRepoItem : repoItem.getFolders()) {
                    EditableMenuItem childMenuItem = new MyRepoBasedMenuItem(childRepoItem, this, request, currentContentBean);
                    this.addChildMenuItem(childMenuItem);
                }
            }

        }

        public void addChildMenuItem(EditableMenuItem childMenuItem) {
            this.childMenuItems.add(childMenuItem);
        }

        public List<EditableMenuItem> getChildMenuItems() {
            return this.childMenuItems;
        }

        public int getDepth() {
            return this.depth;
        }

        public EditableMenu getEditableMenu() {
            return this.editableMenu;
        }

        public HstLink getHstLink() {
            return this.hstLink;
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

        public boolean isExpanded() {
            return this.expanded;
        }

        public boolean isRepositoryBased() {
            return this.repositoryBased;
        }

        public boolean isSelected() {
            return this.selected;
        }

        public ResolvedSiteMapItem resolveToSiteMapItem(HstRequest request) {
            return null;
        }

        public void setExpanded(boolean expanded) {
            this.expanded = expanded;
        }

    }

}
