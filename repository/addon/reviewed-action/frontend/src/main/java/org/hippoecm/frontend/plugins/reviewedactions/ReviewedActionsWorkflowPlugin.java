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

import java.rmi.RemoteException;

import javax.jcr.RepositoryException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.WorkflowPlugin;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.api.WorkflowMappingException;
import org.hippoecm.repository.reviewedactions.ReviewedActionsWorkflow;

public class ReviewedActionsWorkflowPlugin extends WorkflowPlugin {
    private static final long serialVersionUID = 1L;

    public ReviewedActionsWorkflowPlugin(String id, final JcrNodeModel model, WorkflowManager workflowManager,
            WorkflowDescriptor workflowDescriptor) {
        super(id, model, workflowManager, workflowDescriptor);

        Callback obtainEditableInstance = new Callback() {
            public void execute(Workflow workflow) throws WorkflowMappingException, RemoteException, WorkflowException,
                    RepositoryException {
                ((ReviewedActionsWorkflow) workflow).obtainEditableInstance();
            }
        };
        add(workflowMethodCaller("obtainEditableInstance", model, "Obtain editable copy", obtainEditableInstance));

        Callback disposeEditableInstance = new Callback() {
            public void execute(Workflow workflow) throws WorkflowMappingException, RemoteException, WorkflowException,
                    RepositoryException {
                ((ReviewedActionsWorkflow) workflow).disposeEditableInstance();
            }
        };
        add(workflowMethodCaller("disposeEditableInstance", model, "Discard editable copy", disposeEditableInstance));

        Callback requestPublication = new Callback() {
            public void execute(Workflow workflow) throws WorkflowMappingException, RemoteException, WorkflowException,
                    RepositoryException {
                ((ReviewedActionsWorkflow) workflow).requestPublication();
            }
        };
        add(workflowMethodCaller("requestPublication", model, "Request publication", requestPublication));

        Callback requestDepublication = new Callback() {
            public void execute(Workflow workflow) throws WorkflowMappingException, RemoteException, WorkflowException,
                    RepositoryException {
                ((ReviewedActionsWorkflow) workflow).requestDepublication();
            }
        };
        add(workflowMethodCaller("requestDepublication", model, "Request unpublication", requestDepublication));

        Callback requestDeletion = new Callback() {
            public void execute(Workflow workflow) throws WorkflowMappingException, RemoteException, WorkflowException,
                    RepositoryException {
                ((ReviewedActionsWorkflow) workflow).requestDeletion();
            }
        };
        add(workflowMethodCaller("requestDeletion", model, "Request delete", requestDeletion));

        Callback publish = new Callback() {
            public void execute(Workflow workflow) throws WorkflowMappingException, RemoteException, WorkflowException,
                    RepositoryException {
                ((ReviewedActionsWorkflow) workflow).publish();
            }
        };
        add(workflowMethodCaller("publish", model, "Publish", publish));

        Callback depublish = new Callback() {
            public void execute(Workflow workflow) throws WorkflowMappingException, RemoteException, WorkflowException,
                    RepositoryException {
                ((ReviewedActionsWorkflow) workflow).depublish();
            }
        };
        add(workflowMethodCaller("depublish", model, "Unpublish", depublish));

        Callback delete = new Callback() {
            public void execute(Workflow workflow) throws WorkflowMappingException, RemoteException, WorkflowException,
                    RepositoryException {
                ((ReviewedActionsWorkflow) workflow).delete();
            }
        };
        add(workflowMethodCaller("delete", model, "Unpublish and/or delete", delete));
    }

    public void update(final AjaxRequestTarget target, final JcrNodeModel model) {
        //Nothing much to do
    }

}
