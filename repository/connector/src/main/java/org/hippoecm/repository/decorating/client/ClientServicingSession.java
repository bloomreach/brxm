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
package org.hippoecm.repository.decorating.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.rmi.RemoteException;

import javax.jcr.InvalidSerializedDataException;
import javax.jcr.ItemExistsException;
import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.security.AccessControlException;
import javax.jcr.version.VersionException;
import javax.transaction.xa.XAResource;

import org.apache.jackrabbit.rmi.client.ClientSession;
import org.apache.jackrabbit.rmi.client.RemoteRepositoryException;

import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.decorating.remote.RemoteServicingSession;

public class ClientServicingSession extends ClientSession implements HippoSession {

    private RemoteServicingSession remote;

    protected ClientServicingSession(Repository repository, RemoteServicingSession remote, LocalServicingAdapterFactory factory) {
        super(repository, remote, factory);
        this.remote = remote;
    }

    public Node copy(Node original, String absPath) throws RepositoryException {
        try {
            return getNode(this, remote.copy(original.getPath(), absPath));
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    public NodeIterator pendingChanges(Node node, String nodeType, boolean prune) throws NamespaceException,
                                                                                  NoSuchNodeTypeException, RepositoryException {
        try {
            String absPath = (node != null ? node.getPath() : null);
            return getFactory().getNodeIterator(this, remote.pendingChanges(absPath, nodeType, prune));
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    public NodeIterator pendingChanges(Node node, String nodeType) throws NamespaceException, NoSuchNodeTypeException,
                                                                          RepositoryException {
        return pendingChanges(node, nodeType, false);
    }

    public NodeIterator pendingChanges() throws RepositoryException {
        return pendingChanges(null, null, false);
    }

    public void exportDereferencedView(String path, OutputStream output, boolean binaryAsLink, boolean noRecurse)
        throws IOException, PathNotFoundException, RepositoryException, RemoteException {
        try {
            byte[] xml = remote.exportDereferencedView(path, binaryAsLink, noRecurse);
            output.write(xml);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    public void importDereferencedXML(String path, InputStream xml, int uuidBehavior, int referenceBehavior,
            int mergeBehavior) throws IOException, PathNotFoundException, ItemExistsException,
            ConstraintViolationException, VersionException, InvalidSerializedDataException, LockException,
            RepositoryException {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] bytes = new byte[4096];
            for (int n = xml.read(bytes); n != -1; n = xml.read(bytes)) {
                buffer.write(bytes, 0, n);
            }
            remote.importDereferencedXML(path, buffer.toByteArray(), uuidBehavior, referenceBehavior, mergeBehavior);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    public ClassLoader getSessionClassLoader() throws RepositoryException {
        return Thread.currentThread().getContextClassLoader();
    }

    public XAResource getXAResource() {
        return remote.getXAResource();
    }
    
    @Override
    public void checkPermission(String path, String actions)
            throws AccessControlException, RepositoryException {
        try {
            remote.checkPermission(path, actions);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    public void registerSessionCloseCallback(CloseCallback callback) {
        throw new UnsupportedOperationException();
    }
}
