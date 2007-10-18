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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessControlException;

import javax.transaction.SystemException;
import javax.transaction.xa.XAResource;

import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
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
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.api.XASession;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.name.AbstractNamespaceResolver;
import org.apache.jackrabbit.name.NameException;
import org.apache.jackrabbit.name.Path;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

//FIXME: depend only on JTA, not on Atomikos
import com.atomikos.icatch.jta.UserTransactionManager;

import org.hippoecm.repository.FacetedNavigationEngine;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.jackrabbit.HippoSession;

/**
 */
public class ServicingSessionImpl implements ServicingSession, XASession {
    private final static String SVN_ID = "$Id$";

    protected final DecoratorFactory factory;

    protected final Repository repository;

    protected final Session session;

    protected UserTransactionManager utm = null;

    public UserTransactionManager getUserTransactionManager() {
        return utm;
    }

    ServicingSessionImpl(DecoratorFactory factory, Repository repository, Session session) {
        this.factory = factory;
        this.repository = repository;
        this.session = session;
    }

    ServicingSessionImpl(DecoratorFactory factory, Repository repository, XASession session) throws RepositoryException {
        this.factory = factory;
        this.repository = repository;
        this.session = session;
        try {
            utm = new UserTransactionManager();
            utm.setStartupTransactionService(false);
            utm.init();
        } catch (SystemException ex) {
            throw new RepositoryException("cannot initialize transaction manager", ex);
        }
    }

    protected void finalize() {
        if (utm != null) {
            utm.close();
            utm = null;
        }
    }

    FacetedNavigationEngine getFacetedNavigationEngine() {
        return ((RepositoryDecorator) repository).getFacetedNavigationEngine();
    }

    FacetedNavigationEngine.Context getFacetedNavigationContext() {
        return ((HippoSession)session).getFacetedNavigationContext();
    }

    void setFacetedNavigationContext(FacetedNavigationEngine.Context context) {
        ((HippoSession)session).setFacetedNavigationContext(context);
    }

    String[] getQPath(String absPath) throws NameException, NamespaceException, RepositoryException {
        NamespaceRegistry nsreg = session.getWorkspace().getNamespaceRegistry();
        Path.PathElement[] elements = ((SessionImpl) session).getQPath(absPath.startsWith("/") ? absPath.substring(1) : absPath).getElements();
        String[] rtelements = new String[elements.length];
        for(int i=0; i<elements.length; i++) {
            rtelements[i] = elements[i].toJCRName((AbstractNamespaceResolver)nsreg);
        }
        return rtelements;
    }

    public XAResource getXAResource() {
        return ((XASession) session).getXAResource();
    }

    /** {@inheritDoc} */
    public Repository getRepository() {
        return repository;
    }

    /**
     * Forwards the method call to the underlying session.
     */
    public String getUserID() {
        return session.getUserID();
    }

    /**
     * Forwards the method call to the underlying session.
     */
    public Object getAttribute(String name) {
        return session.getAttribute(name);
    }

    /**
     * Forwards the method call to the underlying session.
     */
    public String[] getAttributeNames() {
        return session.getAttributeNames();
    }

    /**
     * Forwards the method call to the underlying session. The returned
     * workspace is wrapped into a workspace decorator using the decorator
     * factory.
     *
     * @return decorated workspace
     */
    public Workspace getWorkspace() {
        return factory.getWorkspaceDecorator(this, session.getWorkspace());
    }

    /**
     * Forwards the method call to the underlying session. The returned
     * session is wrapped into a session decorator using the decorator factory.
     *
     * @return decorated session
     */
    public Session impersonate(Credentials credentials) throws LoginException, RepositoryException {
        Session newSession = session.impersonate(credentials);
        return factory.getSessionDecorator(repository, newSession);
    }

    /**
     * Forwards the method call to the underlying session. The returned
     * node is wrapped into a node decorator using the decorator factory.
     *
     * @return decorated node
     */
    public Node getRootNode() throws RepositoryException {
        Node root = session.getRootNode();
        return factory.getNodeDecorator(this, root, null);
    }

    /**
     * Forwards the method call to the underlying session. The returned
     * node is wrapped into a node decorator using the decorator factory.
     *
     * @return decorated node
     */
    public Node getNodeByUUID(String uuid) throws ItemNotFoundException, RepositoryException {
        Node node = session.getNodeByUUID(uuid);
        return factory.getNodeDecorator(this, node, null);
    }

    /**
     * Forwards the method call to the underlying session. The returned
     * item is wrapped into a node, property, or item decorator using
     * the decorator factory. The decorator type depends on the type
     * of the underlying item.
     *
     * @return decorated item, property, or node
     */
    public Item getItem(String absPath) throws PathNotFoundException, RepositoryException {
        try {
            Item item = session.getItem(absPath);
            return factory.getItemDecorator(this, item);
        } catch (PathNotFoundException ex) {
            try {
                Node node = getRootNode();
                String[] elements = getQPath(absPath);
                if (elements.length == 1)
                    throw ex;
                for (int i = 0; i < elements.length-1; i++) {
                    node = node.getNode(elements[i]);
                }
                try {
                    node = node.getNode(elements[elements.length - 1]);
                    if(!(node instanceof ServicingNodeImpl))
                        node = new ServicingNodeImpl(factory, this, node, absPath, node.getDepth() + 1, null);
                    return node;
                } catch (PathNotFoundException ex2) {
                    return node.getProperty(elements[elements.length - 1]);
                }
            } catch (ClassCastException ex2) {
                throw ex;
            } catch (org.apache.jackrabbit.name.NameException ex2) {
                throw ex;
            }
        }
    }

    /**
     * Forwards the method call to the underlying session.
     */
    public boolean itemExists(String path) throws RepositoryException {
        return session.itemExists(path);
    }

    /**
     * Forwards the method call to the underlying session.
     */
    public void move(String srcAbsPath, String destAbsPath) throws ItemExistsException, PathNotFoundException,
            VersionException, RepositoryException {
        session.move(srcAbsPath, destAbsPath);
        Node movedNode = getRootNode().getNode(destAbsPath.startsWith("/") ? destAbsPath.substring(1) : destAbsPath);
        ServicingNodeImpl.decoratePathProperty(movedNode);
    }

    /**
     * Forwards the method call to the underlying session.
     */
    public void save() throws AccessDeniedException, ConstraintViolationException, InvalidItemStateException,
            VersionException, LockException, RepositoryException {
        ServicingWorkspaceImpl wsp = (ServicingWorkspaceImpl) getWorkspace();
        ((WorkflowManagerImpl) wsp.getWorkflowManager()).save();
        session.save();
    }

    /**
     * Forwards the method call to the underlying session.
     */
    public void refresh(boolean keepChanges) throws RepositoryException {
        session.refresh(keepChanges);
    }

    /**
     * Forwards the method call to the underlying session.
     */
    public boolean hasPendingChanges() throws RepositoryException {
        return session.hasPendingChanges();
    }

    /**
     * Forwards the method call to the underlying session.
     */
    public void checkPermission(String absPath, String actions) throws AccessControlException, RepositoryException {
        session.checkPermission(absPath, actions);
    }

    /**
     * Forwards the method call to the underlying session.
     */
    public ContentHandler getImportContentHandler(String parentAbsPath, int uuidBehaviour)
            throws PathNotFoundException, ConstraintViolationException, VersionException, LockException,
            RepositoryException {
        return session.getImportContentHandler(parentAbsPath, uuidBehaviour);
    }

    /**
     * Forwards the method call to the underlying session.
     */
    public void importXML(String parentAbsPath, InputStream in, int uuidBehaviour) throws IOException,
            PathNotFoundException, ItemExistsException, ConstraintViolationException, VersionException,
            InvalidSerializedDataException, LockException, RepositoryException {
        session.importXML(parentAbsPath, in, uuidBehaviour);
    }

    /**
     * Forwards the method call to the underlying session.
     */
    public void exportSystemView(String absPath, ContentHandler contentHandler, boolean binaryAsLink, boolean noRecurse)
            throws PathNotFoundException, SAXException, RepositoryException {
        session.exportSystemView(absPath, contentHandler, binaryAsLink, noRecurse);
    }

    /**
     * Forwards the method call to the underlying session.
     */
    public void exportSystemView(String absPath, OutputStream out, boolean binaryAsLink, boolean noRecurse)
            throws IOException, PathNotFoundException, RepositoryException {
        session.exportSystemView(absPath, out, binaryAsLink, noRecurse);
    }

    /**
     * Forwards the method call to the underlying session.
     */
    public void exportDocumentView(String absPath, ContentHandler contentHandler, boolean binaryAsLink,
            boolean noRecurse) throws PathNotFoundException, SAXException, RepositoryException {
        session.exportDocumentView(absPath, contentHandler, binaryAsLink, noRecurse);
    }

    /**
     * Forwards the method call to the underlying session.
     */
    public void exportDocumentView(String absPath, OutputStream out, boolean binaryAsLink, boolean noRecurse)
            throws IOException, PathNotFoundException, RepositoryException {
        session.exportDocumentView(absPath, out, binaryAsLink, noRecurse);
    }

    /**
     * Forwards the method call to the underlying session.
     */
    public void setNamespacePrefix(String prefix, String uri) throws NamespaceException, RepositoryException {
        session.setNamespacePrefix(prefix, uri);
    }

    /**
     * Forwards the method call to the underlying session.
     */
    public String[] getNamespacePrefixes() throws RepositoryException {
        return session.getNamespacePrefixes();
    }

    /**
     * Forwards the method call to the underlying session.
     */
    public String getNamespaceURI(String prefix) throws NamespaceException, RepositoryException {
        return session.getNamespaceURI(prefix);
    }

    /**
     * Forwards the method call to the underlying session.
     */
    public String getNamespacePrefix(String uri) throws NamespaceException, RepositoryException {
        return session.getNamespacePrefix(uri);
    }

    /**
     * Forwards the method call to the underlying session.
     */
    public void logout() {
        session.logout();
        ((RepositoryDecorator) repository).getFacetedNavigationEngine().unprepare(getFacetedNavigationContext());
    }

    /**
     * Forwards the method call to the underlying session.
     */
    public void addLockToken(String lt) {
        session.addLockToken(lt);
    }

    /**
     * Forwards the method call to the underlying session.
     */
    public String[] getLockTokens() {
        return session.getLockTokens();
    }

    /**
     * Forwards the method call to the underlying session.
     */
    public void removeLockToken(String lt) {
        session.removeLockToken(lt);
    }

    public ValueFactory getValueFactory() throws UnsupportedRepositoryOperationException, RepositoryException {
        return factory.getValueFactoryDecorator(this, session.getValueFactory());
    }

    public boolean isLive() {
        return session.isLive();
    }
}
