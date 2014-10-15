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
import javax.jcr.RepositoryException;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.bootstrap.InitializeItem;
import org.onehippo.repository.mock.MockNode;

import static junit.framework.Assert.assertFalse;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTENTPROPDELETE;
import static org.hippoecm.repository.api.HippoNodeType.NT_INITIALIZEITEM;

public class ContentPropDeleteInstructionTest {

    private Node root;
    private Node itemNode;
    private ContentPropDeleteInstruction instruction;

    @Before
    public void setUp() throws Exception {
        root = MockNode.root();
        itemNode = root.addNode("initializeItem", NT_INITIALIZEITEM);
        itemNode.setProperty(HIPPO_CONTENTPROPDELETE, "/test");
        instruction = new ContentPropDeleteInstruction(new InitializeItem(itemNode), root.getSession());
    }

    @Test
    public void testContentPropDeleteRemovesProperty() throws Exception {
        root.setProperty("test", "test");
        instruction.execute();
        assertFalse(root.hasProperty("test"));
    }

    @Test
    public void testContentPropDeleteIgnoreNonExistingProperty() throws Exception {
        instruction.execute();
    }

    @Test(expected = RepositoryException.class)
    public void testContentPropDeleteThrowsExceptionOnRelPath() throws Exception {
        itemNode.setProperty(HIPPO_CONTENTPROPDELETE, "test");
        instruction.execute();
    }
}
