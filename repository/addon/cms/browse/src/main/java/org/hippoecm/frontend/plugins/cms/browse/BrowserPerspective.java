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
package org.hippoecm.frontend.plugins.cms.browse;

import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.PluginModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.Notification;
import org.hippoecm.frontend.plugin.channel.Request;

public class BrowserPerspective extends Plugin {
    private static final long serialVersionUID = 1L;

    public BrowserPerspective(PluginDescriptor pluginDescriptor, IPluginModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);
    }

    @Override
    public void receive(Notification notification) {
        if ("browse".equals(notification.getOperation())) {
            Channel channel = getTopChannel();
            if (channel != null) {
                // FIXME: should the map be constructed by the PluginDescriptor?
                PluginModel model = new PluginModel();
                model.put("plugin", getPluginPath());
                Request request = channel.createRequest("focus", model);
                request.setContext(notification.getContext());
                channel.send(request);
            }

            Channel outgoing = getBottomChannel();
            if (outgoing != null) {
                Notification selectNotice = outgoing.createNotification("select", notification.getModel());
                selectNotice.setContext(notification.getContext());
                outgoing.publish(selectNotice);
            }
            return;
        }
        super.receive(notification);
    }

    @Override
    public void handle(Request request) {
        String operation = request.getOperation();
        if ("select".equals(operation) || "list".equals(operation)) {
            Channel outgoing = getBottomChannel();
            if (outgoing != null) {
                outgoing.publish(outgoing.createNotification(request));
            }
            return;
        }
        super.handle(request);
    }
}
