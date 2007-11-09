package org.hippoecm.frontend.dialog;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.JcrEvent;

public abstract class AbstractWorkflowDialog extends AbstractDialog {

    public AbstractWorkflowDialog(DialogWindow dialogWindow) {
        super(dialogWindow);
    }

    protected JcrEvent ok() throws Exception {
        JcrNodeModel nodeModel = dialogWindow.getNodeModel();
        nodeModel.getNode().getSession().save();
        nodeModel.getNode().getSession().refresh(true);

        while (!nodeModel.getNode().getPath().equals("/")) {
            nodeModel = (JcrNodeModel) nodeModel.getParent();
        }
        return new JcrEvent(nodeModel, true);
    }

}
