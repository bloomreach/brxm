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
package org.hippoecm.frontend.model.tree;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.model.event.JcrEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CachedTreeModel implements IJcrTreeModel, IObserver, IDetachable {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(CachedTreeModel.class);

    private List<TreeModelListener> listeners;
    private JcrTreeModel treeModel;

    public CachedTreeModel(JcrTreeModel treeModel) {
        this.treeModel = treeModel;
        this.listeners = new LinkedList<TreeModelListener>();
    }

    public IObservable getObservable() {
        return treeModel;
    }

    public void onEvent(IEvent event) {
        if (event instanceof JcrEvent) {
            Event jcrEvent = ((JcrEvent) event).getEvent();
            try {
                TreeModelEvent tme = newTreeModelEvent(jcrEvent);
                if (tme != null) {
                    for (TreeModelListener l : listeners) {
                        switch (jcrEvent.getType()) {
                        case Event.NODE_ADDED:
                            l.treeNodesInserted(tme);
                            break;
                        case Event.NODE_REMOVED:
                            l.treeNodesRemoved(tme);
                            break;
                        default:
                            log.error("Unexpected event, node " + jcrEvent.getPath());
                        }
                    }
                }
            } catch (RepositoryException ex) {
                log.error("failed to broadcast event", ex);
            }
        }
    }

    // translate the jcr event into a tree model event
    protected TreeModelEvent newTreeModelEvent(Event event) throws RepositoryException {
        JcrNodeModel nodeModel = new JcrNodeModel(event.getPath());
        IJcrTreeNode parentNode = treeModel.lookup(nodeModel.getParentModel());
        if (parentNode.getNodeModel().equals(nodeModel.getParentModel())) {
            if (event.getType() == Event.NODE_ADDED) {
                parentNode.detach();
            }
            IJcrTreeNode childNode = treeModel.lookup(nodeModel);
            if (event.getType() == Event.NODE_REMOVED) {
                parentNode.detach();
            }
            if (childNode != null && childNode.getParent().equals(parentNode)) {
                int[] indices = new int[] { parentNode.getIndex(childNode) };
                Object[] children = new Object[] { wrapNode(childNode) };
                TreeModelEvent tme = new TreeModelEvent(this, getTreePath(parentNode), indices, children);
                return tme;
            } else {
                log.warn("unable to find tree node for event (" + event.getPath() + ")");
            }
        }
        return null;
    }

    private TreePath getTreePath(TreeNode node) {
        List<Object> nodes = new LinkedList<Object>();
        while (node != null) {
            nodes.add(wrapNode(node));
            node = node.getParent();
        }
        Collections.reverse(nodes);
        return new TreePath(nodes.toArray(new Object[nodes.size()]));
    }

    public IJcrTreeNode lookup(JcrNodeModel nodeModel) {
        return (IJcrTreeNode) wrapNode(treeModel.lookup(nodeModel));
    }

    public void addTreeModelListener(TreeModelListener l) {
        listeners.add(l);
    }

    public void removeTreeModelListener(TreeModelListener l) {
        listeners.remove(l);
    }

    public Object getChild(Object parent, int index) {
        return treeModel.getChild(unwrapNode(parent), index);
    }

    public int getChildCount(Object parent) {
        return treeModel.getChildCount(unwrapNode(parent));
    }

    public int getIndexOfChild(Object parent, Object child) {
        return treeModel.getIndexOfChild(unwrapNode(parent), unwrapNode(child));
    }

    public Object getRoot() {
        return wrapNode((TreeNode) treeModel.getRoot());
    }

    public boolean isLeaf(Object node) {
        return treeModel.isLeaf(unwrapNode(node));
    }

    public void valueForPathChanged(TreePath path, Object newValue) {
        throw new UnsupportedOperationException("editing nodes is not supported");
    }

    // overload IDetachable as a cache control
    public void detach() {
    }

    static TreeNode wrapNode(TreeNode object) {
        if (object instanceof IJcrTreeNode) {
            return new CachedTreeNode((IJcrTreeNode) object);
        } else {
            return object;
        }
    }

    static TreeNode unwrapNode(Object object) {
        if (object instanceof CachedTreeNode) {
            return ((CachedTreeNode) object).get();
        } else {
            return (TreeNode) object;
        }
    }
}