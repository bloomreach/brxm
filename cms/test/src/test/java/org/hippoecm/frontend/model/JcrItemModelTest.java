/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import javax.jcr.Node;

import org.hippoecm.frontend.PluginTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JcrItemModelTest extends PluginTest {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
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
        assertEquals("/test/newparent/test", model.getItemModel().getPath());
        model.detach();
    }

    @Test
    public void testExists() throws Exception {
        Node test = this.root.addNode("test", "nt:unstructured");

        // non-existing prop
        JcrItemModel itemModel = new JcrItemModel(test.getPath() + "/prop");
        assertFalse("JcrItemModel reports that item exists while it doesn't", itemModel.exists());

        // existing node
        itemModel = new JcrItemModel(test);
        itemModel.detach();
        assertTrue("JcrItemModel reports that item doesn't exist while it does", itemModel.exists());
    }

    @Test
    public void testPathConstructor() throws Exception {
        Node test = this.root.addNode("test", "nt:unstructured");
        JcrItemModel itemModel = new JcrItemModel("/test");
        Node verify = (Node) itemModel.getObject();
        assertTrue("Node found by path is incorrect", test.isSame(verify));

        itemModel.detach();
        verify = (Node) itemModel.getObject();
        assertTrue("Node found by path is incorrect after detach", test.isSame(verify));
    }

    @Test
    public void testNonExistingPath() throws Exception {
        Node test = this.root.addNode("test", "nt:unstructured");
        test.addMixin("mix:referenceable");
        root.save();

        JcrItemModel itemModel = new JcrItemModel("/test/bla");
        assertTrue(((Node) itemModel.getParentModel().getObject()).isSame(test));
    }

    @Test
    public void testNullNode() throws Exception {
        JcrItemModel itemModel = new JcrItemModel((Node) null);
        itemModel.detach();
        assertNull(itemModel.getObject());
        assertFalse(itemModel.exists());
        assertNull(itemModel.getPath());
    }

    @Test
    public void testFallbackToPath() throws Exception {
        Node test = this.root.addNode("test", "nt:unstructured");
        test.addMixin("mix:referenceable");
        root.save();

        JcrItemModel itemModel = new JcrItemModel("/test");
        itemModel.detach();

        test.remove();
        root.save();
        
        test = root.addNode("test", "nt:unstructured");
        root.save();
        assertTrue(test.isSame((Node) itemModel.getObject()));
    }

    @Test
    public void testPathFollowsUuid() throws Exception {
        Node root = this.root.addNode("test", "nt:unstructured");
        Node test = root.addNode("child");
        test.addMixin("mix:referenceable");

        // retrieve path, making sure that it it cached
        JcrNodeModel model = new JcrNodeModel(test);
        String path = model.getItemModel().getPath();
        assertEquals("/test/child", path);
        model.detach();

        session.move(test.getPath(), root.getPath() + "/newname");

        assertEquals("/test/newname", model.getItemModel().getPath());
        model.detach();
    }

}
