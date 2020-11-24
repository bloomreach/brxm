/*
 *  Copyright 2019-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms.services.validation;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.services.validation.api.internal.ValidatorInstance;
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
@PowerMockIgnore({"org.apache.logging.log4j.*", "javax.management.*", "com.sun.org.apache.xerces.internal.*", "javax.xml.parsers.*", "org.w3c.dom.*", "org.xml.sax.*"})
@PrepareForTest(ValidatorInstanceFactory.class)
public class ValidationServiceConfigTest {

    private MockNode configNode;

    @Before
    public void setUp() throws Exception {
        mockStaticPartial(ValidatorInstanceFactory.class, "createValidatorInstance");
        configNode = MockNode.root().addNode("config", JcrConstants.NT_UNSTRUCTURED);
    }

    @Test
    public void logsErrorWhenRepositoryExceptionIsThrown() throws Exception {
        final Node configNode = createMock(Node.class);
        expect(configNode.getNodes()).andThrow(new RepositoryException());
        replayAll();

        try (final Log4jInterceptor listener = Log4jInterceptor.onError().trap(ValidationServiceConfig.class).build()) {
            new ValidationServiceConfig(configNode);
            assertEquals(1L, listener.messages().count());
            verifyAll();
        }
    }

    @Test
    public void returnsNullWhenNoValidatorsAreConfigured() throws Exception {
        final ValidationServiceConfig config = createConfig();
        assertNull(config.getValidatorInstance("non-existing-validator"));
    }

    @Test
    public void returnsNullWhenValidatorIsNotFound() throws Exception {
        final ValidationServiceConfig config = createConfig("existing-validator");
        assertNull(config.getValidatorInstance("non-existing-validator"));
    }

    @Test
    public void returnsNewValidatorFromConfig() throws Exception {
        final ValidatorInstance mockInstance = createMock(ValidatorInstance.class);
        expect(ValidatorInstanceFactory.createValidatorInstance(isA(JcrValidatorConfig.class)))
                .andReturn(mockInstance);
        replayAll();

        final ValidationServiceConfig config = createConfig("mock-validator");
        assertEquals(mockInstance, config.getValidatorInstance("mock-validator"));
        verifyAll();
    }

    @Test
    public void returnsSameValidatorInstance() throws Exception {
        final ValidatorInstance mockInstance = createMock(ValidatorInstance.class);
        expect(ValidatorInstanceFactory.createValidatorInstance(isA(JcrValidatorConfig.class)))
                .andReturn(mockInstance);
        replayAll();

        final ValidationServiceConfig config = createConfig("mock-validator");
        assertEquals(config.getValidatorInstance("mock-validator"), config.getValidatorInstance("mock-validator"));
    }

    @Test
    public void clearsValidatorInstancesOnReconfigure() throws Exception {
        final ValidatorInstance mockInstance = createMock(ValidatorInstance.class);
        expect(ValidatorInstanceFactory.createValidatorInstance(isA(JcrValidatorConfig.class)))
                .andReturn(mockInstance);
        final ValidatorInstance mockInstance2 = createMock(ValidatorInstance.class);
        expect(ValidatorInstanceFactory.createValidatorInstance(isA(JcrValidatorConfig.class)))
                .andReturn(mockInstance2);
        replayAll();

        final ValidationServiceConfig config = createConfig("mock-validator");
        final ValidatorInstance validator1 = config.getValidatorInstance("mock-validator");
        assertNotNull(validator1);

        // Reconfigure should clear the existing validators and use the ValidatorInstanceFactory to create new instances
        config.reconfigure(configNode);
        final ValidatorInstance validator2 = config.getValidatorInstance("mock-validator");

        assertNotNull(validator2);
        assertNotEquals(validator1, validator2);

        verifyAll();
    }

    private ValidationServiceConfig createConfig(final String... validators) throws RepositoryException {
        for (final String validator : validators) {
            final MockNode validatorConfigNode = configNode.addNode(validator, JcrConstants.NT_UNSTRUCTURED);
            validatorConfigNode.setProperty(JcrValidatorConfig.CLASS_NAME, validator + "-validator-classname");
        }
        return new ValidationServiceConfig(configNode);
    }
}
