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
import org.hippoecm.frontend.legacy.template.TemplateDescriptor;
import org.hippoecm.frontend.legacy.template.model.ItemProvider;
import org.hippoecm.frontend.legacy.template.model.TemplateModel;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.repository.api.HippoNodeType;

//SA port: not necessary, ListViewPlugin does the same
public class NodeTemplatePlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    public NodeTemplatePlugin(PluginDescriptor pluginDescriptor, IPluginModel pluginModel, Plugin parentPlugin) {
        super(pluginDescriptor, new TemplateModel(pluginModel), parentPlugin);

        TemplateModel model = (TemplateModel) getPluginModel();
        TemplateDescriptor descriptor = model.getTemplateDescriptor();

        // FIXME: this will fail when model.getPath() is null, i.e. the item has not yet been created.
        JcrItemModel itemModel = new JcrItemModel(model.getNodeModel().getItemModel().getPath() + "/" + model.getPath());
        ItemProvider provider = new ItemProvider(descriptor, new JcrNodeModel(itemModel));
        add(new ItemView("fields", provider, this));

        setOutputMarkupId(true);
    }

    @Override
    public Plugin addChild(PluginDescriptor childDescriptor) {
        if (!childDescriptor.getWicketId().equals(HippoNodeType.HIPPO_ITEM)) {
            return super.addChild(childDescriptor);
        }
        return null;
    }
}
