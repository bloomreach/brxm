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
package org.hippoecm.repository.servicing;

import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.MergeException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.ReferentialIntegrityException;
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
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;
import javax.jcr.version.OnParentVersionAction;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;

import org.apache.jackrabbit.name.NameException;
import org.apache.jackrabbit.name.Path;

import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;

public class ServicingNodeImpl extends ItemDecorator implements HippoNode {
    final static private String SVN_ID = "$Id$";

    protected Node node;
    protected Session session;

    protected ServicingNodeImpl(DecoratorFactory factory, Session session, Node node) {
        super(factory, session, node);
        this.session = session;
        this.node = node;
    }

    public Node getCanonicalNode() throws RepositoryException {
        if (hasProperty("hippo:uuid")) {
            return getSession().getNodeByUUID(getProperty("hippo:uuid").getString());
        } else {
            return this;
        }
    }

    public String getDisplayName() throws RepositoryException {
        //if (hasProperty(HippoNodeType.HIPPO_UUID) && hasProperty(HippoNodeType.HIPPO_SEARCH)) {
        if (hasProperty(HippoNodeType.HIPPO_SEARCH)) {

            // hipposample:authorId#//element(*,hipposample:author)[hippo:id=?]/@hippo:name
            // just return the resultset
            if (getName().equals(HippoNodeType.HIPPO_RESULTSET)) {
                return HippoNodeType.HIPPO_RESULTSET;
            }

            // the last search is the current one
            Value[] searches = getProperty(HippoNodeType.HIPPO_SEARCH).getValues();
            if (searches.length == 0) {
                return getName();
            }
            String search = searches[searches.length-1].getString();

            // check for search seperator
            if (search.indexOf("#") == -1) {
                return getName();
            }

            // check for sql parameter '?'
            String xpath = search.substring(search.indexOf("#")+1);
            if (xpath.indexOf('?') == -1) {
                return getName();
            }

            // construct query
            xpath = xpath.substring(0,xpath.indexOf('?')) + getName() + xpath.substring(xpath.indexOf('?')+1);

            Query query = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);

            // execute
            QueryResult result = query.execute();
            RowIterator iter = result.getRows();
            if (iter.hasNext()) {
                return iter.nextRow().getValues()[0].getString();
            } else {
                return getName();
            }
        } else {
            return getName();
        }
    }

    public static Node unwrap(Node node) {
        if (node instanceof ServicingNodeImpl) {
            node = (Node) ((ServicingNodeImpl) node).unwrap();
        }
        return node;
    }

    static void decoratePathProperty(Node node) throws RepositoryException {
        if(node.isNodeType(HippoNodeType.NT_DOCUMENT)) {
            try {
                String path = node.getPath();
                if(path.startsWith("/")) {
                    path = path.substring(1);
                }
                String[] pathElements = path.split("/");
                pathElements[0] = "/"+pathElements[0];
                for(int i=1; i<pathElements.length; i++) {
                        pathElements[i] = pathElements[i-1] + "/" + pathElements[i];
                }
                node.setProperty(HippoNodeType.HIPPO_PATHS, pathElements);
            } catch(ValueFormatException ex) {
                // FIXME: log some serious error
                throw ex;
            } catch(VersionException ex) {
                // FIXME: log some serious error
                throw ex;
            } catch(LockException ex) {
                // FIXME: log some serious error
                throw ex;
            } catch(ConstraintViolationException ex) {
                // FIXME: log some serious error
                throw ex;
            }
        }
    }

    /**
     * @inheritDoc
     */
    public Node addNode(String name) throws ItemExistsException, PathNotFoundException, VersionException,
            ConstraintViolationException, LockException, RepositoryException {
        Node child = node.addNode(name);
        decoratePathProperty(child);
        return factory.getNodeDecorator(session, child);
    }

    /**
     * @inheritDoc
     */
    public Node addNode(String name, String type) throws ItemExistsException, PathNotFoundException,
            NoSuchNodeTypeException, LockException, VersionException, ConstraintViolationException, RepositoryException {
        Node child = node.addNode(name, type);
        decoratePathProperty(child);
        return factory.getNodeDecorator(session, child);
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
        Property prop = node.setProperty(name, ServicingNodeImpl.unwrap(value));
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
        Lock lock = node.lock(isDeep, isSessionScoped);
        return factory.getLockDecorator(session, lock);
    }

    /**
     * @inheritDoc
     */
    public Lock getLock() throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException,
            RepositoryException {
        Lock lock = node.getLock();
        return factory.getLockDecorator(session, lock);
    }

    /**
     * @inheritDoc
     */
    public void unlock() throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException,
            InvalidItemStateException, RepositoryException {
        node.unlock();
    }

    /**
     * @inheritDoc
     */
    public boolean holdsLock() throws RepositoryException {
        return node.holdsLock();
    }

    /**
     * @inheritDoc
     */
    public boolean isLocked() throws RepositoryException {
        return node.isLocked();
    }

    public void save()
        throws AccessDeniedException, ConstraintViolationException, InvalidItemStateException,
               ReferentialIntegrityException, VersionException, LockException, RepositoryException
    {
        super.save();
    }
}
