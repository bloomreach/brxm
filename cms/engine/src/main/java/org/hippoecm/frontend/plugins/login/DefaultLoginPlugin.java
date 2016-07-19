/*
 *  Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.template.PackageTextTemplate;
import org.apache.wicket.util.template.TextTemplate;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.session.UserSession;

public class DefaultLoginPlugin extends SimpleLoginPlugin {

    private static final TextTemplate INIT_JS = new PackageTextTemplate(DefaultLoginPlugin.class, "timezones-init.js");
    private static final ResourceReference JSTZ_JS = new JavaScriptResourceReference(DefaultLoginPlugin.class, "jstz.min.js");

    public static final String SHOW_TIMEZONES_CONFIG_PARAM = "show.timezones";
    public static final String SELECTABLE_TIMEZONES_CONFIG_PARAM = "selectable.timezones";
    public static final List<String> ALL_JAVA_TIMEZONES = Arrays.asList(TimeZone.getAvailableIDs());

    public DefaultLoginPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        if (getPluginConfig().getBoolean(SHOW_TIMEZONES_CONFIG_PARAM)) {
            response.render(JavaScriptReferenceHeaderItem.forReference(JSTZ_JS));
            response.render(OnLoadHeaderItem.forScript(INIT_JS.asString()));
        }
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
        private List<String> availableTimeZones;

        public LoginForm(final String id, final boolean autoComplete, final List<String> locales, final LoginHandler handler) {
            super(id, autoComplete, locales, handler);

            if (getPluginConfig().getBoolean(SHOW_TIMEZONES_CONFIG_PARAM)) {
                availableTimeZones = getSelectableTimezones(getPluginConfig().getStringArray(SELECTABLE_TIMEZONES_CONFIG_PARAM));

                // Check if user has previously selected a timezone
                final String cookieTimeZone = getCookieValue(TIMEZONE_COOKIE);
                if (cookieTimeZone != null && availableTimeZones.contains(cookieTimeZone)) {
                    selectedTimeZone = cookieTimeZone;
                }

                // Add the timezone dropdown
                final DropDownChoice<String> timeZone = new DropDownChoice<>("timezone",
                        PropertyModel.of(this, "selectedTimeZone"), availableTimeZones);

                timeZone.setNullValid(false);

                form.add(new Label("timezone-label", new ResourceModel("timezone-label", "Time zone:")));
                form.add(timeZone);

            } else {
                form.add(new Label("timezone-label").setVisible(false));
                form.add(new Label("timezone").setVisible(false));
            }
        }

        @Override
        protected void loginSuccess() {
            if (isSelectedTimeZoneValid()) {
                final TimeZone timeZone = TimeZone.getTimeZone(selectedTimeZone);
                // Store selected timezone in session and cookie
                UserSession.get().getClientInfo().getProperties().setTimeZone(timeZone);
                setCookieValue(TIMEZONE_COOKIE, selectedTimeZone, TIMEZONE_COOKIE_MAX_AGE);
            }
            super.loginSuccess();
        }

        private boolean isSelectedTimeZoneValid() {
            return selectedTimeZone != null && availableTimeZones != null
                    && availableTimeZones.contains(selectedTimeZone);
        }

        private List<String> getSelectableTimezones(final String[] configuredSelectableTimezones) {
            List<String> selectableTimezones = new ArrayList<>();

            if (configuredSelectableTimezones != null) {
                selectableTimezones = Arrays.asList(configuredSelectableTimezones).stream()
                        .filter(StringUtils::isNotBlank)
                        .filter(ALL_JAVA_TIMEZONES::contains)
                        .collect(Collectors.toList());
            }

            return selectableTimezones.isEmpty() ? ALL_JAVA_TIMEZONES : selectableTimezones;
        }
    }
}
