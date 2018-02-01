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

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.datetime.DateTimeLabel;
import org.hippoecm.frontend.plugins.standards.datetime.GMTDateLabel;
import org.hippoecm.frontend.plugins.yui.datetime.DateFieldWidget;
import org.hippoecm.frontend.model.properties.MapEmptyDateToNullModel;
import org.hippoecm.frontend.service.IEditor.Mode;
import org.hippoecm.frontend.service.render.RenderPlugin;

public class DatePickerPlugin extends RenderPlugin<Date> {

    /**
     * @deprecated use {@link Mode#EDIT} instead.
     */
    @Deprecated
    public static final String EDIT = "edit";

    /***
     * @deprecated use {@link Mode#VIEW} instead.
     */
    @Deprecated
    public static final String VIEW = "view";

    public static final String MODE = "mode";
    public static final String VALUE = "value";

    public DatePickerPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        final IModel<Date> model = new MapEmptyDateToNullModel(getModel());
        final Mode mode = Mode.fromString(config.getString(MODE), Mode.VIEW);
        if (mode == Mode.EDIT) {
            add(newDateFieldWidget(context, config, model));
        } else {
            add(newLabel(model));
        }
        setOutputMarkupId(true);
    }

    private DateFieldWidget newDateFieldWidget(final IPluginContext context, final IPluginConfig config, final IModel<Date> valueModel) {
        return new DateFieldWidget(VALUE, valueModel, context, config);
    }

    private Label newLabel(final IModel<Date> valueModel) {
        final boolean dateOnly = getPluginConfig().getAsBoolean(DateFieldWidget.CONFIG_HIDE_TIME, false);
        return  new DateTimeLabel(VALUE, valueModel, FormatStyle.LONG, FormatStyle.SHORT, dateOnly);
    }

}
