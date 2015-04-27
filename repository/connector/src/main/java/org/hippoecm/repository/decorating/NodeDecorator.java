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
package org.hippoecm.repository.decorating;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Calendar;

import javax.jcr.AccessDeniedException;
import javax.jcr.Binary;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.MergeException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
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
import org.hippoecm.repository.api.HippoSession;

public abstract class NodeDecorator extends ItemDecorator implements HippoNode {

    protected final Node node;
    protected HippoSession session;

    protected NodeDecorator(DecoratorFactory factory, Session session, Node node) {
        super(factory, session, node);
        this.session = (HippoSession) session;
        this.node = node;
    }

    public static Node unwrap(Node node) {
        if (node instanceof NodeDecorator) {
            node = (Node) ((NodeDecorator) node).unwrap();
        }
        return node;
    }

    /**
     * @inheritDoc
     */
    public Node addNode(String name) throws ItemExistsException, PathNotFoundException, VersionException,
            ConstraintViolationException, LockException, RepositoryException {
        return factory.getNodeDecorator(session, node.addNode(name));
    }

    /**
     * @inheritDoc
     */
    public Node addNode(String name, String type) throws ItemExistsException, PathNotFoundException,
            NoSuchNodeTypeException, LockException, VersionException, ConstraintViolationException, RepositoryException {
        return factory.getNodeDecorator(session, node.addNode(name, type));
    }

    /**
     * @inheritDoc
     */
    public void orderBefore(String srcChildRelPath, String destChildRelPath)
            throws UnsupportedRepositoryOperationException, VersionException, ConstraintViolationException,
            ItemNotFoundException, LockException, RepositoryException {
        node.orderBefore(srcChildRelPath, destChildRelPath);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, Value value) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        Property prop = node.setProperty(name, value);
        return factory.getPropertyDecorator(session, prop);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, Value value, int type) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        Property prop = node.setProperty(name, value, type);
        return factory.getPropertyDecorator(session, prop);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, Value[] values) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        Property prop = node.setProperty(name, values);
        return factory.getPropertyDecorator(session, prop);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, Value[] values, int type) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        Property prop = node.setProperty(name, values, type);
        return factory.getPropertyDecorator(session, prop);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, String[] values) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        Property prop = node.setProperty(name, values);
        return factory.getPropertyDecorator(session, prop);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, String[] values, int type) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        Property prop = node.setProperty(name, values, type);
        return factory.getPropertyDecorator(session, prop);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, String value) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        Property prop = node.setProperty(name, value);
        return factory.getPropertyDecorator(session, prop);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, String value, int type) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        Property prop = node.setProperty(name, value, type);
        return factory.getPropertyDecorator(session, prop);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, InputStream value) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        Property prop = node.setProperty(name, value);
        return factory.getPropertyDecorator(session, prop);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, boolean value) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
         Property prop = node.setProperty(name, value);
        return factory.getPropertyDecorator(session, prop);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, double value) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        Property prop = node.setProperty(name, value);
        return factory.getPropertyDecorator(session, prop);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, long value) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        Property prop = node.setProperty(name, value);
        return factory.getPropertyDecorator(session, prop);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, Calendar value) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
         Property prop = node.setProperty(name, value);
        return factory.getPropertyDecorator(session, prop);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, Node value) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        Property prop = node.setProperty(name, NodeDecorator.unwrap(value));
        return factory.getPropertyDecorator(session, prop);
    }

    /**
     * @inheritDoc
     */
    public Node getNode(String relPath) throws PathNotFoundException, RepositoryException {
        return factory.getNodeDecorator(session, node.getNode(relPath));
    }

    /**
     * @inheritDoc
     */
    public NodeIterator getNodes() throws RepositoryException {
        NodeIterator iter = new NodeIteratorDecorator(factory, session, node.getNodes(), this);
        return iter;
    }

    /**
     * @inheritDoc
     */
    public NodeIterator getNodes(String namePattern) throws RepositoryException {
        NodeIterator iter = new NodeIteratorDecorator(factory, session, node.getNodes(namePattern), this);
        return iter;
    }

    /**
     * @inheritDoc
     */
    public Property getProperty(String relPath) throws PathNotFoundException, RepositoryException {
        Property prop = node.getProperty(relPath);
        return factory.getPropertyDecorator(session, prop);
    }

    /**
     * @inheritDoc
     */
    public PropertyIterator getProperties() throws RepositoryException {
        return new PropertyIteratorDecorator(factory, session, node.getProperties());
    }

    /**
     * @inheritDoc
     */
    public PropertyIterator getProperties(String namePattern) throws RepositoryException {
        return new PropertyIteratorDecorator(factory, session, node.getProperties(namePattern));
    }

    /**
     * @inheritDoc
     */
    public Item getPrimaryItem() throws ItemNotFoundException, RepositoryException {
        return factory.getItemDecorator(session, node.getPrimaryItem());
    }

    /**
     * @inheritDoc
     */
    public String getUUID() throws UnsupportedRepositoryOperationException, RepositoryException {
        return node.getUUID();
    }

    /**
     * @inheritDoc
     */
    public int getIndex() throws RepositoryException {
        return node.getIndex();
    }

    /**
     * @inheritDoc
     */
    public PropertyIterator getReferences() throws RepositoryException {
        return new PropertyIteratorDecorator(factory, session, node.getReferences());
    }

    /**
     * @inheritDoc
     */
    public boolean hasNode(String relPath) throws RepositoryException {
                    return node.hasNode(relPath);
    }

    /**
     * @inheritDoc
     */
    public boolean hasProperty(String relPath) throws RepositoryException {
        return node.hasProperty(relPath);
    }

    /**
     * @inheritDoc
     */
    public boolean hasNodes() throws RepositoryException {
        return node.hasNodes();
    }

    /**
     * @inheritDoc
     */
    public boolean hasProperties() throws RepositoryException {
        return node.hasProperties();
    }

    /**
     * @inheritDoc
     */
    public NodeType getPrimaryNodeType() throws RepositoryException {
        return node.getPrimaryNodeType();
    }

    /**
     * @inheritDoc
     */
    public NodeType[] getMixinNodeTypes() throws RepositoryException {
        return node.getMixinNodeTypes();
    }

    /**
     * @inheritDoc
     */
    public boolean isNodeType(String nodeTypeName) throws RepositoryException {
        return node.isNodeType(nodeTypeName);
    }

    /**
     * @inheritDoc
     */
    public void addMixin(String mixinName) throws NoSuchNodeTypeException, VersionException,
            ConstraintViolationException, LockException, RepositoryException {
        node.addMixin(mixinName);
    }

    /**
     * @inheritDoc
     */
    public void removeMixin(String mixinName) throws NoSuchNodeTypeException, VersionException,
            ConstraintViolationException, LockException, RepositoryException {
        node.removeMixin(mixinName);
    }

    /**
     * @inheritDoc
     */
    public boolean canAddMixin(String mixinName) throws NoSuchNodeTypeException, RepositoryException {
        return node.canAddMixin(mixinName);
    }

    /**
     * @inheritDoc
     */
    public NodeDefinition getDefinition() throws RepositoryException {
        return node.getDefinition();
    }

    /**
     * @inheritDoc
     */
    public Version checkin() throws VersionException, UnsupportedRepositoryOperationException,
            InvalidItemStateException, LockException, RepositoryException {
        Version version = node.checkin();
        return factory.getVersionDecorator(session, version);
    }

    /**
     * @inheritDoc
     */
    public void checkout() throws UnsupportedRepositoryOperationException, LockException, RepositoryException {
        node.checkout();
    }

    /**
     * @inheritDoc
     */
    public void doneMerge(Version version) throws VersionException, InvalidItemStateException,
            UnsupportedRepositoryOperationException, RepositoryException {
        node.doneMerge(VersionDecorator.unwrap(version));
    }

    /**
     * @inheritDoc
     */
    public void cancelMerge(Version version) throws VersionException, InvalidItemStateException,
            UnsupportedRepositoryOperationException, RepositoryException {
        node.cancelMerge(VersionDecorator.unwrap(version));
    }

    /**
     * @inheritDoc
     */
    public void update(String srcWorkspaceName) throws NoSuchWorkspaceException, AccessDeniedException, LockException,
            InvalidItemStateException, RepositoryException {
        node.update(srcWorkspaceName);
    }

    /**
     * @inheritDoc
     */
    public NodeIterator merge(String srcWorkspace, boolean bestEffort) throws NoSuchWorkspaceException,
            AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
        NodeIterator nodes = node.merge(srcWorkspace, bestEffort);
        return new NodeIteratorDecorator(factory, session, nodes);
    }

    /**
     * @inheritDoc
     */
    public String getCorrespondingNodePath(String workspaceName) throws ItemNotFoundException,
            NoSuchWorkspaceException, AccessDeniedException, RepositoryException {
        return node.getCorrespondingNodePath(workspaceName);
    }

    /**
     * @inheritDoc
     */
    public boolean isCheckedOut() throws RepositoryException {
        return node.isCheckedOut();
    }

    /**
     * @inheritDoc
     */
    public void restore(String versionName, boolean removeExisting) throws VersionException, ItemExistsException,
            UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        node.restore(versionName, removeExisting);
    }

    /**
     * @inheritDoc
     */
    public void restore(Version version, boolean removeExisting) throws VersionException, ItemExistsException,
            UnsupportedRepositoryOperationException, LockException, RepositoryException {
        node.restore(VersionDecorator.unwrap(version), removeExisting);
    }

    /**
     * @inheritDoc
     */
    public void restore(Version version, String relPath, boolean removeExisting) throws PathNotFoundException,
            ItemExistsException, VersionException, ConstraintViolationException,
            UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        node.restore(VersionDecorator.unwrap(version), relPath, removeExisting);
    }

    /**
     * @inheritDoc
     */
    public void restoreByLabel(String versionLabel, boolean removeExisting) throws VersionException,
            ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException,
            RepositoryException {
        node.restoreByLabel(versionLabel, removeExisting);
    }

    /**
     * @inheritDoc
     */
    public VersionHistory getVersionHistory() throws UnsupportedRepositoryOperationException, RepositoryException {
        VersionHistory hist = node.getVersionHistory();
        return factory.getVersionHistoryDecorator(session, hist);
    }

    /**
     * @inheritDoc
     */
    public Version getBaseVersion() throws UnsupportedRepositoryOperationException, RepositoryException {
        return factory.getVersionDecorator(session, node.getBaseVersion());
    }

    /**
     * @inheritDoc
     */
    public Lock lock(boolean isDeep, boolean isSessionScoped) throws UnsupportedRepositoryOperationException,
            LockException, AccessDeniedException, InvalidItemStateException, RepositoryException {
        final LockManager lockManager = session.getWorkspace().getLockManager();
        return lockManager.lock(getPath(), isDeep, isSessionScoped, Long.MAX_VALUE, null);
    }

    /**
     * @inheritDoc
     */
    public Lock getLock() throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException,
            RepositoryException {
        final LockManager lockManager = session.getWorkspace().getLockManager();
        return lockManager.getLock(getPath());
    }

    /**
     * @inheritDoc
     */
    public void unlock() throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException,
            InvalidItemStateException, RepositoryException {
        final LockManager lockManager = session.getWorkspace().getLockManager();
        lockManager.unlock(getPath());
    }

    /**
     * @inheritDoc
     */
    public boolean holdsLock() throws RepositoryException {
        final LockManager lockManager = session.getWorkspace().getLockManager();
        return lockManager.holdsLock(node.getPath());
    }

    /**
     * @inheritDoc
     */
    public boolean isLocked() throws RepositoryException {
        final LockManager lockManager = session.getWorkspace().getLockManager();
        return lockManager.isLocked(getPath());
    }

    /**
     * @inheritDoc
     */
    public NodeIterator pendingChanges(String nodeType, boolean prune) throws RepositoryException {
        return session.pendingChanges(this, nodeType, prune);
    }

    /**
     * @inheritDoc
     */
    public NodeIterator pendingChanges(String nodeType) throws RepositoryException {
        return session.pendingChanges(this, nodeType);
    }

    /**
     * @inheritDoc
     */
    public NodeIterator pendingChanges() throws RepositoryException {
        return session.pendingChanges(this, null);
    }

    /**
     * @inheritDoc
     */
    public void refresh(boolean keepChanges) throws InvalidItemStateException, RepositoryException {
        if (keepChanges == false && getDepth() == 0) {
            session.refresh(keepChanges);
            return;
        }
        super.refresh(keepChanges);
    }


    public Property setProperty(String name, Binary value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return node.setProperty(name, value);
    }

    public Property setProperty(String name, BigDecimal value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return node.setProperty(name, value);
    }

    public NodeIterator getNodes(String[] nameGlobs) throws RepositoryException {
        return node.getNodes(nameGlobs);
    }

    public PropertyIterator getProperties(String[] nameGlobs) throws RepositoryException {
        return node.getProperties(nameGlobs);
    }

    public String getIdentifier() throws RepositoryException {
        return node.getIdentifier();
    }

    public PropertyIterator getReferences(String name) throws RepositoryException {
        return node.getReferences(name);
    }

    public PropertyIterator getWeakReferences() throws RepositoryException {
        return node.getWeakReferences();
    }

    public PropertyIterator getWeakReferences(String name) throws RepositoryException {
        return node.getWeakReferences(name);
    }

    public void setPrimaryType(String nodeTypeName) throws NoSuchNodeTypeException, VersionException, ConstraintViolationException, LockException, RepositoryException {
        node.setPrimaryType(nodeTypeName);
    }

    public NodeIterator getSharedSet() throws RepositoryException {
        return node.getSharedSet();
    }

    public void removeSharedSet() throws VersionException, LockException, ConstraintViolationException, RepositoryException {
        node.removeSharedSet();
    }

    public void removeShare() throws VersionException, LockException, ConstraintViolationException, RepositoryException {
        node.removeShare();
    }

    public void followLifecycleTransition(String transition) throws UnsupportedRepositoryOperationException, RepositoryException {
        node.followLifecycleTransition(transition);
    }

    public String[] getAllowedLifecycleTransistions() throws UnsupportedRepositoryOperationException, RepositoryException {
        return node.getAllowedLifecycleTransistions();
    }
}
