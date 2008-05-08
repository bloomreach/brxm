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

import org.hippoecm.frontend.core.PluginContext;
import org.hippoecm.frontend.plugin.parameters.ParameterValue;
import org.hippoecm.frontend.plugin.workflow.AbstractWorkflowPlugin;
import org.hippoecm.frontend.plugin.workflow.WorkflowDialogAction;
import org.hippoecm.frontend.service.IDynamicService;
import org.hippoecm.frontend.service.IEditService;
import org.hippoecm.frontend.util.ServiceTracker;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.reviewedactions.BasicReviewedActionsWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditingReviewedActionsWorkflowPlugin extends AbstractWorkflowPlugin {
    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(EditingReviewedActionsWorkflowPlugin.class);

    public static final String EDITOR_ID = "workflow.editor";

    private ServiceTracker<IEditService> editor;

    public EditingReviewedActionsWorkflowPlugin() {
        editor = new ServiceTracker<IEditService>(IEditService.class);

        addWorkflowAction("save", "Save", new WorkflowDialogAction() {
            private static final long serialVersionUID = 1L;

            public void execute(Workflow wf) throws Exception {
                BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) wf;
                workflow.commitEditableInstance();
                close();
            }
        });
        addWorkflowAction("revert", "Revert", new WorkflowDialogAction() {
            private static final long serialVersionUID = 1L;

            public void execute(Workflow wf) throws Exception {
                BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) wf;
                workflow.disposeEditableInstance();
                close();
            }
        });
    }

    @Override
    public void init(PluginContext context, String serviceId, Map<String, ParameterValue> properties) {
        super.init(context, serviceId, properties);
        if (properties.get(EDITOR_ID) != null) {
            editor.open(context, properties.get(EDITOR_ID).getStrings().get(0));
        } else {
            log.warn("No editor ({}) specified for service {}", EDITOR_ID, serviceId);
        }
    }

    @Override
    public void destroy() {
        editor.close();
        super.destroy();
    }

    private void close() {
        List<IEditService> services = editor.getServices();
        if (services.size() > 0) {
            if (services.get(0) instanceof IDynamicService) {
                IDynamicService dynamic = (IDynamicService) services.get(0);
                if (dynamic.canDelete()) {
                    dynamic.delete();
                } else {
                    log.warn("Could not close editor");
                }
            } else {
                log.warn("Editor is not dynamic");
            }
        } else {
            log.warn("No editor service found");
        }
    }
}
