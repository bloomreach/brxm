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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.junit.Test;
import org.onehippo.cms7.services.validation.ValidatorConfig;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.util.JcrConstants;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ValidatorConfigTest {

    @Test(expected = IllegalStateException.class)
    public void testThrowsIllegalStateExceptionIfClassNameIsNotConfigured() throws Exception {
        final Node configNode = createConfigNode("validator-name", null);
        new ValidatorConfigImpl(configNode);
    }

    @Test
    public void testRetrievesValidatorNameAndClassName() throws Exception {
        final Node configNode = createConfigNode("validator-name", "validator-class-name");
        final ValidatorConfigImpl validatorConfig = new ValidatorConfigImpl(configNode);

        assertEquals("validator-name", validatorConfig.getName());
        assertEquals("validator-class-name", validatorConfig.getClassName());
    }

    @Test
    public void testAllowsCustomProperties() throws Exception {
        final MockNode configNode = createConfigNode("validator-name", "validator-class-name");
        configNode.setProperty("custom-property", "property-value");
        final ValidatorConfigImpl validatorConfig = new ValidatorConfigImpl(configNode);

        assertTrue(validatorConfig.hasProperty("custom-property"));
        assertEquals("property-value", validatorConfig.getProperty("custom-property"));
    }

    @Test
    public void testIgnoresNamespacedProperties() throws Exception {
        final MockNode configNode = createConfigNode("validator-name", "validator-class-name");
        configNode.setProperty("namespaced:custom-property", "property-value");
        final ValidatorConfigImpl validatorConfig = new ValidatorConfigImpl(configNode);

        assertFalse(validatorConfig.hasProperty("namespaced:custom-property"));
    }

    @Test
    public void testIgnoresMultiplePropertiesAndLogsWarning() throws Exception {
        final MockNode configNode = createConfigNode("validator-name", "validator-class-name");
        configNode.setProperty("custom-property", new String[] {"property-value"});

        try (final Log4jInterceptor listener = Log4jInterceptor.onWarn().trap(ValidatorConfigImpl.class).build()) {
            final ValidatorConfigImpl validatorConfig = new ValidatorConfigImpl(configNode);
            assertEquals(1L, listener.messages().count());
            assertFalse(validatorConfig.hasProperty("custom-property"));
        }
    }

    @Test
    public void testClearsValuesOnReconfigure() throws Exception {
        final Node configNode = createConfigNode("validator-name", "validator-class-name");
        configNode.setProperty("custom-property", "property-value");
        final ValidatorConfigImpl validatorConfig = new ValidatorConfigImpl(configNode);

        final Node newConfigNode = createConfigNode("new-validator-name", "new-validator-class-name");
        newConfigNode.setProperty("new-custom-property", "new-property-value");
        validatorConfig.reconfigure(newConfigNode);

        assertEquals("new-validator-name", validatorConfig.getName());
        assertEquals("new-validator-class-name", validatorConfig.getClassName());
        assertFalse(validatorConfig.hasProperty("custom-property"));
        assertTrue(validatorConfig.hasProperty("new-custom-property"));
        assertEquals("new-property-value", validatorConfig.getProperty("new-custom-property"));
    }

    private MockNode createConfigNode(final String name, final String className) throws RepositoryException {
        final MockNode node = MockNode.root().addNode(name, JcrConstants.NT_UNSTRUCTURED);
        if (className != null) {
            node.setProperty(ValidatorConfig.CLASS_NAME, className);
        }
        return node;
    }

}
