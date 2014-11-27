/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;

import javax.jcr.Credentials;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.retention.RetentionManager;
import javax.jcr.security.AccessControlManager;
import javax.jcr.version.VersionException;
import javax.transaction.xa.XAResource;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.api.HippoSession;
import org.onehippo.repository.security.User;
import org.onehippo.repository.security.domain.DomainRuleExtension;
import org.onehippo.repository.xml.ContentResourceLoader;
import org.onehippo.repository.xml.ImportResult;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Mock version of a {@link Session}. It only returns the root node. Saving changes is ignored.
 * All methods that are not implemented throw an {@link UnsupportedOperationException}.
 */
public class MockSession implements HippoSession {

    private final MockNode root;

    protected MockSession(MockNode root) {
        this.root = root;
    }

    @Override
    public MockNode getRootNode() {
        return root;
    }

    @Override
    public void save() {
        // do nothing
    }

    @Override
    public MockItem getItem(final String absPath) throws RepositoryException {
        if (!absPath.startsWith("/")) {
            throw new IllegalArgumentException("Expected an absolute path");
        }
        MockItem item = getRootNode();
        for (String element : absPath.split("/")) {
            if (element.isEmpty()) {
                continue;
            }
            if (!item.isNode()) {
                throw new PathNotFoundException("No such item: " + absPath);
            }
            MockNode node = (MockNode) item;
            if (node.hasNode(element)) {
                item = node.getNode(element);
            } else if (node.hasProperty(element)) {
                item = node.getProperty(element);
            } else {
                throw new PathNotFoundException("No such item: " + absPath);
            }
        }
        return item;
    }

    @Override
    public MockNode getNode(final String absPath) throws RepositoryException {
        Item item = getItem(absPath);
        if (!item.isNode()) {
            throw new PathNotFoundException("No such node: " + absPath);
        }
        return (MockNode) item;
    }

    @Override
    public boolean itemExists(final String absPath) throws RepositoryException {
        try {
            getItem(absPath);
        } catch (PathNotFoundException e) {
            return false;
        }
        return true;
    }

    @Override
    public boolean nodeExists(final String absPath) throws RepositoryException {
        try {
            getNode(absPath);
        } catch (PathNotFoundException e) {
            return false;
        }
        return true;
    }

    @Override
    public Property getProperty(final String absPath) throws RepositoryException {
        Item item = getItem(absPath);
        if (item.isNode()) {
            throw new PathNotFoundException("No such node: " + absPath);
        }
        return (MockProperty) item;
    }

    @Override
    public boolean propertyExists(final String absPath) throws RepositoryException {
        try {
            getProperty(absPath);
        } catch (PathNotFoundException e) {
            return false;
        }
        return true;
    }

    @Override
    public Node getNodeByIdentifier(final String id) throws RepositoryException {
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }
        final Node result = getNodeOrDecendantByIdentifier(id, this.root);
        if (result == null) {
            throw new ItemNotFoundException("Node with identifier '" + id + "' does not exist");
        }
        return result;
    }

    private static Node getNodeOrDecendantByIdentifier(final String id, final Node node) throws RepositoryException {
        Node result = null;
        if (node.getIdentifier().equals(id)) {
            result = node;
        } else {
            final NodeIterator children = node.getNodes();
            while (result == null && children.hasNext()) {
                result = getNodeOrDecendantByIdentifier(id, children.nextNode());
            }
        }
        return result;
    }

    @Override
    public String getUserID() {
        return "";
    }

    @Override
    public boolean isLive() {
        return true;
    }

    @Override
    public void refresh(final boolean keepChanges) {
    }

    @Override
    public boolean hasPermission(final String absPath, final String actions) {
        return true;
    }

    @Override
    public void checkPermission(final String absPath, final String actions) {
    }

    @Override
    public boolean hasCapability(final String methodName, final Object target, final Object[] arguments) {
        return true;
    }

    @Override
    public void logout() {
    }

    @Override
    public Session impersonate(final Credentials credentials) {
        return this;
    }

    @Override
    public String[] getAttributeNames() {
        return new String[0];
    }

    @Override
    public Object getAttribute(final String name) {
        return null;
    }

    @Override
    public boolean hasPendingChanges() {
        return false;
    }

    @Override
    public NodeIterator pendingChanges(final Node node, final String nodeType, final boolean prune) throws NamespaceException, NoSuchNodeTypeException, RepositoryException {
        return new MockNodeIterator(Collections.<MockNode>emptyList());
    }

    @Override
    public NodeIterator pendingChanges(final Node node, final String nodeType) throws NamespaceException, NoSuchNodeTypeException, RepositoryException {
        return new MockNodeIterator(Collections.<MockNode>emptyList());
    }

    @Override
    public NodeIterator pendingChanges() throws RepositoryException {
        return new MockNodeIterator(Collections.<MockNode>emptyList());
    }

    @Override
    public MockValueFactory getValueFactory() {
        return new MockValueFactory();
    }

    @Override
    public MockWorkspace getWorkspace() {
        return new MockWorkspace(this);
    }

    // REMAINING METHODS ARE NOT IMPLEMENTED

    @Override
    public Repository getRepository() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Node getNodeByUUID(final String uuid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void move(final String srcAbsPath, final String destAbsPath) throws RepositoryException {
        String destParentAbsPath = StringUtils.substringBeforeLast(destAbsPath, "/");
        destParentAbsPath = destParentAbsPath.isEmpty() ? "/" : destParentAbsPath;
        final String destName = StringUtils.substringAfterLast(destAbsPath, "/");
        final MockNode destParentNode = getNode(destParentAbsPath);
        final MockNode srcNode = getNode(srcAbsPath);
        srcNode.remove();
        srcNode.setName(destName);
        destParentNode.addNode(srcNode);
    }

    @Override
    public void removeItem(final String absPath) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ContentHandler getImportContentHandler(final String parentAbsPath, final int uuidBehavior) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void importXML(final String parentAbsPath, final InputStream in, final int uuidBehavior) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void exportSystemView(final String absPath, final ContentHandler contentHandler, final boolean skipBinary, final boolean noRecurse) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void exportSystemView(final String absPath, final OutputStream out, final boolean skipBinary, final boolean noRecurse) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void exportDocumentView(final String absPath, final ContentHandler contentHandler, final boolean skipBinary, final boolean noRecurse) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void exportDocumentView(final String absPath, final OutputStream out, final boolean skipBinary, final boolean noRecurse) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setNamespacePrefix(final String prefix, final String uri) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getNamespacePrefixes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getNamespaceURI(final String prefix) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getNamespacePrefix(final String uri) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addLockToken(final String lt) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getLockTokens() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeLockToken(final String lt) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AccessControlManager getAccessControlManager() {
        throw new UnsupportedOperationException();
    }

    @Override
    public RetentionManager getRetentionManager() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Node copy(final Node srcNode, final String destAbsNodePath) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void exportDereferencedView(final String absPath, final OutputStream out, final boolean binaryAsLink, final boolean noRecurse) throws IOException, PathNotFoundException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void exportDereferencedView(final String absPath, final ContentHandler contentHandler, final boolean binaryAsLink, final boolean noRecurse) throws PathNotFoundException, SAXException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void importDereferencedXML(final String parentAbsPath, final InputStream in, final int uuidBehavior, final int referenceBehavior, final int mergeBehavior) throws IOException, PathNotFoundException, ItemExistsException, ConstraintViolationException, VersionException, InvalidSerializedDataException, LockException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void importDereferencedXML(final String parentAbsPath, final InputStream in, final ContentResourceLoader referredResourceLoader, final int uuidBehavior, final int referenceBehavior, final int mergeBehavior) throws IOException, PathNotFoundException, ItemExistsException, ConstraintViolationException, VersionException, InvalidSerializedDataException, LockException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ImportResult importEnhancedSystemViewXML(final String parentAbsPath, final InputStream in, final int uuidBehavior, final int referenceBehavior, final ContentResourceLoader referredResourceLoader) throws IOException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public File exportEnhancedSystemViewPackage(final String parentAbsPath, final boolean recurse) throws IOException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public XAResource getXAResource() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ClassLoader getSessionClassLoader() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public User getUser() throws ItemNotFoundException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Session createSecurityDelegate(final Session session, final DomainRuleExtension... domainExtensions) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void localRefresh() {
    }

    @Override
    public void disableVirtualLayers() {
    }
}
