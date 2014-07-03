/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import javax.jcr.SimpleCredentials;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;

import org.apache.wicket.Application;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.core.request.mapper.AbstractComponentMapper;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.internal.HtmlHeaderContainer;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.info.PageComponentInfo;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.mapper.parameter.PageParametersEncoder;
import org.apache.wicket.util.string.Strings;
import org.hippoecm.frontend.InvalidLoginPage;
import org.hippoecm.frontend.Main;
import org.hippoecm.frontend.PluginPage;
import org.hippoecm.frontend.model.UserCredentials;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.LoginException;
import org.hippoecm.frontend.session.PluginUserSession;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.util.WebApplicationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginPlugin extends RenderPlugin {

    private static final Logger log = LoggerFactory.getLogger(LoginPlugin.class);

    private static final String ERROR_MESSAGE_LOGIN_FAILURE = "Login failure!";
    private static final String PAGE_PARAMS_KEY_LOGIN_EXCEPTION_CAUSE = LoginException.CAUSE.class.getName();

    public static final String DEFAULT_LOCALE = "en";

    // Sorted by alphabetical order of the language name (see i18n properties), for a more user-friendly form
    public final static String[] LOCALES = {"en", "fr", "nl", "de"};

    private static final long serialVersionUID = 1L;

    private static final String LOCALE_COOKIE = "loc";

    protected String username;
    protected String password;

    public LoginPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        add(createSignInForm("signInForm"));
        add(new Label("pinger"));
    }

    protected SignInForm createSignInForm(String id) {
        return new SignInForm(id);
    }

    @Override
    public void renderHead(HtmlHeaderContainer container) {
        super.renderHead(container);
        container.getHeaderResponse().render(OnDomReadyHeaderItem.forScript("document.forms.signInForm.username.focus();"));
    }

    protected class SignInForm extends Form {
        private static final long serialVersionUID = 1L;

        protected final DropDownChoice<String> locale;

        public String selectedLocale;
        protected final RequiredTextField<String> usernameTextField;
        protected final PasswordTextField passwordTextField;
        private PageParameters parameters;

        public SignInForm(final String id) {
            super(id);

            setOutputMarkupId(true);

            String[] localeArray = getPluginConfig().getStringArray("locales");
            if (localeArray == null) {
                localeArray = LOCALES;
            }
            final Set<String> locales = new HashSet<String>(Arrays.asList(localeArray));

            // by default, use the user's browser settings for the locale
            selectedLocale = DEFAULT_LOCALE;
            if (locales.contains(getSession().getLocale().getLanguage())) {
                selectedLocale = getSession().getLocale().getLanguage();
            }

            // check if user has previously selected a locale
            Cookie[] cookies = WebApplicationHelper.retrieveWebRequest().getContainerRequest().getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (LOCALE_COOKIE.equals(cookie.getName())) {
                        if (locales.contains(cookie.getValue())) {
                            selectedLocale = cookie.getValue();
                            getSession().setLocale(new Locale(selectedLocale));
                        }
                    }
                }
            }

            add(usernameTextField = new RequiredTextField<String>("username", new PropertyModel<String>(
                    LoginPlugin.this, "username")));

            add(passwordTextField = new PasswordTextField("password", new PropertyModel<String>(LoginPlugin.this,
                    "password")));

            add(locale = new DropDownChoice<String>("locale",
                    new PropertyModel<String>(this, "selectedLocale") {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void setObject(final String object) {
                            super.setObject(locales.contains(object) ? object : DEFAULT_LOCALE);
                        }
                    },
                    Arrays.asList(localeArray),
                    // Display the language name from i18n properties
                    new IChoiceRenderer<String>() {
                        private static final long serialVersionUID = 1L;

                        public String getDisplayValue(String object) {
                            Locale locale = new Locale(object);
                            return new StringResourceModel(object, LoginPlugin.this, null, null, locale.getDisplayLanguage()).getString();
                        }

                        public String getIdValue(String object, int index) {
                            return object;
                        }
                    }
            ));

            passwordTextField.setResetPassword(false);

            locale.add(new AjaxFormComponentUpdatingBehavior("onchange") {
                private static final long serialVersionUID = 1L;

                protected void onUpdate(AjaxRequestTarget target) {
                    //immediately set the locale when the user changes it
                    Cookie localeCookie = new Cookie(LOCALE_COOKIE, selectedLocale);
                    localeCookie.setMaxAge(365 * 24 * 3600); // expire one year from now
                    WebApplicationHelper.retrieveWebResponse().addCookie(localeCookie);
                    getSession().setLocale(new Locale(selectedLocale));
                    target.add(SignInForm.this);
                }
            });

            usernameTextField.add(new AjaxFormComponentUpdatingBehavior("onchange") {
                private static final long serialVersionUID = 1L;

                protected void onUpdate(AjaxRequestTarget target) {
                    String username = this.getComponent().getDefaultModelObjectAsString();
                    HttpSession session = ((ServletWebRequest) SignInForm.this.getRequest()).getContainerRequest()
                            .getSession(true);
                    LoginPlugin.this.username = username;
                }
            });

            passwordTextField.add(new AjaxFormComponentUpdatingBehavior("onchange") {
                private static final long serialVersionUID = 1L;

                protected void onUpdate(AjaxRequestTarget target) {
                    LoginPlugin.this.password = password;
                }
            });
        }

        @Override
        public void onDetach() {
            ServletWebRequest webRequest = (ServletWebRequest) RequestCycle.get().getRequest();
            if (!webRequest.getContainerRequest().getMethod().equals("POST") && !webRequest.isAjax()) {
                UserSession.get().releaseJcrSession();
            }
            super.onDetach();
        }

        @Override
        protected void onBeforeRender() {
            super.onBeforeRender();

            Request request = RequestCycle.get().getRequest();

            /**
             * strip the first query parameter from URL
             * Copied from {@link AbstractComponentMapper#removeMetaParameter}
             */
            Url urlCopy = new Url(request.getUrl());
            if (!urlCopy.getQueryParameters().isEmpty() &&
                    Strings.isEmpty(urlCopy.getQueryParameters().get(0).getValue())) {
                String pageComponentInfoCandidate = urlCopy.getQueryParameters().get(0).getName();
                if (PageComponentInfo.parse(pageComponentInfoCandidate) != null) {
                    urlCopy.getQueryParameters().remove(0);
                }
            }

            parameters = new PageParametersEncoder().decodePageParameters(urlCopy);
        }

        @Override
        public void onSubmit() {
            PluginUserSession userSession = (PluginUserSession) getSession();
            String username = usernameTextField.getDefaultModelObjectAsString();
            HttpSession session = ((ServletWebRequest) SignInForm.this.getRequest()).getContainerRequest().getSession(true);

            boolean success = true;
            PageParameters loginExceptionPageParameters = null;
            try {
                userSession.login(new UserCredentials(new SimpleCredentials(username, password == null ? null : password.toCharArray())));
            } catch (LoginException le) {
                log.debug(ERROR_MESSAGE_LOGIN_FAILURE, le);
                success = false;
                loginExceptionPageParameters = buildPageParameters(le.getLoginExceptionCause());
            }

            ConcurrentLoginFilter.validateSession(session, username, false);
            userSession.setLocale(new Locale(selectedLocale));
            redirect(success, loginExceptionPageParameters);
        }

        protected void redirect(boolean success, PageParameters errorParameters) {
            if (success == false) {
                Main main = (Main) Application.get();
                main.resetConnection();

                if (errorParameters != null) {
                    throw new RestartResponseException(new InvalidLoginPage(errorParameters));
                } else {
                    throw new RestartResponseException(InvalidLoginPage.class);
                }
            }

            if (parameters != null) {
                setResponsePage(PluginPage.class, parameters);
            } else {
                setResponsePage(PluginPage.class);
            }
        }

        protected void redirect(boolean success) {
            redirect(success, null);
        }

        protected PageParameters buildPageParameters(LoginException.CAUSE cause) {
            PageParameters pageParameters = new PageParameters();
            pageParameters.add(PAGE_PARAMS_KEY_LOGIN_EXCEPTION_CAUSE, cause.name());
            return pageParameters;
        }

    }

}
