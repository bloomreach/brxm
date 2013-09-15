/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.components;

import java.util.ArrayList;
import java.util.List;

import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoFolderBean;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.onehippo.cms7.essentials.components.info.EssentialsRepositoryMenuComponentInfo;
import org.onehippo.cms7.essentials.components.model.RepositoryMenuItem;
import org.onehippo.marketplace.beans.BaseDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * Hippo HST component for repo-based menus
 *
 * @version "$Id$"
 * @see org.onehippo.cms7.essentials.components.model.RepositoryMenuItem
 */
@ParametersInfo(type = EssentialsRepositoryMenuComponentInfo.class)
public class EssentialsRepositoryMenuComponent extends CommonComponent {

    private static Logger log = LoggerFactory.getLogger(EssentialsRepositoryMenuComponent.class);

    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) {
        final EssentialsRepositoryMenuComponentInfo paramInfo = getComponentParametersInfo(request);
        String rootPath = paramInfo.getRootFolder();

        HippoBean siteContentBaseBean = getSiteContentBaseBean(request);

        if (Strings.isNullOrEmpty(rootPath)) {
            rootPath = rootPath.substring(siteContentBaseBean.getPath().length() + 1);
        }

        HippoBean rootFolderBean = getSiteContentBaseBean(request).getBean(rootPath);

        if (rootFolderBean == null) {
            request.setAttribute("previewMessage", "Please select a root folder");
        } else {
            request.setAttribute("repoBasedMenu", getContents(rootFolderBean, paramInfo.getDepth(), request.getRequestContext().getContentBean()));
        }

        log.debug("Calling EssentialsRepositoryMenuComponentInfo for root folder path:  [{}]", rootPath);

        request.setAttribute("preview", isPreview(request));
        request.setAttribute("info", paramInfo);
    }


    private List<RepositoryMenuItem> getContents(HippoBean bean, int depth, HippoBean contentBean) {
        List<BaseDocument> documents = bean.getChildBeans(BaseDocument.class);
        List<HippoFolderBean> folders = bean.getChildBeans(HippoFolderBean.class);
        List<RepositoryMenuItem> menu = new ArrayList<>();

        for (HippoBean child : folders) {
            RepositoryMenuItem menuItem = new RepositoryMenuItem(child, contentBean);

            if (depth > 1) {
                menuItem.setChildren(getContents(child, depth - 1, contentBean));
            }
            menu.add(menuItem);
        }

        for (HippoBean child : documents) {
            RepositoryMenuItem menuItem = new RepositoryMenuItem(child, contentBean);
            menu.add(menuItem);
        }
        return menu;
    }


}
