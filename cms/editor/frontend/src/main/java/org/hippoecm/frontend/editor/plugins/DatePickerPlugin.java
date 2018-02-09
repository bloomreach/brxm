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
package org.hippoecm.frontend.editor.plugins;

import java.time.format.FormatStyle;
import java.util.Date;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.properties.MapEmptyDateToNullModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.datetime.DateLabel;
import org.hippoecm.frontend.plugins.standards.datetime.DateTimeLabel;
import org.hippoecm.frontend.plugins.yui.datetime.DateFieldWidget;
import org.hippoecm.frontend.service.IEditor.Mode;
import org.hippoecm.frontend.service.render.RenderPlugin;

public class DatePickerPlugin extends RenderPlugin<Date> {

    public static final String VALUE = "value";

    public DatePickerPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        final IModel<Date> model = new MapEmptyDateToNullModel(getModel());
        final Mode mode = Mode.fromConfig(config, Mode.VIEW);
        if (mode == Mode.EDIT) {
            add(new DateFieldWidget(VALUE, model, context, config));
        } else {
            final boolean dateOnly = config.getAsBoolean(DateFieldWidget.CONFIG_HIDE_TIME, false);
            if (dateOnly) {
                add(new DateLabel(VALUE, model, FormatStyle.LONG));
            } else {
                add(new DateTimeLabel(VALUE, model, FormatStyle.LONG, FormatStyle.SHORT));
            }
        }
        setOutputMarkupId(true);
    }
}
