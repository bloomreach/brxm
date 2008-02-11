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

import org.apache.wicket.Application;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.session.UserSession;
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

        public SignInForm(final String id) {
            super(id);
            add(new TextField("username", new PropertyModel(credentials, "username")));
            add(new PasswordTextField("password", new PropertyModel(credentials, "password")));

            Button submit = new Button("submit", new Model("Sign In"));
            add(submit);

            Main main = (Main) Application.get();
            if (main.getRepository() == null) {
                submit.setEnabled(false);
            } else {
                submit.setEnabled(true);
            }
        }

        public final void onSubmit() {
            UserSession userSession = (UserSession) getSession();
            userSession.setJcrCredentials(credentials);
            userSession.getJcrSession();

            log.info("Logged in as " + credentials.getString("username") + " to Hippo CMS 7");
            setResponsePage(Home.class);
        }
    }

}
