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
package org.hippoecm.frontend.plugins.cms.logout;

import java.util.Map;
import java.util.TreeMap;

import org.apache.wicket.Component;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.internal.HtmlHeaderContainer;
import org.apache.wicket.util.template.PackageTextTemplate;
import org.hippoecm.frontend.logout.ActiveLogoutSettings;
import org.hippoecm.frontend.service.ILogoutService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.useractivity.UserActivityHeaderItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs the current user out of the CMS after a certain timespan of inactivity.
 */
public class ActiveLogoutPlugin extends Component {

    private static final String ACTIVE_LOGOUT_JS = "active-logout.js";

    private static final Logger log = LoggerFactory.getLogger(ActiveLogoutPlugin.class);

    private final LogoutBehavior logoutBehavior;

    /**
     * @param id the Wicket ID of this component
     * @param logoutService the service to use for logging out a user.
     */
    public ActiveLogoutPlugin(final String id, final ILogoutService logoutService) {
        super(id);

        logoutBehavior = new LogoutBehavior(logoutService);

        final ActiveLogoutSettings settings = ActiveLogoutSettings.get();
        if (settings.isEnabled()) {
            if (settings.getStaySignedIn(UserSession.get())) {
                log.info("User chose to stay signed in and will not be actively logged out");
            } else {
                log.info("User chose to not stay signed in and will be actively logged out after {} seconds of inactivity", settings.getMaxInactiveIntervalSeconds());
                add(logoutBehavior);
            }
        } else {
            log.info("Active logout is disabled");
        }

        setRenderBodyOnly(true);
    }

    @Override
    public void renderHead(final HtmlHeaderContainer container) {
        super.renderHead(container);

        // always render the user activity API
        final ActiveLogoutSettings settings = ActiveLogoutSettings.get();
        final IHeaderResponse header = container.getHeaderResponse();
        header.render(new UserActivityHeaderItem(settings.getMaxInactiveIntervalSeconds()));

        if (settings.isEnabled() && !settings.getStaySignedIn(UserSession.get())) {
            header.render(OnLoadHeaderItem.forScript(createActiveLogoutScript()));
        }
    }

    @Override
    protected void onRender() {
        // nothing to render
    }

    private String createActiveLogoutScript() {
        final Map<String, String> scriptParams = new TreeMap<>();
        scriptParams.put("logoutCallbackUrl", logoutBehavior.getCallbackUrl().toString());

        final PackageTextTemplate activeLogoutJs = new PackageTextTemplate(ActiveLogoutPlugin.class, ACTIVE_LOGOUT_JS);
        return activeLogoutJs.asString(scriptParams);
    }

}
