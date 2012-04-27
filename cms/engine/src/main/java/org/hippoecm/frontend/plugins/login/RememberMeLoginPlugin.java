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
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.markup.html.captcha.CaptchaImageResource;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebResponse;
import org.hippoecm.frontend.PluginPage;
import org.hippoecm.frontend.custom.ServerCookie;
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

    private static final int COOKIE_DEFAULT_MAX_AGE = 1209600;
    private static final String REMEMBERME_COOKIE_NAME = "rememberme";
    private static final String HIPPO_AUTO_LOGIN_COOKIE_NAME = "hal";
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

        // Check for remember me cookie
        if ((retrieveWebRequest().getCookie(REMEMBERME_COOKIE_NAME) != null)
                && (retrieveWebRequest().getCookie(HIPPO_AUTO_LOGIN_COOKIE_NAME) != null)) {

            tryToAutoLoginWithRememberMe();
        }
    }

    protected void tryToAutoLoginWithRememberMe() {
        SignInForm signInForm = (SignInForm) createSignInForm("rememberMeAutoLoginSignInForm");
        signInForm.onSubmit();
    }

    @Override
    protected LoginPlugin.SignInForm createSignInForm(String id) {
        Cookie rememberMeCookie = retrieveWebRequest().getCookie(REMEMBERME_COOKIE_NAME); 
        boolean rememberme = (rememberMeCookie != null) ? Boolean.valueOf(rememberMeCookie.getValue()) : false;

        if (rememberme) {
            Cookie halCookie = retrieveWebRequest().getCookie(HIPPO_AUTO_LOGIN_COOKIE_NAME);
            if (halCookie != null) {
                String passphrase = rememberMeCookie.getValue();
                if (passphrase != null && passphrase.contains("$")) {
                    username = new String(Base64.decodeBase64(passphrase.split("\\$")[1]));
                    password = "********";
                }
            }
        }

        LoginPlugin.SignInForm form = new SignInForm(id, rememberme);
        return form;
    }

    protected class SignInForm extends org.hippoecm.frontend.plugins.login.LoginPlugin.SignInForm {
        private static final long serialVersionUID = 1L;

        private final CaptchaImageResource captchaImageResource;
        // Random captcha password to match against
        private final String imagePass;
        private final Image captchaImage;
        private final Label captchaLabel;
        private final RequiredTextField<String> captchaTextField;

        private boolean rememberme;
        private String captchaTextValue;

        public void setRememberme(boolean value) {
            rememberme = value;
        }

        public boolean getRememberme() {
            return rememberme;
        }

        public SignInForm(final String id, boolean rememberme) {
            super(id);

            final PluginUserSession session = (PluginUserSession) getSession();

            // Prepare Captcha resources
            imagePass = randomString(6, 8);
            captchaImageResource = new CaptchaImageResource(imagePass);
            captchaLabel = new Label("captchaLabel", new ResourceModel("captcha-label", "Enter the letters above"));
            captchaTextField = new RequiredTextField<String>("captcha", new PropertyModel<String>(SignInForm.this, "captchaTextValue"));
            captchaImage = new Image("captchaImage", captchaImageResource);

            final boolean useCaptcha = getPluginConfig().getAsBoolean("use.captcha", false);
            int nrUnsuccessfulLogins = getPluginConfig().getAsInteger("nr.unsuccessful.logins", 3);
            nrUnsuccessfulLogins = (nrUnsuccessfulLogins < 0) ? 3 : nrUnsuccessfulLogins;
            if (useCaptcha && session.getUnsuccessfulLoginCounter() >= nrUnsuccessfulLogins) {
                captchaImage.setVisible(true);
                captchaTextField.setVisible(true);
                captchaLabel.setVisible(true);
            } else {
                captchaImage.setVisible(false);
                captchaTextField.setVisible(false);
                captchaLabel.setVisible(false);
            }

            add(captchaImage);
            add(captchaTextField);
            add(captchaLabel);

            this.rememberme = rememberme;

            if (RememberMeLoginPlugin.this.getPluginConfig().getAsBoolean("signin.form.autocomplete", false)) {
                add(new AttributeModifier("autocomplete", true, new Model<String>("on")));
            } else {
                add(new AttributeModifier("autocomplete", true, new Model<String>("off")));
            }

            CheckBox rememberMeCheckbox = new CheckBox("rememberme", new PropertyModel<Boolean>(this, "rememberme"));
            add(rememberMeCheckbox);
            rememberMeCheckbox.add(new AjaxFormComponentUpdatingBehavior("onchange") {
                private static final long serialVersionUID = 1L;

                protected void onUpdate(AjaxRequestTarget target) {
                    // When the 'Remember me' check-box is un-checked Username and Password fields should be cleared 
                    if (!SignInForm.this.getRememberme()) {
                        SignInForm.this.usernameTextField.setModelObject("");
                        SignInForm.this.passwordTextField.setModelObject("");
                        // Also remove the cookie which contains user information
                        clearCookie(REMEMBERME_COOKIE_NAME);
                        clearCookie(HIPPO_AUTO_LOGIN_COOKIE_NAME);
                    } else {
                        Cookie remembermeCookie = new Cookie(REMEMBERME_COOKIE_NAME, String.valueOf(true));
                        remembermeCookie.setMaxAge(RememberMeLoginPlugin.this.getPluginConfig().getAsInteger("rememberme.cookie.maxage", COOKIE_DEFAULT_MAX_AGE));
                        retrieveWebResponse().addCookie(remembermeCookie);
                    }

                    setResponsePage(this.getFormComponent().getPage());
                }
            });

            usernameTextField.add(new AjaxFormComponentUpdatingBehavior("onchange") {
                private static final long serialVersionUID = 1L;

                protected void onUpdate(AjaxRequestTarget target) {
                    clearCookie(HIPPO_AUTO_LOGIN_COOKIE_NAME);
                }
            });

            passwordTextField.add(new AjaxFormComponentUpdatingBehavior("onchange") {
                private static final long serialVersionUID = 1L;

                protected void onUpdate(AjaxRequestTarget target) {
                    clearCookie(HIPPO_AUTO_LOGIN_COOKIE_NAME);
                }
            });

        }

        @Override
        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            String username = RememberMeLoginPlugin.this.username;
            String password = RememberMeLoginPlugin.this.password;

            if (rememberme) {
                if (password == null || password.equals("") || password.replaceAll("\\*", "").equals("")) {
                    Cookie remembermeCookie = retrieveWebRequest().getCookie(HIPPO_AUTO_LOGIN_COOKIE_NAME);

                    if (remembermeCookie != null) {
                        String passphrase = remembermeCookie.getValue();
                        String strings[] = passphrase.split("\\$");
                        if (strings.length == 3) {
                            username = new String(Base64.decodeBase64(strings[1]));
                            password = strings[0] + "$" + strings[2];
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
            final PluginUserSession userSession = (PluginUserSession) getSession();

            if (!rememberme) {
                clearCookie(REMEMBERME_COOKIE_NAME);
                clearCookie(HIPPO_AUTO_LOGIN_COOKIE_NAME);
                clearCookie(getClass().getName());
            }

            boolean success = true;
            PageParameters loginExceptionPageParameters = null;

            try {
                userSession.login(new UserCredentials(this));
                // Check if captcha is shown and then check whether the provided value is correct or not
                if (captchaImage.isVisible() && captchaTextField.isVisible()) {
                    success = captchaTextValue.equalsIgnoreCase(imagePass);
                    if (!success) {
                        throw new org.hippoecm.frontend.session.LoginException(
                                org.hippoecm.frontend.session.LoginException.CAUSE.INCORRECT_CAPTACHA);

                    }
                }
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

                            final Cookie halCookie = new Cookie(HIPPO_AUTO_LOGIN_COOKIE_NAME, passphrase);
                            halCookie.setMaxAge(RememberMeLoginPlugin.this.getPluginConfig().getAsInteger("hal.cookie.maxage", COOKIE_DEFAULT_MAX_AGE));
                            halCookie.setSecure(RememberMeLoginPlugin.this.getPluginConfig().getAsBoolean("use.secure.cookies", false));

                            // Replace with Cookie#setHttpOnly when we upgrade to a container compliant with
                            // Servlet API(s) v3.0t his was added cause the setHttpOnly/isHttpOnly at the time of
                            // developing this code were not available cause we used to use Servlet API(s) v2.5
                            RememberMeLoginPlugin.this.addCookieWithHttpOnly(
                                    halCookie,
                                    retrieveWebResponse(),
                                    RememberMeLoginPlugin.this.getPluginConfig().getAsBoolean("use.httponly.cookies",
                                            false));

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

                // Make sure to reset the unsuccessful login counter not to make problems for later logins while
                // the session is not yet invalidated
                userSession.resetUnsuccessfulLoginCounter();
            } else {
                // Increment the number of unsuccessful logins
                userSession.setUnSuccessfulLoginCounter(userSession.getUnsuccessfulLoginCounter() + 1);
                // Get an anonymous session, this is in case the user provided valid username and password
                // but failed to provide a valid captcha is case it was enabled and displayed
                userSession.login();
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
            if (rememberme && success) {
                throw new RestartResponseException(PluginPage.class);
            } else {
                redirect(success, loginExceptionPageParameters);
            }
        }
    }

    private static int randomInt(int min, int max) {
        return (int) (Math.random() * (max - min) + min);
    }

    private String randomString(int min, int max) {
        int num = randomInt(min, max);
        byte b[] = new byte[num];

        for (int i = 0; i < num; i++) {
            b[i] = (byte) randomInt('a', 'z');
        }

        return new String(b);
    }

    // TO be deleted when we upgrade to a container compliant with Servlet API(s) v3.0
    // This was added cause the setHttpOnly/isHttpOnly at the time of developing this code were not available
    // cause we used to use Servlet API(s) v2.5
    private void addCookieWithHttpOnly(Cookie cookie, WebResponse response, boolean useHttpOnly) {
        if (useHttpOnly) {
            final StringBuffer setCookieHeaderBuffer = new StringBuffer();
            ServerCookie.appendCookieValue(setCookieHeaderBuffer, cookie.getVersion(), cookie.getName(),
                    cookie.getValue(), cookie.getPath(), cookie.getDomain(), cookie.getComment(), cookie.getMaxAge(),
                    cookie.getSecure(), useHttpOnly);

            response.getHttpServletResponse().addHeader("Set-Cookie", setCookieHeaderBuffer.toString());
        } else {
            response.addCookie(cookie);
        }
    }

}
