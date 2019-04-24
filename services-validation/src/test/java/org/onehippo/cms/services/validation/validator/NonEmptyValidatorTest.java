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
import org.onehippo.cms.services.validation.api.ValidationContextException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.onehippo.cms.services.validation.validator.ValidatorTestUtils.assertInvalid;
import static org.onehippo.cms.services.validation.validator.ValidatorTestUtils.assertValid;

public class NonEmptyValidatorTest {

    private ValidationContext context;
    private NonEmptyValidator validator;
    private TestViolationFactory violationFactory;

    @Before
    public void setUp() {
        validator = new NonEmptyValidator();
        violationFactory = new TestViolationFactory();
    }

    @Test
    public void validInputForHtml() {
        context = new TestValidationContext("Html", "String");

        assertValid(validator.validate(context, "text", violationFactory));
        assertValid(validator.validate(context, "<p>text</p>", violationFactory));
        assertValid(validator.validate(context, "<img src=\"empty.gif\">", violationFactory));
        assertFalse(violationFactory.isCalled());
    }

    @Test
    public void invalidInputForHtml() {
        context = new TestValidationContext("Html", "String");

        assertInvalid(validator.validate(context, null, violationFactory));
        assertInvalid(validator.validate(context, "", violationFactory));
        assertInvalid(validator.validate(context, " ", violationFactory));
        assertInvalid(validator.validate(context, "<html></html>", violationFactory));
        assertTrue(violationFactory.isCalled());
    }

    @Test
    public void validInputForText() {
        context = new TestValidationContext("non-html", "String");

        assertValid(validator.validate(context, "text", violationFactory));
        assertValid(validator.validate(context, "<p>text</p>", violationFactory));
        assertValid(validator.validate(context, "<html></html>", violationFactory));
        assertFalse(violationFactory.isCalled());
    }

    @Test
    public void invalidInputForText() {
        context = new TestValidationContext("non-html", "String");

        assertInvalid(validator.validate(context, null, violationFactory));
        assertInvalid(validator.validate(context, "", violationFactory));
        assertInvalid(validator.validate(context, " ", violationFactory));
        assertInvalid(validator.validate(context, "\n\r", violationFactory));
        assertTrue(violationFactory.isCalled());
    }
}
