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
package org.onehippo.cms7.services.validation.validator;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.validation.ValidatorConfig;
import org.onehippo.cms7.services.validation.ValidatorContext;
import org.onehippo.cms7.services.validation.mock.MockFieldContext;
import org.onehippo.cms7.services.validation.mock.MockValidatorConfig;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EscapedHtmlValidatorTest {

    private ValidatorContext context;
    private EscapedHtmlValidator validator;

    @Before
    public void setUp() throws Exception {
        context = new MockFieldContext();
        final ValidatorConfig config = new MockValidatorConfig("escaped");
        validator = new EscapedHtmlValidator(config);
    }

    @Test
    public void testIsValid() throws Exception {
        final ValidatorContext context = new MockFieldContext();
        assertTrue(validator.isValid(context, "Test input!"));
    }

    @Test
    public void testIsInvalid() throws Exception {
        assertFalse(validator.isValid(context, "<"));
        assertFalse(validator.isValid(context, "\""));
        assertFalse(validator.isValid(context, "'"));
        assertFalse(validator.isValid(context, ">"));
        assertFalse(validator.isValid(context, "&"));
    }

}
