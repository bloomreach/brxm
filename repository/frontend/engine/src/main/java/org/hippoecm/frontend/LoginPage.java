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
package org.hippoecm.frontend;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.Application;
import org.apache.wicket.RestartResponseAtInterceptPageException;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.repository.api.HippoNodeType;

/**
 * Basic sign in page to let a user sign in to the repository.
 */
public final class LoginPage extends WebPage {
    private static final long serialVersionUID = 1L;

    public LoginPage() {
        add(new FeedbackPanel("feedback"));
        add(new SignInForm("signInForm"));
    }

    private final class SignInForm extends Form {
        private static final long serialVersionUID = 1L;

        private ValueMap credentials = new ValueMap();
        private String frontendApp;

        public SignInForm(final String id) {
            super(id);
            add(new TextField("username", new PropertyModel(credentials, "username")));
            add(new PasswordTextField("password", new PropertyModel(credentials, "password")));

            List<String> hippos = getHippos();
            if (hippos.size() == 0) {
                hippos.add("hippo:console");
            }
            frontendApp = hippos.get(0);
            IModel appChooserModel = new PropertyModel(this, "frontendApp");
            DropDownChoice appChooser = new DropDownChoice("applications", appChooserModel, hippos);
            appChooser.setRequired(true);
            add(appChooser);
        }

        public final void onSubmit() {
            UserSession userSession = (UserSession) getSession();

            userSession.setFrontendApp(frontendApp);
            userSession.setJcrCredentials(credentials);

            if (userSession.getJcrSession() == null) {
                throw new RestartResponseAtInterceptPageException(LoginPage.class);
            }

            if (!continueToOriginalDestination()) {
                setResponsePage(getApplication().getHomePage());
            }
        }

        public void setFrontendApp(String frontendApp) {
            this.frontendApp = frontendApp;
        }

        public String getFrontendApp() {
            return frontendApp;
        }

        private List<String> getHippos() {
            List result = new ArrayList<String>();
            try {
                Main main = (Main) Application.get();
                Node rootNode = main.getRepository().login().getRootNode();
                String path = HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.FRONTEND_PATH;
                if (rootNode.hasNode(path)) {
                    Node configNode = rootNode.getNode(path);
                    NodeIterator iterator = configNode.getNodes();
                    while (iterator.hasNext()) {
                        result.add(iterator.nextNode().getName());
                    }
                }
            } catch (RepositoryException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return result;
        }

    }

}
