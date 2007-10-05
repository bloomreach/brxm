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

import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.hippoecm.frontend.dialog.DialogWindow;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.WorkflowPlugin;
import org.hippoecm.frontend.plugins.reviewedactions.authordialogs.disposeeditableinstance.DisposeEditableInstanceDialog;
import org.hippoecm.frontend.plugins.reviewedactions.authordialogs.obtaineditableinstance.ObtainEditableInstanceDialog;
import org.hippoecm.frontend.plugins.reviewedactions.authordialogs.requestdeletion.RequestDeletionDialog;
import org.hippoecm.frontend.plugins.reviewedactions.authordialogs.requestdepublication.RequestDePublicationDialog;
import org.hippoecm.frontend.plugins.reviewedactions.authordialogs.requestpublication.RequestPublicationDialog;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.reviewedactions.ReviewedActionsWorkflow;
import org.hippoecm.repository.reviewedactions.AuthorReviewedActionsWorkflow;

public class AuthorReviewedActionsWorkflowPlugin extends WorkflowPlugin {
    private static final long serialVersionUID = 1L;

    public AuthorReviewedActionsWorkflowPlugin(String id, final JcrNodeModel model, WorkflowManager workflowManager,
            WorkflowDescriptor workflowDescriptor) {
        super(id, model, workflowManager, workflowDescriptor);

        final DialogWindow obtainEditableInstanceDialog = new DialogWindow("obtainEditableInstance-dialog", model, false);
        obtainEditableInstanceDialog.setPageCreator(new ModalWindow.PageCreator() {
            private static final long serialVersionUID = 1L;
            public Page createPage() {
                return new ObtainEditableInstanceDialog(obtainEditableInstanceDialog, model, (AuthorReviewedActionsWorkflow) getWorkflow());
            }
        });
        add(obtainEditableInstanceDialog);
        add(obtainEditableInstanceDialog.dialogLink("obtainEditableInstance"));
        
        final DialogWindow disposeEditableInstanceDialog = new DialogWindow("disposeEditableInstance-dialog", model, false);
        disposeEditableInstanceDialog.setPageCreator(new ModalWindow.PageCreator() {
            private static final long serialVersionUID = 1L;
            public Page createPage() {
                return new DisposeEditableInstanceDialog(disposeEditableInstanceDialog, model, (AuthorReviewedActionsWorkflow) getWorkflow());
            }
        });
        add(disposeEditableInstanceDialog);
        add(disposeEditableInstanceDialog.dialogLink("disposeEditableInstance"));

        final DialogWindow requestPublicationDialog = new DialogWindow("requestPublication-dialog", model, false);
        requestPublicationDialog.setPageCreator(new ModalWindow.PageCreator() {
            private static final long serialVersionUID = 1L;
            public Page createPage() {
                return new RequestPublicationDialog(requestPublicationDialog, model, (AuthorReviewedActionsWorkflow) getWorkflow());
            }
        });
        add(requestPublicationDialog);
        add(requestPublicationDialog.dialogLink("requestPublication"));

        final DialogWindow requestDePublicationDialog = new DialogWindow("requestDePublication-dialog", model, false);
        requestDePublicationDialog.setPageCreator(new ModalWindow.PageCreator() {
            private static final long serialVersionUID = 1L;
            public Page createPage() {
                return new RequestDePublicationDialog(requestDePublicationDialog, model, (AuthorReviewedActionsWorkflow) getWorkflow());
            }
        });
        add(requestDePublicationDialog);
        add(requestDePublicationDialog.dialogLink("requestDePublication"));

        final DialogWindow requestDeletionDialog = new DialogWindow("requestDeletion-dialog", model, false);
        requestDeletionDialog.setPageCreator(new ModalWindow.PageCreator() {
            private static final long serialVersionUID = 1L;
            public Page createPage() {
                return new RequestDeletionDialog(requestDeletionDialog, model, (AuthorReviewedActionsWorkflow) getWorkflow());
            }
        });
        add(requestDeletionDialog);
        add(requestDeletionDialog.dialogLink("requestDeletion"));
    }

    public void update(final AjaxRequestTarget target, final JcrNodeModel model) {
        //Nothing much to do
    }

}
