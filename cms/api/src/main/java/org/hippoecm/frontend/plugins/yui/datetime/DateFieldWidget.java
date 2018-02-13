/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.yui.datetime;

import java.util.Date;

import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.util.MappingException;
import org.hippoecm.frontend.util.PluginConfigMapper;

public class DateFieldWidget extends GenericPanel<Date> {

    public static final String CONFIG_HIDE_TIME = "time.hide";
    public static final String CONFIG_SHOW_TODAY_BUTTON = "show.today.button";

    private final IPluginConfig config;

    public DateFieldWidget(final String id, final IModel<Date> model, final IPluginContext context, final IPluginConfig config) {
        super(id, model);
        this.config = config;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        final IModel<Date> model = getModel();
        final YuiDatePickerSettings settings = getSettings();
        final YuiDateTimeField dateTimeField = newYuiDateTimeField("widget", model, settings);

        final boolean currentDateLinkVisible = config.getAsBoolean(CONFIG_SHOW_TODAY_BUTTON, true);
        dateTimeField.setCurrentDateLinkVisible(currentDateLinkVisible);

        add(dateTimeField);
    }

    private YuiDatePickerSettings getSettings() {
        final YuiDatePickerSettings settings = new YuiDatePickerSettings();
        settings.setLanguage(getLocale().getLanguage());
        if (config.containsKey("datepicker")) {
            try {
                PluginConfigMapper.populate(settings, config.getPluginConfig("datepicker"));
            } catch (final MappingException e) {
                throw new RuntimeException(e);
            }
        }
        return settings;
    }

    protected YuiDateTimeField newYuiDateTimeField(final String id, final IModel<Date> model, final YuiDatePickerSettings settings) {
        final boolean hideTime = config.getAsBoolean(CONFIG_HIDE_TIME, false);
        return hideTime
                ? new YuiDateField(id, model, settings)
                : new YuiDateTimeField(id, model, settings);
    }
}
