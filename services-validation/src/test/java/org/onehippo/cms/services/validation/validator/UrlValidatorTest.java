/*
 * Copyright 2020 Bloomreach
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms.services.validation.validator;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.onehippo.cms.services.validation.validator.ValidatorTestUtils.assertInvalid;
import static org.onehippo.cms.services.validation.validator.ValidatorTestUtils.assertValid;

public class UrlValidatorTest {

    private TestValidationContext context;
    private UrlValidator validator;

    @Before
    public void setUp() {
        validator = new UrlValidator();
    }

    @Test
    public void validInput() {
        context = new TestValidationContext("myproject:text", "String", "Text");

        assertValid(validator.validate(context, "http://www.bloomreach.com"));
        assertValid(validator.validate(context, "https://www.bloomreach.com"));
        assertFalse(context.isViolationCreated());
    }

    @Test
    public void invalidInput() {
        context = new TestValidationContext("myproject:text", "String", "Text");

        assertInvalid(validator.validate(context, null));
        assertInvalid(validator.validate(context, ""));
        assertInvalid(validator.validate(context, " "));
        assertInvalid(validator.validate(context, "\n\r"));
        assertInvalid(validator.validate(context, "text"));
        assertInvalid(validator.validate(context, "<p>text</p>"));
        assertInvalid(validator.validate(context, "javascript:alert(1)"));
        assertInvalid(validator.validate(context, "bloomreach.com"));

        assertTrue(context.isViolationCreated());
    }
}
