/*
 *  Copyright 2009-2018 Hippo B.V. (http://www.onehippo.com)
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

import java.util.List;

import org.apache.wicket.extensions.markup.html.captcha.CaptchaImageResource;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.session.LoginException;

import static org.hippoecm.frontend.session.LoginException.Cause;

public class SimpleLoginPlugin extends LoginPlugin {

    public static final Cause INCORRECT_CAPTCHA = LoginException.newCause("invalid.captcha");

    public SimpleLoginPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);
    }

    @Override
    protected LoginPanel createLoginPanel(final String id, final boolean autoComplete, final List<String> locales,
                                         final LoginHandler handler) {
        return new CaptchaForm(id, autoComplete, locales, handler);
    }

    protected class CaptchaForm extends LoginPanel {

        // Random captcha password to match against
        private String imagePass;
        private Image captchaImage;
        private Label captchaLabel;
        private CaptchaImageResource captchaImageResource;
        private RequiredTextField<String> captchaTextField;

        private String captchaTextValue;
        private final int allowedAttempts;
        private final boolean useCaptcha;
        private int failedAttempts;

        public CaptchaForm(final String id, final boolean autoComplete, final List<String> locales,
                           final LoginHandler handler) {
            super(id, autoComplete, locales, handler);

            failedAttempts = 0;
            useCaptcha = getPluginConfig().getAsBoolean("use.captcha", false);
            allowedAttempts = (getPluginConfig().getAsInteger("show.captcha.after.how.many.times", 3) < 0) ? 3
                    : getPluginConfig().getAsInteger("show.captcha.after.how.many.times", 3);

            // Create and add captcha related components
            createAndAddCaptcha(false);
        }

        @Override
        protected void login() throws LoginException {
            if (useCaptcha && isCaptchaEnabled() && !captchaTextValue.equalsIgnoreCase(imagePass)) {
                throw new LoginException(INCORRECT_CAPTCHA);
            }

            super.login();
        }

        private boolean isCaptchaEnabled() {
            return captchaImage.isVisible() && captchaTextField.isVisible();
        }

        @Override
        protected void loginFailed(final Cause cause) {
            // Check whether to show captcha or not
            if (useCaptcha && failedAttempts >= allowedAttempts) {
                createAndAddCaptcha(true);
            } else {
                // Increment the number of login trials
                failedAttempts++;
            }
            super.loginFailed(cause);
        }

        protected void createAndAddCaptcha(final boolean isVisible) {
            // Prepare Captcha resources
            if (captchaImage != null) {
                form.remove(captchaImage);
            }

            if (captchaTextField != null) {
                form.remove(captchaTextField);
            }

            if (captchaLabel != null) {
                form.remove(captchaLabel);
            }

            imagePass = randomString(6, 8);
            captchaImageResource = new CaptchaImageResource(imagePass);
            captchaLabel = new Label("captchaLabel", new ResourceModel("captcha-label", "Enter the letters above"));
            // Clear the value of the captcha text field
            captchaTextValue = "";

            final PropertyModel<String> captchaTextValue = PropertyModel.of(CaptchaForm.this, "captchaTextValue");
            captchaTextField = new RequiredTextField<>("captcha", captchaTextValue);
            captchaImage = new Image("captchaImage", captchaImageResource) {

                // This method is overridden to properly forces the browser to refresh the image for the newly created
                // captcha image component
                @Override
                protected void onComponentTag(final ComponentTag tag) {
                    super.onComponentTag(tag);
                    String src = (String) tag.getAttributes().get("src");
                    src = src + "&rand=" + Math.random();
                    tag.getAttributes().put("src", src);
                }

            };

            captchaImage.setVisible(isVisible);
            captchaTextField.setVisible(isVisible);
            captchaLabel.setVisible(isVisible);

            form.add(captchaImage);
            form.add(captchaTextField);
            form.addLabelledComponent(captchaLabel);
        }
    }

    private static int randomInt(final int min, final int max) {
        return (int) (Math.random() * (max - min) + min);
    }

    private String randomString(final int min, final int max) {
        final int num = randomInt(min, max);
        final byte[] b = new byte[num];

        for (int i = 0; i < num; i++) {
            b[i] = (byte) randomInt('a', 'z');
        }

        return new String(b);
    }
}
