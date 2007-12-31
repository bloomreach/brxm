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

import org.hippoecm.frontend.dialog.DialogLink;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.ChannelFactory;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.acceptrequest.AcceptRequestDialog;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.cancelrequest.CancelRequestDialog;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.rejectrequest.RejectRequestDialog;

public class FullRequestWorkflowPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    public FullRequestWorkflowPlugin(PluginDescriptor pluginDescriptor, final JcrNodeModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);

        Channel incoming = pluginDescriptor.getIncoming();
        ChannelFactory factory = getPluginManager().getChannelFactory();
        add(new DialogLink("acceptRequest-dialog", "Approve and execute request",
                AcceptRequestDialog.class, model, incoming, factory));
        add(new DialogLink("rejectRequest-dialog", "Reject request (with reason)",
                RejectRequestDialog.class, model, incoming, factory));
        add(new DialogLink("cancelRequest-dialog", "Cancel request",
                CancelRequestDialog.class, model, incoming, factory));
    }

}
