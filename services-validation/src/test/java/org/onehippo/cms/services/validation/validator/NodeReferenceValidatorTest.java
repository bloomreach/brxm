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
import org.onehippo.cms.services.validation.validator.NodeReferenceValidator;
import org.onehippo.cms.services.validation.api.ValidatorConfig;
import org.onehippo.cms.services.validation.api.ValidatorContext;
import org.onehippo.cms.services.validation.api.InvalidValidatorException;
import org.onehippo.repository.util.JcrConstants;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertFalse;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

public class NodeReferenceValidatorTest {

    private ValidatorContext context;
    private NodeReferenceValidator validator;

    @Before
    public void setUp() {
        final ValidatorConfig config = createMock(ValidatorConfig.class);
        context = createMock(ValidatorContext.class);
        validator = new NodeReferenceValidator(config);
    }

    @Test(expected = InvalidValidatorException.class)
    public void throwsExceptionIfFieldIsNotOfTypeString() throws Exception {
        expect(context.getType()).andReturn("not-a-string");
        replayAll();

        validator.init(context);
    }

    @Test
    public void initializesIfFieldIsOfTypeString() throws Exception {
        expect(context.getType()).andReturn("String");
        replayAll();

        validator.init(context);
        verifyAll();
    }

    @Test
    public void blankStringIsInvalid() {
        assertFalse(validator.isValid(context, null));
        assertFalse(validator.isValid(context, ""));
        assertFalse(validator.isValid(context, " "));
    }

    @Test
    public void jcrRootNodeIdentifierIsInvalid() {
        assertFalse(validator.isValid(context, JcrConstants.ROOT_NODE_ID));
    }

}
