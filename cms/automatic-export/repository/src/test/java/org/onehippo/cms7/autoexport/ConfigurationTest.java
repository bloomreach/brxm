/**
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.autoexport;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.jcr.PathNotFoundException;

import org.apache.jackrabbit.spi.Event;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.mock.observation.MockEvent;

/**
 * Test for {@link Configuration}
 */
public class ConfigurationTest {

    private MockNode rootNode;
    private MockNode autoExportNode;
    private Configuration configuration;
    private MockEvent enabledPropChangeEvent;

    @Before
    public void setUp() throws Exception {
        rootNode = MockNode.root();
        autoExportNode = rootNode.addMockNode("hippo:configuration", "nt:unstructured")
                .addMockNode("hippo:modules", "nt:unstructured").addMockNode("autoexport", "nt:unstructured");
        autoExportNode.addMockNode("hippo:moduleconfig", "nt:unstructured");
        configuration = new Configuration(rootNode.getSession());
        enabledPropChangeEvent = new MockEvent(rootNode.getSession(), Event.PROPERTY_CHANGED,
                Constants.CONFIG_NODE_PATH + "/" + Constants.CONFIG_ENABLED_PROPERTY_NAME, null, null,
                System.currentTimeMillis());
    }

    @Test
    public void testEnabledParameters() throws Exception {
        autoExportNode.getNode("hippo:moduleconfig").setProperty(Constants.CONFIG_ENABLED_PROPERTY_NAME, false);
        assertFalse(configuration.isExportEnabled());

        autoExportNode.getNode("hippo:moduleconfig").setProperty(Constants.CONFIG_ENABLED_PROPERTY_NAME, true);
        configuration.handleConfigurationEvent(enabledPropChangeEvent);
        assertTrue(configuration.isExportEnabled());

        autoExportNode.getNode("hippo:moduleconfig").setProperty(Constants.CONFIG_ENABLED_PROPERTY_NAME, false);
        configuration.handleConfigurationEvent(enabledPropChangeEvent);
        assertFalse(configuration.isExportEnabled());

        // remove /hippo:configuration node to test an exceptional case.
        MockNode hippoConfigNode = (MockNode) rootNode.getNode("hippo:configuration");
        hippoConfigNode.remove();
        assertFalse(rootNode.hasNode("hippo:configuration"));

        try {
            rootNode.getSession().getNode(Constants.CONFIG_NODE_PATH);
        } catch (PathNotFoundException e) {
            // as expected because we remove the hippo:configuration node above.
        }

        // when config node path not found, it should be disabled by default.
        configuration.handleConfigurationEvent(enabledPropChangeEvent);
        assertFalse(configuration.isExportEnabled());

        // add /hippo:configuration node to test if it's restored.
        rootNode.addNode(hippoConfigNode);
        configuration.handleConfigurationEvent(enabledPropChangeEvent);
        assertFalse(configuration.isExportEnabled());

        autoExportNode.getNode("hippo:moduleconfig").setProperty(Constants.CONFIG_ENABLED_PROPERTY_NAME, true);
        configuration.handleConfigurationEvent(enabledPropChangeEvent);
        assertTrue(configuration.isExportEnabled());
    }

}
