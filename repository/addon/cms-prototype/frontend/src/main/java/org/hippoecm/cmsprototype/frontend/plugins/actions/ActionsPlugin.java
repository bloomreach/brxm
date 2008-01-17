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
package org.hippoecm.cmsprototype.frontend.plugins.actions;

import javax.jcr.RepositoryException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.hippoecm.cmsprototype.frontend.model.content.DocumentVariant;
import org.hippoecm.cmsprototype.frontend.model.exception.ModelWrapException;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.Notification;
import org.hippoecm.frontend.plugin.channel.Request;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;

/**
 * This component extends the MenuPlugin but essentially only overrides
 * the markup.
 * TODO: find a way to use alternative markup without having to extend the class
 *
 */
public class ActionsPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    AjaxLink link;

    public ActionsPlugin(PluginDescriptor pluginDescriptor, JcrNodeModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);

        link = new AjaxLink("editlink", model) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                Channel channel = getDescriptor().getIncoming();
                if(channel != null) {
                        Request request = channel.createRequest("edit", getNodeModel().getMapRepresentation());
                        channel.send(request);
                        request.getContext().apply(target);
                }
            }

        };
        add(link);

        try {
            DocumentVariant variant = new DocumentVariant(model);
            link.setVisible(variant.getState().equals("draft"));
        } catch (ModelWrapException e) {
            link.setVisible(false);
        }

    }

    @Override
    public void receive(Notification notification) {
        if ("select".equals(notification.getOperation())) {
            JcrNodeModel model = new JcrNodeModel(notification.getData());
            setNodeModel(model);
            try {
                DocumentVariant variant = new DocumentVariant(model);
                link.setVisible(variant.getState().equals("draft"));
            } catch (ModelWrapException e) {
                link.setVisible(false);
            }
            notification.getContext().addRefresh(this);
        }
        super.receive(notification);
    }
    
}
