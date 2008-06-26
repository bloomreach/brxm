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
package org.hippoecm.frontend.plugins.cms.management.sa;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.FeedbackMessages;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.repository.api.HippoNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeActionsPlugin extends RenderPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(NodeActionsPlugin.class);

    private static final String ACTION_OK = "save";
    private static final String ACTION_CANCEL = "cancel";

    private static final List<String> builtin = new ArrayList<String>();

    static {
        builtin.add(ACTION_OK);
        builtin.add(ACTION_CANCEL);
    }

    private JcrNodeModel nodeModel;

    public NodeActionsPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        nodeModel = (JcrNodeModel) getModel();

        List<String> actions = new ArrayList<String>(builtin);
        String[] extraActions = config.getStringArray("actions");
        if (extraActions != null) {
            for (String action : extraActions) {
                if (!actions.contains(action))
                    actions.add(action);
            }
        }

        final ListView actionsView = new ListView("actions", actions) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem item) {
                final String operation = (String) item.getModelObject();

                item.add(new AjaxButton("action") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form form) {
                        if (builtin.contains(operation)) {
                            executeBuiltinAction(target, operation);
                            // Request request = getTopChannel().createRequest("feedback", null);
                            // getTopChannel().send(request);
                            // request.getContext().apply(target);
                        } else {
                            // Channel top = getTopChannel();
                            // Request request = top.createRequest(operation, model);
                            // top.send(request);
                            // request.getContext().apply(target);
                        }
                    }

                    @Override
                    protected void onError(AjaxRequestTarget target, Form form) {
                        if (operation.equals(ACTION_OK)) {
                            // Request request = getTopChannel().createRequest("feedback", null);
                            // getTopChannel().send(request);
                            // request.getContext().apply(target);
                        } else if (operation.equals(ACTION_CANCEL)) {
                            //TODO: clear error messages
                            executeBuiltinAction(target, operation);
                        }
                        super.onError(target, form);
                    }

                }.add(new Label("actionLabel", new Model(operation))));
            }
        };
        add(actionsView);
    }

    private void executeBuiltinAction(AjaxRequestTarget target, String operation) {
        if (operation.equals(ACTION_OK)) {
            try {
                Node parentNode = nodeModel.getNode().getParent();
                parentNode.save();
                //  Request request = getTopChannel().createRequest("flush", new JcrNodeModel(parentNode));
                //  getTopChannel().send(request);
                //  request.getContext().apply(target);

                info("Action " + ACTION_OK + " successfull");
            } catch (RepositoryException e) {
                log.error("An error occured while executing ACTION_OK", e);
            }
        } else if (operation.equals(ACTION_CANCEL)) {
            //first remove all error messages
            FeedbackMessages msgs = Session.get().getFeedbackMessages();
            msgs.clear(new IFeedbackMessageFilter() {
                private static final long serialVersionUID = 1L;

                public boolean accept(FeedbackMessage message) {
                    return message.isError();
                }

            });

            HippoNode node = nodeModel.getNode();
            try {
                Node parentNode = node.getParent();
                if (node.isNew()) {
                    String displayName = node.getDisplayName();
                    node.remove();
                    // Request request = getTopChannel().createRequest("flush", new JcrNodeModel(parentNode));
                    // getTopChannel().send(request);
                    // request.getContext().apply(target);
                    info("User " + displayName + " removed");
                }
                //  Request selectRequest = getTopChannel().createRequest("select", new JcrNodeModel(parentNode));
                //  getTopChannel().send(selectRequest);
                //  selectRequest.getContext().apply(target);

            } catch (RepositoryException e) {
                log.error("An error occured while executing ACTION_CANCEL", e);
            }
        }
    }

}
