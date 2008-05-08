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

import java.util.List;
import java.util.Map;

import javax.jcr.Node;

import org.hippoecm.frontend.core.PluginContext;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.parameters.ParameterValue;
import org.hippoecm.frontend.plugin.workflow.AbstractWorkflowPlugin;
import org.hippoecm.frontend.plugin.workflow.WorkflowDialogAction;
import org.hippoecm.frontend.service.IEditService;
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

    public static final String EDITOR_ID = "workflow.editor";

    private ServiceTracker<IEditService> editors;

    public BasicReviewedActionsWorkflowPlugin() {
        editors = new ServiceTracker<IEditService>(IEditService.class);

        addWorkflowAction("edit-dialog", "Edit document", new WorkflowDialogAction() {
            private static final long serialVersionUID = 1L;

            public void execute(Workflow wf) throws Exception {
                BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) wf;
                Document docRef = workflow.obtainEditableInstance();
                Node docNode = ((UserSession) getSession()).getJcrSession().getNodeByUUID(docRef.getIdentity());
                List<IEditService> services = editors.getServices();
                if (services.size() > 0) {
                    services.get(0).edit(new JcrNodeModel(docNode));
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
    public void init(PluginContext context, String serviceId, Map<String, ParameterValue> properties) {
        super.init(context, serviceId, properties);
        if (properties.get(EDITOR_ID) != null) {
            editors.open(context, properties.get(EDITOR_ID).getStrings().get(0));
        }
    }

    @Override
    public void destroy() {
        editors.close();
        super.destroy();
    }

}
