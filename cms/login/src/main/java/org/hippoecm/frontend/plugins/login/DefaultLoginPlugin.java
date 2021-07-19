/*
 *  Copyright 2016-2021 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.internal.HtmlHeaderContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.template.PackageTextTemplate;
import org.apache.wicket.util.template.TextTemplate;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.session.UserSession;

public class DefaultLoginPlugin extends LoginPlugin {

    // jstz.min.js is fetched by npm
    private static final ResourceReference JSTZ_JS = new JavaScriptResourceReference(DefaultLoginPlugin.class,
            "jstz.min.js");

    public static final String SHOW_TIMEZONES_CONFIG_PARAM = "show.timezones";
    public static final String SELECTABLE_TIMEZONES_CONFIG_PARAM = "selectable.timezones";
    public static final List<String> SUPPORTED_JAVA_TIMEZONES = Collections.unmodifiableList(
            getSupportedJavaTimeZones());

    private final TextTemplate initScript = new PackageTextTemplate(DefaultLoginPlugin.class, "timezones-init.js");

    /**
     * Exclude POSIX compatible timezones because they may cause confusions
     */
    private static List<String> getSupportedJavaTimeZones() {
        return Arrays.stream(TimeZone.getAvailableIDs())
                .filter(tz -> !tz.startsWith("Etc/"))
                .collect(Collectors.toList());
    }

    public DefaultLoginPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);
    }

    @Override
    protected LoginPanel createLoginPanel(final String id, final LoginConfig config, final LoginHandler handler) {
        return new TimeZonePanel(id, config, handler);
    }

    protected class TimeZonePanel extends LoginPanel {

        private static final String TIMEZONE_COOKIE = "tzcookie";
        private static final int TIMEZONE_COOKIE_MAX_AGE = 365 * 24 * 3600; // expire one year from now

        private String selectedTimeZone;
        private List<String> availableTimeZones = Collections.emptyList();
        private boolean useBrowserTimeZoneIfAvailable;

        public TimeZonePanel(final String id, final LoginConfig config, final LoginHandler handler) {
            super(id, config, handler);

            final IPluginConfig pluginConfig = getPluginConfig();
            final boolean isTimeZoneVisible = !isConsole() && pluginConfig.getBoolean(SHOW_TIMEZONES_CONFIG_PARAM);

            if (isTimeZoneVisible) {
                final String[] configuredTimezones = pluginConfig.getStringArray(SELECTABLE_TIMEZONES_CONFIG_PARAM);
                availableTimeZones = getSelectableTimezones(configuredTimezones);

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
            timeZone.setVisible(isTimeZoneVisible);
            form.add(timeZone);

            final IModel<String> timezoneTooltip = new LoginResourceModel("timezone-label", DefaultLoginPlugin.class);
            form.addAjaxAttributeModifier(timeZone, "title", timezoneTooltip);
        }

        @Override
        public void internalRenderHead(final HtmlHeaderContainer container) {
            super.internalRenderHead(container);
            if (getPluginConfig().getBoolean(SHOW_TIMEZONES_CONFIG_PARAM) && useBrowserTimeZoneIfAvailable) {
                container.getHeaderResponse().render(JavaScriptReferenceHeaderItem.forReference(JSTZ_JS));
                container.getHeaderResponse().render(OnLoadHeaderItem.forScript(initScript.asString()));
            }
        }

        @Override
        protected void loginSuccess() {
            setTimeZone();
            super.loginSuccess();
        }

        protected void setTimeZone() {
            if (isTimeZoneValid(selectedTimeZone)) {
                final TimeZone timeZone = TimeZone.getTimeZone(selectedTimeZone);
                // Store selected timezone in session and cookie
                UserSession.get().getClientInfo().getProperties().setTimeZone(timeZone);
                setCookieValue(TIMEZONE_COOKIE, selectedTimeZone, TIMEZONE_COOKIE_MAX_AGE);
            }
        }

        private boolean isTimeZoneValid(final String timeZone) {
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
