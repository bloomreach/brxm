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
package org.hippoecm.frontend.plugins.cms.management;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.Request;
import org.hippoecm.repository.api.HippoNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeActionsPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(NodeActionsPlugin.class);

    private static final String ACTION_OK = "ok";
    private static final String ACTION_CANCEL = "cancel";

    private static final List<String> builtin = new ArrayList<String>();

    static {
        builtin.add(ACTION_OK);
        builtin.add(ACTION_CANCEL);
    }

    private JcrNodeModel nodeModel;

    public NodeActionsPlugin(final PluginDescriptor pluginDescriptor, final IPluginModel model,
            final Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);

        String nodePath = (String) model.getMapRepresentation().get("node");
        nodeModel = new JcrNodeModel(nodePath);

        List<String> actions = new ArrayList<String>(builtin);
        for (String action : pluginDescriptor.getParameter("actions").getStrings()) {
            if (!actions.contains(action))
                actions.add(action);
        }

        final ListView actionsView = new ListView("actions", actions) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem item) {
                final String operation = (String) item.getModelObject();
                item.add(new AjaxLink("action") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (builtin.contains(operation)) {
                            onBuiltinAction(target, operation);
                        } else {
                            Channel top = getTopChannel();
                            Request request = top.createRequest(operation, model);
                            top.send(request);
                            request.getContext().apply(target);
                        }
                    }
                }.add(new Label("actionLabel", new Model(operation))));

            }
        };
        add(actionsView);
    }

    private void onBuiltinAction(AjaxRequestTarget target, String operation) {
        if (operation.equals(ACTION_OK)) {
            try {
                Node parentNode = nodeModel.getNode().getParent();
                parentNode.save();
                Request request = getTopChannel().createRequest("flush", new JcrNodeModel(parentNode));
                getTopChannel().send(request);
                request.getContext().apply(target);

            } catch (RepositoryException e) {
                log.error("An error occured while executing ACTION_OK", e);
            }
        } else if (operation.equals(ACTION_CANCEL)) {
            HippoNode node = nodeModel.getNode();
            if (node.isNew()) {
                try {
                    Node parentNode = node.getParent();
                    node.remove();
                    Request request = getTopChannel().createRequest("flush", new JcrNodeModel(parentNode));
                    getTopChannel().send(request);
                    request.getContext().apply(target);
                } catch (RepositoryException e) {
                    log.error("An error occured while executing ACTION_CANCEL", e);
                }
            }
        }
    }

}
