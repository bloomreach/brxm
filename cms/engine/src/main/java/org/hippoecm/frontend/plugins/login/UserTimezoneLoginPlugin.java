package org.hippoecm.frontend.plugins.login;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.lang.ArrayUtils;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.internal.HtmlHeaderContainer;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.util.template.PackageTextTemplate;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.session.PluginUserSession;

public class UserTimezoneLoginPlugin extends SimpleLoginPlugin {

    public static final String SHOW_TIMEZONES_CONFIG_PARAM = "show.timezones";
    public static final String SELECTED_TIMEZONES_CONFIG_PARAM = "selected-timezones";

    public UserTimezoneLoginPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);
    }

    @Override
    protected LoginPanel createLoginPanel(final String id, final boolean autoComplete, final List<String> locales,
                                          final LoginHandler handler) {
        return new UserTimezoneLoginForm(id, autoComplete, locales, handler);
    }

    class UserTimezoneLoginForm extends CaptchaForm {

        private static final String TIMEZONE_COOKIE = "tzcookie";
        private static final int TIMEZONE_COOKIE_MAX_AGE = 365 * 24 * 3600; // expire one year from now

        public String selectedTimeZone;

        public UserTimezoneLoginForm(final String id, final boolean autoComplete, final List<String> locales, final LoginHandler handler) {
            super(id, autoComplete, locales, handler);

            if (getPluginConfig().getBoolean(SHOW_TIMEZONES_CONFIG_PARAM)) {

                //Get available timezones
                String[] selectedTimeZonesFromPreferences = getPluginConfig().getStringArray(SELECTED_TIMEZONES_CONFIG_PARAM);
                List<String> availableTimeZonesList = new ArrayList<>(Arrays.asList(
                    ArrayUtils.isEmpty(selectedTimeZonesFromPreferences) ?  TimeZone.getAvailableIDs() : selectedTimeZonesFromPreferences));


                //Check if user has previously selected a timezone
                String cookieTimezone = getCookieValue(TIMEZONE_COOKIE);
                if (cookieTimezone != null && availableTimeZonesList.contains(cookieTimezone)) {
                    selectedTimeZone = cookieTimezone;
                    ((PluginUserSession) getSession()).getClientInfo().getProperties().setTimeZone(
                            TimeZone.getTimeZone(selectedTimeZone));
                }

                //add the timezone dropdown
                DropDownChoice<String> timezone = new DropDownChoice<>("timezone",
                        new PropertyModel<String>(this, "selectedTimeZone") {
                            @Override
                            public void setObject(final String object) {
                                super.setObject(availableTimeZonesList.contains(object) ? object : "GMT");
                            }
                        },
                        availableTimeZonesList,
                        new IChoiceRenderer<String>() {
                            @Override
                            public String getDisplayValue(final String object) {
                                return object;
                            }

                            @Override
                            public String getIdValue(final String object, final int index) {
                                return object;
                            }
                        }
                );

                timezone.setNullValid(true);

                form.add(new Label("timezone-label", new ResourceModel("timezone-label", "Timezone:")));
                form.add(timezone);

            } else {
                form.add(new Label("timezone-label").setVisible(false));
                form.add(new Label("timezone").setVisible(false));
            }
        }


        @Override
        public void renderHead(final HtmlHeaderContainer container) {
            super.renderHead(container);
            if (getPluginConfig().getBoolean(SHOW_TIMEZONES_CONFIG_PARAM)) {
                container.getHeaderResponse().render(JavaScriptReferenceHeaderItem.forReference(new JavaScriptResourceReference(getClass(), "jstz.min.js")));
                container.getHeaderResponse().render(OnLoadHeaderItem.forScript(new PackageTextTemplate(getClass(), "timezones-init.js").asString()));
            }
        }


        @Override
        protected void loginSuccess() {
            if (selectedTimeZone != null) {
                setCookieValue(TIMEZONE_COOKIE, selectedTimeZone, TIMEZONE_COOKIE_MAX_AGE);
                ((PluginUserSession) getSession()).getClientInfo().getProperties().setTimeZone(
                        TimeZone.getTimeZone(selectedTimeZone));
            }
            super.loginSuccess();
        }
    }
}