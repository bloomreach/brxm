package org.hippoecm.frontend.plugins.reviewedactions;

import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.hippoecm.frontend.dialog.DialogWindow;
import org.hippoecm.frontend.model.JcrEvent;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.acceptrequest.AcceptRequestDialog;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.cancelrequest.CancelRequestDialog;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.rejectrequest.RejectRequestDialog;
import org.hippoecm.repository.reviewedactions.FullRequestWorkflow;

public class FullRequestWorkflowPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    public FullRequestWorkflowPlugin(PluginDescriptor pluginDescriptor, final JcrNodeModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);

        final DialogWindow acceptRequestDialog = new DialogWindow("acceptRequest-dialog", model, false);
        acceptRequestDialog.setPageCreator(new ModalWindow.PageCreator() {
            private static final long serialVersionUID = 1L;
            public Page createPage() {
                FullRequestWorkflow workflow = (FullRequestWorkflow) getWorkflow();
                return new AcceptRequestDialog(acceptRequestDialog, workflow);
            }
        });
        add(acceptRequestDialog);
        add(acceptRequestDialog.dialogLink("acceptRequest"));

        final DialogWindow rejectRequestDialog = new DialogWindow("rejectRequest-dialog", model, false);
        rejectRequestDialog.setPageCreator(new ModalWindow.PageCreator() {
            private static final long serialVersionUID = 1L;
            public Page createPage() {
                FullRequestWorkflow workflow = (FullRequestWorkflow) getWorkflow();
                return new RejectRequestDialog(rejectRequestDialog, workflow);
            }
        });
        add(rejectRequestDialog);
        add(rejectRequestDialog.dialogLink("rejectRequest"));

        final DialogWindow cancelRequestDialog = new DialogWindow("cancelRequest-dialog", model, false);
        cancelRequestDialog.setPageCreator(new ModalWindow.PageCreator() {
            private static final long serialVersionUID = 1L;
            public Page createPage() {
                FullRequestWorkflow workflow = (FullRequestWorkflow) getWorkflow();
                return new CancelRequestDialog(cancelRequestDialog, workflow);
            }
        });
        add(cancelRequestDialog);
        add(cancelRequestDialog.dialogLink("cancelRequest"));
    }

    public void update(AjaxRequestTarget target, JcrEvent jcrEvent) {
        //Nothing much to do here
    }

}
