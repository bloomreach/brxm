/*
 *  Copyright 2008 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.plugins.login;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
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
import org.apache.wicket.util.time.Duration;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.Home;
import org.hippoecm.frontend.Main;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;

public class LoginPlugin extends RenderPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    public LoginPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        add(new SignInForm("signInForm"));
    }

    @Override
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

            add(username = new RequiredTextField("username", new StringPropertyModel(credentials, "username")));
            username.setLabel(new ResourceModel("org.hippoecm.frontend.plugins.login.username", "Username"));
            add(new SimpleFormComponentLabel("username-label", username));

            add(password = new PasswordTextField("password", new StringPropertyModel(credentials, "password")));
            //add(password = new RSAPasswordTextField("password", new Model(), this));
            password.setLabel(new ResourceModel("org.hippoecm.frontend.plugins.login.password", "Password"));
            add(new SimpleFormComponentLabel("password-label", password));

            add(new FeedbackPanel("feedback"));

            //add(rememberMe = new CheckBox("rememberMe", new Model(Boolean.FALSE)));
            //rememberMe.setLabel(new ResourceModel("org.hippoecm.frontend.plugins.login.rememberMe", "rememberMe"));
            //add(new SimpleFormComponentLabel("rememberMe-label", rememberMe));

            Button submit = new Button("submit", new ResourceModel("org.hippoecm.frontend.plugins.login.submit",
                    "Sign In"));
            add(submit);

            Main main = (Main) Application.get();
            if (main.getRepository() == null) {
                submit.setEnabled(false);
            } else {
                submit.setEnabled(true);
            }
            
            //Prevent timeout on the login page
            add(new PingBehavior(LoginPlugin.this));
        }

        @Override
        public final void onSubmit() {
            UserSession userSession = (UserSession) getSession();
            userSession.setJcrCredentials(credentials);
            userSession.getJcrSession();

            setResponsePage(Home.class);
        }
    }

    private static class StringPropertyModel extends PropertyModel {
        private static final long serialVersionUID = 1L;

        public StringPropertyModel(Object modelObject, String expression) {
            super(modelObject, expression);
        }

        @Override
        public Class getObjectClass() {
            return String.class;
        }
    }

    private static class PingBehavior extends AbstractAjaxTimerBehavior {
        private static final long serialVersionUID = 1L;

        private Component component;

        public PingBehavior(Component component) {
            //Shortest possible timeout value in web.xml is 1 minute.
            super(Duration.seconds(59));
            this.component = component;
        }

        @Override
        protected void onTimer(AjaxRequestTarget target) {
            target.addComponent(component);
        }
    }
}
