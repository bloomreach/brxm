package org.hippoecm.frontend.tree;

import java.io.Serializable;
import java.util.*;
import java.util.logging.Level;

import javax.swing.tree.*;

/**
 * A tree node that doesn't check its children until it is asked for them. This lazy evaluation alls the system to do
 * "load on demand" from a database.
 *
 * @author Ray Fergerson <fergerson@smi.stanford.edu>
 */
public abstract class LazyTreeNode implements TreeNode, Serializable {
    private LazyTreeNode _parent;
    private Object _userObject;
    private List _childNodes;
    private int _childCount = -1;
    private boolean _isLoaded;
    private boolean isDuplicate;

    protected LazyTreeNode(LazyTreeNode parent, Object userObject) {
        _parent = parent;
        _userObject = userObject;
        isDuplicate = isDuplicate(userObject, parent);
    }

    private static boolean isDuplicate(Object userObject, LazyTreeNode parent) {
        boolean isDuplicate = false;
        LazyTreeNode ancestor = parent;
        while (ancestor != null) {
            if (ancestor.getUserObject().equals(userObject)) {
                isDuplicate = true;
                break;
            }
            ancestor = ancestor.getLazyTreeNodeParent();
        }
        return isDuplicate;
    }

    public boolean isDuplicate() {
        return isDuplicate;
    }

    public void childAdded(Object o) {
        int index = (_isLoaded) ? _childNodes.size() : -1;
        childAdded(o, index);
    }

    public void childAdded(Object o, int index) {
        if (_isLoaded) {
            LazyTreeNode child = createNode(o);
            _childNodes.add(index, child);
            ++_childCount;
            notifyChildNodeAdded(this, index, child);
            // Log.trace("added", this, "childAdded", o, new Integer(index));
        } else {
            ensureChildrenLoaded();
            _childCount = _childNodes.size();
            index = getIndex(o);
            if (index != -1) {
                LazyTreeNode child = (LazyTreeNode) _childNodes.get(index);
                notifyChildNodeAdded(this, index, child);
            }
        }
    }

    public void childRemoved(Object o) {
        if (_childCount != -1) {
            --_childCount;
        }
        // Log.enter(this, "childRemoved", o);
        if (_isLoaded) {
            int index = getIndex(o);
            if (index < 0) {
                // Log.warning("node not found", this, "childRemoved", o);
                ++_childCount;
            } else {
                LazyTreeNode child = (LazyTreeNode) _childNodes.remove(index);
                child.dispose();
                notifyChildNodeRemoved(this, index, child);
            }
        }
    }

    public Enumeration children() {
        ensureChildrenLoaded();
        return Collections.enumeration(_childNodes);
    }

    private void clearNodes() {
        if (_childNodes != null) {
            _childCount = 0;
            Iterator i = _childNodes.iterator();
            while (i.hasNext()) {
                LazyTreeNode node = (LazyTreeNode) i.next();
                node.dispose();
            }
            _childNodes.clear();
        }
    }

    private LazyTreeNode createErrorNode(Object o) {
        return new ErrorLazyTreeNode(this, o);
    }

    protected abstract LazyTreeNode createNode(Object o);

    protected void dispose() {
        if (_childNodes != null) {
            Iterator i = _childNodes.iterator();
            while (i.hasNext()) {
                LazyTreeNode node = (LazyTreeNode) i.next();
                node.dispose();
            }
        }
    }

    private void ensureChildCountLoaded() {
        if (_childCount == -1) {
            _childCount = getChildObjectCount();
        }
    }

    private void ensureChildrenLoaded() {
        if (!_isLoaded) {
            loadNodes();
            _isLoaded = true;
        }
    }

    public boolean getAllowsChildren() {
        return true;
    }

    public TreeNode getChildAt(int i) {
        ensureChildrenLoaded();
        // Protect against children which fail to load, for whatever reason
        if (i >= _childNodes.size()) {
            i = _childNodes.size() - 1;
        }
        return (i == -1) ? null : (TreeNode) _childNodes.get(i);
    }

    public int getChildCount() {
        ensureChildCountLoaded();
        return _childCount;
    }

    protected abstract int getChildObjectCount();

    protected abstract Collection getChildObjects();

    protected abstract Comparator getComparator();

    public int getIndex(Object o) {
        int nChildren = _childNodes.size();
        int index = -1;
        for (int i = 0; i < nChildren; ++i) {
            LazyTreeNode node = (LazyTreeNode) _childNodes.get(i);
            if (equals(node.getUserObject(), o)) {
                index = i;
            }
        }
        return index;
    }

    public int getIndex(TreeNode node) {
        ensureChildrenLoaded();
        return _childNodes.indexOf(node);
    }

    public LazyTreeNode getLazyTreeNodeParent() {
        return _parent;
    }

    public TreeNode getParent() {
        return _parent;
    }

    public Object getUserObject() {
        return _userObject;
    }

    public int getUserObjectIndex(Object o) {
        ensureChildrenLoaded();
        int index = -1;
        int nNodes = _childNodes.size();
        for (int i = 0; i < nNodes; ++i) {
            LazyTreeNode node = (LazyTreeNode) _childNodes.get(i);
            if (equals(node.getUserObject(), o)) {
                index = i;
                break;
            }
        }
        return index;
    }

    public boolean isLeaf() {
        if (isDuplicate()) {
            return true;
        }
        ensureChildCountLoaded();
        return _childCount == 0;
    }

    private void loadChildObjects(Collection childObjects) {
        if (_childNodes == null) {
            _childNodes = new ArrayList();
        } else {
            _childNodes.clear();
        }
        Iterator i = childObjects.iterator();
        while (i.hasNext()) {
            Object child = i.next();
            TreeNode childNode;
            try {
                childNode = createNode(child);
            } catch (Exception e) {
              // TODO log error
              e.printStackTrace();
              childNode = createErrorNode(child);
            }
            _childNodes.add(childNode);
        }
        _childCount = _childNodes.size();
        // Collections.sort(childNodes, getComparator());
    }

    private void loadNodes() {
        Collection childObjects = getChildObjects();
        loadChildObjects(childObjects);
    }

    public void notifyChildNodeAdded(LazyTreeNode parent, int index, LazyTreeNode child) {
        if (_parent == null) {
            //Log.getLogger().warning("Notification message lost: " + child);
            System.out.println("Notification message lost: " + child);
        } else {
            _parent.notifyChildNodeAdded(parent, index, child);
        }
    }

    public void notifyChildNodeRemoved(LazyTreeNode parent, int index, LazyTreeNode child) {
        if (_parent == null) {
            //Log.getLogger().warning("Notification message lost: " + child);
            System.out.println("Notification message lost: " + child);
        } else {
            _parent.notifyChildNodeRemoved(parent, index, child);
        }
    }

    public void notifyNodeChanged(LazyTreeNode node) {
        if (_parent == null) {
            //Log.getLogger().warning("Notification message lost: " + node);
            System.out.println("Notification message lost: " + node);
        } else {
            _parent.notifyNodeChanged(node);
        }
    }

    public void notifyNodeStructureChanged(LazyTreeNode node) {
        if (_parent == null) {
            //Log.getLogger().warning("Notification message lost: " + node);
            System.out.println("Notification message lost: " + node);
        } else {
            _parent.notifyNodeStructureChanged(node);
        }
    }

    public void reload() {
        clearNodes();
        loadNodes();
        // should make correct notification call
        notifyNodeStructureChanged(this);
    }

    public void reload(Object userObject) {
        _userObject = userObject;
        reload();
    }

    public String toString() {
        return "LazyTreeNode(" + _userObject + ")";
    }

    public static boolean equals(Object o1, Object o2) {
        //return SystemUtilities.equals(o1, o2);
        return (o1 == null) ? o2 == null : o1.equals(o2);
    }
}