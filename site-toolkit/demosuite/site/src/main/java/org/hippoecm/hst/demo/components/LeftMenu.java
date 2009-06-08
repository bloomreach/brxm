package org.hippoecm.hst.demo.components;

import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoFolderBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.sitemenu.EditableMenu;
import org.hippoecm.hst.core.sitemenu.EditableMenuItem;
import org.hippoecm.hst.core.sitemenu.HstSiteMenu;
import org.hippoecm.hst.demo.util.DemoRepoBasedMenuItem;

public class LeftMenu extends BaseHstComponent {

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);

        HstSiteMenu menu = request.getRequestContext().getHstSiteMenus().getSiteMenu("main");

        EditableMenu editable = menu.getEditableMenu();
        EditableMenuItem item = editable.getDeepestExpandedItem();

        if (item != null && item.isRepositoryBased() && item.getDepth() > 0) {
            HippoBean deepestMenuBean = this.getBeanForResolvedSiteMapItem(request, item.resolveToSiteMapItem(request));

            if (deepestMenuBean.isHippoFolderBean()) {
                for (HippoFolderBean repoItem : ((HippoFolderBean) deepestMenuBean).getFolders()) {
                    EditableMenuItem repoMenuItem = new DemoRepoBasedMenuItem(repoItem, item, request, this
                            .getContentBean(request));
                    item.addChildMenuItem(repoMenuItem);
                }
            }
        }
        request.setAttribute("menu", editable);

    }

}
