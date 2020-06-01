/*
 *  Copyright 2008-2015 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.util.string.Strings;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.model.properties.StringConverter;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.diff.TextDiffModel;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValueTemplatePlugin extends RenderPlugin<String> {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ValueTemplatePlugin.class);

    public ValueTemplatePlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        final StringConverter stringModel = new StringConverter((JcrPropertyValueModel) getModel());
        final IEditor.Mode mode = IEditor.Mode.fromString(config.getString("mode"), IEditor.Mode.VIEW);
        switch (mode) {
            case EDIT:
                TextFieldWidget widget = new TextFieldWidget("value", stringModel);
                if (config.getString("size") != null) {
                    widget.setSize(config.getString("size"));
                }
                if (config.getString("maxlength") != null) {
                    widget.setMaxlength(config.getString("maxlength"));
                }
                add(widget);
                break;
            case COMPARE:
                final IModel<?> baseModel = context.getService(config.getString("model.compareTo"), IModelReference.class)
                        .getModel();
                final IModel<String> stringBaseModel = new LoadableDetachableModel<String>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected String load() {
                        return Strings.toString(baseModel.getObject());
                    }

                    @Override
                    public void detach() {
                        super.detach();
                        baseModel.detach();
                    }
                };
                add(new Label("value", new TextDiffModel(stringBaseModel, stringModel)).setEscapeModelStrings(false));
                break;
            default:
                add(new Label("value", stringModel));
        }
    }

}
