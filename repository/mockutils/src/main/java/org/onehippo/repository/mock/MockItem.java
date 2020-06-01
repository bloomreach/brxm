/*
 *  Copyright 2012-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.mock;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryManager;

/**
 * Mock version of a {@link Item}. Limitations:
 * <ul>
 *     <li>Item cannot be removed</li>
 *     <li>Saving changes is ignored</li>
 * </ul>
 * All methods that are not implemented throw an {@link UnsupportedOperationException}.
 */
public abstract class MockItem implements Item {

    private String name;
    private MockNode parent;
    private MockSession session;
    protected final QueryManager queryManager;

    public MockItem(String name) {
        this(name, null);
    }

    MockItem(String name, final QueryManager queryManager) {
        this.name = name;
        this.queryManager = queryManager;
    }

    @Override
    public String getName() {
        return isRootNode() ? "" : name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    protected boolean isRootNode() {
        return isNode() && parent == null;
    }

    void setParent(final MockNode parent) {
        this.parent = parent;
    }

    @Override
    public MockNode getParent() throws ItemNotFoundException {
        if (isRootNode() || parent == null) {
            throw new ItemNotFoundException("A root node does not have a parent");
        }
        return parent;
    }

    MockNode getParentOrNull() {
        return parent;
    }

    @Override
    public String getPath() throws RepositoryException {
        StringBuilder pathBuilder = new StringBuilder();
        buildPath(pathBuilder);
        return pathBuilder.toString();
    }

    void buildPath(StringBuilder builder) throws RepositoryException {
        if (isRootNode()) {
            builder.append('/');
        } else {
            parent.buildPath(builder);
            if (builder.charAt(builder.length() - 1) != '/') {
                builder.append('/');
            }
            builder.append(name);

            if (this instanceof MockNode) {
                int index = ((MockNode) this).getIndex();
                if (index > 1) {
                    builder.append('[').append(index).append(']');
                }
            }
        }
    }

    @Override
    public boolean isSame(final Item otherItem) {
        return equals(otherItem);
    }

    @Override
    public MockSession getSession() throws RepositoryException {
        if (session != null) {
            return session;
        }
        MockNode root = getRootNode();
        session = new MockSession(root, queryManager);
        return session;
    }

    MockNode getRootNode() {
        if (isRootNode()) {
            return (MockNode) this;
        }
        return parent.getRootNode();
    }

    @Override
    public int getDepth() throws RepositoryException {
        int depth = 0;
        MockNode cursor = parent;
        while (cursor != null) {
            depth += 1;
            cursor = cursor.getParentOrNull();
        }
        return depth;
    }

    @Override
    public void save() {
        // do nothing
    }

    @Override
    public void refresh(final boolean keepChanges) {
        // do nothing
    }

    // REMAINING METHODS ARE NOT IMPLEMENTED

    @Override
    public Item getAncestor(final int depth) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isNew() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isModified() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void accept(final ItemVisitor visitor) throws RepositoryException {
        if (this.isNode()) {
            visitor.visit((Node) this);
        } else {
            visitor.visit((Property) this);
        }
    }
}
