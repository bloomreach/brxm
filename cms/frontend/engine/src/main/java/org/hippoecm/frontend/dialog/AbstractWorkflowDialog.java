package org.hippoecm.frontend.dialog;

import javax.jcr.Session;

import org.hippoecm.frontend.UserSession;
import org.hippoecm.frontend.model.JcrEvent;
import org.hippoecm.frontend.model.JcrNodeModel;

public abstract class AbstractWorkflowDialog extends AbstractDialog {

    public AbstractWorkflowDialog(DialogWindow dialogWindow) {
        super(dialogWindow);
    }

    protected JcrEvent ok() throws Exception {
        UserSession wicketSession = (UserSession)getSession();
        Session jcrSession = wicketSession.getJcrSession();
        
        jcrSession.save();
        jcrSession.refresh(true);
        
        JcrNodeModel handle = (JcrNodeModel)dialogWindow.getNodeModel().getParent();
        return new JcrEvent(handle);
    }

}
