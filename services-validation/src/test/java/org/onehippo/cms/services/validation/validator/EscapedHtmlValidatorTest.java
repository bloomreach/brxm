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
import org.onehippo.cms.services.validation.validator.EscapedHtmlValidator;
import org.onehippo.cms.services.validation.api.ValidatorConfig;
import org.onehippo.cms.services.validation.api.ValidatorContext;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.createMock;

public class EscapedHtmlValidatorTest {

    private ValidatorContext context;
    private EscapedHtmlValidator validator;

    @Before
    public void setUp() {
        final ValidatorConfig config = createMock(ValidatorConfig.class);
        context = createMock(ValidatorContext.class);
        validator = new EscapedHtmlValidator(config);
    }

    @Test
    public void textContainingCommonCharactersIsValid() {
        assertTrue(validator.isValid(context, "abcdefghijklmnopqrstuvwxyz"));
        assertTrue(validator.isValid(context, "ABCDEFGHIJKLMNOPQRSTUVWXYZ"));
        assertTrue(validator.isValid(context, "12345567890"));
        assertTrue(validator.isValid(context, "\f\n\t\n"));
        assertTrue(validator.isValid(context, "!@#$%^*()-_+=±§{}[]:;|\\,.?/~`"));
    }

    @Test
    public void textContainingLesserThanIsInvalid() {
        assertFalse(validator.isValid(context, "<"));
    }

    @Test
    public void textContainingGreaterThanIsInvalid() {
        assertFalse(validator.isValid(context, ">"));
    }

    @Test
    public void textContainingAmpersandIsInvalid() {
        assertFalse(validator.isValid(context, "&"));
    }

    @Test
    public void textContainingSingleQuoteIsInvalid() {
        assertFalse(validator.isValid(context, "'"));
    }

    @Test
    public void textContainingDoubleQuoteIsInvalid() {
        assertFalse(validator.isValid(context, "\""));
    }

}
