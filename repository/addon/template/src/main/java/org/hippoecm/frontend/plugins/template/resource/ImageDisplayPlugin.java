/*
 * Copyright 2008 Hippo
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
package org.hippoecm.frontend.plugins.template.resource;

import java.io.IOException;

import org.apache.wicket.markup.html.image.Image;
import org.hippoecm.frontend.legacy.model.IPluginModel;
import org.hippoecm.frontend.legacy.plugin.Plugin;
import org.hippoecm.frontend.legacy.plugin.PluginDescriptor;
import org.hippoecm.frontend.legacy.plugin.channel.Notification;
import org.hippoecm.frontend.legacy.template.model.ItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.resource.JcrResource;
import org.hippoecm.frontend.resource.JcrResourceStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageDisplayPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ImageDisplayPlugin.class);

    private JcrResourceStream resource;

    public ImageDisplayPlugin(PluginDescriptor descriptor, IPluginModel model, Plugin parentPlugin) {
        super(descriptor, new ItemModel(model), parentPlugin);

        ItemModel itemModel = (ItemModel) getModel();
        resource = new JcrResourceStream(itemModel.getNodeModel().getNode());

        add(new Image("image", new JcrResource(resource)));
    }

    @Override
    public void receive(Notification notification) {
        if ("flush".equals(notification.getOperation())) {
            JcrNodeModel newModel = new JcrNodeModel(notification.getModel());
            String path = newModel.getItemModel().getPath();
            String target = ((ItemModel) getModel()).getNodeModel().getItemModel().getPath();
            if (target.length() >= path.length() && target.substring(0, path.length()).equals(path)) {
                try {
                    resource.close();
                } catch (IOException ex) {
                    log.error(ex.getMessage());
                }
            }
        }
        super.receive(notification);
    }

    @Override
    public void destroy() {
        try {
            resource.close();
        } catch (IOException ex) {
            log.error(ex.getMessage());
        }
        super.destroy();
    }
}
