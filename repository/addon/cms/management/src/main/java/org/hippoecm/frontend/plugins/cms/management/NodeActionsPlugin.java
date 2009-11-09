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
package org.hippoecm.frontend.plugins.cms.management;

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
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
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

    public NodeActionsPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        final ListView actionsView = new ListView("actions", new ArrayList<String>(builtin)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem item) {
                final String operation = (String) item.getModelObject();

                item.add(new AjaxButton("action") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form form) {
                        executeBuiltinAction(target, operation);
                    }

                    @Override
                    protected void onError(AjaxRequestTarget target, Form form) {
                        if (operation.equals(ACTION_OK)) {
                            //show feedback
                        } else if (operation.equals(ACTION_CANCEL)) {
                            //TODO: clear error messages
                            executeBuiltinAction(target, operation);
                        }
                        super.onError(target, form);
                    }

                }.add(new Label("actionLabel", new StringResourceModel(operation, this, null))));
            }
        };
        add(actionsView);
    }

    private void executeBuiltinAction(AjaxRequestTarget target, String operation) {
        JcrNodeModel nodeModel = (JcrNodeModel) getModel();
        if (operation.equals(ACTION_OK)) {
            try {
                Node node = nodeModel.getNode();
                if (node.getSession().hasPendingChanges() && node.getParent() != null) {
                    node.getParent().getSession().save();
                    info("Action " + ACTION_OK + " successfull.");
                    List<RenderPlugin> x = getPluginContext().getServices("service.management.node", RenderPlugin.class);
                    for (RenderPlugin y : x) {
                        y.modelChanged();
                    }
                    //setModel(new JcrNodeModel(nodeModel.getItemModel().getPath()));
                }
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

            Node node = nodeModel.getNode();
            try {
                if (node.isNew()) {
                    String displayName = node.getName();
                    node.remove();
                    info("User " + displayName + " removed");
                }
            } catch (RepositoryException e) {
                log.error("An error occured while executing ACTION_CANCEL", e);
            }
        }
    }

}
