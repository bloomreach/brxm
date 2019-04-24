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
package org.onehippo.cms.services.validation;

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.junit.Test;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.util.JcrConstants;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class JcrValidatorConfigTest {

    @Test(expected = IllegalStateException.class)
    public void throwsIllegalStateExceptionIfClassNameIsNotConfigured() throws Exception {
        final Node configNode = createConfigNode("validator-name", null);
        new JcrValidatorConfig(configNode);
    }

    @Test
    public void loadsValidatorNameAndClassName() throws Exception {
        final Node configNode = createConfigNode("validator-name", "validator-class-name");
        final JcrValidatorConfig validatorConfig = new JcrValidatorConfig(configNode);

        assertEquals("validator-class-name", validatorConfig.getClassName());
    }

    @Test
    public void loadsCustomProperties() throws Exception {
        final MockNode configNode = createConfigNode("validator-name", "validator-class-name");
        configNode.setProperty("custom-property", "property-value");
        final JcrValidatorConfig validatorConfig = new JcrValidatorConfig(configNode);

        final Map<String, String> properties = validatorConfig.getProperties();
        assertThat(properties.size(), equalTo(1));
        assertThat(properties.get("custom-property"), equalTo("property-value"));
    }

    @Test
    public void ignoresNamespacedCustomProperties() throws Exception {
        final MockNode configNode = createConfigNode("validator-name", "validator-class-name");
        configNode.setProperty("namespaced:custom-property", "property-value");
        final JcrValidatorConfig validatorConfig = new JcrValidatorConfig(configNode);

        assertFalse(validatorConfig.getProperties().containsKey("namespaced:custom-property"));
    }

    @Test
    public void ignoresMultiplePropertiesAndLogsWarning() throws Exception {
        final MockNode configNode = createConfigNode("validator-name", "validator-class-name");
        configNode.setProperty("custom-property", new String[] {"property-value"});

        try (final Log4jInterceptor listener = Log4jInterceptor.onWarn().trap(JcrValidatorConfig.class).build()) {
            final JcrValidatorConfig validatorConfig = new JcrValidatorConfig(configNode);
            assertEquals(1L, listener.messages().count());
            assertFalse(validatorConfig.getProperties().containsKey("custom-property"));
        }
    }

    @Test
    public void clearsValuesOnReconfigure() throws Exception {
        final Node configNode = createConfigNode("validator-name", "validator-class-name");
        configNode.setProperty("custom-property", "property-value");
        final JcrValidatorConfig validatorConfig = new JcrValidatorConfig(configNode);

        final Node newConfigNode = createConfigNode("new-validator-name", "new-validator-class-name");
        newConfigNode.setProperty("new-custom-property", "new-property-value");
        validatorConfig.reconfigure(newConfigNode);

        assertEquals("new-validator-class-name", validatorConfig.getClassName());

        final Map<String, String> properties = validatorConfig.getProperties();
        assertFalse(properties.containsKey("custom-property"));
        assertTrue(properties.containsKey("new-custom-property"));
        assertEquals("new-property-value", properties.get("new-custom-property"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void customPropertiesCannotBeModified() throws Exception {
        final MockNode configNode = createConfigNode("validator-name", "validator-class-name");
        final JcrValidatorConfig validatorConfig = new JcrValidatorConfig(configNode);

        final Map<String, String> properties = validatorConfig.getProperties();
        properties.put("key", "value");
    }

    private MockNode createConfigNode(final String name, final String className) throws RepositoryException {
        final MockNode node = MockNode.root().addNode(name, JcrConstants.NT_UNSTRUCTURED);
        if (className != null) {
            node.setProperty(JcrValidatorConfig.CLASS_NAME, className);
        }
        return node;
    }

}
