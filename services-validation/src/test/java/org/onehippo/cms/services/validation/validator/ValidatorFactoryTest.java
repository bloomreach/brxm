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
package org.onehippo.cms.services.validation.validator;

import java.util.Optional;

import org.junit.Test;
import org.onehippo.cms.services.validation.api.Validator;
import org.onehippo.cms.services.validation.api.ValidatorConfig;
import org.onehippo.cms.services.validation.api.ValidationContext;
import org.onehippo.cms.services.validation.api.Violation;
import org.onehippo.cms.services.validation.api.ValidationContextException;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

public class ValidatorFactoryTest {

    @Test
    public void instantiatesNewValidator() {
        final ValidatorConfig config = createMock(ValidatorConfig.class);
        expect(config.getClassName()).andReturn(MockValidator.class.getName());
        replayAll();

        final Validator validator = ValidatorFactory.create(config);
        assertNotNull(validator);
        assertTrue(validator instanceof MockValidator);
        verifyAll();
    }

    @Test
    public void returnsNullAndLogsErrorIfClassNotFound() {
        final ValidatorConfig config = createMock(ValidatorConfig.class);
        expect(config.getClassName()).andReturn("non-existing");
        replayAll();

        try (final Log4jInterceptor listener = Log4jInterceptor.onError().trap(ValidatorFactory.class).build()) {
            final Validator validator = ValidatorFactory.create(config);
            assertEquals(1L, listener.messages().count());
            assertNull(validator);
            verifyAll();
        }
    }

    @Test
    public void returnsNullAndLogsErrorIfConstructorIsMissing() {
        final ValidatorConfig config = createMock(ValidatorConfig.class);
        expect(config.getClassName()).andReturn(BaseMockValidator.class.getName());
        replayAll();

        try (final Log4jInterceptor listener = Log4jInterceptor.onError().trap(ValidatorFactory.class).build()) {
            final Validator validator = ValidatorFactory.create(config);
            assertEquals(1L, listener.messages().count());
            assertNull(validator);
            verifyAll();
        }
    }

    private static class BaseMockValidator implements Validator {
        @Override
        public Optional<Violation> validate(final ValidationContext context, final String value) {
            return Optional.empty();
        }
    }

    private static class MockValidator extends BaseMockValidator {
    }

}
