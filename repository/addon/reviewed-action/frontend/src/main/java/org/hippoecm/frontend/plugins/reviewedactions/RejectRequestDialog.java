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

import org.apache.wicket.extensions.ajax.markup.html.AjaxEditableLabel;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogWindow;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.reviewedactions.RequestWorkflow;

public class RejectRequestDialog extends AbstractDialog {
    private static final long serialVersionUID = 1L;

    private RequestWorkflow workflow;
    private String reason;

    public RejectRequestDialog(final DialogWindow dialogWindow, JcrNodeModel model, RequestWorkflow workflow) {
        super(dialogWindow, model);
        dialogWindow.setTitle("Reject request");       
        this.workflow = workflow;

        add(new AjaxEditableLabel("reason", new PropertyModel(this, "reason")));
        if (model.getNode() == null) {
            ok.setVisible(false);
        }
    }

    public void ok() throws RepositoryException, RemoteException, WorkflowException {
        workflow.rejectRequest(reason);
    }

    public void cancel() {
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;;
    }


}
