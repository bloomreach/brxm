/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.bootstrap.instructions;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.bootstrap.InitializeItem;
import org.onehippo.repository.bootstrap.util.BootstrapConstants;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.testutils.slf4j.LoggerRecordingWrapper;
import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTENTDELETE;
import static org.hippoecm.repository.api.HippoNodeType.NT_INITIALIZEITEM;
import static org.onehippo.repository.util.JcrConstants.NT_UNSTRUCTURED;

public class ContentDeleteInstructionTest {

    private Node root;
    private Node itemNode;
    private ContentDeleteInstruction instruction;
    private Logger originalBootstrapLogger;
    private LoggerRecordingWrapper loggingRecorder;

    @Before
    public void setUp() throws Exception {
        root = MockNode.root();
        itemNode = root.addNode("initializeItem", NT_INITIALIZEITEM);
        instruction = new ContentDeleteInstruction(new InitializeItem(itemNode), root.getSession());
        originalBootstrapLogger = BootstrapConstants.log;
        loggingRecorder = new LoggerRecordingWrapper(NOPLogger.NOP_LOGGER);
        BootstrapConstants.log = loggingRecorder;
    }

    @After
    public void shutDown() throws Exception {
        BootstrapConstants.log = originalBootstrapLogger;
    }

    @Test
    public void testContentDeleteInstructionRemovesNode() throws Exception {
        itemNode.setProperty(HIPPO_CONTENTDELETE, "/test");
        root.addNode("test", NT_UNSTRUCTURED);
        instruction.execute();
        assertFalse(root.hasNode("test"));
    }

    @Test
    public void testContentDeleteInstructionIgnoresNonExistentNode() throws Exception {
        itemNode.setProperty(HIPPO_CONTENTDELETE, "/test");
        instruction.execute();
        assertEquals(1, loggingRecorder.getInfoMessages().size());
    }

    @Test (expected = RepositoryException.class)
    public void testContentDeleteThrowsExceptionWhenAttemptingToDeleteRootNode() throws Exception {
        itemNode.setProperty(HIPPO_CONTENTDELETE, "/");
        instruction.execute();
    }

    @Test (expected = RepositoryException.class)
    public void testContentDeleteThrowsExceptionOnRelPath() throws Exception {
        itemNode.setProperty(HIPPO_CONTENTDELETE, "relpath");
        instruction.execute();
    }

    @Test
    public void testContentDeleteDoesNotDeleteSNS() throws Exception {
        itemNode.setProperty(HIPPO_CONTENTDELETE, "/test");
        root.addNode("test", NT_UNSTRUCTURED);
        root.addNode("test", NT_UNSTRUCTURED);
        instruction.execute();
        assertEquals(2, root.getNodes("test").getSize());
        assertEquals(1, loggingRecorder.getWarnMessages().size());
    }
}
