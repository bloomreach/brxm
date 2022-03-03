/*
 *  Copyright 2008-2022 Hippo B.V. (http://www.onehippo.com)
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
import org.apache.wicket.util.string.Strings;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.diff.DefaultHtmlDiffService;
import org.hippoecm.frontend.plugins.standards.diff.DiffService;
import org.hippoecm.frontend.plugins.standards.diff.HtmlDiffModel;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.widgets.TextAreaWidget;

public class TextTemplatePlugin extends RenderPlugin<String> {

    public TextTemplatePlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        final IModel<String> valueModel = getModel();
        final IEditor.Mode mode = IEditor.Mode.fromString(config.getString("mode"), IEditor.Mode.VIEW);
        switch (mode) {
            case EDIT:
                final TextAreaWidget widget = new TextAreaWidget("value", valueModel);
                if (config.getString("rows") != null) {
                    widget.setRows(config.getString("rows"));
                }
                if (config.getString("maxlength") != null) {
                    widget.setMaxlength(config.getString("maxlength"));
                }
                add(widget);
                break;

            case COMPARE:
                final IModel<String> baseModel = context.getService(config.getString("model.compareTo"),
                        IModelReference.class).getModel();

                final IModel<String> compareModel = new HtmlDiffModel(new NewLinesToBrModel(baseModel),
                        new NewLinesToBrModel(valueModel), getDiffService(context));
                add(new Label("value", compareModel).setEscapeModelStrings(false));
                break;

            default:
                add(new Label("value", new NewLinesToBrModel(valueModel)).setEscapeModelStrings(false));
        }
    }

    private DiffService getDiffService(final IPluginContext context) {
        final String serviceId = getPluginConfig().getString(DiffService.SERVICE_ID);
        return context.getService(serviceId, DefaultHtmlDiffService.class);
    }

    private static class NewLinesToBrModel implements IModel<String> {

        final IModel<String> wrapped;

        NewLinesToBrModel(final IModel<String> wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public String getObject() {
            if (wrapped == null) {
                return null;
            }

            final String object = wrapped.getObject();
            if (object == null) {
                return null;
            }

            final String escaped = Strings.escapeMarkup(object).toString();
            return Strings.replaceAll(escaped, "\n", "<br/>").toString();
        }

        @Override
        public void detach() {
            if (wrapped != null) {
                wrapped.detach();
            }
        }
    }
}
