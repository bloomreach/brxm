/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.logout;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.protocol.http.WebApplication;
import org.hippoecm.frontend.logout.CmsLogoutService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.ILogoutService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.util.WebApplicationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs the current user out of the Console. This implementation performs the following tasks upon logout:
 * <ol>
 *     <li>Remove the Hippo auto login cookie</li>
 *     <li>Log out the user session</li>
 *     <li>Redirect to the login page</li>
 * </ol>
 */
public class ConsoleLogoutService extends Plugin implements ILogoutService {

    private static final Logger log = LoggerFactory.getLogger(CmsLogoutService.class);

    public ConsoleLogoutService(final IPluginContext context, final IPluginConfig config) {
        super(context, config);
        context.registerService(this, SERVICE_ID);
    }

    @Override
    public void logout() {
        WebApplicationHelper.clearCookie(WebApplicationHelper.getFullyQualifiedCookieName(WebApplicationHelper.HIPPO_AUTO_LOGIN_COOKIE_BASE_NAME));

        UserSession.get().logout();

        if (WebApplication.exists()) {
            throw new RestartResponseException(WebApplication.get().getHomePage());
        }
    }

}