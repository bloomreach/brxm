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
package org.hippoecm.frontend.plugins.template.editor;

import org.apache.wicket.model.Model;
import org.hippoecm.frontend.dialog.DialogLink;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.ChannelFactory;
import org.hippoecm.frontend.plugin.channel.Notification;
import org.hippoecm.frontend.plugin.channel.Request;
import org.hippoecm.frontend.plugins.admin.menu.save.SaveDialog;

public class EditorPlugin extends Plugin {

    private static final long serialVersionUID = 1L;

    private EditorForm form;

    public EditorPlugin(PluginDescriptor pluginDescriptor, IPluginModel model, Plugin parentPlugin) {
        super(pluginDescriptor, new JcrNodeModel(model), parentPlugin);

        JcrNodeModel jcrModel = (JcrNodeModel) getModel();
        Channel channel = getTopChannel();
        ChannelFactory factory = getPluginManager().getChannelFactory();

        DialogLink save = new DialogLink("save-dialog", new Model("Save"), SaveDialog.class, jcrModel, channel, factory);
        add(save);
        form = new EditorForm("form", (JcrNodeModel) getModel(), this);
        add(form);

        setOutputMarkupId(true);
    }

    @Override
    public void receive(Notification notification) {
        if ("select".equals(notification.getOperation())) {
            JcrNodeModel nodeModel = new JcrNodeModel(notification.getModel());
            if (!nodeModel.equals(getPluginModel())) {
                form.destroy();
                replace(form = new EditorForm("form", nodeModel, this));
                notification.getContext().addRefresh(this);
            }
        }
        super.receive(notification);
    }

    @Override
    public void handle(Request request) {
        if ("template.select".equals(request.getOperation())) {
            Channel bottom = getBottomChannel();
            bottom.publish(bottom.createNotification(request));
            return;
        }
        super.handle(request);
    }
}
