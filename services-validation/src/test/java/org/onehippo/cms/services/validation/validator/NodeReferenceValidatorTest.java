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
import org.onehippo.repository.util.JcrConstants;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.onehippo.cms.services.validation.validator.ValidatorTestUtils.assertInvalid;

public class NodeReferenceValidatorTest {

    private ValidationContext context;
    private NodeReferenceValidator validator;
    private TestViolationFactory violationFactory;

    @Before
    public void setUp() {
        validator = new NodeReferenceValidator();
        violationFactory = new TestViolationFactory();
    }

    @Test(expected = ValidationContextException.class)
    public void throwsExceptionIfFieldIsNotOfTypeString() {
        context = new TestValidationContext(null, "not-a-string");
        validator.validate(context, null, violationFactory);
    }

    @Test
    public void blankStringIsInvalid() {
        context = new TestValidationContext(null, "String");

        assertInvalid(validator.validate(context, null, violationFactory));
        assertInvalid(validator.validate(context, "", violationFactory));
        assertInvalid(validator.validate(context, " ", violationFactory));
        assertTrue(violationFactory.isCalled());
    }

    @Test
    public void jcrRootNodeIdentifierIsInvalid() {
        context = new TestValidationContext(null, "String");

        assertInvalid(validator.validate(context, JcrConstants.ROOT_NODE_ID, violationFactory));
        assertTrue(violationFactory.isCalled());
    }
}
