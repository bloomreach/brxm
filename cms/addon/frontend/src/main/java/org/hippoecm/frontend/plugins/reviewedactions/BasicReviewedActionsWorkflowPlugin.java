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

import org.hippoecm.frontend.core.IPluginConfig;
import org.hippoecm.frontend.core.PluginContext;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.workflow.AbstractWorkflowPlugin;
import org.hippoecm.frontend.plugin.workflow.WorkflowDialogAction;
import org.hippoecm.frontend.plugin.workflow.WorkflowPlugin;
import org.hippoecm.frontend.service.IViewService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.util.ServiceTracker;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.reviewedactions.BasicReviewedActionsWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicReviewedActionsWorkflowPlugin extends AbstractWorkflowPlugin {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(BasicReviewedActionsWorkflowPlugin.class);

    private ServiceTracker<IViewService> viewers;

    public BasicReviewedActionsWorkflowPlugin() {
        viewers = new ServiceTracker<IViewService>(IViewService.class);

        addWorkflowAction("edit-dialog", "Edit document", new WorkflowDialogAction() {
            private static final long serialVersionUID = 1L;

            public void execute(Workflow wf) throws Exception {
                BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) wf;
                Document docRef = workflow.obtainEditableInstance();
                Node docNode = ((UserSession) getSession()).getJcrSession().getNodeByUUID(docRef.getIdentity());
                IViewService viewer = viewers.getService();
                if (viewer != null) {
                    viewer.view(new JcrNodeModel(docNode));
                }
            }
        });
        addWorkflowAction("requestPublication-dialog", "Request publication", new WorkflowDialogAction() {
            private static final long serialVersionUID = 1L;

            public void execute(Workflow wf) throws Exception {
                BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) wf;
                workflow.requestPublication();
            }
        });
        addWorkflowAction("requestDePublication-dialog", "Request unpublication", new WorkflowDialogAction() {
            private static final long serialVersionUID = 1L;

            public void execute(Workflow wf) throws Exception {
                BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) wf;
                workflow.requestDepublication();
            }
        });
        addWorkflowAction("requestDeletion-dialog", "Request delete", new WorkflowDialogAction() {
            private static final long serialVersionUID = 1L;

            public void execute(Workflow wf) throws Exception {
                BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) wf;
                workflow.requestDeletion();
            }
        });
    }

    @Override
    public void init(PluginContext context, IPluginConfig properties) {
        super.init(context, properties);
        if (properties.get(WorkflowPlugin.VIEWER_ID) != null) {
            viewers.open(context, properties.getString(WorkflowPlugin.VIEWER_ID));
        }
    }

    @Override
    public void destroy() {
        viewers.close();
        super.destroy();
    }

}
