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
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.onehippo.cms.services.validation.validator.ValidatorTestUtils.assertInvalid;
import static org.onehippo.cms.services.validation.validator.ValidatorTestUtils.assertValid;

public class NonEmptyHtmlValidatorTest {

    private TestValidationContext context;
    private NonEmptyHtmlValidator validator;

    @Before
    public void setUp() {
        context = new TestValidationContext(null, "String");
        validator = new NonEmptyHtmlValidator();
    }

    @Test
    public void warnsIfValidatorIsUsedWithHtmlField() {
        context = new TestValidationContext("Html", "String");

        try (final Log4jInterceptor listener = Log4jInterceptor.onWarn().trap(NonEmptyHtmlValidator.class).build()) {
            try {
                validator.validate(context, null);
            } finally {
                assertEquals(1L, listener.messages().count());
            }
        }
    }

    @Test
    public void textIsValid() {
        assertValid(validator.validate(context, "text"));
        assertFalse(context.isViolationCreated());
    }

    @Test
    public void paragraphWithTextIsValid() {
        assertValid(validator.validate(context, "<p>text</p>"));
        assertFalse(context.isViolationCreated());
    }

    @Test
    public void imgIsValid() {
        assertValid(validator.validate(context, "<img src=\"empty.gif\">"));
        assertFalse(context.isViolationCreated());
    }

    @Test
    public void nullIsInvalid() {
        assertInvalid(validator.validate(context, null));
        assertTrue(context.isViolationCreated());
    }

    @Test
    public void blankStringIsInvalid() {
        assertInvalid(validator.validate(context, ""));
        assertInvalid(validator.validate(context, " "));
        assertTrue(context.isViolationCreated());
    }

    @Test
    public void emptyHtmlIsInvalid() {
        assertInvalid(validator.validate(context, "<html></html>"));
        assertTrue(context.isViolationCreated());
    }

    @Test
    public void emptyParagraphInvalid() {
        assertInvalid(validator.validate(context, "<p></p>"));
        assertTrue(context.isViolationCreated());
    }

}
