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
package org.onehippo.repository.bootstrap.instructions;

import javax.jcr.Node;

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
import static junit.framework.Assert.assertTrue;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTENTPROPSET;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTENTROOT;
import static org.hippoecm.repository.api.HippoNodeType.NT_INITIALIZEITEM;

public class ContentPropSetInstructionTest {

    private Node root;
    private Node itemNode;
    private ContentPropSetInstruction instruction;
    private Logger originalBootstrapLogger;
    private LoggerRecordingWrapper loggingRecorder;

    @Before
    public void setUp() throws Exception {
        root = MockNode.root();
        itemNode = root.addNode("initializeItem", NT_INITIALIZEITEM);
        itemNode.setProperty(HIPPO_CONTENTROOT, "/test");
        instruction = new ContentPropSetInstruction(new InitializeItem(itemNode), root.getSession());
        originalBootstrapLogger = BootstrapConstants.log;
        loggingRecorder = new LoggerRecordingWrapper(NOPLogger.NOP_LOGGER);
        BootstrapConstants.log = loggingRecorder;
    }

    @After
    public void shutDown() throws Exception {
        BootstrapConstants.log = originalBootstrapLogger;
    }

    @Test
    public void testSetExistingSingle() throws Exception {
        root.setProperty("test", "test");
        itemNode.setProperty(HIPPO_CONTENTPROPSET, "foo");
        instruction.execute();
        assertTrue(root.hasProperty("test"));
        assertEquals("foo", root.getProperty("test").getString());
    }

    @Test
    public void testSetExistingSingleWithMultiple() throws Exception {
        root.setProperty("test", "test");
        itemNode.setProperty(HIPPO_CONTENTPROPSET, new String[] { "foo", "bar" });
        instruction.execute();
        assertTrue(root.hasProperty("test"));
        assertEquals("test", root.getProperty("test").getString());
        assertEquals(1, loggingRecorder.getWarnMessages().size());
    }

    @Test
    public void testSetExistingMultiple() throws Exception {
        root.setProperty("test", new String[] { "test" });
        itemNode.setProperty(HIPPO_CONTENTPROPSET, new String[] { "foo" });
        instruction.execute();
        assertTrue(root.hasProperty("test"));
        assertEquals(1, root.getProperty("test").getValues().length);
        assertEquals("foo", root.getProperty("test").getValues()[0].getString());
    }

    @Test
    public void testSetNewEmptyMultiple() throws Exception {
        itemNode.setProperty(HIPPO_CONTENTPROPSET, new String[] {});
        instruction.execute();
        assertEquals(1, loggingRecorder.getWarnMessages().size());
    }
}
