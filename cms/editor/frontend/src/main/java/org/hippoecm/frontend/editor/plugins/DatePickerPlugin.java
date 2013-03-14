/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Date;

import javax.jcr.RepositoryException;

import org.apache.wicket.datetime.StyleDateConverter;
import org.apache.wicket.datetime.markup.html.basic.DateLabel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.datetime.DateFieldWidget;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatePickerPlugin extends RenderPlugin<Date> {

    private static final long serialVersionUID = 1L;

    public static final String DATESTYLE = "LS";
    public static final String EDIT = "edit";
    public static final String MODE = "mode";
    public static final String VIEW = "view";
    public static final String VALUE = "value";

    public DatePickerPlugin(IPluginContext context, IPluginConfig config) throws RepositoryException {
        super(context, config);

        IModel<Date> valueModel = getModel();
        if (EDIT.equals(config.getString(MODE, VIEW))) {
            add(new DateFieldWidget(VALUE, valueModel, context, config));
        } else {
            add(new DateLabel(VALUE, valueModel, new StyleDateConverter(DATESTYLE, true)));
        }
        setOutputMarkupId(true);
    }

}
