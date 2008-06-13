/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.plugins.template.item;

import org.hippoecm.frontend.legacy.model.IPluginModel;
import org.hippoecm.frontend.legacy.plugin.Plugin;
import org.hippoecm.frontend.legacy.plugin.PluginDescriptor;
import org.hippoecm.frontend.legacy.template.ItemDescriptor;
import org.hippoecm.frontend.legacy.template.model.ItemModel;
import org.hippoecm.frontend.legacy.template.model.ItemProvider;
import org.hippoecm.frontend.plugins.template.ItemView;
import org.hippoecm.repository.api.HippoNodeType;

public class ItemListPlugin extends Plugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    public ItemListPlugin(PluginDescriptor pluginDescriptor, IPluginModel pluginModel, Plugin parentPlugin) {
        super(pluginDescriptor, new ItemModel(pluginModel), parentPlugin);

        ItemModel model = (ItemModel) getPluginModel();
        ItemDescriptor descriptor = model.getDescriptor();

        ItemProvider provider = new ItemProvider(descriptor, model.getNodeModel());
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
