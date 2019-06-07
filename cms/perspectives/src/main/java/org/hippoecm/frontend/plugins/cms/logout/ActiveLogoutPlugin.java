/*
 * Copyright 2015-2018 Hippo B.V. (http://www.onehippo.com)
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
import org.apache.wicket.util.time.Duration;
import org.hippoecm.frontend.NavAppToAppHeaderItem;
import org.hippoecm.frontend.service.ILogoutService;
import org.hippoecm.frontend.util.WebApplicationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs the current user out of the CMS after a certain timespan of inactivity.
 */
public class ActiveLogoutPlugin extends Component {

    private static final String ACTIVE_LOGOUT_JS = "active-logout.js";

    private static final Logger log = LoggerFactory.getLogger(ActiveLogoutPlugin.class);

    private final int maxInactiveIntervalMinutes;
    private final LogoutBehavior logoutBehavior;

    /**
     * @param id the Wicket ID of this component
     * @param maxInactiveIntervalMinutes the number of minutes a user has to be inactive before being logged out.
     *        A value of zero or less means means 'infinite' and will disable the active logout.
     * @param logoutService the service to use for logging out a user.
     */
    public ActiveLogoutPlugin(final String id, final int maxInactiveIntervalMinutes, final ILogoutService logoutService) {
        super(id);

        this.maxInactiveIntervalMinutes = maxInactiveIntervalMinutes;
        logoutBehavior = new LogoutBehavior(logoutService);

        add(logoutBehavior);
        setRenderBodyOnly(true);
    }

    private boolean isActive() {
        return maxInactiveIntervalMinutes > 0 && !WebApplicationHelper.isDevelopmentMode();
    }

    @Override
    public void internalRenderHead(final HtmlHeaderContainer container) {
        super.internalRenderHead(container);

        final IHeaderResponse header = container.getHeaderResponse();
        header.render(new NavAppToAppHeaderItem(getLogoutCallbackUrl()));

        if (isActive()) {
            log.info("Inactive user sessions will be logged out automatically after {}", Duration.minutes(maxInactiveIntervalMinutes));
            header.render(OnLoadHeaderItem.forScript(createActiveLogoutScript()));
        } else {
            log.info("Inactive user sessions will not be logged out automatically");
        }
    }

    @Override
    protected void onRender() {
        // nothing to render
    }

    private String createActiveLogoutScript() {
        final Map<String, String> scriptParams = new TreeMap<>();
        scriptParams.put("logoutCallbackUrl", getLogoutCallbackUrl());

        final PackageTextTemplate activeLogoutJs = new PackageTextTemplate(ActiveLogoutPlugin.class, ACTIVE_LOGOUT_JS);
        return activeLogoutJs.asString(scriptParams);
    }

    private String getLogoutCallbackUrl() {
        return logoutBehavior.getCallbackUrl().toString();
    }

}
