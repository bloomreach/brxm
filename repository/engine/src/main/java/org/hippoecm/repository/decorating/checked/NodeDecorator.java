/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.repository.decorating.checked;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Calendar;

import javax.jcr.AccessDeniedException;
import javax.jcr.Binary;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidLifecycleTransitionException;
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
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;

import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.Localized;

public class NodeDecorator extends ItemDecorator implements HippoNode {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    protected HippoNode node;
    protected SessionDecorator session;
    private String nodeIdentifier = null;

    protected NodeDecorator(DecoratorFactory factory, SessionDecorator session, HippoNode node) {
        super(factory, session, node);
        this.session = session;
        this.node = node;

        try {
            nodeIdentifier = node.getIdentifier();
        } catch (RepositoryException ex) {
        }
    }

    @Override
    protected void repair(Session upstreamSession) throws RepositoryException {
        node = (HippoNode) upstreamSession.getNodeByIdentifier(nodeIdentifier);
        item = node;
    }

    public Node getCanonicalNode() throws RepositoryException {
        check();
        Node canonical = node.getCanonicalNode();
        if (canonical != null) {
            return factory.getNodeDecorator(session, canonical);
        } else {
            return null;
        }
    }

    public String getLocalizedName() throws RepositoryException {
        check();
        return node.getLocalizedName();
    }

    public String getLocalizedName(Localized localized) throws RepositoryException {
        check();
        return node.getLocalizedName(localized);
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
        check();
        return factory.getNodeDecorator(session, node.addNode(name));
    }

    /**
     * @inheritDoc
     */
    public Node addNode(String name, String type) throws ItemExistsException, PathNotFoundException,
            NoSuchNodeTypeException, LockException, VersionException, ConstraintViolationException, RepositoryException {
        check();
        return factory.getNodeDecorator(session, node.addNode(name, type));
    }

    /**
     * @inheritDoc
     */
    public void orderBefore(String srcChildRelPath, String destChildRelPath)
            throws UnsupportedRepositoryOperationException, VersionException, ConstraintViolationException,
            ItemNotFoundException, LockException, RepositoryException {
        check();
        node.orderBefore(srcChildRelPath, destChildRelPath);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, Value value) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        check();
        Property prop = node.setProperty(name, value);
        return factory.getPropertyDecorator(session, prop, this);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, Value value, int type) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        check();
        Property prop = node.setProperty(name, value, type);
        return factory.getPropertyDecorator(session, prop, this);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, Value[] values) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        check();
        Property prop = node.setProperty(name, values);
        return factory.getPropertyDecorator(session, prop, this);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, Value[] values, int type) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        check();
        Property prop = node.setProperty(name, values, type);
        return factory.getPropertyDecorator(session, prop, this);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, String[] values) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        check();
        Property prop = node.setProperty(name, values);
        return factory.getPropertyDecorator(session, prop, this);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, String[] values, int type) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        check();
        Property prop = node.setProperty(name, values, type);
        return factory.getPropertyDecorator(session, prop, this);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, String value) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        check();
        Property prop = node.setProperty(name, value);
        return factory.getPropertyDecorator(session, prop, this);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, String value, int type) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        check();
        Property prop = node.setProperty(name, value, type);
        return factory.getPropertyDecorator(session, prop, this);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, InputStream value) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        check();
        Property prop = node.setProperty(name, value);
        return factory.getPropertyDecorator(session, prop, this);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, boolean value) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        check();
        Property prop = node.setProperty(name, value);
        return factory.getPropertyDecorator(session, prop, this);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, double value) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        check();
        Property prop = node.setProperty(name, value);
        return factory.getPropertyDecorator(session, prop, this);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, long value) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        check();
        Property prop = node.setProperty(name, value);
        return factory.getPropertyDecorator(session, prop, this);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, Calendar value) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        check();
        Property prop = node.setProperty(name, value);
        return factory.getPropertyDecorator(session, prop, this);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, Node value) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        check();
        Property prop = node.setProperty(name, NodeDecorator.unwrap(value));
        return factory.getPropertyDecorator(session, prop, this);
    }

    /**
     * @inheritDoc
     */
    public Node getNode(String relPath) throws PathNotFoundException, RepositoryException {
        check();
        return factory.getNodeDecorator(session, node.getNode(relPath));
    }

    /**
     * @inheritDoc
     */
    public NodeIterator getNodes() throws RepositoryException {
        check();
        NodeIterator iter = new NodeIteratorDecorator(factory, session, node.getNodes(), this);
        return iter;
    }

    /**
     * @inheritDoc
     */
    public NodeIterator getNodes(String namePattern) throws RepositoryException {
        check();
        NodeIterator iter = new NodeIteratorDecorator(factory, session, node.getNodes(namePattern), this);
        return iter;
    }

    /**
     * @inheritDoc
     */
    public Property getProperty(String relPath) throws PathNotFoundException, RepositoryException {
        check();
        Property prop = node.getProperty(relPath);
        return factory.getPropertyDecorator(session, prop, this);
    }

    /**
     * @inheritDoc
     */
    public PropertyIterator getProperties() throws RepositoryException {
        check();
        return new PropertyIteratorDecorator(factory, session, node.getProperties(), this);
    }

    /**
     * @inheritDoc
     */
    public PropertyIterator getProperties(String namePattern) throws RepositoryException {
        check();
        return new PropertyIteratorDecorator(factory, session, node.getProperties(namePattern), this);
    }

    /**
     * @inheritDoc
     */
    public Item getPrimaryItem() throws ItemNotFoundException, RepositoryException {
        check();
        return factory.getItemDecorator(session, node.getPrimaryItem());
    }

    /**
     * @inheritDoc
     */
    public String getUUID() throws UnsupportedRepositoryOperationException, RepositoryException {
        check();
        return node.getUUID();
    }

    /**
     * @inheritDoc
     */
    public int getIndex() throws RepositoryException {
        check();
        return node.getIndex();
    }

    /**
     * @inheritDoc
     */
    public PropertyIterator getReferences() throws RepositoryException {
        check();
        return new PropertyIteratorDecorator(factory, session, node.getReferences()); // note: do not pass this as fourth arg
    }

    /**
     * @inheritDoc
     */
    public boolean hasNode(String relPath) throws RepositoryException {
        check();
        return node.hasNode(relPath);
    }

    /**
     * @inheritDoc
     */
    public boolean hasProperty(String relPath) throws RepositoryException {
        check();
        return node.hasProperty(relPath);
    }

    /**
     * @inheritDoc
     */
    public boolean hasNodes() throws RepositoryException {
        check();
        return node.hasNodes();
    }

    /**
     * @inheritDoc
     */
    public boolean hasProperties() throws RepositoryException {
        check();
        return node.hasProperties();
    }

    /**
     * @inheritDoc
     */
    public NodeType getPrimaryNodeType() throws RepositoryException {
        check();
        return node.getPrimaryNodeType();
    }

    /**
     * @inheritDoc
     */
    public NodeType[] getMixinNodeTypes() throws RepositoryException {
        check();
        return node.getMixinNodeTypes();
    }

    /**
     * @inheritDoc
     */
    public boolean isNodeType(String nodeTypeName) throws RepositoryException {
        check();
        return node.isNodeType(nodeTypeName);
    }

    /**
     * @inheritDoc
     */
    public void addMixin(String mixinName) throws NoSuchNodeTypeException, VersionException,
            ConstraintViolationException, LockException, RepositoryException {
        check();
        node.addMixin(mixinName);
    }

    /**
     * @inheritDoc
     */
    public void removeMixin(String mixinName) throws NoSuchNodeTypeException, VersionException,
            ConstraintViolationException, LockException, RepositoryException {
        check();
        node.removeMixin(mixinName);
    }

    /**
     * @inheritDoc
     */
    public boolean canAddMixin(String mixinName) throws NoSuchNodeTypeException, RepositoryException {
        check();
        return node.canAddMixin(mixinName);
    }

    /**
     * @inheritDoc
     */
    public NodeDefinition getDefinition() throws RepositoryException {
        check();
        return node.getDefinition();
    }

    /**
     * @inheritDoc
     */
    public Version checkin() throws VersionException, UnsupportedRepositoryOperationException,
            InvalidItemStateException, LockException, RepositoryException {
        check();
        Version version = node.checkin();
        return factory.getVersionDecorator(session, version);
    }

    /**
     * @inheritDoc
     */
    public void checkout() throws UnsupportedRepositoryOperationException, LockException, RepositoryException {
        check();
        node.checkout();
    }

    /**
     * @inheritDoc
     */
    public void doneMerge(Version version) throws VersionException, InvalidItemStateException,
            UnsupportedRepositoryOperationException, RepositoryException {
        check();
        node.doneMerge(VersionDecorator.unwrap(version));
    }

    /**
     * @inheritDoc
     */
    public void cancelMerge(Version version) throws VersionException, InvalidItemStateException,
            UnsupportedRepositoryOperationException, RepositoryException {
        check();
        node.cancelMerge(VersionDecorator.unwrap(version));
    }

    /**
     * @inheritDoc
     */
    public void update(String srcWorkspaceName) throws NoSuchWorkspaceException, AccessDeniedException, LockException,
            InvalidItemStateException, RepositoryException {
        check();
        node.update(srcWorkspaceName);
    }

    /**
     * @inheritDoc
     */
    public NodeIterator merge(String srcWorkspace, boolean bestEffort) throws NoSuchWorkspaceException,
            AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
        check();
        NodeIterator nodes = node.merge(srcWorkspace, bestEffort);
        return new NodeIteratorDecorator(factory, session, nodes);
    }

    /**
     * @inheritDoc
     */
    public String getCorrespondingNodePath(String workspaceName) throws ItemNotFoundException,
            NoSuchWorkspaceException, AccessDeniedException, RepositoryException {
        check();
        return node.getCorrespondingNodePath(workspaceName);
    }

    /**
     * @inheritDoc
     */
    public boolean isCheckedOut() throws RepositoryException {
        check();
        return node.isCheckedOut();
    }

    /**
     * @inheritDoc
     */
    public void restore(String versionName, boolean removeExisting) throws VersionException, ItemExistsException,
            UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        check();
        node.restore(versionName, removeExisting);
    }

    /**
     * @inheritDoc
     */
    public void restore(Version version, boolean removeExisting) throws VersionException, ItemExistsException,
            UnsupportedRepositoryOperationException, LockException, RepositoryException {
        check();
        node.restore(VersionDecorator.unwrap(version), removeExisting);
    }

    /**
     * @inheritDoc
     */
    public void restore(Version version, String relPath, boolean removeExisting) throws PathNotFoundException,
            ItemExistsException, VersionException, ConstraintViolationException,
            UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        check();
        node.restore(VersionDecorator.unwrap(version), relPath, removeExisting);
    }

    /**
     * @inheritDoc
     */
    public void restoreByLabel(String versionLabel, boolean removeExisting) throws VersionException,
            ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException,
            RepositoryException {
        check();
        node.restoreByLabel(versionLabel, removeExisting);
    }

    /**
     * @inheritDoc
     */
    public VersionHistory getVersionHistory() throws UnsupportedRepositoryOperationException, RepositoryException {
        check();
        VersionHistory hist = node.getVersionHistory();
        return factory.getVersionHistoryDecorator(session, hist);
    }

    /**
     * @inheritDoc
     */
    public Version getBaseVersion() throws UnsupportedRepositoryOperationException, RepositoryException {
        check();
        return factory.getVersionDecorator(session, node.getBaseVersion());
    }

    /**
     * @inheritDoc
     */
    public Lock lock(boolean isDeep, boolean isSessionScoped) throws UnsupportedRepositoryOperationException,
            LockException, AccessDeniedException, InvalidItemStateException, RepositoryException {
        check();
        Lock lock = node.lock(isDeep, isSessionScoped);
        return factory.getLockDecorator(session, lock);
    }

    /**
     * @inheritDoc
     */
    public Lock getLock() throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException,
            RepositoryException {
        check();
        Lock lock = node.getLock();
        return factory.getLockDecorator(session, lock);
    }

    /**
     * @inheritDoc
     */
    public void unlock() throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException,
            InvalidItemStateException, RepositoryException {
        check();
        node.unlock();
    }

    /**
     * @inheritDoc
     */
    public boolean holdsLock() throws RepositoryException {
        check();
        return node.holdsLock();
    }

    /**
     * @inheritDoc
     */
    public boolean isLocked() throws RepositoryException {
        check();
        return node.isLocked();
    }

    /**
     * @inheritDoc
     */
    public NodeIterator pendingChanges(String nodeType, boolean prune) throws RepositoryException {
        check();
        return session.pendingChanges(this, nodeType, prune);
    }

    /**
     * @inheritDoc
     */
    public NodeIterator pendingChanges(String nodeType) throws RepositoryException {
        check();
        return session.pendingChanges(this, nodeType);
    }

    /**
     * @inheritDoc
     */
    public NodeIterator pendingChanges() throws RepositoryException {
        check();
        return session.pendingChanges(this, null);
    }

    public Property setProperty(String name, Binary value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        check();
        return node.setProperty(name, value);
    }

    public Property setProperty(String name, BigDecimal value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        check();
        return node.setProperty(name, value);
    }

    public NodeIterator getNodes(String[] nameGlobs) throws RepositoryException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public PropertyIterator getProperties(String[] nameGlobs) throws RepositoryException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getIdentifier() throws RepositoryException {
        check();
        return node.getIdentifier();
    }

    public PropertyIterator getReferences(String name) throws RepositoryException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public PropertyIterator getWeakReferences() throws RepositoryException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public PropertyIterator getWeakReferences(String name) throws RepositoryException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setPrimaryType(String nodeTypeName) throws NoSuchNodeTypeException, VersionException, ConstraintViolationException, LockException, RepositoryException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public NodeIterator getSharedSet() throws RepositoryException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void removeSharedSet() throws VersionException, LockException, ConstraintViolationException, RepositoryException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void removeShare() throws VersionException, LockException, ConstraintViolationException, RepositoryException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void followLifecycleTransition(String transition) throws UnsupportedRepositoryOperationException, InvalidLifecycleTransitionException, RepositoryException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String[] getAllowedLifecycleTransistions() throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
