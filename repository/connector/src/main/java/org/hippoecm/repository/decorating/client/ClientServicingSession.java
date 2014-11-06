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
package org.hippoecm.repository.decorating.client;

import java.io.ByteArrayInputStream;
import java.io.File;
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
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.security.AccessControlException;
import javax.jcr.version.VersionException;
import javax.transaction.xa.XAResource;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.jackrabbit.rmi.client.ClientSession;
import org.apache.jackrabbit.rmi.client.RemoteRepositoryException;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.decorating.remote.RemoteServicingSession;
import org.onehippo.repository.security.User;
import org.onehippo.repository.security.domain.DomainRuleExtension;
import org.onehippo.repository.xml.ContentResourceLoader;
import org.onehippo.repository.xml.ImportResult;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public class ClientServicingSession extends ClientSession implements HippoSession {

    private RemoteServicingSession remote;

    protected ClientServicingSession(Repository repository, RemoteServicingSession remote, LocalServicingAdapterFactory factory) {
        super(repository, remote, factory);
        this.remote = remote;
    }

    @Override
    public Node copy(Node original, String absPath) throws RepositoryException {
        try {
            return getNode(this, remote.copy(original.getPath(), absPath));
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    @Override
    public NodeIterator pendingChanges(Node node, String nodeType, boolean prune) throws NamespaceException,
                                                                                  NoSuchNodeTypeException, RepositoryException {
        try {
            String absPath = (node != null ? node.getPath() : null);
            return getFactory().getNodeIterator(this, remote.pendingChanges(absPath, nodeType, prune));
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    @Override
    public NodeIterator pendingChanges(Node node, String nodeType) throws NamespaceException, NoSuchNodeTypeException,
                                                                          RepositoryException {
        return pendingChanges(node, nodeType, false);
    }

    @Override
    public NodeIterator pendingChanges() throws RepositoryException {
        return pendingChanges(null, null, false);
    }

    @Override
    public void exportDereferencedView(String path, OutputStream output, boolean binaryAsLink, boolean noRecurse)
        throws IOException, PathNotFoundException, RepositoryException {
        try {
            byte[] xml = remote.exportDereferencedView(path, binaryAsLink, noRecurse);
            output.write(xml);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    @Override
    public void exportDereferencedView(String absPath, ContentHandler contentHandler, boolean binaryAsLink, boolean noRecurse)
            throws PathNotFoundException, SAXException, RepositoryException {
        try {
            byte[] xml = remote.exportDereferencedView(absPath, binaryAsLink, noRecurse);

            Source source = new StreamSource(new ByteArrayInputStream(xml));
            Result result = new SAXResult(contentHandler);

            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            transformer.transform(source, result);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        } catch (IOException ex) {
            throw new SAXException(ex);
        } catch (TransformerConfigurationException ex) {
            throw new SAXException(ex);
        } catch (TransformerException ex) {
            throw new SAXException(ex);
        }
    }

    @Override
    public void importDereferencedXML(String path, InputStream xml, int uuidBehavior, int referenceBehavior,
            int mergeBehavior) throws IOException, PathNotFoundException, ItemExistsException,
            ConstraintViolationException, VersionException, InvalidSerializedDataException, LockException,
            RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void importDereferencedXML(String path, InputStream xml, ContentResourceLoader referredResourceLoader, int uuidBehavior, int referenceBehavior,
            int mergeBehavior) throws IOException, PathNotFoundException, ItemExistsException,
            ConstraintViolationException, VersionException, InvalidSerializedDataException, LockException,
            RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ImportResult importEnhancedSystemViewXML(final String parentAbsPath, final InputStream in,
                                                    final int uuidBehavior, final int referenceBehavior,
                                                    final ContentResourceLoader referredResourceLoader) throws IOException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public File exportEnhancedSystemViewPackage(final String parentAbsPath, final boolean recurse) throws IOException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ClassLoader getSessionClassLoader() throws RepositoryException {
        return Thread.currentThread().getContextClassLoader();
    }

    @Override
    public User getUser() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
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

    @Override
    public Session createSecurityDelegate(final Session session, DomainRuleExtension... domainExtensions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void localRefresh() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void disableVirtualLayers() {
        throw new UnsupportedOperationException();
    }
}
