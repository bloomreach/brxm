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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;

import org.apache.jackrabbit.core.security.authentication.CredentialsCallback;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebResponse;
import org.hippoecm.frontend.model.UserCredentials;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.LoginException;
import org.hippoecm.frontend.session.PluginUserSession;
import org.hippoecm.repository.WebCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DirectLoginPlugin extends RenderPlugin implements CallbackHandler {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final Logger log = LoggerFactory.getLogger(DirectLoginPlugin.class);

    private static final long serialVersionUID = 1L;

    private static final String LOCALE_COOKIE = "loc";

    protected final DropDownChoice locale;
    public String selectedLocale;
    private Label userLabel;
    private Map<String,String[]> parameters;
    protected boolean allowParameterBased = true;
    private boolean rendered = false;

    public DirectLoginPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        if (config.containsKey("allowParameterBased")) {
            allowParameterBased = config.getBoolean("allowParameterBased");
        }

        parameters = RequestCycle.get().getRequest().getParameterMap();

        String[] localeArray = getPluginConfig().getStringArray("locales");
        if (localeArray == null) {
            localeArray = LoginPlugin.LOCALES;
        }
        List<String> locales = Arrays.asList(localeArray);

        // by default, use the user's browser settings for the locale
        selectedLocale = "en";
        if (locales.contains(getSession().getLocale().getLanguage())) {
            selectedLocale = getSession().getLocale().getLanguage();
        }

        // check if user has previously selected a locale
        Cookie[] cookies = ((WebRequest)RequestCycle.get().getRequest()).getHttpServletRequest().getCookies();
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                if (LOCALE_COOKIE.equals(cookies[i].getName())) {
                    if (locales.contains(cookies[i].getValue())) {
                        selectedLocale = cookies[i].getValue();
                        getSession().setLocale(new Locale(selectedLocale));
                    }
                }
            }
        }

        add(locale = new DropDownChoice("locale", new PropertyModel(this, "selectedLocale"), locales));

        locale.add(new AjaxFormComponentUpdatingBehavior("onchange") {
            private static final long serialVersionUID = 1L;

            protected void onUpdate(AjaxRequestTarget target) {
                //immediately set the locale when the user changes it
                Cookie localeCookie = new Cookie(LOCALE_COOKIE, selectedLocale);
                localeCookie.setMaxAge(365 * 24 * 3600); // expire one year from now
                ((WebResponse)RequestCycle.get().getResponse()).addCookie(localeCookie);
                getSession().setLocale(new Locale(selectedLocale));
                setResponsePage(this.getFormComponent().getPage());
            }
        });

        add(new FeedbackPanel("feedback").setEscapeModelStrings(false));
        add(new Label("pinger"));
    }

    protected void login() throws LoginException {
        PluginUserSession userSession = (PluginUserSession)getSession();
        HttpSession session = ((WebRequest)getRequest()).getHttpServletRequest().getSession(true);
        userSession.login(new UserCredentials(this));
        ConcurrentLoginFilter.validateSession(session, username(), false);
        userSession.setLocale(new Locale(selectedLocale));
        userSession.getJcrSession();
        /* FIXME: this would be a much better solution than a refresh,
         * but the YUI framework is broken for the first request
        if (parameters != null) {
            setResponsePage(Home.class, new PageParameters(parameters));
            throw new RestartResponseException(Home.class, new PageParameters(parameters));
        } else {
            setResponsePage(Home.class);
            throw new RestartResponseException(Home.class);
        }
        */
    }

    protected String username() {
        String username = (String)((WebRequest)getRequest()).getHttpServletRequest().getAttribute("id");
        if (allowParameterBased && username == null) {
            username = (String)((WebRequest)getRequest()).getHttpServletRequest().getParameter("id");   
        }
        return username;
    }

    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (Callback callback : callbacks) {
            if (callback instanceof NameCallback) {
                NameCallback nameCallback = (NameCallback)callback;
                String username = username();
                if (username != null) {
                    nameCallback.setName(username);
                }
            } else if (callback instanceof PasswordCallback) {
                continue;
            } else if (callback instanceof CredentialsCallback) {
                CredentialsCallback credentialsCallback = (CredentialsCallback) callback;
                credentialsCallback.setCredentials(new WebCredentials(((WebRequest)getRequest()).getHttpServletRequest()));
            }
        }
    }
}
