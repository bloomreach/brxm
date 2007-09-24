package org.hippoecm.frontend.tree;

import java.io.Serializable;
import java.util.*;

import javax.swing.tree.*;

/**
 * TreeModel for holding LazyTreeRoots and LazyTreeNodes.
 *
 * @author    Ray Fergerson <fergerson@smi.stanford.edu>
 */
public class LazyTreeModel extends DefaultTreeModel implements Serializable {
    private static final long serialVersionUID = 1L;

    private static class EmptyRoot extends LazyTreeNode {
        private static final long serialVersionUID = 1L;

        EmptyRoot() {
            super(null, "EmptyRoot");
        }

        public LazyTreeNode createNode(Object o) {
            return null;
        }

        public Comparator getComparator() {
            return null;
        }

        protected int getChildObjectCount() {
            return 0;
        }

        protected Collection getChildObjects() {
            return null;
        }
    }

    public LazyTreeModel() {
        this(null);
    }

    public LazyTreeModel(LazyTreeNode root) {
        super(root == null ? new EmptyRoot() : root);
    }

    public void dispose() {
        removeRoot();
    }

    private LazyTreeNode getLazyTreeRoot() {
        return (LazyTreeNode) getRoot();
    }

    private void removeRoot() {
        getLazyTreeRoot().dispose();
    }

    public void setRoot(TreeNode root) {
        if (root == null) {
            root = new EmptyRoot();
        }
        removeRoot();
        super.setRoot(root);
    }
}