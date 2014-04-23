/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.components;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.sitemenu.HstSiteMenu;
import org.onehippo.cms7.essentials.components.info.EssentialsMenuComponentInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;

/**
 * HST component used for HST menus.
 *
 * @version "$Id$"
 */
@ParametersInfo(type = EssentialsMenuComponentInfo.class)
public class EssentialsMenuComponent extends CommonComponent {

    private static Logger log = LoggerFactory.getLogger(EssentialsMenuComponent.class);

    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) {
        final EssentialsMenuComponentInfo paramInfo = getComponentParametersInfo(request);
        String siteMenu = paramInfo.getSiteMenu();
        if (Strings.isNullOrEmpty(siteMenu)) {
            // check if set as component parameter:
            siteMenu = getComponentParameter("menuName");
            if (Strings.isNullOrEmpty(siteMenu)) {
                log.warn("No site menu is selected within EssentialsMenuComponent nor set as a component parameter (menuName)");
                return;
            }

        }
        siteMenu = CharMatcher.WHITESPACE.trimFrom(siteMenu);
        final HstSiteMenu menu = request.getRequestContext().getHstSiteMenus().getSiteMenu(siteMenu);
        if (menu == null) {
            log.warn("Invalid site menu is selected within EssentialsMenuComponent: {}", siteMenu);
            return;
        }
        log.debug("Using site menu:[{}]", siteMenu);
        request.setAttribute("menu", menu);
        request.setAttribute(REQUEST_ATTR_PARAM_INFO, paramInfo);
    }
}
