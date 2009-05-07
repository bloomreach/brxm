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

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.model.event.JcrEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CachedTreeModel extends JcrTreeModel implements IObserver {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(CachedTreeModel.class);

    public CachedTreeModel(IJcrTreeNode rootModel) {
        super(rootModel);
    }

    public IObservable getObservable() {
        return this;
    }

    public void onEvent(Iterator<? extends IEvent> iter) {
        while (iter.hasNext()) {
            IEvent event = iter.next();
            if (event instanceof JcrEvent) {
                Event jcrEvent = ((JcrEvent) event).getEvent();
                try {
                    TreeModelEvent tme = newTreeModelEvent(jcrEvent);
                    if (tme != null) {
                        for (TreeModelListener l : getListeners(TreeModelListener.class)) {
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

}
