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

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.util.string.Strings;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.diff.HtmlDiffModel;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.widgets.TextAreaWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextTemplatePlugin extends RenderPlugin<String> {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(TextTemplatePlugin.class);
    private static final CssResourceReference DIFF_CSS = new CssResourceReference(HtmlDiffModel.class, "diff.css");

    public TextTemplatePlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        final IModel<String> valueModel = getModel();
        IEditor.Mode mode = IEditor.Mode.fromString(config.getString("mode", "view"));
        if (IEditor.Mode.EDIT == mode) {
            TextAreaWidget widget = new TextAreaWidget("value", valueModel);
            if (config.getString("rows") != null) {
                widget.setRows(config.getString("rows"));
            }
            add(widget);
        } else if (IEditor.Mode.COMPARE == mode) {

            final IModel<String> baseModel = context.getService(config.getString("model.compareTo"),
                    IModelReference.class).getModel();

            IModel<String> compareModel = new HtmlDiffModel(new NewLinesToBrModel(baseModel),
                    new NewLinesToBrModel(valueModel));
            add(new Label("value", compareModel).setEscapeModelStrings(false));
        } else {
            add(new Label("value", new NewLinesToBrModel(valueModel)).setEscapeModelStrings(false));
        }
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        IEditor.Mode mode = IEditor.Mode.fromString(getPluginConfig().getString("mode", "view"));
        if (IEditor.Mode.COMPARE == mode) {
            response.render(CssHeaderItem.forReference(DIFF_CSS));
        }
    }

    class NewLinesToBrModel extends AbstractReadOnlyModel<String> {

        IModel<String> wrapped;

        NewLinesToBrModel(IModel<String> wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public String getObject() {
            if (wrapped != null && wrapped.getObject() != null) {
                String object = Strings.escapeMarkup(wrapped.getObject()).toString();
                return Strings.replaceAll(object, "\n", "<br/>").toString();
            }
            return null;
        }

        @Override
        public void detach() {
            if (wrapped != null) {
                wrapped.detach();
            }
        }
    }
}
