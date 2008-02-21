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
package org.hippoecm.cmsprototype.frontend.plugins.actions;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.model.Model;
import org.hippoecm.cmsprototype.frontend.model.content.DocumentVariant;
import org.hippoecm.cmsprototype.frontend.model.exception.ModelWrapException;
import org.hippoecm.frontend.dialog.DialogLink;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.ChannelFactory;
import org.hippoecm.frontend.plugin.channel.Notification;
import org.hippoecm.frontend.plugin.channel.Request;
import org.hippoecm.repository.api.HippoNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple plugin to list the available non-workflow actions for a
 * {@ link DocumentVariant}.
 * 
 * At the moment there is only one (hardcoded) action ("edit")
 * for {@ link DocumentVariant}s with state "draft".
 *
 */
public class ActionsPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ActionsPlugin.class);

    private AjaxLink edit;
    private DialogLink copy;
    private DialogLink move;
    private DialogLink delete;
    private DialogLink rename;

    public ActionsPlugin(PluginDescriptor pluginDescriptor, IPluginModel model, Plugin parentPlugin) {
        super(pluginDescriptor, new JcrNodeModel(model), parentPlugin);

        edit = new AjaxLink("edit-link", getPluginModel()) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                Channel channel = getDescriptor().getIncoming();
                if (channel != null) {
                    Request request = channel.createRequest("edit", getPluginModel());
                    channel.send(request);
                    request.getContext().apply(target);
                }
            }

        };
        add(edit);

        JcrNodeModel jcrModel = (JcrNodeModel) getModel();
        Channel incoming = pluginDescriptor.getIncoming();
        ChannelFactory factory = getPluginManager().getChannelFactory();

        copy = new DialogLink("copy-dialog", new Model("Copy"), CopyDialog.class, jcrModel, incoming, factory);
        add(copy);

        move = new DialogLink("move-dialog", new Model("Move"), MoveDialog.class, jcrModel, incoming, factory);
        add(move);

        delete = new DialogLink("delete-dialog", new Model("Delete"), DeleteDialog.class, jcrModel, incoming, factory);
        add(delete);

        rename = new DialogLink("rename-dialog", new Model("Rename"), RenameDialog.class, jcrModel, incoming, factory);
        add(rename);

        setVisibilities();
    }

    @Override
    public void receive(Notification notification) {
        if ("select".equals(notification.getOperation())) {
            JcrNodeModel model = new JcrNodeModel(notification.getModel());
            setPluginModel(model);
            setVisibilities();
            notification.getContext().addRefresh(this);
        }
        super.receive(notification);
    }

    private void setVisibilities() {
        try {
            Node node = new JcrNodeModel(getPluginModel()).getNode();
            Node canonical = ((HippoNode)node).getCanonicalNode();

            boolean isRoot = node.isNodeType("rep:root");            
            boolean isVirtual = canonical == null || !canonical.isSame(node);
            
            if (isRoot || isVirtual) {
                toggle(false);
            } else {
                toggle(true);
                /* FIXME: kill the cmsprototype.frontend.model package, it's fundamentally flawed */
                DocumentVariant variant = new DocumentVariant((JcrNodeModel) getPluginModel());
                edit.setVisible(variant.getState().equals("draft") || variant.getState().equals(DocumentVariant.NO_STATE));
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
            toggle(false);
        } catch (ModelWrapException e) {
            edit.setVisible(false);
        }
    }

    private void toggle(boolean enabled) {
        copy.setVisible(enabled);
        move.setVisible(enabled);
        delete.setVisible(enabled);
        rename.setVisible(enabled);
    }

}
