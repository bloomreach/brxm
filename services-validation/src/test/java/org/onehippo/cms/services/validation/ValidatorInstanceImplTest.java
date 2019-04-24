/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms.services.validation;

import java.util.Locale;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms.services.validation.api.ValidationContext;
import org.onehippo.cms.services.validation.api.ValidationContextException;
import org.onehippo.cms.services.validation.api.Validator;
import org.onehippo.cms.services.validation.api.ValidatorConfig;
import org.onehippo.cms.services.validation.api.Violation;
import org.onehippo.cms.services.validation.api.ViolationFactory;
import org.onehippo.cms.services.validation.validator.TestValidationContext;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ValidatorInstanceImplTest {

    @Test
    public void getConfig() {
        final ValidatorConfig config = new TestValidatorConfig("test-validator", TestValidator1.class.getName());
        final ValidatorInstanceImpl instance = new ValidatorInstanceImpl(new TestValidator1(), config);
        assertThat(instance.getConfig(), equalTo(config));
    }

    @Test
    public void validateOnce() {
        final String validatorName = "test-validator-1";
        final ValidatorConfig config = new TestValidatorConfig(validatorName, TestValidator1.class.getName());
        final ValidatorInstanceImpl instance = new ValidatorInstanceImpl(new TestValidator1(), config);
        testValidate(instance, new Locale("nl"), validatorName);
    }

    @Test
    public void validateTwiceResetsLocale() {
        final String validatorName = "test-validator-1";
        final ValidatorConfig config = new TestValidatorConfig(validatorName, TestValidator1.class.getName());
        final ValidatorInstanceImpl instance = new ValidatorInstanceImpl(new TestValidator1(), config);

        testValidate(instance, new Locale("nl"), validatorName);
        testValidate(instance, new Locale("en"), validatorName);
    }

    @Test
    public void validateWithSubKeyViolation() {
        final String validatorName = "test-validator-2";
        final ValidatorConfig config = new TestValidatorConfig(validatorName, TestValidator2.class.getName());
        final ValidatorInstanceImpl instance = new ValidatorInstanceImpl(new TestValidator2(), config);

        final String expectedViolationKey = validatorName + "#subKey";
        testValidate(instance, new Locale("en"), expectedViolationKey);
    }

    private void testValidate(final ValidatorInstanceImpl instance, final Locale locale, final String expectedViolationKey) {
        final TestValidationContext context = new TestValidationContext("name", "type", locale);

        final Optional<Violation> violation = instance.validate(context, "value");

        assertTrue(violation.isPresent());

        final TranslatedViolation translation = (TranslatedViolation) violation.get();
        assertThat(translation.getKey(), equalTo(expectedViolationKey));
        assertThat(translation.getLocale(), equalTo(locale));
    }

    private static class TestValidator1 implements Validator {

        @Override
        public Optional<Violation> validate(final ValidationContext context, final String value, final ViolationFactory violationFactory) throws ValidationContextException {
            return Optional.of(violationFactory.createViolation());
        }
    }

    private static class TestValidator2 implements Validator {

        @Override
        public Optional<Violation> validate(final ValidationContext context, final String value, final ViolationFactory violationFactory) throws ValidationContextException {
            return Optional.of(violationFactory.createViolation("subKey"));
        }
    }

}