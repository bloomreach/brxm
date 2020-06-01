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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms.services.validation.api.ValidationContextException;
import org.onehippo.cms.services.validation.api.Validator;
import org.onehippo.repository.mock.MockNode;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.onehippo.cms.services.validation.validator.ValidatorTestUtils.assertInvalid;
import static org.onehippo.cms.services.validation.validator.ValidatorTestUtils.assertValid;

public class RegExpValidatorTest {

    private TestValidationContext context;
    private Node config;

    @Before
    public void setUp() throws RepositoryException {
        config = MockNode.root();
        config.setProperty("regexp.pattern", "[abc]");
    }

    @Test(expected = ValidationContextException.class)
    public void throwsExceptionIfRegexpPatternPropertyIsMissing() {
        new RegExpValidator(MockNode.root());
    }

    @Test
    public void testValidInput() {
        context = new TestValidationContext(null, "String");

        final Validator<String> validator = new RegExpValidator(config);
        assertValid(validator.validate(context, "abc"));
        assertFalse(context.isViolationCreated());
    }

    @Test
    public void testInvalidInput() {
        context = new TestValidationContext(null, "String");

        final Validator<String> validator = new RegExpValidator(config);
        assertInvalid(validator.validate(context, "xyz"));
        assertTrue(context.isViolationCreated());
    }
}
