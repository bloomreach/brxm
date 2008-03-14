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
package org.hippoecm.cmsprototype.frontend.plugins.perspectives;

import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.PluginModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.Notification;
import org.hippoecm.frontend.plugin.channel.Request;

/**
 * Panel representing the content panel for the first tab.
 */
public class EditPerspective extends Plugin {
    private static final long serialVersionUID = 1L;

    public EditPerspective(PluginDescriptor pluginDescriptor, IPluginModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);
    }

    @Override
    public void receive(Notification notification) {
        if ("edit".equals(notification.getOperation())) {
            JcrNodeModel model = new JcrNodeModel(notification.getModel());
            if (model.equals(getPluginModel())) {
                Channel channel = getTopChannel();
                if (channel != null) {
                    PluginModel pluginModel = new PluginModel();
                    pluginModel.put("plugin", getPluginPath());
                    Request request = channel.createRequest("focus", pluginModel);
                    request.setContext(notification.getContext());
                    channel.send(request);
                }

                Channel outgoing = getBottomChannel();
                if (outgoing != null) {
                    Notification selectNotice = outgoing.createNotification("select", notification.getModel());
                    selectNotice.setContext(notification.getContext());
                    outgoing.publish(selectNotice);
                }
            }

            // don't propagate edit notification to children
            return;
        }
        super.receive(notification);
    }
}
