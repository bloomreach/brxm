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

import org.hippoecm.frontend.sa.plugin.workflow.AbstractWorkflowPlugin;
import org.hippoecm.frontend.sa.plugin.workflow.WorkflowAction;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.reviewedactions.FullRequestWorkflow;

public class FullRequestWorkflowPlugin extends AbstractWorkflowPlugin {
    private static final long serialVersionUID = 1L;

    public FullRequestWorkflowPlugin() {
        addWorkflowAction("acceptRequest-dialog", "Approve and execute request", new WorkflowAction() {
            private static final long serialVersionUID = 1L;

            public void execute(Workflow wf) throws Exception {
                FullRequestWorkflow workflow = (FullRequestWorkflow) wf;
                workflow.acceptRequest();
            }
        });
        addWorkflowDialog("rejectRequest-dialog", "Reject request (with reason)", "Reject request (with reason)",
                new WorkflowAction() {
                    private static final long serialVersionUID = 1L;

                    public void execute(Workflow wf) throws Exception {
                        FullRequestWorkflow workflow = (FullRequestWorkflow) wf;
                        workflow.rejectRequest(""); // FIXME
                    }
                });
        addWorkflowAction("cancelRequest-dialog", "Cancel request", new WorkflowAction() {
            private static final long serialVersionUID = 1L;

            public void execute(Workflow wf) throws Exception {
                FullRequestWorkflow workflow = (FullRequestWorkflow) wf;
                workflow.cancelRequest();
            }
        });
    }
}
