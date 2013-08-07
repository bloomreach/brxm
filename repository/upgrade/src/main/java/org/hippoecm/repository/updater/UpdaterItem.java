/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.updater;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

public abstract class UpdaterItem implements Item {

    Item origin;
    UpdaterNode parent;
    UpdaterSession session;

    UpdaterItem(UpdaterSession session, UpdaterNode target) {
        this.session = session;
        this.origin = null;
        this.parent = target;
    }

    UpdaterItem(UpdaterSession session, Item origin, UpdaterNode target) {
        this.session = session;
        this.origin = origin;
        this.parent = target;
    }

    public void setName(String name) throws RepositoryException {
        if (parent == null)
            throw new RepositoryException("cannot rename the root node");
        String oldName = parent.reverse.get(this);
        Iterator<UpdaterItem> iter = parent.children.get(oldName).iterator();
        for (int index = 0; iter.hasNext(); index++) {
            if (iter.next() == this) {
                iter.remove();
                if(parent.children.get(oldName).size() == 0) {
                    parent.children.remove(oldName);
                }
                List<UpdaterItem> siblings;
                if (parent.children.containsKey(name)) {
                    siblings = parent.children.get(name);
                } else {
                    siblings = new LinkedList<UpdaterItem>();
                }
                siblings.add(this);
                parent.children.put(name, siblings);
                parent.reverse.remove(this);
                if(name.contains("[")) {
                    name = name.substring(0, name.indexOf("["));
                }
                parent.reverse.put(this, name);
                return;
            }
        }
        throw new UpdaterException("internal error");
    }

    // javax.jcr.Item interface

    public String getPath() throws RepositoryException {
        if (getParent() != null) {
            String name = getName();
            String path = getParent().getPath();
            int index = isNode() ? ((Node) this).getIndex() : 1;
            if (index > 1)
                name += "[" + index + "]";
            if (path.endsWith("/"))
                return path + name;
            else
                return path + "/" + name;
        } else {
            return "/";
        }
    }

    public String getName() throws RepositoryException {
        if (parent == null)
            return "jcr:root";
        String name = parent.reverse.get(this);
        if(name != null) {
            if (name.startsWith(":")) {
                return name.substring(1);
            } else {
                return name;
            }
        } else {
            return origin.getName();
        }
    }

    public Item getAncestor(int depth) throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        Item ancestor;
        depth = getDepth() - depth;
        if (depth < 0)
            throw new ItemNotFoundException();
        for (ancestor = this; depth > 0; depth--)
            ancestor = getParent();
        return ancestor;
    }

    public Node getParent() throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        return (Node) parent;
    }

    public int getDepth() throws RepositoryException {
        if (parent != null)
            return parent.getDepth() + 1;
        else
            return 0;
    }

    public Session getSession() throws RepositoryException {
        return session;
    }

    public abstract boolean isNode();

    @Deprecated
    public boolean isNew() {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public boolean isModified() {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public boolean isSame(Item otherItem) throws RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public abstract void accept(ItemVisitor visitor) throws RepositoryException;

    @Deprecated
    public void save() throws AccessDeniedException, ItemExistsException, ConstraintViolationException, InvalidItemStateException, ReferentialIntegrityException, VersionException, LockException, NoSuchNodeTypeException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public void refresh(boolean keepChanges) throws InvalidItemStateException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public void remove() throws VersionException, LockException, ConstraintViolationException, RepositoryException {
        if(UpdaterEngine.log.isDebugEnabled()) {
            UpdaterEngine.log.debug("action remove "+getPath());
        }
        String name = parent.reverse.remove(this);
        if (parent.children.containsKey(name)) {
            Iterator<UpdaterItem> iter = parent.children.get(name).iterator();
            while (iter.hasNext()) {
                UpdaterItem item = iter.next();
                if (item == this) {
                    iter.remove();
                    parent.removed.add(this);
                    break;
                }
            }
            if (parent.children.get(name).size() == 0) {
                parent.children.remove(name);
            }
        }
    }
}
