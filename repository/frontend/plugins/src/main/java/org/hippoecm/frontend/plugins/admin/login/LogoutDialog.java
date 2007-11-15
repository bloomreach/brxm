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
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.Main;
import org.hippoecm.frontend.UserSession;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogWindow;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.JcrEvent;
import org.hippoecm.repository.HippoRepository;

public class LogoutDialog extends AbstractDialog {
    private static final long serialVersionUID = 1L;

    public LogoutDialog(DialogWindow dialogWindow) {
        super(dialogWindow);
        dialogWindow.setTitle("Logout");
        add(new Label("logout-message", "Do you want to logout?"));
    }

    @Override
    protected void cancel() {
    }

    @Override
    protected JcrEvent ok() throws Exception {
        Main main = (Main) Application.get();
        HippoRepository repository = main.getRepository();
        Session jcrSession = repository.login();
        UserSession userSession = (UserSession) getSession();
        userSession.setJcrSession(jcrSession, new ValueMap());
        
        JcrNodeModel nodeModel = dialogWindow.getNodeModel();
        while (nodeModel.getParent() != null) {
            nodeModel = (JcrNodeModel)nodeModel.getParent();
        }
        return new JcrEvent(nodeModel, true);
    }

}
