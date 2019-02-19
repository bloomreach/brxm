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
import org.onehippo.cms7.services.validation.exception.InvalidValidatorException;
import org.onehippo.cms7.services.validation.mock.MockFieldContext;
import org.onehippo.repository.mock.MockNode;

import static org.junit.Assert.assertFalse;

public class NodeReferenceValidatorTest {

    private NodeReferenceValidator validator;

    @Before
    public void setUp() throws Exception {
        final AbstractValidatorConfig config = new AbstractValidatorConfig(new MockNode("config"));
        validator = new NodeReferenceValidator(config);
    }

    @Test(expected = InvalidValidatorException.class)
    public void testThrowsIfFieldIsNotStringType() throws Exception {
        final MockFieldContext context = new MockFieldContext("not-a-string", "not-a-string");
        validator.init(context);
    }

    @Test
    public void testCanBeInitializedIfFieldIsStringType() throws Exception {
        final MockFieldContext context = new MockFieldContext("String", "String");
        validator.init(context);
    }


    @Test
    public void testIsInvalidForBlankString() throws Exception {
        assertFalse(validator.isValid(new MockFieldContext(), null));
        assertFalse(validator.isValid(new MockFieldContext(), ""));
        assertFalse(validator.isValid(new MockFieldContext(), " "));
    }

    @Test
    public void testIsInvalidForRootIdentifier() throws Exception {
        assertFalse(validator.isValid(new MockFieldContext(), "cafebabe-cafe-babe-cafe-babecafebabe"));
    }

    @Test
    public void testIsValidForRootIdentifier() throws Exception {
        assertFalse(validator.isValid(new MockFieldContext(), "cafebabe-cafe-babe-cafe-babecafebabe"));
    }

}
