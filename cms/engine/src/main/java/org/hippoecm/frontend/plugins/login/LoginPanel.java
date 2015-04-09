/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.jcr.SimpleCredentials;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;

import org.apache.wicket.Application;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.util.collections.MiniMap;
import org.apache.wicket.util.template.PackageTextTemplate;
import org.hippoecm.frontend.Main;
import org.hippoecm.frontend.model.UserCredentials;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;
import org.hippoecm.frontend.session.LoginException;
import org.hippoecm.frontend.session.PluginUserSession;
import org.hippoecm.frontend.util.WebApplicationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginPanel extends Panel {

    public static final Logger log = LoggerFactory.getLogger(LoginPanel.class);

    private static final String LOCALE_COOKIE = "loc";
    private static final int LOCALE_COOKIE_MAXAGE = 365 * 24 * 3600; // expire one year from now
    private final static String DEFAULT_KEY = "invalid.login";

    protected final static Map<LoginException.CAUSE, String> causeKeys = new MiniMap<>(10);

    static {
        causeKeys.put(LoginException.CAUSE.INCORRECT_CREDENTIALS, "invalid.login");
        causeKeys.put(LoginException.CAUSE.ACCESS_DENIED, "access.denied");
        causeKeys.put(LoginException.CAUSE.REPOSITORY_ERROR, "repository.error");
        causeKeys.put(LoginException.CAUSE.PASSWORD_EXPIRED, "password.expired");
        causeKeys.put(LoginException.CAUSE.ACCOUNT_EXPIRED, "account.expired");
    }

    private final LoginSuccessHandler successHandler;

    protected final LoginForm form;
    protected String username;
    protected String password;
    protected String selectedLocale;

    public LoginPanel(final String id, final boolean autoComplete, final List<String> locales,
                      final LoginSuccessHandler successHandler) {
        super(id);

        this.successHandler = successHandler;

        add(CssClass.append("hippo-login-panel-center"));

        add(form = new LoginForm(autoComplete, locales));
    }

    protected void login() throws LoginException {
        PluginUserSession userSession = PluginUserSession.get();

        final char[] pwdAsChars = password == null ? new char[]{} : password.toCharArray();
        userSession.login(new UserCredentials(new SimpleCredentials(username, pwdAsChars)));

        HttpSession session = WebApplicationHelper.retrieveWebRequest().getContainerRequest().getSession(true);
        ConcurrentLoginFilter.validateSession(session, username, false);

        userSession.setLocale(new Locale(selectedLocale));
    }

    protected void loginFailed(final LoginException.CAUSE cause) {
        Main main = (Main) Application.get();
        main.resetConnection();

        String key = cause != null && causeKeys.containsKey(cause) ? causeKeys.get(cause) : DEFAULT_KEY;
        info(getString(key));
    }

    protected  void loginSuccess() {
        if (successHandler != null) {
            successHandler.loginSuccess();
        }
    }

    protected class LoginForm extends Form {

        protected final FeedbackPanel feedback;
        protected final DropDownChoice<String> locale;
        protected final RequiredTextField<String> usernameTextField;
        protected final PasswordTextField passwordTextField;
        protected final Button submitButton;
        protected final List<Component> labels = new ArrayList<>();

        public LoginForm(final boolean autoComplete, final List<String> locales) {
            super("login-form");

            setOutputMarkupId(true);

            if (locales == null || locales.isEmpty()) {
                throw new IllegalArgumentException("Argument locales can not be null or empty");
            }

            add(new AttributeModifier("autocomplete", autoComplete ? "on" : "off"));

            add(feedback = new FeedbackPanel("feedback"));
            feedback.setOutputMarkupId(true);
            feedback.setEscapeModelStrings(false);

            addLabelledComponent(new Label("username-label", new ResourceModel("username-label")));
            add(usernameTextField = new RequiredTextField<>("username", PropertyModel.of(LoginPanel.this, "username")));
            usernameTextField.setOutputMarkupId(true);

            addLabelledComponent(new Label("password-label", new ResourceModel("password-label")));
            add(passwordTextField = new PasswordTextField("password", PropertyModel.of(LoginPanel.this, "password")));
            passwordTextField.setResetPassword(false);

            final String defaultLocale = locales.get(0);
            final String cookieLocale = getCookieValue(LOCALE_COOKIE);
            final String sessionLocale = getSession().getLocale().getLanguage();
            if (cookieLocale != null && locales.contains(cookieLocale)) {
                selectedLocale = cookieLocale;
            } else if (sessionLocale != null && locales.contains(sessionLocale)) {
                selectedLocale = sessionLocale;
            } else {
                selectedLocale = defaultLocale;
            }
            getSession().setLocale(new Locale(selectedLocale));

            addLabelledComponent(new Label("locale-label", new ResourceModel("locale-label")));
            add(locale = new DropDownChoice<>("locale",
                    new PropertyModel<String>(LoginPanel.this, "selectedLocale") {
                        @Override
                        public void setObject(final String object) {
                            super.setObject(locales.contains(object) ? object : defaultLocale);
                        }
                    },
                    locales,
                    // Display the language name from i18n properties
                    new IChoiceRenderer<String>() {
                        public String getDisplayValue(String key) {
                            final Locale locale = new Locale(key);
                            final String displayLanguage = locale.getDisplayLanguage();
                            return getString(key, Model.of(displayLanguage), displayLanguage);
                        }

                        public String getIdValue(String object, int index) {
                            return object;
                        }
                    }
            ));
            locale.add(new OnChangeAjaxBehavior() {
                protected void onUpdate(AjaxRequestTarget target) {
                    //immediately set the locale when the user changes it
                    setCookieValue(LOCALE_COOKIE, selectedLocale, LOCALE_COOKIE_MAXAGE);
                    getSession().setLocale(new Locale(selectedLocale));
                    for (Component component : labels) {
                        target.add(component);
                    }
                }
            });

            submitButton = new Button("submit", new ResourceModel("submit-label"));
            addLabelledComponent(submitButton);
        }

        @Override
        public void renderHead(final IHeaderResponse response) {
            final String script = String.format("$('#%s').focus()", usernameTextField.getMarkupId());
            response.render(OnDomReadyHeaderItem.forScript(script));
            final PackageTextTemplate template = new PackageTextTemplate(LoginPanel.class, "prevent-resubmit.js");
            final Map<String, String> variables = new HashMap<String, String>(1){{
                put("submitButtonId", submitButton.getMarkupId());
            }};
            response.render(OnDomReadyHeaderItem.forScript(template.asString(variables)));
        }

        @Override
        public void onSubmit() {
            try {
                login();
                loginSuccess();
            } catch (LoginException le) {
                log.debug("Login failure!", le);
                loginFailed(le.getLoginExceptionCause());
            } catch (AccessControlException ace) {
                // Invalidate the current obtained JCR session and create an anonymous one
                PluginUserSession.get().login();
                loginFailed(LoginException.CAUSE.ACCESS_DENIED);
            }
        }

        protected void addLabelledComponent(final Component component) {
            component.setOutputMarkupId(true);
            add(component);
            labels.add(component);
        }
    }

    private void setCookieValue(final String cookieName, final String cookieValue, final int maxAge) {
        Cookie localeCookie = new Cookie(cookieName, cookieValue);
        localeCookie.setMaxAge(maxAge);
        WebApplicationHelper.retrieveWebResponse().addCookie(localeCookie);
    }

    private String getCookieValue(final String cookieName) {
        Cookie[] cookies = WebApplicationHelper.retrieveWebRequest().getContainerRequest().getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
