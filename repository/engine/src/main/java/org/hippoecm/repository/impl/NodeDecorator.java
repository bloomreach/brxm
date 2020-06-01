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

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Calendar;

import javax.jcr.AccessDeniedException;
import javax.jcr.Binary;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.MergeException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyIterator;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.lock.LockManager;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;

import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.deriveddata.DerivedDataEngine;

import static org.onehippo.repository.util.JcrConstants.ROOT_NODE_ID;

public class NodeDecorator extends ItemDecorator implements HippoNode {

    protected final Node node;

    static NodeDecorator newNodeDecorator(SessionDecorator session, Node node) {
        if (node instanceof Version) {
            return new VersionDecorator(session, (Version) node);
        } else if (node instanceof VersionHistory) {
            return new VersionHistoryDecorator(session, (VersionHistory) node);
        } else if (node == null) {
            return null;
        } else {
            return new NodeDecorator(session, node);
        }
    }

    public static Node unwrap(final Node node) {
        if (node instanceof NodeDecorator) {
            return ((NodeDecorator) node).node;
        }
        return node;
    }

    NodeDecorator(final SessionDecorator session, Node node) {
        super(session, node);
        this.node = unwrap(node);
    }

    public NodeDecorator addNode(final String name) throws ItemExistsException, PathNotFoundException, VersionException,
            ConstraintViolationException, LockException, RepositoryException {
        return newNodeDecorator(session, node.addNode(name));
    }

    public NodeDecorator addNode(final String name, final String type) throws ItemExistsException, PathNotFoundException,
            NoSuchNodeTypeException, LockException, VersionException, ConstraintViolationException, RepositoryException {
        return newNodeDecorator(session, node.addNode(name, type));
    }

    public PropertyDecorator setProperty(final String name, final Value value) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        return new PropertyDecorator(session, node.setProperty(name, value));
    }

    public PropertyDecorator setProperty(final String name, final Value value, final int type) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        return new PropertyDecorator(session, node.setProperty(name, value, type));
    }

    public PropertyDecorator setProperty(final String name, final Value[] values) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        return new PropertyDecorator(session, node.setProperty(name, values));
    }

    public PropertyDecorator setProperty(final String name, final Value[] values, final int type) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        return new PropertyDecorator(session, node.setProperty(name, values, type));
    }

    public PropertyDecorator setProperty(final String name, final String[] values) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        return new PropertyDecorator(session, node.setProperty(name, values));
    }

    public PropertyDecorator setProperty(final String name, final String[] values, final int type) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        return new PropertyDecorator(session, node.setProperty(name, values, type));
    }

    public PropertyDecorator setProperty(final String name, final String value) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        return new PropertyDecorator(session, node.setProperty(name, value));
    }

    public PropertyDecorator setProperty(final String name, final String value, final int type) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        return new PropertyDecorator(session, node.setProperty(name, value, type));
    }

    public PropertyDecorator setProperty(final String name, final InputStream value) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        return new PropertyDecorator(session, node.setProperty(name, value));
    }

    public PropertyDecorator setProperty(final String name, final boolean value) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        return new PropertyDecorator(session, node.setProperty(name, value));
    }

    public PropertyDecorator setProperty(final String name, final double value) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        return new PropertyDecorator(session, node.setProperty(name, value));
    }

    public PropertyDecorator setProperty(final String name, final long value) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        return new PropertyDecorator(session, node.setProperty(name, value));
    }

    public PropertyDecorator setProperty(final String name, final Calendar value) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        return new PropertyDecorator(session, node.setProperty(name, value));
    }

    public PropertyDecorator setProperty(final String name, final Node value) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        return new PropertyDecorator(session, node.setProperty(name, NodeDecorator.unwrap(value)));
    }

    public NodeDecorator getNode(final String relPath) throws PathNotFoundException, RepositoryException {
        return newNodeDecorator(session, node.getNode(relPath));
    }

    public NodeIteratorDecorator getNodes() throws RepositoryException {
        return new NodeIteratorDecorator(session, node.getNodes());
    }

    public NodeIteratorDecorator getNodes(final String namePattern) throws RepositoryException {
        return new NodeIteratorDecorator(session, node.getNodes(namePattern));
    }

    public PropertyDecorator getProperty(final String relPath) throws PathNotFoundException, RepositoryException {
        return new PropertyDecorator(session, node.getProperty(relPath));
    }

    public PropertyIteratorDecorator getProperties() throws RepositoryException {
        return new PropertyIteratorDecorator(session, node.getProperties());
    }

    public PropertyIteratorDecorator getProperties(final String namePattern) throws RepositoryException {
        return new PropertyIteratorDecorator(session, node.getProperties(namePattern));
    }

    public ItemDecorator getPrimaryItem() throws ItemNotFoundException, RepositoryException {
        return ItemDecorator.newItemDecorator(session, node.getPrimaryItem());
    }

    public String getUUID() throws UnsupportedRepositoryOperationException, RepositoryException {
        return node.getUUID();
    }

    public int getIndex() throws RepositoryException {
        return node.getIndex();
    }

    public PropertyIterator getReferences() throws RepositoryException {
        return new PropertyIteratorDecorator(session, node.getReferences());
    }

    public boolean hasNode(final String relPath) throws RepositoryException {
        return node.hasNode(relPath);
    }

    public boolean hasProperty(final String relPath) throws RepositoryException {
        return node.hasProperty(relPath);
    }

    public boolean hasNodes() throws RepositoryException {
        return node.hasNodes();
    }

    public boolean hasProperties() throws RepositoryException {
        return node.hasProperties();
    }

    public NodeType getPrimaryNodeType() throws RepositoryException {
        return node.getPrimaryNodeType();
    }

    public NodeType[] getMixinNodeTypes() throws RepositoryException {
        return node.getMixinNodeTypes();
    }

    public boolean isNodeType(final String nodeTypeName) throws RepositoryException {
        return node.isNodeType(nodeTypeName);
    }

    public void addMixin(final String mixinName) throws NoSuchNodeTypeException, VersionException,
            ConstraintViolationException, LockException, RepositoryException {
        node.addMixin(mixinName);
    }

    public NodeDecorator getCanonicalNode() throws RepositoryException {
        // Note that HREPTWO-2127 is still unresolved, even though the
        // previous implementation did have problems with it, but the current
        // implementation hasn't.  The point is that if you try to perform a
        // hasProperty you do not have the same view as with getProperty,
        // which is wrong.
        return newNodeDecorator(session, session.getCanonicalNode(node));
    }

    @Override
    public boolean isVirtual() throws RepositoryException {
        return getIdentifier().startsWith("cafeface");
    }

    @Override
    public boolean recomputeDerivedData() throws RepositoryException {
        return session.computeDerivedData(node);
    }

    @Override
    public void save() throws AccessDeniedException, ConstraintViolationException, InvalidItemStateException,
            ReferentialIntegrityException, VersionException, LockException, RepositoryException {
        session.postSave(node);
        super.save();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove() throws VersionException, LockException, RepositoryException {
        try {
            session.postMountEnabled(false);
            DerivedDataEngine.removal(this);
            super.remove();
        } finally {
            session.postMountEnabled(true);
        }
    }

    public VersionDecorator checkin() throws VersionException, UnsupportedRepositoryOperationException,
            InvalidItemStateException, LockException, RepositoryException {
        try {
            session.postMountEnabled(false);
            return new VersionDecorator(session, node.checkin());
        } finally {
            session.postMountEnabled(true);
        }
    }

    public void checkout() throws UnsupportedRepositoryOperationException, LockException, RepositoryException {
        if(!isCheckedOut()) {
            try {
                session.postMountEnabled(false);
                session.postRefreshEnabled(false);
                node.checkout();
            } finally {
                session.postMountEnabled(true);
                session.postRefreshEnabled(true);
            }
        }
    }

    public void removeMixin(final String mixinName) throws NoSuchNodeTypeException, VersionException,
                                                     ConstraintViolationException, LockException, RepositoryException {
        try {
            session.postMountEnabled(false);
            node.removeMixin(mixinName);
        } finally {
            session.postMountEnabled(true);
        }
    }

    public boolean canAddMixin(final String mixinName) throws NoSuchNodeTypeException, RepositoryException {
        return node.canAddMixin(mixinName);
    }

    public NodeDefinition getDefinition() throws RepositoryException {
        return node.getDefinition();
    }

    public void orderBefore(final String srcChildRelPath, final String destChildRelPath)
            throws UnsupportedRepositoryOperationException, VersionException, ConstraintViolationException,
            ItemNotFoundException, LockException, RepositoryException {
        try {
            session.postMountEnabled(false);
            node.orderBefore(srcChildRelPath, destChildRelPath);
        } finally {
            session.postMountEnabled(true);
        }
    }

    public void doneMerge(final Version version) throws VersionException, InvalidItemStateException,
            UnsupportedRepositoryOperationException, RepositoryException {
        node.doneMerge(VersionDecorator.unwrap(version));
    }

    public void cancelMerge(final Version version) throws VersionException, InvalidItemStateException,
            UnsupportedRepositoryOperationException, RepositoryException {
        node.cancelMerge(VersionDecorator.unwrap(version));
    }

    public void update(final String srcWorkspaceName) throws NoSuchWorkspaceException, AccessDeniedException, LockException,
            InvalidItemStateException, RepositoryException {
        node.update(srcWorkspaceName);
    }

    public NodeIteratorDecorator merge(final String srcWorkspace, final boolean bestEffort) throws NoSuchWorkspaceException,
            AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
        try {
            session.postMountEnabled(false);
            return new NodeIteratorDecorator(session, node.merge(srcWorkspace, bestEffort));
        } finally {
            session.postMountEnabled(true);
        }
    }

    public String getCorrespondingNodePath(final String workspaceName) throws ItemNotFoundException,
            NoSuchWorkspaceException, AccessDeniedException, RepositoryException {
        return node.getCorrespondingNodePath(workspaceName);
    }

    public boolean isCheckedOut() throws RepositoryException {
        return node.isCheckedOut();
    }

    public void restore(final String versionName, final boolean removeExisting) throws VersionException, ItemExistsException,
            UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        try {
            session.postMountEnabled(false);
            node.restore(versionName, removeExisting);
        } finally {
            session.postMountEnabled(true);
        }
    }

    public void restore(final Version version, final boolean removeExisting) throws VersionException, ItemExistsException,
            UnsupportedRepositoryOperationException, LockException, RepositoryException {
        try {
            session.postMountEnabled(false);
            node.restore(VersionDecorator.unwrap(version), removeExisting);
        } finally {
            session.postMountEnabled(true);
        }
    }

    public void restore(final Version version, final String relPath, final boolean removeExisting) throws PathNotFoundException,
            ItemExistsException, VersionException, ConstraintViolationException,
            UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        try {
            session.postMountEnabled(false);
            node.restore(VersionDecorator.unwrap(version), relPath, removeExisting);
        } finally {
            session.postMountEnabled(true);
        }
    }

    public void restoreByLabel(final String versionLabel, final boolean removeExisting) throws VersionException,
            ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException,
            RepositoryException {
        try {
            session.postMountEnabled(false);
            node.restoreByLabel(versionLabel, removeExisting);
        } finally {
            session.postMountEnabled(true);
        }
    }

    public VersionHistoryDecorator getVersionHistory() throws UnsupportedRepositoryOperationException, RepositoryException {
        return new VersionHistoryDecorator(session, node.getVersionHistory());
    }

    public VersionDecorator getBaseVersion() throws UnsupportedRepositoryOperationException, RepositoryException {
        return new VersionDecorator(session, node.getBaseVersion());
    }

    public Lock lock(final boolean isDeep, final boolean isSessionScoped) throws UnsupportedRepositoryOperationException,
            LockException, AccessDeniedException, InvalidItemStateException, RepositoryException {
        final LockManager lockManager = session.getWorkspace().getLockManager();
        return lockManager.lock(getPath(), isDeep, isSessionScoped, Long.MAX_VALUE, null);
    }

    public Lock getLock() throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException,
            RepositoryException {
        final LockManager lockManager = session.getWorkspace().getLockManager();
        return lockManager.getLock(getPath());
    }

    public void unlock() throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException,
            InvalidItemStateException, RepositoryException {
        final LockManager lockManager = session.getWorkspace().getLockManager();
        lockManager.unlock(getPath());
    }

    public boolean holdsLock() throws RepositoryException {
        final LockManager lockManager = session.getWorkspace().getLockManager();
        return lockManager.holdsLock(node.getPath());
    }

    public boolean isLocked() throws RepositoryException {
        final LockManager lockManager = session.getWorkspace().getLockManager();
        return lockManager.isLocked(getPath());
    }

    public NodeIteratorDecorator pendingChanges(final String nodeType, final boolean prune) throws RepositoryException {
        return session.pendingChanges(this, nodeType, prune);
    }

    public NodeIteratorDecorator pendingChanges(final String nodeType) throws RepositoryException {
        return session.pendingChanges(this, nodeType);
    }

    public NodeIteratorDecorator pendingChanges() throws RepositoryException {
        return session.pendingChanges(this, null);
    }

    public void refresh(final boolean keepChanges) throws InvalidItemStateException, RepositoryException {
        if (keepChanges == false && getDepth() == 0) {
            session.refresh(keepChanges);
            return;
        }
        super.refresh(keepChanges);
    }


    public PropertyDecorator setProperty(final String name, final Binary value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return new PropertyDecorator(session, node.setProperty(name, value));
    }

    public PropertyDecorator setProperty(final String name, final BigDecimal value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return new PropertyDecorator(session, node.setProperty(name, value));
    }

    public NodeIteratorDecorator getNodes(final String[] nameGlobs) throws RepositoryException {
        return new NodeIteratorDecorator(session, node.getNodes(nameGlobs));
    }

    public PropertyIteratorDecorator getProperties(final String[] nameGlobs) throws RepositoryException {
        return new PropertyIteratorDecorator(session, node.getProperties(nameGlobs));
    }

    public String getIdentifier() throws RepositoryException {
        return node.getIdentifier();
    }

    public PropertyIteratorDecorator getReferences(final String name) throws RepositoryException {
        return new PropertyIteratorDecorator(session, node.getReferences(name));
    }

    public PropertyIteratorDecorator getWeakReferences() throws RepositoryException {
        return new PropertyIteratorDecorator(session, node.getWeakReferences());
    }

    public PropertyIteratorDecorator getWeakReferences(final String name) throws RepositoryException {
        return new PropertyIteratorDecorator(session, node.getWeakReferences(name));
    }

    public void setPrimaryType(final String nodeTypeName) throws NoSuchNodeTypeException, VersionException, ConstraintViolationException, LockException, RepositoryException {
        node.setPrimaryType(nodeTypeName);
    }

    public NodeIteratorDecorator getSharedSet() throws RepositoryException {
        return new NodeIteratorDecorator(session, node.getSharedSet());
    }

    public void removeSharedSet() throws VersionException, LockException, ConstraintViolationException, RepositoryException {
        node.removeSharedSet();
    }

    public void removeShare() throws VersionException, LockException, ConstraintViolationException, RepositoryException {
        node.removeShare();
    }

    public void followLifecycleTransition(final String transition) throws UnsupportedRepositoryOperationException, RepositoryException {
        node.followLifecycleTransition(transition);
    }

    public String[] getAllowedLifecycleTransistions() throws UnsupportedRepositoryOperationException, RepositoryException {
        return node.getAllowedLifecycleTransistions();
    }

    @Override
    public String getDisplayName() throws RepositoryException {
        Node node = this;
        if (isVirtual()) {
            node = getCanonicalNode();
            if (node == null) {
                return getName();
            }
        }
        if (!node.isNodeType(HippoNodeType.NT_NAMED)) {
            if (ROOT_NODE_ID.equals(node.getIdentifier())) {
                return getName();
            }
            final Node parent = node.getParent();
            if (parent.isNodeType(HippoNodeType.NT_HANDLE) && parent.isNodeType(HippoNodeType.NT_NAMED)) {
                node = parent;
            }
            else {
                return getName();
            }
        }
        return node.getProperty(HippoNodeType.HIPPO_NAME).getString();
    }
}
