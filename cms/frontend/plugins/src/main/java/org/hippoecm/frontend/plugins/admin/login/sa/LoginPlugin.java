/*
 * Copyright 2008 Hippo
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
package org.hippoecm.frontend.plugins.admin.login.sa;

import org.apache.wicket.Application;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.SimpleFormComponentLabel;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.internal.HtmlHeaderContainer;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.Main;
import org.hippoecm.frontend.sa.Home;
import org.hippoecm.frontend.sa.plugin.impl.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;

public class LoginPlugin extends RenderPlugin {

    private static final long serialVersionUID = 1L;

    public LoginPlugin() {
        add(new SignInForm("signInForm"));
    }

    public void renderHead(HtmlHeaderContainer container) {
        super.renderHead(container);
        container.getHeaderResponse().renderOnLoadJavascript("document.forms.signInForm.username.focus();");
    }

    private final class SignInForm extends Form {
        private static final long serialVersionUID = 1L;

        private ValueMap credentials = new ValueMap();

        //private CheckBox rememberMe;
        private TextField username, password;

        public SignInForm(final String id) {
            super(id);

            add(username = new RequiredTextField("username", new PropertyModel(credentials, "username")));
            username.setLabel(new ResourceModel("org.hippoecm.frontend.sa.plugins.login.username", "Username"));
            add(new SimpleFormComponentLabel("username-label", username));

            add(password = new PasswordTextField("password", new PropertyModel(credentials, "password")));
            //add(password = new RSAPasswordTextField("password", new Model(), this));
            password.setLabel(new ResourceModel("org.hippoecm.frontend.sa.plugins.login.password", "Password"));
            add(new SimpleFormComponentLabel("password-label", password));

            add(new FeedbackPanel("feedback"));

            //add(rememberMe = new CheckBox("rememberMe", new Model(Boolean.FALSE)));
            //rememberMe.setLabel(new ResourceModel("org.hippoecm.frontend.sa.plugins.login.rememberMe", "rememberMe"));
            //add(new SimpleFormComponentLabel("rememberMe-label", rememberMe));

            Button submit = new Button("submit", new ResourceModel("org.hippoecm.frontend.sa.plugins.login.submit",
                    "Sign In"));
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

            setResponsePage(Home.class);
        }
    }
}
