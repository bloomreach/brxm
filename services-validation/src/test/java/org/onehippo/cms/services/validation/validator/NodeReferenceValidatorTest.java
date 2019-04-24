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
import org.onehippo.repository.util.JcrConstants;

import static org.junit.Assert.assertTrue;
import static org.onehippo.cms.services.validation.validator.ValidatorTestUtils.assertInvalid;

public class NodeReferenceValidatorTest {

    private NodeReferenceValidator validator;
    private TestViolationFactory violationFactory;

    @Before
    public void setUp() {
        validator = new NodeReferenceValidator();
        violationFactory = new TestViolationFactory();
    }

    @Test
    public void blankStringIsInvalid() {
        assertInvalid(validator.validate(null, null, violationFactory));
        assertInvalid(validator.validate(null, "", violationFactory));
        assertInvalid(validator.validate(null, " ", violationFactory));
        assertTrue(violationFactory.isCalled());
    }

    @Test
    public void jcrRootNodeIdentifierIsInvalid() {
        assertInvalid(validator.validate(null, JcrConstants.ROOT_NODE_ID, violationFactory));
        assertTrue(violationFactory.isCalled());
    }
}
