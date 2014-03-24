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
package org.hippoecm.hst.demo.components;

import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoFolderBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.sitemenu.EditableMenu;
import org.hippoecm.hst.core.sitemenu.EditableMenuItem;
import org.hippoecm.hst.core.sitemenu.HstSiteMenu;
import org.hippoecm.hst.demo.util.DemoRepoBasedMenuItem;

public class LeftMenu extends BaseHstComponent {

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);

        final HstRequestContext requestContext = request.getRequestContext();
        HstSiteMenu menu = requestContext.getHstSiteMenus().getSiteMenu("main");
        request.setAttribute("menu", menu);

        if (menu != null) {
            EditableMenu editable = menu.getEditableMenu();
            EditableMenuItem item = editable.getDeepestExpandedItem();

            if (item != null && item.isRepositoryBased() && item.getDepth() > 0) {
                HippoBean deepestMenuBean = this.getBeanForResolvedSiteMapItem(request, item.resolveToSiteMapItem(request));

                if (deepestMenuBean != null && deepestMenuBean.isHippoFolderBean()) {
                    for (HippoFolderBean repoItem : ((HippoFolderBean) deepestMenuBean).getFolders()) {
                        EditableMenuItem repoMenuItem = new DemoRepoBasedMenuItem(repoItem, item, request, requestContext.getContentBean());
                        item.addChildMenuItem(repoMenuItem);
                    }
                }
            }
            request.setAttribute("fullMenu", editable);
        }

    }

}
