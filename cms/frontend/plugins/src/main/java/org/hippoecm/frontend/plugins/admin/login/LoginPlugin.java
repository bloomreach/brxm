package org.hippoecm.frontend.plugins.admin.login;

import org.apache.wicket.Application;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
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
import org.hippoecm.frontend.Home;
import org.hippoecm.frontend.Main;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.session.UserSession;

public class LoginPlugin extends Plugin {

    private static final long serialVersionUID = 1L;

    public LoginPlugin(PluginDescriptor pluginDescriptor, IPluginModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);

        add(new SignInForm("signInForm"));
    }

    public void renderHead(HtmlHeaderContainer container) {
        super.renderHead(container);
        container.getHeaderResponse().renderOnLoadJavascript("document.forms.signInForm.username.focus();");
    }

    private final class SignInForm extends Form {
        private static final long serialVersionUID = 1L;

        private ValueMap credentials = new ValueMap();

        private CheckBox rememberMe;
        private TextField username, password;

        public SignInForm(final String id) {
            super(id);

            add(username = new RequiredTextField("username", new PropertyModel(credentials, "username")));
            username.setLabel(new ResourceModel("org.hippoecm.cmsprototype.panels.login.username", "Username"));
            add(new SimpleFormComponentLabel("username-label", username));

            add(password = new PasswordTextField("password", new PropertyModel(credentials, "password")));
            //add(password = new RSAPasswordTextField("password", new Model(), this));
            password.setLabel(new ResourceModel("org.hippoecm.cmsprototype.panels.login.password", "Password"));
            add(new SimpleFormComponentLabel("password-label", password));

            add(new FeedbackPanel("feedback"));

            //add(rememberMe = new CheckBox("rememberMe", new Model(Boolean.FALSE)));
            //rememberMe.setLabel(new ResourceModel("org.hippoecm.cmsprototype.panels.login.rememberMe", "rememberMe"));
            //add(new SimpleFormComponentLabel("rememberMe-label", rememberMe));

            Button submit = new Button("submit", new ResourceModel("org.hippoecm.cmsprototype.panels.login.submit",
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
