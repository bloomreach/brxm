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
package org.hippoecm.frontend.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.jcr.Node;

import org.hippoecm.frontend.HippoTester;
import org.hippoecm.repository.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JcrItemModelTest extends TestCase {

    Node root;
    HippoTester tester;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp(true);
        root = session.getRootNode();
        tester = new HippoTester(new JcrSessionModel(null) {
            private static final long serialVersionUID = 1L;

            @Override
            protected Object load() {
                return session;
            }
        });
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testParentMove() throws Exception {
        Node root = this.root.addNode("test", "nt:unstructured");
        Node parent = root.addNode("parent");
        Node test = parent.addNode("test");
        test.addMixin("mix:referenceable");

        JcrNodeModel model = new JcrNodeModel(test);
        model.detach();
        
        // verify that model can reattach
        Node node = model.getNode();
        assertNotNull(node);
        model.detach();

        // model can reattach after parent move
        session.move(parent.getPath(), root.getPath() + "/newparent");

        node = model.getNode();
        assertNotNull(node);
        model.detach();
    }

    @Test
    public void testExists() throws Exception {
        Node test = this.root.addNode("test", "nt:unstructured");
        JcrItemModel itemModel = new JcrItemModel(test.getPath() + "/prop");
        assertFalse("JcrItemModel reports that item exists while it doesn't", itemModel.exists());
    }

    @Test
    public void testPathConstructor() throws Exception {
        Node test = this.root.addNode("test", "nt:unstructured");
        JcrItemModel itemModel = new JcrItemModel("/test");
        Node verify = (Node) itemModel.getObject();
        assertTrue("Node found by path is incorrect", test.isSame(verify));
    }
}
