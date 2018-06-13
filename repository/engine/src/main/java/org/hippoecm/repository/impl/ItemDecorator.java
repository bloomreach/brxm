/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.impl;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;

public class ItemDecorator extends SessionBoundDecorator implements Item {

    protected final Item item;

    static ItemDecorator newItemDecorator(SessionDecorator session, Object item) throws UnsupportedOperationException {
        if (item instanceof Version) {
            return new VersionDecorator(session, (Version) item);
        } else if (item instanceof VersionHistory) {
            return new VersionHistoryDecorator(session, (VersionHistory) item);
        } else if (item instanceof Node) {
            return new NodeDecorator(session, (Node) item);
        } else if (item instanceof Property) {
            return new PropertyDecorator(session, (Property) item);
        } else if (item instanceof Item) {
            return new ItemDecorator(session, (Item)item);
        } else if (item == null) {
            return null;
        } else {
            throw new UnsupportedOperationException("No decorator available for item of type " + item.getClass());
        }
    }

    public static Item unwrap(final Item item) {
        if (item instanceof ItemDecorator) {
            return ((ItemDecorator) item).item;
        }
        return item;
    }

    ItemDecorator(final SessionDecorator session, final Item item) {
        super(session);
        this.item = unwrap(item);
    }

    public SessionDecorator getSession() throws RepositoryException {
        return session;
    }

    public String getPath() throws RepositoryException {
        return item.getPath();
    }

    public String getName() throws RepositoryException {
        return item.getName();
    }

    public ItemDecorator getAncestor(final int depth) throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        final Item ancestor = item.getAncestor(depth);
        return newItemDecorator(session, ancestor);
    }

    public Node getParent() throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        final Node parent = item.getParent();
        return NodeDecorator.newNodeDecorator(session, parent);
    }

    public int getDepth() throws RepositoryException {
        return item.getDepth();
    }

    public boolean isNode() {
        return item.isNode();
    }

    public boolean isNew() {
        return item.isNew();
    }

    public boolean isModified() {
        return item.isModified();
    }

    public boolean isSame(final Item otherItem) throws RepositoryException {
        return item.isSame(unwrap(otherItem));
    }

    public void accept(final ItemVisitor visitor) throws RepositoryException {
        item.accept(new ItemVisitorDecorator(session, visitor));
    }

    public void save() throws AccessDeniedException, ConstraintViolationException, InvalidItemStateException,
            ReferentialIntegrityException, VersionException, LockException, RepositoryException {
        item.save();
    }

    public void refresh(final boolean keepChanges) throws InvalidItemStateException, RepositoryException {
        item.refresh(keepChanges);
    }

    public void remove() throws VersionException, LockException, RepositoryException {
        item.remove();
    }

    public boolean equals(final Object obj) {
        if (obj instanceof ItemDecorator) {
            return item.equals(((ItemDecorator)obj).item);
        }
        return false;
    }

    public int hashCode() {
        return item.hashCode();
    }
}
