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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.jcr.Repository;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;

import org.apache.wicket.PageParameters;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.internal.HtmlHeaderContainer;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.apache.wicket.protocol.http.WebResponse;
import org.apache.wicket.util.value.ValueMap;

import org.hippoecm.frontend.Home;
import org.hippoecm.frontend.model.JcrSessionModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.HippoRepositoryFactory;

public class LoginPlugin extends RenderPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final String LOCALE_COOKIE = "loc";
    
    protected ValueMap credentials = new ValueMap();

    public LoginPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        add(createSignInForm("signInForm"));
        add(new Label("pinger"));

        Label versionLabel, buildLabel, repositoryLabel;
        add(versionLabel = new Label("version"));
        add(buildLabel = new Label("build"));
        add(repositoryLabel = new Label("repository"));
        ServletContext servletContext = ((WebApplication)getApplication()).getServletContext();
        try {
            InputStream istream = servletContext.getResourceAsStream("META-INF/MANIFEST.MF");
            if (istream == null) {
                File manifestFile = new File(servletContext.getRealPath("/"), "META-INF/MANIFEST.MF");
                if(manifestFile.exists()) {
                    istream = new FileInputStream(manifestFile);
                }
            }
            if (istream == null) {
                try {
                    istream = HippoRepositoryFactory.getManifest(getClass()).openStream();
                } catch(FileNotFoundException ex) {
                } catch(IOException ex) {
                }
            }
            if (istream != null) {
                Manifest manifest = new Manifest(istream);
                Attributes atts = manifest.getMainAttributes();
                if (atts.getValue("Implementation-Version") != null) {
                    versionLabel.setDefaultModel(new Model(atts.getValue("Implementation-Version")));
                }
                if (atts.getValue("Implementation-Build") != null) {
                    buildLabel.setDefaultModel(new Model(atts.getValue("Implementation-Build")));
                }
            }
        } catch(IOException ex) {
            // delibate ignore
        }
        if(((UserSession) getSession()).getJcrSession() != null) {
            Repository repository = ((UserSession) getSession()).getJcrSession().getRepository();
            if(repository != null) {
                StringBuffer sb = new StringBuffer();
                sb.append(repository.getDescriptor(Repository.REP_NAME_DESC));
                sb.append(" ");
                sb.append(repository.getDescriptor(Repository.REP_VERSION_DESC));
                repositoryLabel.setDefaultModel(new Model(new String(sb)));
            }
        }
    }

    protected SignInForm createSignInForm(String id) {
        return new SignInForm(id);
    }

    @Override
    public void renderHead(HtmlHeaderContainer container) {
        super.renderHead(container);
        container.getHeaderResponse().renderOnLoadJavascript("document.forms.signInForm.username.focus();");
    }

    protected class SignInForm extends Form {
        private static final long serialVersionUID = 1L;
        
        protected final DropDownChoice locale;
        private List<String> locales = Arrays.asList(new String[] { "nl", "en" });
        public String selectedLocale;
        protected final RequiredTextField usernameTextField;
        protected final PasswordTextField passwordTextField; 
        private Label userLabel;
        private Map parameters;
        
        public SignInForm(final String id) {
            super(id);
            
            parameters = RequestCycle.get().getRequest().getParameterMap();

            // by default, use the user's browser settings for the locale
            selectedLocale = "en";
            if (locales.contains(getSession().getLocale().getLanguage())) {
                selectedLocale = getSession().getLocale().getLanguage();
            }
            
            // check if user has previously selected a locale
            Cookie[] cookies = ((WebRequest)RequestCycle.get().getRequest()).getHttpServletRequest().getCookies();
            if(cookies != null) {
                for (int i = 0; i < cookies.length; i++) {
                    if (LOCALE_COOKIE.equals(cookies[i].getName())) {
                        if (locales.contains(cookies[i].getValue())) {
                            selectedLocale = cookies[i].getValue();
                            getSession().setLocale(new Locale(selectedLocale));
                        }
                    }
                }
            }

            add(usernameTextField = new RequiredTextField("username",  new StringPropertyModel(credentials, "username")));
            add(passwordTextField = new PasswordTextField("password", new StringPropertyModel(credentials, "password")));
            add(locale = new DropDownChoice("locale", new PropertyModel(this, "selectedLocale"), locales));

            passwordTextField.setResetPassword(false);
            
            locale.add(new AjaxFormComponentUpdatingBehavior("onchange") {
                private static final long serialVersionUID = 1L;

                protected void onUpdate(AjaxRequestTarget target) {
                    //immediately set the locale when the user changes it
                    Cookie localeCookie = new Cookie(LOCALE_COOKIE, selectedLocale);
                    localeCookie.setMaxAge(365*24*3600); // expire one year from now
                    ((WebResponse)RequestCycle.get().getResponse()).addCookie(localeCookie);
                    getSession().setLocale(new Locale(selectedLocale));
                    setResponsePage(this.getFormComponent().getPage());
                }
            });

            usernameTextField.add(new AjaxFormComponentUpdatingBehavior("onchange") {
                private static final long serialVersionUID = 1L;
                protected void onUpdate(AjaxRequestTarget target) {
                    String username = this.getComponent().getDefaultModelObjectAsString();
                    HttpSession session = ((WebRequest)SignInForm.this.getRequest()).getHttpServletRequest().getSession(true);
                    if(ConcurrentLoginFilter.isConcurrentSession(session, username)) {
                        userLabel.setDefaultModel(new StringResourceModel("alreadylogin", LoginPlugin.this, null, new Object[] {username}));
                    } else {
                        userLabel.setDefaultModel(new Model(""));
                    }
                    target.addComponent(userLabel);
                    credentials.put("username", username);
                }
            });

            passwordTextField.add(new AjaxFormComponentUpdatingBehavior("onchange") {
                private static final long serialVersionUID = 1L;
                protected void onUpdate(AjaxRequestTarget target) {
                    credentials.put("password", this.getComponent().getDefaultModelObjectAsString());
                }
            });

            add(new FeedbackPanel("feedback").setEscapeModelStrings(false));
            add(userLabel = new Label("infouserlogin", ""));
            userLabel.setOutputMarkupId(true);
            Button submit = new Button("submit", new ResourceModel("submit-label"));
            add(submit);
        }

        @Override
        public void onDetach() {
            WebRequest webRequest = ((WebRequestCycle)RequestCycle.get()).getWebRequest();
            if (!webRequest.getHttpServletRequest().getMethod().equals("POST") && !webRequest.isAjax()) {
                ((UserSession)getSession()).getJcrSessionModel().flush();
            }
            super.onDetach();
        }

        @Override
        public void onSubmit() {
            UserSession userSession = (UserSession) getSession();
            String username = usernameTextField.getDefaultModelObjectAsString();
            HttpSession session = ((WebRequest)SignInForm.this.getRequest()).getHttpServletRequest().getSession(true);
            ConcurrentLoginFilter.validateSession(session, username, false);
            userSession.setJcrSessionModel(new JcrSessionModel(credentials));
            userSession.setLocale(new Locale(selectedLocale));
            userSession.getJcrSession();
            redirect();
        }

        protected void redirect() {
            if (parameters != null) {
                setResponsePage(Home.class, new PageParameters(parameters));
            } else {
                setResponsePage(Home.class);
            }
        }
    }

    protected static class StringPropertyModel extends PropertyModel<String> {
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
