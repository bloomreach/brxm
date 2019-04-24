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

import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms.services.validation.api.ValidationContext;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.onehippo.cms.services.validation.validator.ValidatorTestUtils.assertInvalid;
import static org.onehippo.cms.services.validation.validator.ValidatorTestUtils.assertValid;

public class EscapedHtmlValidatorTest {

    private ValidationContext context;
    private EscapedHtmlValidator validator;
    private TestViolationFactory violationFactory;

    @Before
    public void setUp() {
        context = new TestValidationContext();
        validator = new EscapedHtmlValidator();
        violationFactory = new TestViolationFactory();
    }

    @Test
    public void textContainingCommonCharactersIsValid() {
        assertValid(validator.validate(context, "abcdefghijklmnopqrstuvwxyz", violationFactory));
        assertValid(validator.validate(context, "ABCDEFGHIJKLMNOPQRSTUVWXYZ", violationFactory));
        assertValid(validator.validate(context, "12345567890", violationFactory));
        assertValid(validator.validate(context, "\f\n\t\n", violationFactory));
        assertValid(validator.validate(context, "!@#$%^*()-_+=±§{}[]:;|\\,.?/~`", violationFactory));
        assertFalse(violationFactory.isCalled());
    }

    @Test
    public void textContainingLesserThanIsInvalid() {
        assertInvalid(validator.validate(context, "<", violationFactory));
        assertTrue(violationFactory.isCalled());
    }

    @Test
    public void textContainingGreaterThanIsInvalid() {
        assertInvalid(validator.validate(context, ">", violationFactory));
        assertTrue(violationFactory.isCalled());
    }

    @Test
    public void textContainingAmpersandIsInvalid() {
        assertInvalid(validator.validate(context, "&", violationFactory));
        assertTrue(violationFactory.isCalled());
    }

    @Test
    public void textContainingSingleQuoteIsInvalid() {
        assertInvalid(validator.validate(context, "'", violationFactory));
        assertTrue(violationFactory.isCalled());
    }

    @Test
    public void textContainingDoubleQuoteIsInvalid() {
        assertInvalid(validator.validate(context, "\"", violationFactory));
        assertTrue(violationFactory.isCalled());
    }

}
