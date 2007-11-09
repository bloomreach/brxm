package org.hippoecm.frontend.plugins.reviewedactions;

import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.hippoecm.frontend.dialog.DialogWindow;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.JcrEvent;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.cancelrequest.CancelRequestDialog;
import org.hippoecm.repository.reviewedactions.BasicRequestWorkflow;

public class BasicRequestWorkflowPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    public BasicRequestWorkflowPlugin(PluginDescriptor pluginDescriptor, JcrNodeModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);

        final DialogWindow cancelRequestDialog = new DialogWindow("cancelRequest-dialog", model, false);
        cancelRequestDialog.setPageCreator(new ModalWindow.PageCreator() {
            private static final long serialVersionUID = 1L;
            public Page createPage() {
                BasicRequestWorkflow workflow = (BasicRequestWorkflow) getWorkflow();
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
