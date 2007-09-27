package org.hippoecm.frontend.plugins.reviewedactions;

import java.rmi.RemoteException;

import javax.jcr.RepositoryException;

import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.hippoecm.frontend.dialog.DialogWindow;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.WorkflowPlugin;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.api.WorkflowMappingException;
import org.hippoecm.repository.reviewedactions.RequestWorkflow;

public class RequestWorkflowPlugin extends WorkflowPlugin {
    private static final long serialVersionUID = 1L;

    public RequestWorkflowPlugin(String id, final JcrNodeModel model, WorkflowManager workflowManager,
            WorkflowDescriptor workflowDescriptor) {
        super(id, model, workflowManager, workflowDescriptor);

        Callback acceptRequest = new Callback() {
            public void execute(Workflow workflow) throws WorkflowMappingException, RemoteException, WorkflowException,
                    RepositoryException {
                ((RequestWorkflow) workflow).acceptRequest();
            }
        };
        add(workflowMethodCaller("acceptRequest", model, "", acceptRequest));

        final DialogWindow nodeDialog = new DialogWindow("rejectRequest-dialog", model, false);
        nodeDialog.setPageCreator(new ModalWindow.PageCreator() {
            private static final long serialVersionUID = 1L;
            public Page createPage() {
                return new RejectRequestDialog(nodeDialog, model, (RequestWorkflow) getWorkflow());
            }
        });
        add(nodeDialog);
        add(nodeDialog.dialogLink("rejectRequest"));

        Callback cancelRequest = new Callback() {
            public void execute(Workflow workflow) throws WorkflowMappingException, RemoteException, WorkflowException,
                    RepositoryException {
                ((RequestWorkflow) workflow).cancelRequest();
            }
        };
        add(workflowMethodCaller("cancelRequest", model, "", cancelRequest));
    }

    public void update(AjaxRequestTarget target, JcrNodeModel model) {
        // TODO Auto-generated method stub

    }

}
