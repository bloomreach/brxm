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

import org.hippoecm.frontend.legacy.model.IPluginModel;
import org.hippoecm.frontend.legacy.plugin.Plugin;
import org.hippoecm.frontend.legacy.plugin.PluginDescriptor;
import org.hippoecm.frontend.legacy.template.model.TemplateModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.widgets.BooleanFieldWidget;
import org.hippoecm.frontend.widgets.PasswordTextFieldWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// port me!
// (not necessary for defaultcontent, i.e. lower prio)
public class BooleanValueTemplatePlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(BooleanValueTemplatePlugin.class);

    private JcrPropertyValueModel valueModel;

    public BooleanValueTemplatePlugin(PluginDescriptor pluginDescriptor, IPluginModel pluginModel, Plugin parentPlugin) {
        super(pluginDescriptor, new TemplateModel(pluginModel), parentPlugin);

        TemplateModel model = (TemplateModel) getPluginModel();
        valueModel = model.getJcrPropertyValueModel();
        add(new BooleanFieldWidget("value", valueModel));

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
