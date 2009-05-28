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
package org.hippoecm.hst.utilities;

import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoFolderBean;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.sitemenu.EditableMenuItem;
import org.hippoecm.hst.core.sitemenu.EditableMenuItemImpl;

public class MyRepoBasedMenuItem extends EditableMenuItemImpl {

    public MyRepoBasedMenuItem(HippoFolderBean repoItem, EditableMenuItem parentItem, HstRequest request,
            HippoBean currentContentBean) {
        super(parentItem);
        this.name = repoItem.getName();
        this.depth = parentItem.getDepth() - 1;
        this.repositoryBased = true;
        this.hstLink = request.getRequestContext().getHstLinkCreator().create(repoItem, request.getRequestContext());

        if (repoItem.isAncestor(currentContentBean)) {
            this.expanded = true;
        }
        if (repoItem.isSelf(currentContentBean)) {
            this.expanded = true;
            this.selected = true;
            this.getEditableMenu().setSelectedMenuItem(this);
        }

        if (this.depth > 0 && this.expanded) {
            for (HippoFolderBean childRepoItem : repoItem.getFolders()) {
                EditableMenuItem childMenuItem = new MyRepoBasedMenuItem(childRepoItem, this, request,
                        currentContentBean);
                this.addChildMenuItem(childMenuItem);
            }
        }

    }

}
