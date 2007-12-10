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

import javax.jcr.Session;

import org.apache.wicket.Application;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.Main;
import org.hippoecm.frontend.UserSession;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogWindow;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.JcrEvent;
import org.hippoecm.frontend.plugin.PluginEvent;
import org.hippoecm.frontend.widgets.PasswordTextFieldWidget;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.hippoecm.repository.HippoRepository;

public class LoginDialog extends AbstractDialog {
    private static final long serialVersionUID = 1L;
    
    private ValueMap credentials;
    private ValueMap oldCredentials;

    public LoginDialog(DialogWindow dialogWindow) {
        super(dialogWindow);
        dialogWindow.setTitle("Login");

        UserSession session = (UserSession) getSession();
        oldCredentials = session.getCredentials();

        credentials = new ValueMap();
        credentials.add("username", oldCredentials.getString("username"));
        credentials.add("password", oldCredentials.getString("password"));

        add(new TextFieldWidget("username", new PropertyModel(credentials, "username")));
        add(new PasswordTextFieldWidget("password", new PropertyModel(credentials, "password")));
    }

    @Override
    public PluginEvent ok() throws Exception {
        String username = credentials.getString("username");
        String password = credentials.getString("password");

        Main main = (Main) Application.get();
        HippoRepository repository = main.getRepository();
        Session jcrSession = repository.login(username, password.toCharArray());

        UserSession userSession = (UserSession) getSession();
        userSession.setJcrSession(jcrSession, credentials);
        
        JcrNodeModel nodeModel = dialogWindow.getNodeModel();
        
        PluginEvent result = new PluginEvent(getOwningPlugin(), JcrEvent.NEW_MODEL, nodeModel);
        result.chainEvent(JcrEvent.NEEDS_RELOAD, nodeModel.findRootModel());
        return result;
    }

    @Override
    public void cancel() {
        credentials = oldCredentials;
    }

}
