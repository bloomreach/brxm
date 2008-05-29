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
package org.hippoecm.frontend.sa.plugins.reviewedactions;

import javax.jcr.Node;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.sa.plugin.IPluginContext;
import org.hippoecm.frontend.sa.plugin.config.IPluginConfig;
import org.hippoecm.frontend.sa.plugin.workflow.AbstractWorkflowPlugin;
import org.hippoecm.frontend.sa.plugin.workflow.WorkflowAction;
import org.hippoecm.frontend.sa.plugin.workflow.WorkflowPlugin;
import org.hippoecm.frontend.sa.service.IViewService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.reviewedactions.BasicReviewedActionsWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicReviewedActionsWorkflowPlugin extends AbstractWorkflowPlugin {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(BasicReviewedActionsWorkflowPlugin.class);

    public BasicReviewedActionsWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        addWorkflowAction("edit-dialog", "Edit document", new WorkflowAction() {
            private static final long serialVersionUID = 1L;

            public void execute(Workflow wf) throws Exception {
                BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) wf;
                Document docRef = workflow.obtainEditableInstance();
                Node docNode = ((UserSession) getSession()).getJcrSession().getNodeByUUID(docRef.getIdentity());
                IViewService viewer = getPluginContext().getService(
                        getPluginConfig().getString(WorkflowPlugin.VIEWER_ID), IViewService.class);
                if (viewer != null) {
                    viewer.view(new JcrNodeModel(docNode));
                }
            }
        });
        addWorkflowAction("requestPublication-dialog", "Request publication", new WorkflowAction() {
            private static final long serialVersionUID = 1L;

            public void execute(Workflow wf) throws Exception {
                BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) wf;
                workflow.requestPublication();
            }
        });
        addWorkflowAction("requestDePublication-dialog", "Request unpublication", new WorkflowAction() {
            private static final long serialVersionUID = 1L;

            public void execute(Workflow wf) throws Exception {
                BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) wf;
                workflow.requestDepublication();
            }
        });
        addWorkflowAction("requestDeletion-dialog", "Request delete", new WorkflowAction() {
            private static final long serialVersionUID = 1L;

            public void execute(Workflow wf) throws Exception {
                BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) wf;
                workflow.requestDeletion();
            }
        });
    }

}
