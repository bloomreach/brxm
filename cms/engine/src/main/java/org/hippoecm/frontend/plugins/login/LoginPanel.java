/*
 *  Copyright 2015-2018 Hippo B.V. (http://www.onehippo.com)
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
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;

import javax.jcr.SimpleCredentials;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Application;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
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
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.hippoecm.frontend.Main;
import org.hippoecm.frontend.model.UserCredentials;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;
import org.hippoecm.frontend.session.LoginException;
import org.hippoecm.frontend.session.PluginUserSession;
import org.hippoecm.frontend.util.WebApplicationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.frontend.session.LoginException.Cause;

public class LoginPanel extends Panel {

    public static final Logger log = LoggerFactory.getLogger(LoginPanel.class);

    public static final JavaScriptResourceReference PREVENT_RESUBMIT_SCRIPT_REFERENCE =
            new JavaScriptResourceReference(LoginPanel.class, "PreventResubmit.js");

    private static final String CONSOLE_LOCALE = "en";
    private static final String LOCALE_COOKIE = "loc";
    private static final int LOCALE_COOKIE_MAXAGE = 365 * 24 * 3600; // expire one year from now
    private final static String DEFAULT_KEY = "invalid.login";

    private final LoginHandler handler;

    protected final LoginForm form;
    protected String username;
    protected String password;
    protected String selectedLocale;

    public LoginPanel(final String id, final boolean autoComplete, final List<String> locales, final LoginHandler handler) {
        super(id);

        if (locales == null || locales.isEmpty()) {
            throw new IllegalArgumentException("Argument locales can not be null or empty");
        }

        this.handler = handler;

        add(CssClass.append("hippo-login-panel-center"));

        add(form = new LoginForm(autoComplete, locales));
    }

    protected void login() throws LoginException {
        PluginUserSession userSession = PluginUserSession.get();

        if (userSession.getAuthorizedAppCounter() == 0) {
            log.debug("Invalidating user session to make sure a new session id is created");
            userSession.invalidateNow();
            userSession = PluginUserSession.get();
        } else {
            final String alreadyAuthorizedUser = userSession.getUserName();
            if (alreadyAuthorizedUser.equals(username) || isDevMode()) {
                log.debug("User is already authenticated to /cms or /cms/console and now logs in into second app. Hence we " +
                        "should not invalidate the user session.");
            } else {
                log.info("Invalidating http session because attempt to login to different app with different user name");
                userSession.invalidateNow();
                userSession = PluginUserSession.get();
            }
        }

        final char[] pwdAsChars = password == null ? new char[]{} : password.toCharArray();
        userSession.login(new UserCredentials(new SimpleCredentials(username, pwdAsChars)));

        HttpSession session = WebApplicationHelper.retrieveWebRequest().getContainerRequest().getSession(true);
        ConcurrentLoginFilter.validateSession(session, username, false);

        userSession.setLocale(getSelectedLocale());
    }

    private boolean isDevMode() {
        return System.getProperty("project.basedir") != null;
    }

    private Locale getSelectedLocale() {
        if (selectedLocale.equals(Locale.CHINESE.getLanguage())) {
            // always use simplified Chinese, Wicket does not known Chinese without a country
            return Locale.SIMPLIFIED_CHINESE;
        }
        return new Locale(selectedLocale);
    }

    protected void loginFailed(final Cause cause) {
        Main main = (Main) Application.get();
        main.resetConnection();

        info(getReason(cause));
    }

    private String getReason(final Cause cause) {
        if (cause != null) {
            try {
                final String reason = getString(cause.getKey());
                if (reason != null) {
                    return reason;
                }
            } catch (MissingResourceException ignore) {
            }
        }
        return getString(DEFAULT_KEY);
    }

    protected  void loginSuccess() {
        if (handler != null) {
            handler.loginSuccess();
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

            add(new AttributeModifier("autocomplete", autoComplete ? "on" : "off"));

            add(feedback = new FeedbackPanel("feedback"));
            feedback.setOutputMarkupId(true);
            feedback.setEscapeModelStrings(false);
            feedback.setFilter(message -> !message.isRendered());

            addLabelledComponent(new Label("header-label", new ResourceModel("header")));

            addLabelledComponent(new Label("username-label", new ResourceModel("username-label")));
            add(usernameTextField = new RequiredTextField<>("username", PropertyModel.of(LoginPanel.this, "username")));
            usernameTextField.setOutputMarkupId(true);

            addLabelledComponent(new Label("password-label", new ResourceModel("password-label")));
            add(passwordTextField = new PasswordTextField("password", PropertyModel.of(LoginPanel.this, "password")));
            passwordTextField.setResetPassword(false);

            final String defaultLocale = locales.get(0);

            final boolean consoleLogin = WebApplicationHelper.getApplicationName().equals(Main.PLUGIN_APPLICATION_VALUE_CONSOLE);

            if (consoleLogin) {
                // forced language (en) selection for console app
                selectedLocale = CONSOLE_LOCALE;
            }
            else {
                final String cookieLocale = getCookieValue(LOCALE_COOKIE);
                final String sessionLocale = getSession().getLocale().getLanguage();
                if (cookieLocale != null && locales.contains(cookieLocale)) {
                    selectedLocale = cookieLocale;
                } else if (sessionLocale != null && locales.contains(sessionLocale)) {
                    selectedLocale = sessionLocale;
                } else {
                    selectedLocale = defaultLocale;
                }
            }
            getSession().setLocale(getSelectedLocale());

            final Label localeLabel = new Label("locale-label", new ResourceModel("locale-label"));
            addLabelledComponent(localeLabel);
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
                            return StringUtils.capitalize(locale.getDisplayLanguage(locale));
                        }

                        public String getIdValue(String object, int index) {
                            return object;
                        }

                        @Override
                        public String getObject(final String id, final IModel<? extends List<? extends String>> choicesModel) {
                            final List<? extends String> choices = choicesModel.getObject();
                            return choices.contains(id) ? id : null;
                        }
                    }
            ));
            locale.add(new OnChangeAjaxBehavior() {
                protected void onUpdate(AjaxRequestTarget target) {
                    // Store locale in cookie
                    setCookieValue(LOCALE_COOKIE, selectedLocale, LOCALE_COOKIE_MAXAGE);

                    // and update the session locale
                    getSession().setLocale(getSelectedLocale());

                    // redraw labels, feedback panel and provided components
                    labels.stream().filter(Component::isVisible).forEach(target::add);
                    target.add(feedback);
                    if (handler != null) {
                        handler.localeChanged(selectedLocale, target);
                    }
                }
            });

            submitButton = new Button("submit", new ResourceModel("submit-label"));
            addLabelledComponent(submitButton);

            // hide language selection for console app
            if (consoleLogin) {
                locale.setVisible(false);
            }
        }

        @Override
        public void renderHead(final IHeaderResponse response) {
            final String focusScript = String.format("$('#%s').focus()", usernameTextField.getMarkupId());
            response.render(OnDomReadyHeaderItem.forScript(focusScript));

            response.render(JavaScriptReferenceHeaderItem.forReference(PREVENT_RESUBMIT_SCRIPT_REFERENCE));
            final String preventResubmitScript = String.format("if (Hippo && Hippo.PreventResubmit) { " +
                            "Hippo.PreventResubmit('#%s'); }", form.getMarkupId());
            response.render(OnDomReadyHeaderItem.forScript(preventResubmitScript));
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
                loginFailed(Cause.ACCESS_DENIED);
            }
        }

        public void addLabelledComponent(final Component component) {
            component.setOutputMarkupId(true);
            add(component);
            labels.add(component);
        }
    }

    protected void setCookieValue(final String cookieName, final String cookieValue, final int maxAge) {
        Cookie localeCookie = new Cookie(cookieName, cookieValue);
        localeCookie.setMaxAge(maxAge);
        WebApplicationHelper.retrieveWebResponse().addCookie(localeCookie);
    }

    protected String getCookieValue(final String cookieName) {
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
