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
package org.hippoecm.frontend.plugins.reviewedactions;

import org.apache.wicket.model.Model;
import org.hippoecm.frontend.dialog.DialogLink;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.ChannelFactory;
import org.hippoecm.frontend.plugin.channel.Notification;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.disposeeditableinstance.DisposeEditableInstanceDialog;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.obtaineditableinstance.ObtainEditableInstanceDialog;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.requestdeletion.RequestDeletionDialog;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.requestdepublication.RequestDePublicationDialog;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.requestpublication.RequestPublicationDialog;

public class BasicReviewedActionsWorkflowPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    public BasicReviewedActionsWorkflowPlugin(PluginDescriptor pluginDescriptor, IPluginModel model, Plugin parentPlugin) {
        super(pluginDescriptor, new JcrNodeModel(model), parentPlugin);

        JcrNodeModel jcrModel = (JcrNodeModel) getPluginModel();
        Channel channel = getTopChannel();
        ChannelFactory factory = getPluginManager().getChannelFactory();
        add(new DialogLink("obtainEditableInstance-dialog", new Model("Obtain editable copy"),
                ObtainEditableInstanceDialog.class, jcrModel, channel, factory));
        add(new DialogLink("disposeEditableInstance-dialog", new Model("Dispose editable copy"),
                DisposeEditableInstanceDialog.class, jcrModel, channel, factory));
        add(new DialogLink("requestPublication-dialog", new Model("Request publication"),
                RequestPublicationDialog.class, jcrModel, channel, factory));
        add(new DialogLink("requestDePublication-dialog", new Model("Request unpublication"),
                RequestDePublicationDialog.class, jcrModel, channel, factory));
        add(new DialogLink("requestDeletion-dialog", new Model("Request delete"),
                RequestDeletionDialog.class, jcrModel, channel, factory));
    }

    @Override
    public void receive(Notification notification) {
        if ("select".equals(notification.getOperation())) {
            setPluginModel(new JcrNodeModel(notification.getModel()));
        }
        super.receive(notification);
    }

}
