/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.bootstrap;

import java.net.URL;

import javax.jcr.Node;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;

import static org.hippoecm.repository.api.HippoNodeType.CONFIGURATION_PATH;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_RELOADONSTARTUP;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_STATUS;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_VERSION;
import static org.hippoecm.repository.api.HippoNodeType.INITIALIZE_PATH;
import static org.hippoecm.repository.api.HippoNodeType.NT_INITIALIZEFOLDER;
import static org.hippoecm.repository.api.HippoNodeType.NT_INITIALIZEITEM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.onehippo.repository.bootstrap.util.BootstrapConstants.ITEM_STATUS_PENDING;
import static org.onehippo.repository.util.JcrConstants.NT_UNSTRUCTURED;

public class InitializeItemTest {

    private Node initializeFolder;
    private Node tempItemNode;
    private Extension extension;

    @Before
    public void setUp() throws Exception {
        final Node root = MockNode.root();
        initializeFolder = root.addNode(CONFIGURATION_PATH, NT_UNSTRUCTURED).addNode(INITIALIZE_PATH, NT_INITIALIZEFOLDER);
        tempItemNode = root.addNode("initItem", NT_INITIALIZEITEM);
        final URL extensionURL = getClass().getResource("/bootstrap/hippoecm-extension.xml");
        assumeTrue(extensionURL != null);
        extension = new Extension(root.getSession(), extensionURL);

    }

    @Test
    public void testInitializeInitializeItem() throws Exception {
        InitializeItem initializeItem = new InitializeItem(tempItemNode, extension);
        initializeItem.initialize();
        assertNotNull(initializeItem.getItemNode());
        assertEquals("initItem", initializeItem.getName());
        assertTrue(initializeItem.getItemNode().hasProperty(HIPPO_STATUS));
        assertEquals(ITEM_STATUS_PENDING, initializeItem.getItemNode().getProperty(HIPPO_STATUS).getString());
    }

    @Test
    public void testInitializeReloadInitializeItem() throws Exception {
        tempItemNode.setProperty(HIPPO_VERSION, "1");
        tempItemNode.setProperty(HIPPO_RELOADONSTARTUP, true);
        initializeFolder.addNode("initItem", NT_INITIALIZEITEM);
        InitializeItem initializeItem = new InitializeItem(tempItemNode, extension);
        initializeItem.initialize();
        assertNotNull(initializeItem.getItemNode());
        assertEquals("initItem", initializeItem.getName());
        assertTrue(initializeItem.getItemNode().hasProperty(HIPPO_STATUS));
        assertEquals(ITEM_STATUS_PENDING, initializeItem.getItemNode().getProperty(HIPPO_STATUS).getString());
    }

    @Test
    public void testShouldReloadNewerItemVersion() throws Exception {
        tempItemNode.setProperty(HIPPO_VERSION, "2");
        tempItemNode.setProperty(HIPPO_RELOADONSTARTUP, true);
        final Node itemNode = initializeFolder.addNode("initItem", NT_INITIALIZEITEM);
        itemNode.setProperty(HIPPO_VERSION, "1");
        final InitializeItem initializeItem = new InitializeItem(itemNode, tempItemNode, extension);
        assertTrue(initializeItem.shouldReload());
    }

    @Test
    public void testShouldNotReloadOlderItemVersion() throws Exception {
        tempItemNode.setProperty(HIPPO_VERSION, "1");
        tempItemNode.setProperty(HIPPO_RELOADONSTARTUP, true);
        Node itemNode = initializeFolder.addNode("initItem", NT_INITIALIZEITEM);
        itemNode.setProperty(HIPPO_VERSION, "2");
        final InitializeItem initializeItem = new InitializeItem(itemNode, tempItemNode, extension);
        assertFalse(initializeItem.shouldReload());
    }

}
