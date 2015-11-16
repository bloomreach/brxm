/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.bootstrap.InitializeItem;
import org.onehippo.repository.mock.MockNode;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_RESOURCEBUNDLES;
import static org.hippoecm.repository.api.HippoNodeType.NT_INITIALIZEITEM;
import static org.hippoecm.repository.api.HippoNodeType.NT_RESOURCEBUNDLE;
import static org.hippoecm.repository.api.HippoNodeType.NT_RESOURCEBUNDLES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ResourceBundlesInstructionTest {

    private Node translations;
    private ResourceBundlesInstruction instruction;

    @Before
    public void setUp() throws Exception {
        final MockNode root = MockNode.root();
        final Node itemNode = root.addNode("item", NT_INITIALIZEITEM);
        itemNode.setProperty(HIPPO_RESOURCEBUNDLES, getClass().getResource("/bootstrap/resourcebundle.json").toString());
        final InitializeItem initializeItem = new InitializeItem(itemNode);
        instruction = new ResourceBundlesInstruction(initializeItem, itemNode.getSession());
        translations = root.addNode("hippo:configuration", "nt:unstructured").addNode("hippo:translations", NT_RESOURCEBUNDLES);
    }

    @Test
    public void testIsDownstream() throws Exception {
        assertTrue(instruction.isDownstream(new String[]{"/hippo:configuration/hippo:translations/foo/bar/en"}));
        assertTrue(instruction.isDownstream(new String[]{"/hippo:configuration/hippo:translations/foo/bar/nl"}));
        assertTrue(instruction.isDownstream(new String[]{"/hippo:configuration/hippo:translations"}));
    }

    @Test
    public void testExecute() throws Exception {
        instruction.execute();
        assertTrue(translations.hasNode("foo/bar"));
        final Node bar = translations.getNode("foo/bar");
        assertEquals(NT_RESOURCEBUNDLES, bar.getPrimaryNodeType().getName());
        assertTrue(bar.hasNode("en") && bar.hasNode("nl"));
        final Node en = bar.getNode("en");
        assertEquals(NT_RESOURCEBUNDLE, en.getPrimaryNodeType().getName());
        assertTrue(en.hasProperty("key"));
        assertEquals("value", en.getProperty("key").getString());
        final Node nl = bar.getNode("nl");
        assertEquals(NT_RESOURCEBUNDLE, nl.getPrimaryNodeType().getName());
        assertTrue(nl.hasProperty("key"));
        assertEquals("waarde", nl.getProperty("key").getString());
    }
}
