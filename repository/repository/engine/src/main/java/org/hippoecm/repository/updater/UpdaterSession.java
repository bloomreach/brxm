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
package org.hippoecm.repository.updater;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessControlException;
import java.util.StringTokenizer;

import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.VersionException;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

final class UpdaterSession implements Session {
    Session upstream;
    ValueFactory valueFactory;
    UpdaterNode root;

    public UpdaterSession(Session session) throws UnsupportedRepositoryOperationException, RepositoryException {
        this.upstream = session;
        this.valueFactory = session.getValueFactory();
        this.root = new UpdaterNode(this, session.getRootNode(), null);
    }

    NodeType getNewType(String type) throws RepositoryException {
        return upstream.getWorkspace().getNodeTypeManager().getNodeType(type);
    }

    public void commit() throws RepositoryException {
        root.commit();
    }

    // interface javax.jcr.Session

    @Deprecated
    public Repository getRepository() {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public String getUserID() {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public Object getAttribute(String name) {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public String[] getAttributeNames() {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public Workspace getWorkspace() {
        throw new UpdaterException("illegal method");
    }

    public Session impersonate(Credentials credentials) throws LoginException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public Node getRootNode() throws RepositoryException {
        return root;
    }

    @Deprecated
    public Node getNodeByUUID(String uuid) throws ItemNotFoundException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public Item getItem(String absPath) throws PathNotFoundException, RepositoryException {
        Node node = getRootNode();
        for (StringTokenizer iter = new StringTokenizer(absPath.substring(1), "/"); iter.hasMoreTokens();) {
            node = node.getNode(iter.nextToken());
        }
        return node;
    }

    public boolean itemExists(String absPath) throws RepositoryException {
        try {
            getItem(absPath);
            return true;
        } catch (PathNotFoundException ex) {
            return false;
        }
    }

    public void move(String srcAbsPath, String destAbsPath) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException, RepositoryException {
        UpdaterNode node = (UpdaterNode)getItem(srcAbsPath);
        UpdaterNode destination = (UpdaterNode)getItem(destAbsPath.substring(0, destAbsPath.lastIndexOf("/") - 1));
        node.parent = destination;
        node.setName(destAbsPath.substring(destAbsPath.lastIndexOf("/")));
    }

    @Deprecated
    public void save() throws AccessDeniedException, ItemExistsException, ConstraintViolationException, InvalidItemStateException, VersionException, LockException, NoSuchNodeTypeException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public void refresh(boolean keepChanges) throws RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public boolean hasPendingChanges() throws RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public ValueFactory getValueFactory() throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public void checkPermission(String absPath, String actions) throws AccessControlException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public ContentHandler getImportContentHandler(String parentAbsPath, int uuidBehavior) throws PathNotFoundException, ConstraintViolationException, VersionException, LockException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public void importXML(String parentAbsPath, InputStream in, int uuidBehavior) throws IOException, PathNotFoundException, ItemExistsException, ConstraintViolationException, VersionException, InvalidSerializedDataException, LockException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public void exportSystemView(String absPath, ContentHandler contentHandler, boolean skipBinary, boolean noRecurse) throws PathNotFoundException, SAXException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public void exportSystemView(String absPath, OutputStream out, boolean skipBinary, boolean noRecurse) throws IOException, PathNotFoundException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public void exportDocumentView(String absPath, ContentHandler contentHandler, boolean skipBinary, boolean noRecurse) throws PathNotFoundException, SAXException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public void exportDocumentView(String absPath, OutputStream out, boolean skipBinary, boolean noRecurse) throws IOException, PathNotFoundException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public void setNamespacePrefix(String newPrefix, String existingUri) throws NamespaceException, RepositoryException {
        upstream.setNamespacePrefix(newPrefix, existingUri);
    }

    public String[] getNamespacePrefixes() throws RepositoryException {
        return upstream.getNamespacePrefixes();
    }

    public String getNamespaceURI(String prefix) throws NamespaceException, RepositoryException {
        return upstream.getNamespaceURI(prefix);
    }

    public String getNamespacePrefix(String uri) throws NamespaceException, RepositoryException {
        return upstream.getNamespacePrefix(uri);
    }

    @Deprecated
    public void logout() {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public boolean isLive() {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public void addLockToken(String lt) {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public String[] getLockTokens() {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public void removeLockToken(String lt) {
        throw new UpdaterException("illegal method");
    }
}
