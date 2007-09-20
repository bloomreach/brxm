package org.hippoecm.frontend.plugins.admin.login;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.AjaxEditableLabel;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.Home;
import org.hippoecm.frontend.UserSession;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;

public class LoginPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    public LoginPlugin(String id, JcrNodeModel model) {
        super(id, model);

        UserSession session = (UserSession) getSession();
        final ValueMap credentials = session.getCredentials();
        
        add(new AjaxEditableLabel("username", new PropertyModel(credentials, "username")));
        add(new AjaxEditableLabel("password", new PropertyModel(credentials, "password")));

        add(new AjaxLink("login") {
            private static final long serialVersionUID = 1L;
            public void onClick(AjaxRequestTarget target) {
                UserSession session = (UserSession) getSession();
                session.setCredentials(credentials);

                Home home = (Home) getWebPage();
                home.reset(target);
            }
        });
    }

    public void update(AjaxRequestTarget target, JcrNodeModel model) {
        // nothing much to do here
    }

}
