package org.hippoecm.frontend.tree;

import java.io.Serializable;
import java.util.*;

/**
 * Lazy tree node implementation if something goes wrong with the other lazy tree logic.  The common case when a call
 * to getChildCount() returns a larger number than getChildren().  This case results in a disaster for the tree where
 * nothing appears to work.  Instead we just create a node with some noticable text.
 */

class ErrorLazyTreeNode extends LazyTreeNode implements Serializable {

    ErrorLazyTreeNode(LazyTreeNode parent, Object o) {
        super(parent, "INVALID -- " + o);
    }

    public LazyTreeNode createNode(Object o) {
        return null;
    }

    public int getChildObjectCount() {
        return 0;
    }

    public Collection getChildObjects() {
        return Collections.EMPTY_SET;
    }

    public Comparator getComparator() {
        return null;
    }
}