/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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


import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuItemConfiguration;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstSiteMenuUtils {

    private static final Logger log = LoggerFactory.getLogger(HstSiteMenuUtils.class);
    private HstSiteMenuUtils(){

    }

    public static boolean isUserInRole(final HstSiteMenuItemConfiguration hstSiteMenuItemConfiguration, final HstRequestContext hstRequestContext) {
        for (String role : hstSiteMenuItemConfiguration.getRoles()) {
            if (hstRequestContext.getServletRequest().isUserInRole(role)) {
                log.debug("Found HstSiteMenuItemConfiguration '{}' to be in role '{}'", hstSiteMenuItemConfiguration.getName(), role);
                return true;
            }
        }
        log.debug("No matching role found for HstSiteMenuItemConfiguration '{}'", hstSiteMenuItemConfiguration.getName());
        return false;
    }
}
