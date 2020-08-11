/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms.services.validation.validator;

import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms.services.validation.api.ValidationContext;
import org.onehippo.cms.services.validation.api.ValidationContextException;
import org.onehippo.cms.services.validation.api.Violation;
import org.onehippo.repository.mock.MockNode;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;

public class RequiredValidatorTest {

    private Node configNode;

    @Before
    public void setUp() {
        configNode = MockNode.root();
    }

    @Test(expected = ValidationContextException.class)
    public void errorReadingValidators() throws RepositoryException {
        configNode = createMock(Node.class);
        expect(configNode.getNodes()).andThrow(new RepositoryException());
        replayAll();

        new RequiredValidator(configNode);
    }

    @Test
    public void zeroValidators() {
        final ValidationContext validationContext = new TestValidationContext("jcrName", "jcrType", "type");
        final RequiredValidator requiredValidator = new RequiredValidator(configNode);

        final Optional<Violation> violation = requiredValidator.validate(validationContext, "value");
        assertFalse(violation.isPresent());
    }

    @Test
    public void validatorForType() throws RepositoryException {
        final Node validatorConfig = configNode.addNode("String", "hipposys:moduleconfig");
        validatorConfig.setProperty("hipposys:className", RequiredStringValidator.class.getName());

        final ValidationContext validationContext = new TestValidationContext("myproject:string", "String");
        final RequiredValidator requiredValidator = new RequiredValidator(configNode);

        final Optional<Violation> violation = requiredValidator.validate(validationContext, "");
        assertTrue(violation.isPresent());
    }

    @Test
    public void validatorForJcrType() throws RepositoryException {
        final Node validatorConfig = configNode.addNode("String", "hipposys:moduleconfig");
        validatorConfig.setProperty("hipposys:className", RequiredStringValidator.class.getName());

        final ValidationContext validationContext = new TestValidationContext("myproject:string", "String", "Text");
        final RequiredValidator requiredValidator = new RequiredValidator(configNode);

        final Optional<Violation> violation = requiredValidator.validate(validationContext, "");
        assertTrue(violation.isPresent());
    }

    @Test
    public void validatorForTypeWithoutRequiredValidator() {
        final ValidationContext validationContext = new TestValidationContext("myproject:boolean", "Boolean");
        final RequiredValidator requiredValidator = new RequiredValidator(configNode);

        final Optional<Violation> violation = requiredValidator.validate(validationContext, true);
        assertFalse(violation.isPresent());
    }
}