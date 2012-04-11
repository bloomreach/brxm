/*
 *  Copyright 2009 Hippo.
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.servlet.http.Cookie;

import org.apache.commons.codec.binary.Base64;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.protocol.http.WebRequest;
import org.hippoecm.frontend.model.UserCredentials;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.session.PluginUserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.cms7.event.HippoSecurityEventConstants;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.eventbus.HippoEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RememberMeLoginPlugin extends LoginPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: $";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(LoginPlugin.class);

    /** Algorithm to use for creating the passkey secret.
        Intentionally a relative weak algorithm, as this whole procedure isn't
        too safe to begin with.
    */
    static final String ALGORITHM = "MD5";

    public RememberMeLoginPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        String[] supported = config.getStringArray("browsers.supported");
        if (supported != null) {
            add(new BrowserCheckBehavior(supported));
        }
    }

    @Override
    protected LoginPlugin.SignInForm createSignInForm(String id) {
        boolean rememberme = false;
        Cookie[] cookies = retrieveWebRequest().getHttpServletRequest().getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (RememberMeLoginPlugin.class.getName().equals(cookie.getName())) {
                    String passphrase = cookie.getValue();
                    if (passphrase != null && passphrase.contains("$")) {
                        username = new String(Base64.decodeBase64(passphrase.split("\\$")[1]));
                        password = "********";
                        rememberme = true;
                    }
                    break;
                }
            }
        }
        LoginPlugin.SignInForm form = new SignInForm(id, rememberme);
        return form;
    }

    protected class SignInForm extends org.hippoecm.frontend.plugins.login.LoginPlugin.SignInForm {
        private static final long serialVersionUID = 1L;

        private boolean rememberme;

        public void setRememberme(boolean value) {
            rememberme = value;
        }

        public boolean getRememberme() {
            return rememberme;
        }

        public SignInForm(final String id, boolean rememberme) {
            super(id);

            this.rememberme = rememberme;

            if (rememberme) {
                add(new AttributeModifier("autocomplete", true, new Model<String>("off")));
            }

            CheckBox rememberMeCheckbox = new CheckBox("rememberme", new PropertyModel<Boolean>(this, "rememberme"));
            add(rememberMeCheckbox);
            rememberMeCheckbox.add(new AjaxFormComponentUpdatingBehavior("onchange") {
                private static final long serialVersionUID = 1L;
                protected void onUpdate(AjaxRequestTarget target) {
                    setResponsePage(this.getFormComponent().getPage());
                }
            });

            usernameTextField.add(new AjaxFormComponentUpdatingBehavior("onchange") {
                private static final long serialVersionUID = 1L;

                protected void onUpdate(AjaxRequestTarget target) {
                    SignInForm.this.clearCookie(RememberMeLoginPlugin.class.getName());
                }
            });

            passwordTextField.add(new AjaxFormComponentUpdatingBehavior("onchange") {
                private static final long serialVersionUID = 1L;

                protected void onUpdate(AjaxRequestTarget target) {
                    SignInForm.this.clearCookie(RememberMeLoginPlugin.class.getName());
                }
            });
        }

        protected void clearCookie(String cookieName) {
            Cookie cookie = retrieveWebRequest().getCookie(cookieName);

            if (cookie != null) {
                cookie.setMaxAge(0);
                cookie.setValue(null);
                retrieveWebResponse().addCookie(cookie);
            }
        }

        @Override
        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            String username = RememberMeLoginPlugin.this.username;
            String password = RememberMeLoginPlugin.this.password;
            if (rememberme) {
                if (password == null || password.equals("") || password.replaceAll("\\*", "").equals("")) {
                    Cookie[] cookies = retrieveWebRequest().getHttpServletRequest().getCookies();
                    for (Cookie cookie : cookies) {
                        if (RememberMeLoginPlugin.class.getName().equals(cookie.getName())) {
                            String passphrase = cookie.getValue();
                            String strings[] = passphrase.split("\\$");
                            if (strings.length == 3) {
                                username = new String(Base64.decodeBase64(strings[1]));
                                password = strings[0] + "$" + strings[2];
                            }
                            break;
                        }
                    }
                    for (Callback callback : callbacks) {
                        if (callback instanceof NameCallback) {
                            ((NameCallback)callback).setName(username);
                        } else if (callback instanceof PasswordCallback) {
                            ((PasswordCallback)callback).setPassword(password.toCharArray());
                        }
                    }
                    return;
                }
            }
            super.handle(callbacks);
        }

        @Override
        public final void onSubmit() {
            PluginUserSession userSession = (PluginUserSession) getSession();
            if (!rememberme) {
                Cookie[] cookies = retrieveWebRequest().getHttpServletRequest().getCookies();
                if (cookies != null) {
                    for (Cookie cookie : cookies) {
                        if (RememberMeLoginPlugin.class.getName().equals(cookie.getName()) || getClass().getName().equals(cookie.getName())) {
                            retrieveWebResponse().clearCookie(cookie);
                        }
                    }
                }
            }

            boolean success = true;
            PageParameters loginExceptionPageParameters = null;

            try {
                userSession.login(new UserCredentials(this));
            } catch (org.hippoecm.frontend.session.LoginException le) {
                success = false;
                loginExceptionPageParameters = buildPageParameters(le);
            }

            if (success) {
                ConcurrentLoginFilter.validateSession(((WebRequest) SignInForm.this.getRequest()).getHttpServletRequest().getSession(true)
                        ,usernameTextField.getDefaultModelObjectAsString()
                        ,false);

                if (rememberme) {
                    Session jcrSession = userSession.getJcrSession();
                    if (jcrSession.getUserID().equals(username)) {
                        try {
                            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
                            digest.update(username.getBytes());
                            digest.update(password.getBytes());
                            String passphrase = digest.getAlgorithm() + "$"
                                    + Base64.encodeBase64URLSafeString(username.getBytes()) + "$"
                                    + Base64.encodeBase64URLSafeString(digest.digest());

                            retrieveWebResponse().addCookie(new Cookie(RememberMeLoginPlugin.class.getName(), passphrase));
                            Node userinfo = jcrSession.getRootNode().getNode(HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.USERS_PATH + "/" + username);
                            String[] strings = passphrase.split("\\$");
                            userinfo.setProperty(HippoNodeType.HIPPO_PASSKEY, strings[0] + "$" + strings[2]);
                            userinfo.save();
                        } catch (NoSuchAlgorithmException ex) {
                            log.error(ex.getClass().getName() + ": " + ex.getMessage());
                        } catch (LoginException ex) {
                            log.info("Invalid login as user: " + username);
                        } catch (RepositoryException ex) {
                            log.error(ex.getClass().getName() + ": " + ex.getMessage());
                        }
                    }
                }

                HippoEventBus eventBus = HippoServiceRegistry.getService(HippoEventBus.class);
                if (eventBus != null) {
                    HippoEvent event = new HippoEvent(userSession.getApplicationName())
                            .user(userSession.getJcrSession().getUserID())
                            .action("login")
                            .category(HippoSecurityEventConstants.CATEGORY_SECURITY)
                            .message(username + " logged in");
                    eventBus.post(event);
                }
            } else {
                HippoEventBus eventBus = HippoServiceRegistry.getService(HippoEventBus.class);
                if (eventBus != null) {
                    HippoEvent event = new HippoEvent(userSession.getApplicationName())
                            .user(userSession.getJcrSession().getUserID())
                            .action("login")
                            .category(HippoSecurityEventConstants.CATEGORY_SECURITY)
                            .result("failure")
                            .message(username + " failed to login");
                    eventBus.post(event);
                }
            }

            userSession.setLocale(new Locale(selectedLocale));
            redirect(success, loginExceptionPageParameters);
        }
    }

}
