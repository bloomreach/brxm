/*
 *  Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.login;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.internal.HtmlHeaderContainer;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.template.PackageTextTemplate;
import org.apache.wicket.util.template.TextTemplate;
import org.hippoecm.frontend.Main;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.util.WebApplicationHelper;

public class DefaultLoginPlugin extends SimpleLoginPlugin {

    private static final TextTemplate INIT_JS = new PackageTextTemplate(DefaultLoginPlugin.class, "timezones-init.js");
    // jstz.min.js is fetched by npm
    private static final ResourceReference JSTZ_JS = new JavaScriptResourceReference(DefaultLoginPlugin.class, "jstz.min.js");

    public static final String SHOW_TIMEZONES_CONFIG_PARAM = "show.timezones";
    public static final String SELECTABLE_TIMEZONES_CONFIG_PARAM = "selectable.timezones";
    public static final List<String> SUPPORTED_JAVA_TIMEZONES = excludeEtcTimeZones(Arrays.asList(TimeZone.getAvailableIDs()));

    /**
     * Exclude POSIX compatible timezones because they may cause confusions
     */
    private static List<String> excludeEtcTimeZones(final List<String> timezones) {
        return timezones.stream()
                .filter((tz) -> !tz.startsWith("Etc/"))
                .collect(Collectors.toList());
    }

    public DefaultLoginPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);
    }

    @Override
    protected LoginPanel createLoginPanel(final String id, final boolean autoComplete, final List<String> locales,
                                          final LoginHandler handler) {
        return new LoginForm(id, autoComplete, locales, handler);
    }

    protected class LoginForm extends CaptchaForm {

        private static final String TIMEZONE_COOKIE = "tzcookie";
        private static final int TIMEZONE_COOKIE_MAX_AGE = 365 * 24 * 3600; // expire one year from now

        private String selectedTimeZone;
        private List<String> availableTimeZones = Collections.emptyList();
        private boolean useBrowserTimeZoneIfAvailable;

        public LoginForm(final String id, final boolean autoComplete, final List<String> locales, final LoginHandler handler) {
            super(id, autoComplete, locales, handler);

            final IPluginConfig config = getPluginConfig();
            final boolean consoleLogin = WebApplicationHelper.getApplicationName().equals(Main.PLUGIN_APPLICATION_VALUE_CONSOLE);
            final boolean isTimeZoneVisible = !consoleLogin && config.getBoolean(SHOW_TIMEZONES_CONFIG_PARAM);

            if (isTimeZoneVisible) {
                availableTimeZones = getSelectableTimezones(config.getStringArray(SELECTABLE_TIMEZONES_CONFIG_PARAM));

                // Check if user has previously selected a timezone
                final String cookieTimeZone = getCookieValue(TIMEZONE_COOKIE);
                if (isTimeZoneValid(cookieTimeZone)) {
                    selectedTimeZone = cookieTimeZone;
                } else {
                    selectedTimeZone = availableTimeZones.get(0);
                    useBrowserTimeZoneIfAvailable = true;
                }
            }

            // Add the time zone dropdown
            final PropertyModel<String> selected = PropertyModel.of(this, "selectedTimeZone");
            final DropDownChoice<String> timeZone = new DropDownChoice<>("timezone", selected, availableTimeZones);
            timeZone.setNullValid(false);

            final Label timeZoneLabel = new Label("timezone-label", new ResourceModel("timezone-label", "Time zone:"));
            timeZoneLabel.setVisible(isTimeZoneVisible);
            form.addLabelledComponent(timeZoneLabel);
            form.add(timeZone);
        }

        @Override
        public void internalRenderHead(HtmlHeaderContainer container) {
            super.internalRenderHead(container);
            if (getPluginConfig().getBoolean(SHOW_TIMEZONES_CONFIG_PARAM) && useBrowserTimeZoneIfAvailable) {
                container.getHeaderResponse().render(JavaScriptReferenceHeaderItem.forReference(JSTZ_JS));
                container.getHeaderResponse().render(OnLoadHeaderItem.forScript(INIT_JS.asString()));
            }
        }

        @Override
        protected void loginSuccess() {
            if (isTimeZoneValid(selectedTimeZone)) {
                final TimeZone timeZone = TimeZone.getTimeZone(selectedTimeZone);
                // Store selected timezone in session and cookie
                UserSession.get().getClientInfo().getProperties().setTimeZone(timeZone);
                setCookieValue(TIMEZONE_COOKIE, selectedTimeZone, TIMEZONE_COOKIE_MAX_AGE);
            }
            super.loginSuccess();
        }

        private boolean isTimeZoneValid(String timeZone) {
            return timeZone != null && availableTimeZones != null
                    && availableTimeZones.contains(timeZone);
        }

        private List<String> getSelectableTimezones(final String[] configuredSelectableTimezones) {
            List<String> selectableTimezones = new ArrayList<>();

            if (configuredSelectableTimezones != null) {
                selectableTimezones = Arrays.stream(configuredSelectableTimezones)
                        .filter(StringUtils::isNotBlank)
                        .filter(SUPPORTED_JAVA_TIMEZONES::contains)
                        .collect(Collectors.toList());
            }

            return selectableTimezones.isEmpty() ? SUPPORTED_JAVA_TIMEZONES : selectableTimezones;
        }
    }
}
