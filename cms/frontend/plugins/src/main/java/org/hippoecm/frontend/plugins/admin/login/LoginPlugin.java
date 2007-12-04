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
package org.hippoecm.frontend.plugins.admin.login;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.IAuthenticatedWebPage;
import org.hippoecm.frontend.SignIn;
import org.hippoecm.frontend.UserSession;
import org.hippoecm.frontend.dialog.DialogWindow;
import org.hippoecm.frontend.dialog.DynamicDialogFactory;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.JcrEvent;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;

public class LoginPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    private static final String LOGIN_DIALOG_LABEL = "Login";
    private static final String LOGOUT_DIALOG_LABEL = "Logout";
    
    private static final String LOGIN_DIALOG_CLASSNAME = "org.hippoecm.frontend.plugins.admin.login.LoginDialog";
    private static final String LOGOUT_DIALOG_CLASSNAME = "org.hippoecm.frontend.plugins.admin.login.LogoutDialog";
    
    private DynamicDialogFactory loginDialogFactory;
    private String username;
    private Label loginDialogLinkLabel;

    public LoginPlugin(PluginDescriptor pluginDescriptor, JcrNodeModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);

        UserSession session = (UserSession) getSession();
        ValueMap credentials = session.getCredentials();
        username = credentials.getString("username");
        
        String label;
        String className;
        
        if (username == null || username.equals("")) {
            username = "anonymous";
            label = LOGIN_DIALOG_LABEL;
            className = LOGIN_DIALOG_CLASSNAME;
        }
        else {
            label = LOGOUT_DIALOG_LABEL;
            className = LOGOUT_DIALOG_CLASSNAME;
        }

        add(new Label("username", new PropertyModel(this, "username")));

        DialogWindow loginDialog = new DialogWindow("login-dialog", model);
        add(loginDialog);
        
        // the login/logout dialog link
        AjaxLink dialogLink = loginDialog.dialogLink("login-dialog-link");
        loginDialogLinkLabel = new Label("login-dialog-link-label", label); 
        dialogLink.add(loginDialogLinkLabel);
        add(dialogLink);

        // the factory providing the login or logout dialog based on the logged in/out status
        loginDialogFactory = new DynamicDialogFactory(loginDialog, className);
        loginDialog.setPageCreator( loginDialogFactory );
    }

    public void update(AjaxRequestTarget target, JcrEvent jcrEvent) {
        UserSession session = (UserSession) getSession();
        ValueMap credentials = session.getCredentials();
        username = credentials.getString("username");
        
        if (username == null || username.equals("")) {
            username = "anonymous";
            loginDialogFactory.setClassName(LOGIN_DIALOG_CLASSNAME);
            loginDialogLinkLabel.setModelObject(LOGIN_DIALOG_LABEL);

            // FIXME should this be here?
            if (IAuthenticatedWebPage.class.isAssignableFrom(getApplication().getHomePage())) {
            	findPage().setResponsePage(SignIn.class);
            }
        
        }
        else {
            loginDialogFactory.setClassName(LOGOUT_DIALOG_CLASSNAME);
            loginDialogLinkLabel.setModelObject(LOGOUT_DIALOG_LABEL);
        }
        
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
