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
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic sign in page to let a user sign in to the repository.
 */
public final class LoginPage extends WebPage {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(LoginPage.class);

    public LoginPage() {
        add(new FeedbackPanel("feedback"));
        add(new SignInForm("signInForm"));
    }

    private final class SignInForm extends Form {
        private static final long serialVersionUID = 1L;

        private ValueMap credentials = new ValueMap();
        private String selectedHippo;

        public SignInForm(final String id) {
            super(id);
            add(new TextField("username", new PropertyModel(credentials, "username")));
            add(new PasswordTextField("password", new PropertyModel(credentials, "password")));

            Button submit = new Button("submit", new Model("Sign In"));
            add(submit);

            Label messageLabel = new Label("message", "No connection to repository");
            add(messageLabel);

            List<String> hippos = getHippos();
            hippos.add("hippo:console (builtin)");
            selectedHippo = hippos.get(0);
            IModel hippoChooserModel = new PropertyModel(this, "hippo");
            DropDownChoice hippoChooser = new DropDownChoice("hippos", hippoChooserModel, hippos);
            hippoChooser.setRequired(true);
            add(hippoChooser);

            Main main = (Main) Application.get();
            if (main.getRepository() == null) {
                submit.setEnabled(false);
                hippoChooser.setVisible(false);
                messageLabel.setVisible(true);
            } else {
                submit.setEnabled(true);
                hippoChooser.setVisible(true);
                messageLabel.setVisible(false);
            }
        }

        public final void onSubmit() {
            UserSession userSession = (UserSession) getSession();
            userSession.setHippo(selectedHippo);
            userSession.setJcrCredentials(credentials);
            userSession.getJcrSession();
            
            setResponsePage(getApplication().getHomePage());
        }

        public void setHippo(String hippo) {
            this.selectedHippo = hippo;
        }

        public String getHippo() {
            return selectedHippo;
        }

        private List<String> getHippos() {
            List result = new ArrayList<String>();
            Main main = (Main) Application.get();
            HippoRepository repository = main.getRepository();
            if (repository != null) {
                try {
                    Node rootNode = repository.login().getRootNode();
                    String path = HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.FRONTEND_PATH;
                    if (rootNode.hasNode(path)) {
                        Node configNode = rootNode.getNode(path);
                        NodeIterator iterator = configNode.getNodes();
                        while (iterator.hasNext()) {
                            result.add(iterator.nextNode().getName());
                        }
                    }
                } catch (RepositoryException e) {
                    log.error(e.getMessage());
                    main.resetConnection();
                }
            }
            return result;
        }

    }

}
