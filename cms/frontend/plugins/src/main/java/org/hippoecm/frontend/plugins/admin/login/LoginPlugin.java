package org.hippoecm.frontend.plugins.admin.login;

import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.UserSession;
import org.hippoecm.frontend.dialog.DialogWindow;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;

public class LoginPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    private String username;

    public LoginPlugin(String id, final JcrNodeModel model) {
        super(id, model);

        UserSession session = (UserSession) getSession();
        ValueMap credentials = session.getCredentials();
        username = credentials.getString("username");
        username = (username == null || username.equals("")) ? "anonymous" : username;
        add(new Label("username", new PropertyModel(this, "username")));

        final DialogWindow loginDialog = new DialogWindow("login-dialog", model, true);
        loginDialog.setPageCreator(new ModalWindow.PageCreator() {
            private static final long serialVersionUID = 1L;
            public Page createPage() {
                return new LoginDialog(loginDialog, model);
            }
        });
        add(loginDialog);
        add(loginDialog.dialogLink("login-dialog-link"));
    }

    public void update(AjaxRequestTarget target, JcrNodeModel model) {
        UserSession session = (UserSession) getSession();
        ValueMap credentials = session.getCredentials();
        username = credentials.getString("username");
        username = (username == null || username.equals("")) ? "anonymous" : username;
        setUsername(username);
        if (target != null) {
            target.addComponent(this);
        }
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

}
