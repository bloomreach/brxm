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
package org.hippoecm.frontend.plugins.template;

import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.template.model.TemplateModel;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValueTemplatePlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ValueTemplatePlugin.class);

    private JcrPropertyValueModel valueModel;

    public ValueTemplatePlugin(PluginDescriptor pluginDescriptor, IPluginModel pluginModel, Plugin parentPlugin) {
        super(pluginDescriptor, new TemplateModel(pluginModel), parentPlugin);

        TemplateModel model = (TemplateModel) getPluginModel();
        valueModel = model.getJcrPropertyValueModel();

        add(new TextFieldWidget("value", valueModel));

        setOutputMarkupId(true);
    }

    @Override
    public void onDetach() {
        if (valueModel == null) {
            log.error("ValueModel is null: " + getPluginModel().toString());
        } else {
            valueModel.detach();
        }
        super.onDetach();
    }
}
