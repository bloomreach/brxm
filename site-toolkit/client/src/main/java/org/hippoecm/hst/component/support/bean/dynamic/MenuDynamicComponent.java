/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.component.support.bean.dynamic;

import org.hippoecm.hst.component.support.bean.info.dynamic.MenuDynamicComponentInfo;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.sitemenu.HstSiteMenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;

/**
 * Menu Dynamic component used for HST menus.
 *
 * @version "$Id$"
 */
@ParametersInfo(type = MenuDynamicComponentInfo.class)
public class MenuDynamicComponent extends BaseHstDynamicComponent {

    private final static Logger log = LoggerFactory.getLogger(MenuDynamicComponent.class);
    public final static String MENU_MODEL = "menu";

    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);

        final MenuDynamicComponentInfo paramInfo = getComponentParametersInfo(request);
        String siteMenu = paramInfo.getMenu();
        if (Strings.isNullOrEmpty(siteMenu)) {
            log.warn("No site menu is selected within MenuDynamicComponent nor set as a component parameter (menuName)");
            return;
        }
        siteMenu = CharMatcher.whitespace().trimFrom(siteMenu);
        final HstSiteMenu menu = request.getRequestContext().getHstSiteMenus().getSiteMenu(siteMenu);
        if (menu == null) {
            log.warn("Invalid site menu is selected within MenuDynamicComponent: {}", siteMenu);
            return;
        }
        log.debug("Using site menu:[{}]", siteMenu);
        request.setModel(MENU_MODEL, menu);
    }
}
