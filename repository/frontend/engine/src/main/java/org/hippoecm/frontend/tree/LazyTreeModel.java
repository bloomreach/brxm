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

import java.io.Serializable;
import java.util.*;

import javax.swing.tree.*;

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