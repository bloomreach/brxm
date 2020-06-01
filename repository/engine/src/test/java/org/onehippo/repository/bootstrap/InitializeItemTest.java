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
import javax.jcr.RepositoryException;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;

import static org.hippoecm.repository.api.HippoNodeType.CONFIGURATION_PATH;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTENTRESOURCE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTENTROOT;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_ERRORMESSAGE;
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
import static org.junit.Assert.fail;
import static org.onehippo.repository.bootstrap.util.BootstrapConstants.ERROR_MESSAGE_RELOAD_DISABLED;
import static org.onehippo.repository.bootstrap.util.BootstrapConstants.ITEM_STATUS_FAILED;
import static org.onehippo.repository.bootstrap.util.BootstrapConstants.ITEM_STATUS_PENDING;
import static org.onehippo.repository.bootstrap.util.BootstrapConstants.ITEM_STATUS_RELOAD;
import static org.onehippo.repository.bootstrap.util.BootstrapConstants.SYSTEM_RELOAD_PROPERTY;
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
        assertEquals(ITEM_STATUS_RELOAD, initializeItem.getItemNode().getProperty(HIPPO_STATUS).getString());
    }

    @Test
    public void testShouldReloadNewerItemVersion() throws Exception {
        tempItemNode.setProperty(HIPPO_VERSION, "2");
        tempItemNode.setProperty(HIPPO_RELOADONSTARTUP, true);
        final Node itemNode = initializeFolder.addNode("initItem", NT_INITIALIZEITEM);
        itemNode.setProperty(HIPPO_VERSION, "1");
        final InitializeItem initializeItem = new InitializeItem(itemNode, tempItemNode, extension);
        assertTrue(initializeItem.isReloadRequested());
    }

    @Test
    public void testShouldNotReloadOlderItemVersion() throws Exception {
        tempItemNode.setProperty(HIPPO_VERSION, "1");
        tempItemNode.setProperty(HIPPO_RELOADONSTARTUP, true);
        Node itemNode = initializeFolder.addNode("initItem", NT_INITIALIZEITEM);
        itemNode.setProperty(HIPPO_VERSION, "2");
        final InitializeItem initializeItem = new InitializeItem(itemNode, tempItemNode, extension);
        assertFalse(initializeItem.isReloadRequested());
    }

    @Test
    public void testProcessingItemFailedSetsStatus() throws Exception {
        Node itemNode = initializeFolder.addNode("initItem", NT_INITIALIZEITEM);
        itemNode.setProperty(HIPPO_CONTENTROOT, "/");
        itemNode.setProperty(HIPPO_CONTENTRESOURCE, "missing.xml");
        final InitializeItem initializeItem = new InitializeItem(itemNode);
        try {
            initializeItem.process();
            fail("Processing item with missing content resource should throw exception");
        } catch (RepositoryException expected) {
        }
        assertTrue(itemNode.hasProperty(HIPPO_STATUS));
        assertEquals(ITEM_STATUS_FAILED, itemNode.getProperty(HIPPO_STATUS).getString());
        assertTrue(itemNode.hasProperty(HIPPO_ERRORMESSAGE));
    }

    @Test
    public void testInitializeInitializeReloadItemReloadDisabledSetsStatus() throws Exception {
        final String systemReloadProperty = System.getProperty(SYSTEM_RELOAD_PROPERTY, "true");
        try {
            System.setProperty(SYSTEM_RELOAD_PROPERTY, "false");
            tempItemNode.setProperty(HIPPO_VERSION, "1");
            tempItemNode.setProperty(HIPPO_RELOADONSTARTUP, true);
            final Node initItem = initializeFolder.addNode("initItem", NT_INITIALIZEITEM);
            InitializeItem initializeItem = new InitializeItem(tempItemNode, extension);
            initializeItem.initialize();
            assertTrue(initItem.hasProperty(HIPPO_STATUS));
            assertEquals(ITEM_STATUS_FAILED, initItem.getProperty(HIPPO_STATUS).getString());
            assertTrue(initItem.hasProperty(HIPPO_ERRORMESSAGE));
            assertEquals(ERROR_MESSAGE_RELOAD_DISABLED, initItem.getProperty(HIPPO_ERRORMESSAGE).getString());
        } finally {
            System.setProperty(SYSTEM_RELOAD_PROPERTY, systemReloadProperty);
        }
    }
}
