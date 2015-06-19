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
package org.hippoecm.frontend.plugins.cms.admin.users;

import org.apache.wicket.extensions.validation.validator.RfcCompliantEmailAddressValidator;
import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidationError;
import org.apache.wicket.validation.validator.EmailAddressValidator;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.Assert;

/**
 */
public class TestCreateUserPanel {
    private static final Logger log = LoggerFactory.getLogger(TestCreateUserPanel.class);

    /* CMS7-9148 */
    @Test
    public void testEmailValidation(){

        String email = "o'brian-01_01@e-mail-01.com";

        IValidatable<String> emailValidatable = new EmailValidatable(email);
        EmailAddressValidator emailAddressValidator = EmailAddressValidator.getInstance();
        emailAddressValidator.validate(emailValidatable);
        Assert.assertFalse(emailValidatable.isValid());

        emailValidatable = new EmailValidatable(email);
        RfcCompliantEmailAddressValidator rfcCompliantEmailAddressValidator = RfcCompliantEmailAddressValidator.getInstance();
        rfcCompliantEmailAddressValidator.validate(emailValidatable);
        Assert.assertTrue(emailValidatable.isValid());
    }

    private class EmailValidatable implements IValidatable<String> {

        private boolean valid = true;
        private final String email;

        public EmailValidatable(final String email){
            this.email = email;
        }

        @Override
        public String getValue() {
            return this.email;
        }

        @Override
        public void error(final IValidationError error) {
            log.error("Validation error: ", error);
            this.valid = false;
        }

        @Override
        public boolean isValid() {
            return this.valid;
        }

        @Override
        public IModel<String> getModel() {
            return null;
        }
    }
}
