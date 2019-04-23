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
import org.onehippo.cms.services.validation.api.Violation;
import org.onehippo.repository.util.JcrConstants;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.onehippo.cms.services.validation.validator.ValidatorTestUtils.assertInvalid;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

public class NodeReferenceValidatorTest {

    private ValidationContext context;
    private NodeReferenceValidator validator;

    @Before
    public void setUp() {
        validator = new NodeReferenceValidator();
    }

    @Test(expected = ValidationContextException.class)
    public void throwsExceptionIfFieldIsNotOfTypeString() {
        context = new TestValidationContext("not-a-string", null);
        validator.validate(context, null);
    }

    @Test
    public void blankStringIsInvalid() {
        context = new TestValidationContext("String", null);

        assertInvalid(validator.validate(context, null));
        assertInvalid(validator.validate(context, ""));
        assertInvalid(validator.validate(context, " "));
    }

    @Test
    public void jcrRootNodeIdentifierIsInvalid() {
        context = new TestValidationContext("String", null);

        assertInvalid(validator.validate(context, JcrConstants.ROOT_NODE_ID));
    }
}
