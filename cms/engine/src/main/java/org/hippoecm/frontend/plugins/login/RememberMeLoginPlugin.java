/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.markup.html.captcha.CaptchaImageResource;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.ResourceLink;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.CssResourceReference;
import org.hippoecm.frontend.PageExpiredErrorPage;
import org.hippoecm.frontend.PluginApplication;
import org.hippoecm.frontend.PluginPage;
import org.hippoecm.frontend.custom.ServerCookie;
import org.hippoecm.frontend.model.UserCredentials;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.session.LoginException;
import org.hippoecm.frontend.session.PluginUserSession;
import org.hippoecm.frontend.util.AclChecker;
import org.hippoecm.frontend.util.WebApplicationHelper;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RememberMeLoginPlugin extends LoginPlugin {

    private final static String DEFAULT_KEY = "invalid.login";
    private final static Map<String, String> causeKeys;

    static {
        causeKeys = new HashMap<String, String>(6);
        causeKeys.put(LoginException.CAUSE.INCORRECT_CREDENTIALS.name(), "invalid.login");
        causeKeys.put(LoginException.CAUSE.INCORRECT_CAPTCHA.name(), "invalid.captcha");
        causeKeys.put(LoginException.CAUSE.ACCESS_DENIED.name(), "access.denied");
        causeKeys.put(LoginException.CAUSE.REPOSITORY_ERROR.name(), "repository.error");
        causeKeys.put(LoginException.CAUSE.PASSWORD_EXPIRED.name(), "password.expired");
        causeKeys.put(LoginException.CAUSE.ACCOUNT_EXPIRED.name(), "account.expired");
    }

    private static final int COOKIE_DEFAULT_MAX_AGE = 1209600;
    private static final String HAL_REQUEST_ATTRIBUTE_NAME = "in_try_hippo_autologin";
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(RememberMeLoginPlugin.class);

    /**
     * Algorithm to use for creating the passkey secret. Intentionally a relative weak algorithm, as this whole
     * procedure isn't too safe to begin with.
     */
    private static final String ALGORITHM = "MD5";
    private static final String EDITION_PROPERTY = "edition";

    private final String REMEMBERME_COOKIE_NAME = WebApplicationHelper.getFullyQualifiedCookieName(WebApplicationHelper.REMEMBERME_COOKIE_BASE_NAME);
    private final String HIPPO_AUTO_LOGIN_COOKIE_NAME = WebApplicationHelper.getFullyQualifiedCookieName(WebApplicationHelper.HIPPO_AUTO_LOGIN_COOKIE_BASE_NAME);

    public RememberMeLoginPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        String[] supported = config.getStringArray("browsers.supported");
        if (supported != null) {
            add(new BrowserCheckBehavior(supported));
        }
        add(new ResourceLink("faviconLink", ((PluginApplication) getApplication()).getPluginApplicationFavIconReference()));
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(new CssResourceReference(RememberMeLoginPlugin.class, "login.css")));

        IPluginConfig config = getPluginConfig();
        if (config.containsKey(EDITION_PROPERTY)) {
            String edition = config.getString(EDITION_PROPERTY);
            // In case of using a different edition, add extra CSS rules to show the required styling
            final CssResourceReference editionResourceRef = new CssResourceReference(RememberMeLoginPlugin.class, "login_" + edition + ".css");
            response.render(CssHeaderItem.forReference(editionResourceRef));
        }
    }

    // Determine whether to try to auto-login or not
    @Override
    protected void onInitialize() {
        if (!PageExpiredErrorPage.class.isInstance(getPage())) {
            // Check for remember me cookie
            final ServletWebRequest servletWebRequest = WebApplicationHelper.retrieveWebRequest();
            final HttpServletRequest httpServletRequest = servletWebRequest.getContainerRequest();
            if ((servletWebRequest.getCookie(REMEMBERME_COOKIE_NAME) != null)
                    && (servletWebRequest.getCookie(HIPPO_AUTO_LOGIN_COOKIE_NAME) != null)
                    && (httpServletRequest.getAttribute(HAL_REQUEST_ATTRIBUTE_NAME) == null)) {

                httpServletRequest.setAttribute(HAL_REQUEST_ATTRIBUTE_NAME, true);
                try {
                    tryToAutoLoginWithRememberMe();
                } finally {
                    httpServletRequest.removeAttribute(HAL_REQUEST_ATTRIBUTE_NAME);
                }
            }
        }

        super.onInitialize();
    }

    protected void tryToAutoLoginWithRememberMe() {
        SignInForm signInForm = (SignInForm) get("signInForm");

        Cookie remembermeCookie = WebApplicationHelper.retrieveWebRequest().getCookie(HIPPO_AUTO_LOGIN_COOKIE_NAME);
        String passphrase = remembermeCookie.getValue();
        String strings[] = passphrase.split("\\$");
        if (strings.length == 3) {
            username = new String(Base64.decodeBase64(strings[1]));
            password = strings[0] + "$" + strings[2];

            // final LoginStatusTuple loginStatus = signInForm.login();
            if (signInForm.login()) {
                signInForm.redirect(true);
            }
        } else {
            error("Invalid cookie format for " + HIPPO_AUTO_LOGIN_COOKIE_NAME);
        }

    }

    @Override
    protected LoginPlugin.SignInForm createSignInForm(String id) {
        Cookie rememberMeCookie = WebApplicationHelper.retrieveWebRequest().getCookie(
                WebApplicationHelper.getFullyQualifiedCookieName(WebApplicationHelper.REMEMBERME_COOKIE_BASE_NAME));

        boolean rememberme = (rememberMeCookie != null) ? Boolean.valueOf(rememberMeCookie.getValue()) : false;

        if (rememberme) {
            Cookie halCookie = WebApplicationHelper.retrieveWebRequest().getCookie(HIPPO_AUTO_LOGIN_COOKIE_NAME);
            if (halCookie != null) {
                String passphrase = rememberMeCookie.getValue();
                if (passphrase != null && passphrase.contains("$")) {
                    username = new String(Base64.decodeBase64(passphrase.split("\\$")[1]));
                    password = "********";
                }
            }
        }

        return new SignInForm(id, rememberme);
    }

    protected class SignInForm extends org.hippoecm.frontend.plugins.login.LoginPlugin.SignInForm {
        private static final long serialVersionUID = 1L;

        // Random captcha password to match against
        private String imagePass;
        private Image captchaImage;
        private Label captchaLabel;
        private CaptchaImageResource captchaImageResource;
        private RequiredTextField<String> captchaTextField;
        private final FeedbackPanel feedback;

        private boolean rememberme;
        private String captchaTextValue;
        private final int nrUnsuccessfulLogins;
        private final boolean useCaptcha;
        private int loginTrialsCounter;

        public void setRememberme(boolean value) {
            rememberme = value;
        }

        public boolean getRememberme() {
            return rememberme;
        }

        public SignInForm(final String id, boolean rememberme) {
            super(id);

            loginTrialsCounter = 0;
            useCaptcha = getPluginConfig().getAsBoolean("use.captcha", false);
            nrUnsuccessfulLogins = (getPluginConfig().getAsInteger("show.captcha.after.how.many.times", 3) < 0) ? 3
                    : getPluginConfig().getAsInteger("show.captcha.after.how.many.times", 3);

            // Create and add captcha related components
            createAndAddCaptcha(false);

            this.rememberme = rememberme;

            if (RememberMeLoginPlugin.this.getPluginConfig().getAsBoolean("signin.form.autocomplete", true)) {
                add(new AttributeModifier("autocomplete", true, new Model<String>("on")));
            } else {
                add(new AttributeModifier("autocomplete", true, new Model<String>("off")));
            }

            CheckBox rememberMeCheckbox = new CheckBox("rememberme", new PropertyModel<Boolean>(this, "rememberme"));
            add(rememberMeCheckbox);
            rememberMeCheckbox.add(new AjaxFormComponentUpdatingBehavior("onchange") {
                private static final long serialVersionUID = 1L;

                protected void onUpdate(AjaxRequestTarget target) {
                    // When the 'Remember me' check-box is un-checked clean the password field if the user did not fill
                    // in his/her own password
                    if (!SignInForm.this.getRememberme()) {
                        final String passwordValue = SignInForm.this.passwordTextField.getModelObject();

                        if (StringUtils.isNotBlank(passwordValue) && passwordValue.equals("********")) {
                            SignInForm.this.passwordTextField.setModelObject("");
                        }
                        target.add(passwordTextField);

                        // Also remove the cookie which contains user information
                        WebApplicationHelper.clearCookie(REMEMBERME_COOKIE_NAME);
                        WebApplicationHelper.clearCookie(HIPPO_AUTO_LOGIN_COOKIE_NAME);
                    } else {
                        Cookie remembermeCookie = new Cookie(REMEMBERME_COOKIE_NAME, String.valueOf(true));
                        remembermeCookie.setMaxAge(RememberMeLoginPlugin.this.getPluginConfig().getAsInteger(
                                "rememberme.cookie.maxage", COOKIE_DEFAULT_MAX_AGE));

                        WebApplicationHelper.retrieveWebResponse().addCookie(remembermeCookie);
                    }
                }
            });

            usernameTextField.add(new AjaxFormComponentUpdatingBehavior("onchange") {
                private static final long serialVersionUID = 1L;

                protected void onUpdate(AjaxRequestTarget target) {
                    WebApplicationHelper.clearCookie(HIPPO_AUTO_LOGIN_COOKIE_NAME);
                }
            });

            passwordTextField.add(new AjaxFormComponentUpdatingBehavior("onchange") {
                private static final long serialVersionUID = 1L;

                protected void onUpdate(AjaxRequestTarget target) {
                    WebApplicationHelper.clearCookie(HIPPO_AUTO_LOGIN_COOKIE_NAME);
                }
            });

            feedback = new FeedbackPanel("feedback");
            feedback.setOutputMarkupId(true);
            feedback.setEscapeModelStrings(false);
            add(feedback);

            AjaxButton ajaxButton = new AjaxButton("submit", new ResourceModel("submit-label")) {

                @Override
                protected void onError(AjaxRequestTarget target, Form<?> form) {
                    target.add(feedback);
                }

                @Override
                protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                    // final LoginStatusTuple loginStatus = SignInForm.this.login();
                    if (SignInForm.this.login()) {
                        redirect(true);
                    } else {
                        // Check whether to show captcha or not
                        if (useCaptcha && loginTrialsCounter >= nrUnsuccessfulLogins) {
                            createAndAddCaptcha(true);
                        } else {
                            // Increment the number of login trials
                            loginTrialsCounter++;
                        }

                        target.add(SignInForm.this);
                    }
                }

            };

            // AjaxFormValidatingBehavior.addToAllFormComponents(this, "onkeyup", Duration.ONE_SECOND);
            // ajaxButton.setDefaultFormProcessing(false);
            add(ajaxButton);
        }

        public boolean login() {
            final PluginUserSession userSession = (PluginUserSession) getSession();

            if (!rememberme) {
                WebApplicationHelper.clearCookie(REMEMBERME_COOKIE_NAME);
                WebApplicationHelper.clearCookie(HIPPO_AUTO_LOGIN_COOKIE_NAME);
                // This still exists in case there is a cookie with the old name still exists in browser's cache
                WebApplicationHelper.clearCookie(getClass().getName());
            }

            boolean success = true;
            PageParameters loginExceptionPageParameters = null;

            try {
                // Check if captcha is shown and then check whether the provided value is correct or not
                if (captchaImage.isVisible() && captchaTextField.isVisible()) {
                    success = captchaTextValue.equalsIgnoreCase(imagePass);
                    if (!success) {
                        throw new LoginException(LoginException.CAUSE.INCORRECT_CAPTCHA);

                    }
                }

                userSession.login(new UserCredentials(new SimpleCredentials(username, password.toCharArray())));
                AclChecker.checkAccess(getPluginConfig(), userSession.getJcrSession(), PluginApplication.get().getPluginApplicationName());
            } catch (LoginException le) {
                success = false;
                loginExceptionPageParameters = buildPageParameters(le.getLoginExceptionCause());
            } catch (AccessControlException ace) {
                success = false;
                // Invalidate the current obtained JCR session and create an anonymous one
                userSession.login();
                loginExceptionPageParameters = buildPageParameters(LoginException.CAUSE.ACCESS_DENIED);
            } catch (RepositoryException re) {
                success = false;
                // Invalidate the current obtained JCR session and create an anonymous one
                userSession.login();
                if (log.isDebugEnabled()) {
                    log.warn("Repository error while trying to access the "
                            + PluginApplication.get().getPluginApplicationName() + " application with user '" + username
                            + "'", re);
                }

                loginExceptionPageParameters = buildPageParameters(LoginException.CAUSE.REPOSITORY_ERROR);
            }

            if (success) {
                final ServletWebRequest servletWebRequest = WebApplicationHelper.retrieveWebRequest();

                ConcurrentLoginFilter.validateSession(servletWebRequest.getContainerRequest().getSession(true), username, false);

                // If rememberme checkbox is checked and there is no cookie already, this happens in case of autologin
                if (rememberme&& servletWebRequest.getCookie(HIPPO_AUTO_LOGIN_COOKIE_NAME) == null) {
                    Session jcrSession = userSession.getJcrSession();
                    if (jcrSession.getUserID().equals(username)) {
                        try {
                            Node userinfo = RememberMeLoginPlugin.this.getUserInfo(jcrSession);

                            if (userinfo != null) {
                                MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
                                digest.update(username.getBytes());
                                digest.update(password.getBytes());
                                String passphrase = digest.getAlgorithm() + "$"
                                        + Base64.encodeBase64URLSafeString(username.getBytes()) + "$"
                                        + Base64.encodeBase64URLSafeString(digest.digest());

                                String[] strings = passphrase.split("\\$");
                                userinfo.setProperty(HippoNodeType.HIPPO_PASSKEY, strings[0] + "$" + strings[2]);
                                userinfo.save();

                                final Cookie halCookie = new Cookie(HIPPO_AUTO_LOGIN_COOKIE_NAME, passphrase);
                                halCookie.setMaxAge(RememberMeLoginPlugin.this.getPluginConfig().getAsInteger(
                                        "hal.cookie.maxage", COOKIE_DEFAULT_MAX_AGE));

                                halCookie.setSecure(RememberMeLoginPlugin.this.getPluginConfig().getAsBoolean(
                                        "use.secure.cookies", false));

                                // Replace with Cookie#setHttpOnly when we upgrade to a container compliant with
                                // Servlet API(s) v3.0t his was added cause the setHttpOnly/isHttpOnly at the time of
                                // developing this code were not available cause we used to use Servlet API(s) v2.5
                                RememberMeLoginPlugin.this.addCookieWithHttpOnly(
                                        halCookie,
                                        WebApplicationHelper.retrieveWebResponse(),
                                        RememberMeLoginPlugin.this.getPluginConfig().getAsBoolean("use.httponly.cookies",
                                                false));
                            } else {
                                loginExceptionPageParameters = buildPageParameters(LoginException.CAUSE.REPOSITORY_ERROR);
                                handleLoginFailure(loginExceptionPageParameters, userSession);
                                success = false;
                            }
                        } catch (NoSuchAlgorithmException ex) {
                            log.error(ex.getClass().getName() + ": " + ex.getMessage());
                        } catch (RepositoryException ex) {
                            log.error(ex.getClass().getName() + ": " + ex.getMessage());
                        }
                    }
                }
            } else {
                handleLoginFailure(loginExceptionPageParameters, userSession);
            }

            userSession.setLocale(new Locale(selectedLocale));

            if (rememberme && success) {
                throw new RestartResponseException(PluginPage.class);
            }

            return success;
        }

        @Override
        public void onSubmit() {
        }

        protected void createAndAddCaptcha(boolean isVisible) {
            // Prepare Captcha resources
            if (captchaImage != null) {
                remove(captchaImage);
            }

            if (captchaTextField != null) {
                remove(captchaTextField);
            }

            if (captchaLabel != null) {
                remove(captchaLabel);
            }

            imagePass = randomString(6, 8);
            captchaImageResource = new CaptchaImageResource(imagePass);
            captchaLabel = new Label("captchaLabel", new ResourceModel("captcha-label", "Enter the letters above"));
            // Clear the value of the captcha text field
            captchaTextValue = "";
            captchaTextField = new RequiredTextField<String>("captcha", new PropertyModel<String>(SignInForm.this,
                    "captchaTextValue"));
            captchaImage = new Image("captchaImage", captchaImageResource) {

                private static final long serialVersionUID = 1L;

                // This method is overridden to properly forces the browser to refresh the image for the newly created
                // captcha image component
                @Override
                protected void onComponentTag(ComponentTag tag) {
                    super.onComponentTag(tag);
                    String src = (String) tag.getAttributes().get("src");
                    src = src + "&rand=" + Math.random();
                    tag.getAttributes().put("src", src);
                }

            };

            captchaImage.setVisible(isVisible);
            captchaTextField.setVisible(isVisible);
            captchaLabel.setVisible(isVisible);

            add(captchaImage);
            add(captchaTextField);
            add(captchaLabel);
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

            response.addHeader("Set-Cookie", setCookieHeaderBuffer.toString());
        } else {
            response.addCookie(cookie);
        }
    }

    private Node getUserInfo(final Session session) throws RepositoryException {
        final String userId = sanitize(session.getUserID());
        StringBuilder statement = new StringBuilder();

        statement.append("//element");
        statement.append("(*, ").append(HippoNodeType.NT_USER).append(")");
        statement.append('[').append("fn:name() = ").append("'").append(NodeNameCodec.encode(userId, true)).append("'").append(']');

        try {
            Query q = session.getWorkspace().getQueryManager().createQuery(statement.toString(), Query.XPATH);
            QueryResult result = q.execute();
            NodeIterator nodesIterator = result.getNodes();

            if (nodesIterator.hasNext()) {
                return nodesIterator.nextNode();
            }
        } catch (RepositoryException rex) {
            log.info("Could not retrieve information of user: '{}'", userId);

            if (log.isDebugEnabled()) {
                log.debug("Error happened while retrieving information of user: '" + userId + "'", rex);
            }

            throw rex;
        }

        return null;
    }

    private String sanitize(final String userId) {
        return userId.trim();
    }

    private void handleLoginFailure(PageParameters loginExceptionPageParameters, PluginUserSession userSession) {
        String key = DEFAULT_KEY;
        if (loginExceptionPageParameters != null) {
            Object loginExceptionCause = loginExceptionPageParameters.get(LoginException.CAUSE.class.getName());

            if (loginExceptionCause instanceof String) {
                key = causeKeys.get(loginExceptionCause);
                key = StringUtils.isNotBlank(key) ? key : DEFAULT_KEY;
            }

            info(new StringResourceModel(key, this, null).getString());
        }

        // Clear the Hippo Auto Login cookie
        WebApplicationHelper.clearCookie(HIPPO_AUTO_LOGIN_COOKIE_NAME);
        // Get an anonymous session, this is in case the user provided valid username and password
        // but failed to provide a valid captcha is case it was enabled and displayed
        userSession.login();
    }

}
