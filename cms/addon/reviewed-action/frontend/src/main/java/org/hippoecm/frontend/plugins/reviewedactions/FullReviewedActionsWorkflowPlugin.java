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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.WorkflowsModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.workflow.AbstractWorkflowPlugin;
import org.hippoecm.frontend.plugin.workflow.WorkflowAction;
import org.hippoecm.frontend.service.IViewService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.ISO9075Helper;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.reviewedactions.FullReviewedActionsWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FullReviewedActionsWorkflowPlugin extends AbstractWorkflowPlugin {
    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(FullReviewedActionsWorkflowPlugin.class);

    @SuppressWarnings("unused")
    private String caption = "unknown document";
    private String stateSummary = "UNKNOWN";

    public FullReviewedActionsWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        add(new Label("caption", new PropertyModel(this, "caption")));

        add(new Label("status", new PropertyModel(this, "stateSummary")));

        addWorkflowAction("edit-dialog", "Edit document", new WorkflowAction() {
            private static final long serialVersionUID = 1L;

            public void execute(Workflow wf) throws Exception {
                FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) wf;
                Document docRef = workflow.obtainEditableInstance();
                Node docNode = ((UserSession) getSession()).getJcrSession().getNodeByUUID(docRef.getIdentity());
                IViewService editor = getPluginContext().getService(
                        getPluginConfig().getString(IViewService.VIEWER_ID), IViewService.class);
                if (editor != null) {
                    editor.view(new JcrNodeModel(docNode));
                } else {
                    log.warn("No editor found to edit {}", docNode.getPath());
                }
            }
        });

        addWorkflowAction("requestPublication-dialog", "Request publication", new Visibility() {
            private static final long serialVersionUID = 1L;

            public boolean isVisible() {
                return !(stateSummary.equals("review") || stateSummary.equals("live"));
            }
        }, new WorkflowAction() {
            private static final long serialVersionUID = 1L;

            public void execute(Workflow wf) throws Exception {
                FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) wf;
                workflow.requestPublication();
            }
        });

        addWorkflowAction("requestDePublication-dialog", "Request unpublication", new Visibility() {
            private static final long serialVersionUID = 1L;

            public boolean isVisible() {
                return !(stateSummary.equals("review") || stateSummary.equals("new"));
            }
        }, new WorkflowAction() {
            private static final long serialVersionUID = 1L;

            public void execute(Workflow wf) throws Exception {
                FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) wf;
                workflow.requestDepublication();
            }
        });

        addWorkflowAction("requestDeletion-dialog", "Request delete", new Visibility() {
            private static final long serialVersionUID = 1L;

            public boolean isVisible() {
                return !(stateSummary.equals("review") || stateSummary.equals("live"));
            }
        }, new WorkflowAction() {
            private static final long serialVersionUID = 1L;

            public void execute(Workflow wf) throws Exception {
                FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) wf;
                workflow.requestDeletion();
            }
        });

        addWorkflowAction("publish-dialog", "Publish", new Visibility() {
            private static final long serialVersionUID = 1L;

            public boolean isVisible() {
                return !(stateSummary.equals("review") || stateSummary.equals("live"));

            }
        }, new WorkflowAction() {
            private static final long serialVersionUID = 1L;

            public void execute(Workflow wf) throws Exception {
                FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) wf;
                workflow.publish();
            }
        });

        addWorkflowAction("dePublish-dialog", "Unpublish", new Visibility() {
            private static final long serialVersionUID = 1L;

            public boolean isVisible() {
                return !(stateSummary.equals("review") || stateSummary.equals("new"));
            }
        }, new WorkflowAction() {
            private static final long serialVersionUID = 1L;

            public void execute(Workflow wf) throws Exception {
                FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) wf;
                workflow.depublish();
            }
        });

        addWorkflowAction("delete-dialog", "Unpublish and/or delete", new WorkflowAction() {
            private static final long serialVersionUID = 1L;

            public void execute(Workflow wf) throws Exception {
                FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) wf;
                workflow.delete();
            }
        });

        WorkflowsModel model = (WorkflowsModel) getModel();
        try {
            Node node = model.getNodeModel().getNode();
            caption = ISO9075Helper.decodeLocalName(node.getName());

            if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                for (NodeIterator iter = node.getNodes(node.getName()); iter.hasNext();)
                    node = iter.nextNode(); // FIXME: take the last one, the first should be good enough
            }
            if (node.hasProperty("hippostd:stateSummary"))
                stateSummary = node.getProperty("hippostd:stateSummary").getString();
        } catch (RepositoryException ex) {
            // status unknown, maybe there are legit reasons for this, so don't emit a warning
        }
    }

}
