/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.swing.tree.TreeNode;

import org.apache.wicket.IClusterable;

public abstract class LazyTreeNode implements TreeNode, IClusterable {
    private static final long serialVersionUID = 1L;

    protected LazyTreeNode parent;
    protected List childNodes;
    protected int childCount = -1;
    protected boolean isLoaded;

    //constructor
    
    protected LazyTreeNode(LazyTreeNode parent) {
        this.parent = parent;
    }

    //Abstract methods

    protected abstract LazyTreeNode createNode(Object o);

    protected abstract int getChildObjectCount();

    protected abstract Collection getChildObjects();
    
    protected abstract Object getUserObject();


    //TreeNode implementation

    public Enumeration children() {
        ensureChildrenLoaded();
        return Collections.enumeration(childNodes);
    }

    public boolean getAllowsChildren() {
        return true;
    }

    public TreeNode getChildAt(int i) {
        ensureChildrenLoaded();
        // Protect against children which fail to load, for whatever reason
        if (i >= childNodes.size()) {
            i = childNodes.size() - 1;
        }
        return (i == -1) ? null : (TreeNode) childNodes.get(i);
    }

    public int getChildCount() {
        ensureChildCountLoaded();
        return childCount;
    }

    public int getIndex(TreeNode node) {
        ensureChildrenLoaded();
        return childNodes.indexOf(node);
    }

    public TreeNode getParent() {
        return parent;
    }

    public boolean isLeaf() {
        ensureChildCountLoaded();
        return childCount == 0;
    }

    // important (?) public stuff
    // try to move as much of this down to the private section

    public void reload() {
        clearNodes();
        loadNodes();
    }

    public void childAdded(Object o) {
        int index = (isLoaded) ? childNodes.size() : -1;
        childAdded(o, index);
    }

    public void childAdded(Object o, int index) {
        if (isLoaded) {
            LazyTreeNode child = createNode(o);
            childNodes.add(index, child);
            ++childCount;
        } else {
            ensureChildrenLoaded();
            childCount = childNodes.size();
        }
    }

    public void childRemoved(Object o) {
        if (childCount != -1) {
            --childCount;
        }
        if (isLoaded) {
            int index = getIndex(o);
            if (index < 0) {
                ++childCount;
            } else {
                LazyTreeNode child = (LazyTreeNode) childNodes.remove(index);
                child.clearNodes();
            }
        }
    }


    // privates
    
    private int getIndex(Object o) {
        int nChildren = childNodes.size();
        int index = -1;
        for (int i = 0; i < nChildren; ++i) {
            LazyTreeNode node = (LazyTreeNode) childNodes.get(i);
            if (equals(node.getUserObject(), o)) {
                index = i;
            }
        }
        return index;
    }

    private void clearNodes() {
        if (childNodes != null) {
            childCount = 0;
            Iterator i = childNodes.iterator();
            while (i.hasNext()) {
                LazyTreeNode node = (LazyTreeNode) i.next();
                node.clearNodes();
            }
            childNodes.clear();
        }
    }

    private void ensureChildCountLoaded() {
        if (childCount == -1) {
            childCount = getChildObjectCount();
        }
    }

    private void ensureChildrenLoaded() {
        if (!isLoaded) {
            loadNodes();
            isLoaded = true;
        }
    }

    private void loadChildObjects(Collection childObjects) {
        if (childNodes == null) {
            childNodes = new ArrayList();
        } else {
            childNodes.clear();
        }
        Iterator i = childObjects.iterator();
        while (i.hasNext()) {
            Object child = i.next();
            try {
                TreeNode childNode = createNode(child);
                childNodes.add(childNode);
            } catch (Exception e) {
                // TODO log error
                e.printStackTrace();
            }
        }
        childCount = childNodes.size();
        // Collections.sort(childNodes, getComparator());
    }

    private void loadNodes() {
        Collection childObjects = getChildObjects();
        loadChildObjects(childObjects);
    }
    
    private boolean equals(Object o1, Object o2) {
        return (o1 == null) ? o2 == null : o1.equals(o2);
    }
    

}