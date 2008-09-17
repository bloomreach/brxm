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

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.internal.HtmlHeaderContainer;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.Home;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.widgets.Pinger;

public class LoginPlugin extends RenderPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    public LoginPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        add(new SignInForm("signInForm"));
        add(new Pinger("pinger"));
    }

    @Override
    public void renderHead(HtmlHeaderContainer container) {
        super.renderHead(container);
        container.getHeaderResponse().renderOnLoadJavascript("document.forms.signInForm.username.focus();");
    }

    private final class SignInForm extends Form {
        private static final long serialVersionUID = 1L;

        private ValueMap credentials = new ValueMap();
        private DropDownChoice locale;
        private List<String> locales = Arrays.asList(new String[] { "nl", "en" });
        public String selectedLocale;

        public SignInForm(final String id) {
            super(id);

            // by default, use the user's browser settings for the locale
            selectedLocale = getSession().getLocale().getLanguage();

            add(new RequiredTextField("username", new StringPropertyModel(credentials, "username")));
            add(new PasswordTextField("password", new StringPropertyModel(credentials, "password")));
            add(locale = new DropDownChoice("locale", new PropertyModel(this, "selectedLocale"), locales));

            locale.add(new AjaxFormComponentUpdatingBehavior("onchange") {
                private static final long serialVersionUID = 1L;

                protected void onUpdate(AjaxRequestTarget target) {
                    //immediately set the locale when the user changes it
                    getSession().setLocale(new Locale(selectedLocale));
                    setResponsePage(new Home());
                }
            });

            add(new FeedbackPanel("feedback"));
            Button submit = new Button("submit", new ResourceModel("submit-label"));
            add(submit);
        }

        @Override
        public final void onSubmit() {
            UserSession userSession = (UserSession) getSession();
            userSession.setJcrCredentials(credentials);
            userSession.setLocale(new Locale(selectedLocale));
            userSession.getJcrSession();
            setResponsePage(new Home());
        }
    }

    private static class StringPropertyModel extends PropertyModel {
        private static final long serialVersionUID = 1L;

        public StringPropertyModel(Object modelObject, String expression) {
            super(modelObject, expression);
        }

        @Override
        @SuppressWarnings("unchecked")
        public Class getObjectClass() {
            return String.class;
        }
    }

}
