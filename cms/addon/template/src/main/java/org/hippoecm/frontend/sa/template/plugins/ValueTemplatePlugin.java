/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.sa.template.plugins;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.sa.plugin.IPluginContext;
import org.hippoecm.frontend.sa.plugin.config.IPluginConfig;
import org.hippoecm.frontend.sa.service.render.RenderPlugin;
import org.hippoecm.frontend.template.config.TemplateConfig;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValueTemplatePlugin extends RenderPlugin {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ValueTemplatePlugin.class);

    public ValueTemplatePlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        add(new EmptyPanel("value"));
    }

    @Override
    public void onModelChanged() {
        super.onModelChanged();

        JcrPropertyValueModel valueModel = (JcrPropertyValueModel) getModel();
        IPluginConfig config = getPluginConfig();

        String mode = config.getString("mode");
        if (TemplateConfig.EDIT_MODE.equals(mode)) {
            TextFieldWidget widget = new TextFieldWidget("value", valueModel);
            if (config.getString("size") != null) {
                widget.setSize(config.getString("size"));
            }
            replace(widget);
        } else {
            replace(new Label("value", valueModel));
        }
    }

}
