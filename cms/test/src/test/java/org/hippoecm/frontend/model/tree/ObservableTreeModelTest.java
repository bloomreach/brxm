/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.model.tree;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.swing.tree.TreePath;

import org.apache.wicket.markup.html.tree.DefaultTreeState;
import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.model.event.Observer;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class ObservableTreeModelTest extends PluginTest {

    private DefaultTreeState treeState;
    private List received;
    private JcrTreeNode rootNode;
    private ObservableTreeModel treeModel;

    @Before
    public void buildTree() throws RepositoryException {
        root.addNode("test").addNode("tree");
        session.save();

        rootNode = new JcrTreeNode(new JcrNodeModel("/test/tree"), null);
        treeModel = new ObservableTreeModel(rootNode);

        treeState = new DefaultTreeState();
        treeModel.setTreeState(treeState);

        received = new LinkedList();
        context.registerService(new Observer(treeModel) {

            @Override
            public void onEvent(final Iterator events) {
                while (events.hasNext()) {
                    received.add(events.next());
                }
            }
        }, IObserver.class.getName());
    }

    @Override
    protected void refreshPage() {
        rootNode.detach();
        super.refreshPage();
    }

    @Test
    public void testLookupCreatesTreePath() throws RepositoryException {
        Node jcrNode = session.getNode("/test/tree");
        jcrNode.addNode("child");
        session.save();

        final TreePath treePath = treeModel.lookup(new JcrNodeModel("/test/tree/child"));
        final Object[] elements = treePath.getPath();
        assertEquals(2, elements.length);
    }

    @Test
    public void testAddNodeEventIsSentForExpandedNode() throws RepositoryException {
        treeState.expandNode(rootNode);

        Node jcrNode = session.getNode("/test/tree");
        jcrNode.addNode("child");
        session.save();

        refreshPage();

        assertTrue(received.size() > 0);
    }

    @Test
    public void testRemoveNodeEventIsSentForExpandedNode() throws RepositoryException {
        Node jcrNode = session.getNode("/test/tree");
        final Node child = jcrNode.addNode("child");
        session.save();

        treeState.expandNode(rootNode);

        refreshPage();

        assertEquals(0, received.size());

        child.remove();
        session.save();

        rootNode.detach();
        refreshPage();

        assertTrue(received.size() > 0);
    }

    @Test
    public void testNoNodeEventIsSentForCollapsedDescendantNode() throws RepositoryException {
        Node jcrNode = session.getNode("/test/tree");
        final Node grandchild = jcrNode.addNode("child").addNode("grandchild");
        session.save();

        treeState.expandNode(rootNode);
        final IJcrTreeNode childTreeNode = rootNode.getChild("child");
        treeState.expandNode(childTreeNode);

        refreshPage();
        assertFalse(received.size() > 0);

        treeState.collapseNode(childTreeNode);

        grandchild.remove();
        session.save();

        refreshPage();

        assertFalse(received.size() > 0);
    }

    @Test
    public void testNoEventIsSentForCollapsedNode() throws RepositoryException {
        Node jcrNode = session.getNode("/test/tree");
        jcrNode.addNode("child");
        session.save();

        refreshPage();

        assertFalse(received.size() > 0);
    }

    @Test
    public void testNoEventIsSentAfterNodeWasCollapsed() throws RepositoryException {
        treeState.expandNode(rootNode);

        refreshPage();

        treeState.collapseNode(rootNode);

        Node jcrNode = session.getNode("/test/tree");
        jcrNode.addNode("child");
        session.save();

        refreshPage();

        assertFalse(received.size() > 0);
    }

}
