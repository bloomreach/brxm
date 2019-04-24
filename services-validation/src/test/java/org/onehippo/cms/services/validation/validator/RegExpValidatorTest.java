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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms.services.validation.api.ValidationContext;
import org.onehippo.cms.services.validation.api.ValidationContextException;
import org.onehippo.cms.services.validation.api.Validator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.onehippo.cms.services.validation.validator.ValidatorTestUtils.assertInvalid;
import static org.onehippo.cms.services.validation.validator.ValidatorTestUtils.assertValid;

public class RegExpValidatorTest {

    private ValidationContext context;
    private Map<String, String> parameters;
    private TestViolationFactory violationFactory;

    @Before
    public void setUp() {
        parameters = new HashMap<>();
        parameters.put("regexp.pattern", "[abc]");

        violationFactory = new TestViolationFactory();
    }

    @Test(expected = ValidationContextException.class)
    public void throwsExceptionIfRegexpPatternPropertyIsMissing() {
        new RegExpValidator(Collections.emptyMap());
    }

    @Test
    public void testValidInput() {
        context = new TestValidationContext(null, "String");

        final Validator<String> validator = new RegExpValidator(parameters);
        assertValid(validator.validate(context, "abc", violationFactory));
        assertFalse(violationFactory.isCalled());
    }

    @Test
    public void testInvalidInput() {
        context = new TestValidationContext(null, "String");

        final Validator<String> validator = new RegExpValidator(parameters);
        assertInvalid(validator.validate(context, "xyz", violationFactory));
        assertTrue(violationFactory.isCalled());
    }
}
