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
import org.hippoecm.frontend.plugin.JcrEvent;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.disposeeditableinstance.DisposeEditableInstanceDialog;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.obtaineditableinstance.ObtainEditableInstanceDialog;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.requestdeletion.RequestDeletionDialog;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.requestdepublication.RequestDePublicationDialog;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.requestpublication.RequestPublicationDialog;
import org.hippoecm.repository.reviewedactions.BasicReviewedActionsWorkflow;

public class BasicReviewedActionsWorkflowPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    public BasicReviewedActionsWorkflowPlugin(PluginDescriptor pluginDescriptor, JcrNodeModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);

        final DialogWindow obtainEditableInstanceDialog = new DialogWindow("obtainEditableInstance-dialog", model, false);
        obtainEditableInstanceDialog.setPageCreator(new ModalWindow.PageCreator() {
            private static final long serialVersionUID = 1L;
            public Page createPage() {
                BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) getWorkflow();
                return new ObtainEditableInstanceDialog(obtainEditableInstanceDialog, workflow);
            }
        });
        add(obtainEditableInstanceDialog);
        add(obtainEditableInstanceDialog.dialogLink("obtainEditableInstance"));
        
        final DialogWindow disposeEditableInstanceDialog = new DialogWindow("disposeEditableInstance-dialog", model, false);
        disposeEditableInstanceDialog.setPageCreator(new ModalWindow.PageCreator() {
            private static final long serialVersionUID = 1L;
            public Page createPage() {
                BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) getWorkflow();
                return new DisposeEditableInstanceDialog(disposeEditableInstanceDialog, workflow);
            }
        });
        add(disposeEditableInstanceDialog);
        add(disposeEditableInstanceDialog.dialogLink("disposeEditableInstance"));

        final DialogWindow requestPublicationDialog = new DialogWindow("requestPublication-dialog", model, false);
        requestPublicationDialog.setPageCreator(new ModalWindow.PageCreator() {
            private static final long serialVersionUID = 1L;
            public Page createPage() {
                BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) getWorkflow();
                return new RequestPublicationDialog(requestPublicationDialog, workflow);
            }
        });
        add(requestPublicationDialog);
        add(requestPublicationDialog.dialogLink("requestPublication"));

        final DialogWindow requestDePublicationDialog = new DialogWindow("requestDePublication-dialog", model, false);
        requestDePublicationDialog.setPageCreator(new ModalWindow.PageCreator() {
            private static final long serialVersionUID = 1L;
            public Page createPage() {
                BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) getWorkflow();
                return new RequestDePublicationDialog(requestDePublicationDialog, workflow);
            }
        });
        add(requestDePublicationDialog);
        add(requestDePublicationDialog.dialogLink("requestDePublication"));

        final DialogWindow requestDeletionDialog = new DialogWindow("requestDeletion-dialog", model, false);
        requestDeletionDialog.setPageCreator(new ModalWindow.PageCreator() {
            private static final long serialVersionUID = 1L;
            public Page createPage() {
                BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) getWorkflow();
                return new RequestDeletionDialog(requestDeletionDialog, workflow);
            }
        });
        add(requestDeletionDialog);
        add(requestDeletionDialog.dialogLink("requestDeletion"));
    }

    public void update(AjaxRequestTarget target, JcrEvent jcrEvent) {
        //Nothing much to do here
    }

}
