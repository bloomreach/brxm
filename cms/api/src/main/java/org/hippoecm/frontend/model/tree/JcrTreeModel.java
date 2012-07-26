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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.apache.wicket.markup.html.tree.DefaultTreeState;
import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.model.tree.ObservableTreeModel.ObservableTreeModelEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrTreeModel implements IJcrTreeModel, IObserver<ObservableTreeModel>, IDetachable {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(JcrTreeModel.class);

    private ObservableTreeModel jcrTreeModel;
    private List<TreeModelListener> listeners;

    public JcrTreeModel(IJcrTreeNode rootModel) {
        jcrTreeModel = new ObservableTreeModel(rootModel);
        listeners = new LinkedList<TreeModelListener>();
    }

    public ObservableTreeModel getObservable() {
        return jcrTreeModel;
    }

    public void onEvent(Iterator<? extends IEvent<ObservableTreeModel>> iter) {
        while (iter.hasNext()) {
            IEvent<ObservableTreeModel> event = iter.next();
            if (event instanceof ObservableTreeModelEvent) {
                Event jcrEvent = ((ObservableTreeModelEvent) event).getJcrEvent().getEvent();
                try {
                    TreeModelEvent tme = newTreeModelEvent(jcrEvent);
                    if (tme != null) {
                        for (TreeModelListener l : listeners) {
                            l.treeStructureChanged(tme);
                        }
                    }
                } catch (RepositoryException ex) {
                    log.error("failed to broadcast event", ex);
                }
            }
        }
    }

    // translate the jcr event into a tree model event
    protected TreeModelEvent newTreeModelEvent(Event event) throws RepositoryException {
        JcrNodeModel nodeModel = new JcrNodeModel(event.getPath());
        if (event.getType() != 0) {
            TreePath parentPath = lookup(nodeModel.getParentModel());
            if (parentPath != null) {
                TreeNode parentNode = (TreeNode) parentPath.getLastPathComponent();
                if ((parentNode instanceof IJcrTreeNode)
                        && ((IJcrTreeNode) parentNode).getNodeModel().equals(nodeModel.getParentModel())) {
                    return new TreeModelEvent(this, parentPath);
                }
            }
        } else {
            TreePath nodePath = lookup(nodeModel);
            if (nodePath != null) {
                TreeNode treeNode = (TreeNode) nodePath.getLastPathComponent();
                if ((treeNode instanceof IJcrTreeNode) && ((IJcrTreeNode) treeNode).getNodeModel().equals(nodeModel)) {
                    return new TreeModelEvent(this, nodePath);
                }
            }
        }
        return null;
    }

    public void addTreeModelListener(TreeModelListener l) {
        listeners.add(l);
    }

    public void removeTreeModelListener(TreeModelListener l) {
        listeners.remove(l);
    }

    public TreePath lookup(JcrNodeModel model) {
        return jcrTreeModel.lookup(model);
    }

    public Object getChild(Object parent, int index) {
        return jcrTreeModel.getChild(parent, index);
    }

    public int getChildCount(Object parent) {
        return jcrTreeModel.getChildCount(parent);
    }

    public int getIndexOfChild(Object parent, Object child) {
        return jcrTreeModel.getIndexOfChild(parent, child);
    }

    public Object getRoot() {
        return jcrTreeModel.getRoot();
    }

    public boolean isLeaf(Object node) {
        return jcrTreeModel.isLeaf(node);
    }

    public void valueForPathChanged(TreePath path, Object newValue) {
        jcrTreeModel.valueForPathChanged(path, newValue);
    }

    public void detach() {
        jcrTreeModel.detach();
    }

    public void setTreeState(final DefaultTreeState state) {
        jcrTreeModel.setTreeState(state);
    }
}
