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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.jcr.SimpleCredentials;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.extensions.markup.html.captcha.CaptchaImageResource;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.ResourceLink;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.CssResourceReference;
import org.hippoecm.frontend.PluginApplication;
import org.hippoecm.frontend.model.UserCredentials;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.session.LoginException;
import org.hippoecm.frontend.session.PluginUserSession;
import org.hippoecm.frontend.util.WebApplicationHelper;

public class SimpleLoginPlugin extends LoginPlugin {

    private static final long serialVersionUID = 1L;
    private final static String DEFAULT_KEY = "invalid.login";
    private final static Map<String, String> causeKeys = new HashMap<String, String>(6) {{
        put(LoginException.CAUSE.INCORRECT_CREDENTIALS.name(), "invalid.login");
        put(LoginException.CAUSE.INCORRECT_CAPTCHA.name(), "invalid.captcha");
        put(LoginException.CAUSE.ACCESS_DENIED.name(), "access.denied");
        put(LoginException.CAUSE.REPOSITORY_ERROR.name(), "repository.error");
        put(LoginException.CAUSE.PASSWORD_EXPIRED.name(), "password.expired");
        put(LoginException.CAUSE.ACCOUNT_EXPIRED.name(), "account.expired");
    }};
    private static final String EDITION_PROPERTY = "edition";

    public SimpleLoginPlugin(IPluginContext context, IPluginConfig config) {
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
        response.render(CssHeaderItem.forReference(new CssResourceReference(SimpleLoginPlugin.class, "login.css")));

        IPluginConfig config = getPluginConfig();
        if (config.containsKey(EDITION_PROPERTY)) {
            String edition = config.getString(EDITION_PROPERTY);
            // In case of using a different edition, add extra CSS rules to show the required styling
            final CssResourceReference editionResourceRef = new CssResourceReference(SimpleLoginPlugin.class, "login_" + edition + ".css");
            response.render(CssHeaderItem.forReference(editionResourceRef));
        }
    }

    @Override
    protected LoginPlugin.SignInForm createSignInForm(String id) {
        return new SignInForm(id);
    }

    protected class SignInForm extends LoginPlugin.SignInForm {
        private static final long serialVersionUID = 1L;

        // Random captcha password to match against
        private String imagePass;
        private Image captchaImage;
        private Label captchaLabel;
        private CaptchaImageResource captchaImageResource;
        private RequiredTextField<String> captchaTextField;
        private final FeedbackPanel feedback;

        private String captchaTextValue;
        private final int allowedAttempts;
        private final boolean useCaptcha;
        private int attemptsCounter;

        public SignInForm(final String id) {
            super(id);

            attemptsCounter = 0;
            useCaptcha = getPluginConfig().getAsBoolean("use.captcha", false);
            allowedAttempts = (getPluginConfig().getAsInteger("show.captcha.after.how.many.times", 3) < 0) ? 3
                    : getPluginConfig().getAsInteger("show.captcha.after.how.many.times", 3);

            // Create and add captcha related components
            createAndAddCaptcha(false);

            final boolean autocomplete = getPluginConfig().getAsBoolean("signin.form.autocomplete", true);
            add(new AttributeModifier("autocomplete", new Model<>(autocomplete ? "on" : "off")));

            feedback = new FeedbackPanel("feedback");
            feedback.setOutputMarkupId(true);
            feedback.setEscapeModelStrings(false);
            add(feedback);
            add(new Button("submit", new ResourceModel("submit-label")));
        }

        public boolean login() {
            final PluginUserSession userSession = (PluginUserSession) getSession();

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
            } catch (LoginException le) {
                success = false;
                loginExceptionPageParameters = buildPageParameters(le.getLoginExceptionCause());
            } catch (AccessControlException ace) {
                success = false;
                // Invalidate the current obtained JCR session and create an anonymous one
                userSession.login();
                loginExceptionPageParameters = buildPageParameters(LoginException.CAUSE.ACCESS_DENIED);
            }

            if (success) {
                final ServletWebRequest servletWebRequest = WebApplicationHelper.retrieveWebRequest();

                ConcurrentLoginFilter.validateSession(servletWebRequest.getContainerRequest().getSession(true), username, false);

            } else {
                handleLoginFailure(loginExceptionPageParameters, userSession);
            }

            userSession.setLocale(new Locale(selectedLocale));

            return success;
        }

        @Override
        public void onSubmit() {
            // final LoginStatusTuple loginStatus = SignInForm.this.login();
            if (login()) {
                redirect(true);
            } else {
                // Check whether to show captcha or not
                if (useCaptcha && attemptsCounter >= allowedAttempts) {
                    createAndAddCaptcha(true);
                } else {
                    // Increment the number of login trials
                    attemptsCounter++;
                }
            }
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
            captchaTextField = new RequiredTextField<String>("captcha", new PropertyModel<String>(SignInForm.this, "captchaTextValue"));
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
        // Get an anonymous session, this is in case the user provided valid username and password
        // but failed to provide a valid captcha is case it was enabled and displayed
        userSession.login();
    }

}
