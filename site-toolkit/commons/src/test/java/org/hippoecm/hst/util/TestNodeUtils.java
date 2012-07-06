/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.hst.util;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.jcr.Node;

import org.junit.Test;

/**
 * TestNodeUtils
 */
public class TestNodeUtils {

    @Test
    public void testBasicUsage() throws Exception {
        Node node = createNiceMock(Node.class);
        expect(node.isNodeType("demosite:newsdocument")).andReturn(true).anyTimes();
        expect(node.isNodeType("demosite:basedocument")).andReturn(true).anyTimes();
        expect(node.isNodeType("demosite:agendadocument")).andReturn(false).anyTimes();
        expect(node.isNodeType("demosite:blogdocument")).andReturn(false).anyTimes();
        replay(node);

        assertTrue(NodeUtils.isNodeType(node, "demosite:newsdocument"));
        assertFalse(NodeUtils.isNodeType(node, "demosite:agendadocument"));
        assertTrue(NodeUtils.isNodeType(node, "demosite:basedocument"));

        assertTrue(NodeUtils.isNodeType(node, "demosite:newsdocument", "demosite:basedocument"));
        assertTrue(NodeUtils.isNodeType(node, "demosite:agendadocument", "demosite:newsdocument"));
        assertFalse(NodeUtils.isNodeType(node, "demosite:agendadocument", "demosite:blogdocument"));
    }
}