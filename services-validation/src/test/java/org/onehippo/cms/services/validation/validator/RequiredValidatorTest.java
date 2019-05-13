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

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.services.validation.api.ValidationContext;
import org.onehippo.cms.services.validation.api.ValidationContextException;
import org.onehippo.cms.services.validation.api.Violation;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.contenttype.ContentTypeService;
import org.onehippo.cms7.services.contenttype.EffectiveNodeType;
import org.onehippo.cms7.services.contenttype.EffectiveNodeTypes;
import org.onehippo.repository.mock.MockNode;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.mockStaticPartial;
import static org.powermock.api.easymock.PowerMock.replayAll;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
@PrepareForTest(HippoServiceRegistry.class)
public class RequiredValidatorTest {

    private ContentTypeService contentTypeService;
    private Node configNode;

    @Before
    public void setUp() {
        mockStaticPartial(HippoServiceRegistry.class, "getService");

        configNode = MockNode.root();

        contentTypeService = createMock(ContentTypeService.class);
        expect(HippoServiceRegistry.getService(ContentTypeService.class)).andReturn(contentTypeService);
    }

    @Test(expected = ValidationContextException.class)
    public void errorReadingValidators() throws RepositoryException {
        configNode = createMock(Node.class);
        expect(configNode.getNodes()).andThrow(new RepositoryException());
        replayAll();

        new RequiredValidator(configNode);
    }

    @Test(expected = ValidationContextException.class)
    public void zeroValidators() throws RepositoryException {
        setUpSuperTypes("type", Collections.emptySet());
        replayAll();

        final ValidationContext validationContext = new TestValidationContext("jcrName", "jcrType", "type");
        final RequiredValidator requiredValidator = new RequiredValidator(configNode);

        requiredValidator.validate(validationContext, "value");
    }

    @Test
    public void validatorForType() throws RepositoryException {
        setUpSuperTypes("String", Collections.emptySet());

        final Node validatorConfig = configNode.addNode("String", "hipposys:moduleconfig");
        validatorConfig.setProperty("hipposys:className", RequiredStringValidator.class.getName());

        replayAll();

        final ValidationContext validationContext = new TestValidationContext("myproject:string", "String");
        final RequiredValidator requiredValidator = new RequiredValidator(configNode);

        final Optional<Violation> violation = requiredValidator.validate(validationContext, "");
        assertTrue(violation.isPresent());
    }

    @Test
    public void validatorForSuperType() throws RepositoryException {
        setUpSuperTypes("CalendarDate", Collections.singleton("Date"));

        final Node validatorConfig = configNode.addNode("Date", "hipposys:moduleconfig");
        validatorConfig.setProperty("hipposys:className", RequiredDateValidator.class.getName());

        replayAll();

        final ValidationContext validationContext = new TestValidationContext("myproject:calendarDate", "CalendarDate");
        final RequiredValidator requiredValidator = new RequiredValidator(configNode);

        final Optional<Violation> violation = requiredValidator.validate(validationContext, RequiredDateValidator.EMPTY_DATE);
        assertTrue(violation.isPresent());
    }

    @Test(expected = ValidationContextException.class)
    public void validatorForTypeWithoutRequiredValidator() throws RepositoryException {
        setUpSuperTypes("Boolean", Collections.emptySet());
        replayAll();

        final ValidationContext validationContext = new TestValidationContext("myproject:boolean", "Boolean");
        final RequiredValidator requiredValidator = new RequiredValidator(configNode);

        requiredValidator.validate(validationContext, true);
    }

    @Test(expected = ValidationContextException.class)
    public void validatorForSuperTypeWithoutRequiredValidator() throws RepositoryException {
        setUpSuperTypes("CustomBoolean", Collections.singleton("Boolean"));
        setUpSuperTypes("Boolean", Collections.emptySet());
        replayAll();

        final ValidationContext validationContext = new TestValidationContext("myproject:customBoolean", "CustomBoolean");
        final RequiredValidator requiredValidator = new RequiredValidator(configNode);

        requiredValidator.validate(validationContext, true);
    }

    @Test(expected = ValidationContextException.class)
    public void getEffectiveNodeTypesThrowsException() throws RepositoryException {
        expect(contentTypeService.getEffectiveNodeTypes()).andThrow(new RepositoryException());
        replayAll();

        final ValidationContext validationContext = new TestValidationContext("myproject:calendarDate", "CalendarDate");
        final RequiredValidator requiredValidator = new RequiredValidator(configNode);

        requiredValidator.validate(validationContext, RequiredDateValidator.EMPTY_DATE);
    }

    @Test(expected = ValidationContextException.class)
    public void noEffectiveNodeType() throws RepositoryException {
        final EffectiveNodeTypes effectiveNodeTypes = createMock(EffectiveNodeTypes.class);
        expect(contentTypeService.getEffectiveNodeTypes()).andReturn(effectiveNodeTypes);

        expect(effectiveNodeTypes.getType("Date")).andReturn(null);
        replayAll();

        final ValidationContext validationContext = new TestValidationContext("myproject:date", "Date");
        final RequiredValidator requiredValidator = new RequiredValidator(configNode);

        requiredValidator.validate(validationContext, RequiredDateValidator.EMPTY_DATE);
    }

    private void setUpSuperTypes(final String type, final Set<String> superTypes) throws RepositoryException {
        final EffectiveNodeTypes effectiveNodeTypes = createMock(EffectiveNodeTypes.class);
        expect(contentTypeService.getEffectiveNodeTypes()).andReturn(effectiveNodeTypes);

        final EffectiveNodeType effectiveNodeType = createMock(EffectiveNodeType.class);
        expect(effectiveNodeTypes.getType(type)).andReturn(effectiveNodeType);

        expect(effectiveNodeType.getSuperTypes()).andReturn(new TreeSet<>(superTypes));
    }
}