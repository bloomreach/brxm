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
package org.onehippo.cms7.services.validation;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms7.services.validation.exception.ValidatorConfigurationException;
import org.onehippo.cms7.services.validation.validator.ValidatorFactory;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.util.JcrConstants;
import org.onehippo.testutils.log4j.Log4jInterceptor;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.mockStaticPartial;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;


@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
@PrepareForTest(ValidatorFactory.class)
public class ValidatorServiceConfigTest {

    private MockNode configNode;

    @Before
    public void setUp() throws Exception {
        final MockNode root = MockNode.root();
        configNode = root.addNode("config", JcrConstants.NT_UNSTRUCTURED);
        mockStaticPartial(ValidatorFactory.class, "create");
    }

    @Test
    public void testLogsErrorWhenRepositoryExceptionIsThrown() throws Exception {
        final Node configNode = createMock(Node.class);
        expect(configNode.getNodes()).andThrow(new RepositoryException());
        replayAll();

        try (final Log4jInterceptor listener = Log4jInterceptor.onError().trap(ValidatorServiceConfig.class).build()) {
            try {
                new ValidatorServiceConfig(configNode);
            } finally {
                assertEquals(1L, listener.messages().count());
                verifyAll();
            }
        }
    }

    @Test
    public void testReturnsNullIfNotFound() throws Exception {
        ValidatorServiceConfig config = new ValidatorServiceConfig(configNode);
        assertNull(config.getValidator("validator-1"));

        addValidatorConfig("validator-1");
        config = new ValidatorServiceConfig(configNode);
        assertNull(config.getValidator("validator-2"));

        addValidatorConfig("validator-2");
        config.reconfigure(configNode);
        assertNull(config.getValidator("validator-3"));
    }

    @Test(expected = ValidatorConfigurationException.class)
    public void testThrowsExceptionWhenValidatorCreationFailed() throws Exception {
        expect(ValidatorFactory.create(isA(ValidatorConfig.class))).andReturn(null);
        replayAll();

        addValidatorConfig("validator-1");
        final ValidatorServiceConfig config = new ValidatorServiceConfig(configNode);
        config.getValidator("validator-1");
    }

    @Test
    public void testReturnsNewValidatorFromConfig() throws Exception {
        final Validator mockValidator = createMock(Validator.class);
        expect(ValidatorFactory.create(isA(ValidatorConfig.class))).andReturn(mockValidator);
        replayAll();

        addValidatorConfig("validator-1");
        final ValidatorServiceConfig config = new ValidatorServiceConfig(configNode);
        assertEquals(mockValidator, config.getValidator("validator-1"));
        verifyAll();
    }

    @Test
    public void testReturnsSameHtmlProcessorInstance() throws Exception {
        final Validator mockValidator = createMock(Validator.class);
        expect(ValidatorFactory.create(isA(ValidatorConfig.class))).andReturn(mockValidator);
        replayAll();

        addValidatorConfig("validator-1");
        final ValidatorServiceConfig config = new ValidatorServiceConfig(configNode);
        assertEquals(config.getValidator("validator-1"), config.getValidator("validator-1"));
    }

    @Test
    public void testClearsOnReconfigure() throws Exception {
        final Validator mockValidator = createMock(Validator.class);
        expect(ValidatorFactory.create(isA(ValidatorConfig.class))).andReturn(mockValidator);
        final Validator mockValidator2 = createMock(Validator.class);
        expect(ValidatorFactory.create(isA(ValidatorConfig.class))).andReturn(mockValidator2);
        replayAll();

        addValidatorConfig("validator-1");
        final ValidatorServiceConfig config = new ValidatorServiceConfig(configNode);

        final Validator validator1 = config.getValidator("validator-1");
        config.reconfigure(configNode);
        final Validator validator2 = config.getValidator("validator-1");

        assertNotNull(validator1);
        assertNotNull(validator2);
        assertNotEquals(validator1, validator2);
    }


    private void addValidatorConfig(final String id) throws RepositoryException {
        final MockNode validatorConfig = configNode.addNode(id, JcrConstants.NT_UNSTRUCTURED);
        validatorConfig.setProperty(ValidatorConfig.CLASS_NAME, "validator-classname");
    }
}
