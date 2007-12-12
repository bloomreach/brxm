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

import java.util.Arrays;
import java.util.List;

import javax.jcr.Session;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RadioChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.value.ValueMap;

/**
 * Basic sign in page to let a user sign in to the repository.
 */
public final class LoginPage extends WebPage {
    private static final long serialVersionUID = 1L;

    static final List<String> APPLICATIONS = Arrays.asList(new String[] { "hippo:cms", "hippo:console" });

    public LoginPage() {
        add(new FeedbackPanel("feedback"));
        add(new SignInForm("signInForm"));
    }

    private final class SignInForm extends Form {
        private static final long serialVersionUID = 1L;

        private ValueMap credentials = new ValueMap();
        private String frontendApp = APPLICATIONS.get(0);

        public SignInForm(final String id) {
            super(id);
            add(new TextField("username", new PropertyModel(credentials, "username")));
            add(new PasswordTextField("password", new PropertyModel(credentials, "password")));

            IModel appChooserModel = new PropertyModel(this, "frontendApp");
            RadioChoice appChooser = new RadioChoice("application", appChooserModel, APPLICATIONS);
            appChooser.setSuffix("");
            appChooser.setRequired(true);
            add(appChooser);
        }

        public final void onSubmit() {
            UserSession userSession = (UserSession) getSession();
            
            userSession.setFrontendApp(frontendApp);
            userSession.setJcrCredentials(credentials);
            
            Session jcrSession = userSession.getJcrSession();
            if (jcrSession == null) {
                error("Failed to log in");
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
    }

}
