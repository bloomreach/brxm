package org.hippoecm.frontend.plugins.reviewedactions;

import org.apache.wicket.Page;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.hippoecm.frontend.dialog.DialogWindow;
import org.hippoecm.frontend.model.JcrEvent;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.WorkflowPlugin;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.acceptrequest.AcceptRequestDialog;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.cancelrequest.CancelRequestDialog;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.rejectrequest.RejectRequestDialog;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.reviewedactions.RequestWorkflow;

public class RequestWorkflowPlugin extends WorkflowPlugin {
    private static final long serialVersionUID = 1L;

    public RequestWorkflowPlugin(String id, final JcrNodeModel model, WorkflowManager workflowManager,
            WorkflowDescriptor workflowDescriptor) {
        super(id, model, workflowManager, workflowDescriptor);

        final DialogWindow acceptRequestDialog = new DialogWindow("acceptRequest-dialog", model, false);
        acceptRequestDialog.setPageCreator(new ModalWindow.PageCreator() {
            private static final long serialVersionUID = 1L;
            public Page createPage() {
                return new AcceptRequestDialog(acceptRequestDialog, (RequestWorkflow) getWorkflow());
            }
        });
        add(acceptRequestDialog);
        add(acceptRequestDialog.dialogLink("acceptRequest"));

        final DialogWindow rejectRequestDialog = new DialogWindow("rejectRequest-dialog", model, false);
        rejectRequestDialog.setPageCreator(new ModalWindow.PageCreator() {
            private static final long serialVersionUID = 1L;
            public Page createPage() {
                return new RejectRequestDialog(rejectRequestDialog, (RequestWorkflow) getWorkflow());
            }
        });
        add(rejectRequestDialog);
        add(rejectRequestDialog.dialogLink("rejectRequest"));

        final DialogWindow cancelRequestDialog = new DialogWindow("cancelRequest-dialog", model, false);
        cancelRequestDialog.setPageCreator(new ModalWindow.PageCreator() {
            private static final long serialVersionUID = 1L;
            public Page createPage() {
                return new CancelRequestDialog(cancelRequestDialog, (RequestWorkflow) getWorkflow());
            }
        });
        add(cancelRequestDialog);
        add(cancelRequestDialog.dialogLink("cancelRequest"));
    }

    public void update(JcrEvent jcrEvent) {
        //Nothing much to do here
    }

}
