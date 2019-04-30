/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms.services.validation;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;
import org.onehippo.cms.services.validation.api.ValidationContext;
import org.onehippo.cms.services.validation.api.Validator;
import org.onehippo.cms.services.validation.api.internal.ValidatorConfig;
import org.onehippo.cms.services.validation.api.Violation;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ValidatorFactoryTest {

    @Test
    public void createValidatorWithoutProperties() {
        final ValidatorConfig config = new TestValidatorConfig("mockValidator", MockValidator.class.getName());

        final Validator validator = ValidatorFactory.createValidator(config);

        assertTrue(validator instanceof MockValidator);
    }

    @Test
    public void createValidatorWithProperties() {
        final Map<String, String> properties = Collections.singletonMap("key", "value");
        final TestValidatorConfig config = new TestValidatorConfig("parameterized",
                ParameterizedMockValidator.class.getName(), properties);

        final Validator validator = ValidatorFactory.createValidator(config);

        assertThat(((ParameterizedMockValidator)validator).properties, equalTo(properties));
    }

    @Test
    public void classNotFound() {
        final ValidatorConfig config = new TestValidatorConfig("none", "noSuchClass");
        assertErrorAndNoValidator(config, "failed to locate class");
    }

    @Test
    public void incompatibleConstructor() {
        final ValidatorConfig config = new TestValidatorConfig("incompatible", IncompatibleMockValidator.class.getName());
        assertErrorAndNoValidator(config, "does not have a public constructor");
    }

    @Test
    public void abstractClass() {
        final ValidatorConfig config = new TestValidatorConfig("abstract", AbstractValidator.class.getName());
        assertErrorAndNoValidator(config, "failed to instantiate class");
    }

    private void assertErrorAndNoValidator(final ValidatorConfig config, final String errorSnippet) {
        try (final Log4jInterceptor listener = Log4jInterceptor.onError().trap(ValidatorFactory.class).build()) {
            final Validator validator = ValidatorFactory.createValidator(config);
            assertThat(listener.messages().count(), equalTo(1L));
            assertThat(listener.messages().findFirst().get(), containsString(errorSnippet));
            assertNull(validator);
        }
    }

    // public so it exposes a default public constructor
    public static class MockValidator implements Validator<String> {
        @Override
        public Optional<Violation> validate(final ValidationContext context, final String value) {
            return Optional.empty();
        }
    }

    public static class ParameterizedMockValidator extends MockValidator {

        final Map<String, String> properties;

        public ParameterizedMockValidator(final Map<String, String> properties) {
            this.properties = properties;
        }
    }

    private static class IncompatibleMockValidator extends MockValidator {
        @SuppressWarnings("unused")
        public IncompatibleMockValidator(final String incompatibleArgument) {
        }
    }

    private static abstract class AbstractValidator implements Validator {
        public AbstractValidator() {
        }
    }
}
