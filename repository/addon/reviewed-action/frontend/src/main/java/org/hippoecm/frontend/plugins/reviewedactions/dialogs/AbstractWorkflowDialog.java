package org.hippoecm.frontend.plugins.reviewedactions.dialogs;

import javax.jcr.Session;

import org.hippoecm.frontend.UserSession;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogWindow;
import org.hippoecm.frontend.model.JcrNodeModel;

public abstract class AbstractWorkflowDialog extends AbstractDialog {

    public AbstractWorkflowDialog(DialogWindow dialogWindow, JcrNodeModel model) {
        super(dialogWindow, model);
    }

    protected void ok() throws Exception {
        UserSession wicketSession = (UserSession)getSession();
        Session jcrSession = wicketSession.getJcrSession();
        
        jcrSession.save();
        jcrSession.refresh(true);
    }

}
