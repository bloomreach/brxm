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
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.util.TraversingItemVisitor;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.api.XASession;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.spi.Path;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.jackrabbit.HippoNodeId;
import org.hippoecm.repository.servicing.ServicingNodeImpl;

/**
 */
public class SessionDecorator implements XASession, HippoSession {
    
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    protected final DecoratorFactory factory;

    protected final Repository repository;

    protected final Session session;

    SessionDecorator(DecoratorFactory factory, Repository repository, Session session) {
        this.factory = factory;
        this.repository = repository;
        this.session = session;
    }

    SessionDecorator(DecoratorFactory factory, Repository repository, XASession session) throws RepositoryException {
        this.factory = factory;
        this.repository = repository;
        this.session = session;
    }

    public static Session unwrap(Session session) {
        if (session == null) {
            return null;
        }
        if (session instanceof SessionDecorator) {
            session = ((SessionDecorator)session).session;
        }
        return session;
    }

    protected void finalize() {
        /* FIXME
        if (utm != null) {
            utm.close();
            utm = null;
        }
        */
    }

    String[] getQPath(String absPath) throws  NamespaceException, RepositoryException {
        NamespaceRegistry nsreg = session.getWorkspace().getNamespaceRegistry();
        Path.Element[] elements = ((SessionImpl) session).getQPath(absPath.startsWith("/") ? absPath.substring(1)
                                                                                           : absPath).getElements();
        String[] rtelements = new String[elements.length];
        for(int i=0; i<elements.length; i++) {
                if(nsreg.getPrefix(elements[i].getName().getNamespaceURI()).equals("")){
                        rtelements[i] = elements[i].getName().getLocalName();
                }
                else {
                        rtelements[i] = nsreg.getPrefix(elements[i].getName().getNamespaceURI()) + ":"
                          + elements[i].getName().getLocalName();
                }
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
        return factory.getNodeDecorator(this, root);
    }

    /**
     * Forwards the method call to the underlying session. The returned
     * node is wrapped into a node decorator using the decorator factory.
     *
     * @return decorated node
     */
    public Node getNodeByUUID(String uuid) throws ItemNotFoundException, RepositoryException {
        Node node = session.getNodeByUUID(uuid);
        return factory.getNodeDecorator(this, node);
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
        Item item = session.getItem(absPath);
        return factory.getItemDecorator(this, item);
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
        movedNode.accept(new TraversingItemVisitor.Default() {
                public void leaving(Node node, int level) throws RepositoryException {
                    System.err.println("BERRY BERRY BERRY "+node.getPath());
                    ServicingNodeImpl.decoratePathProperty(node);
                }
            });
    }

    /**
     * Forwards the method call to the underlying session.
     */
    public void save() throws AccessDeniedException, ConstraintViolationException, InvalidItemStateException,
            VersionException, LockException, RepositoryException {
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

    /**
     * Convenience function to copy a node to a destination path in the same workspace
     *
     * @param srcNode the source path node to copy
     * @param destAbsNodePath the absolute path of the to be created target
     * node which will be a copy of srcNode
     * @returns the resulting copy
     */
    public Node copy(Node srcNode, String destAbsNodePath) throws PathNotFoundException, ItemExistsException,
      LockException, VersionException, RepositoryException {
        
        if (destAbsNodePath.startsWith(srcNode.getPath()) && !destAbsNodePath.equals(srcNode.getPath())) {
            String msg = srcNode.getPath() + ": Invalid destination path (cannot be descendant of source path)";
            throw new RepositoryException(msg);
        }

        while (destAbsNodePath.startsWith("/")) {
            destAbsNodePath = destAbsNodePath.substring(1);
        }
        Node destNode = getRootNode();
        int p = destAbsNodePath.lastIndexOf("/");
        if(p > 0) {
            destNode = destNode.getNode(destAbsNodePath.substring(0,p));
            destAbsNodePath = destAbsNodePath.substring(p+1);
        }
        try {
            destNode = destNode.addNode(destAbsNodePath, srcNode.getPrimaryNodeType().getName());
            copy(srcNode, destNode);
            return destNode;
        } catch(ConstraintViolationException ex) {
            throw new RepositoryException("Internal error", ex); // this cannot happen
        } catch(NoSuchNodeTypeException ex) {
            throw new RepositoryException("Internal error", ex); // this cannot happen
        }
    }

    static void copy(Node srcNode, Node destNode) throws ItemExistsException, LockException, RepositoryException {
        try {
            Node canonical = ((HippoNode)srcNode).getCanonicalNode();
            boolean copyChildren = (canonical != null && canonical.isSame(srcNode));

            for(PropertyIterator iter = ServicingNodeImpl.unwrap(srcNode).getProperties(); iter.hasNext(); ) {
                Property property = iter.nextProperty();
                if(!property.getName().equals("jcr:primaryType") && !property.getName().equals("jcr:uuid")) {
                    if(property.getDefinition().isMultiple())
                        destNode.setProperty(property.getName(), property.getValues());
                    else
                        destNode.setProperty(property.getName(), property.getValue());
                }
            }

            ServicingNodeImpl.decoratePathProperty(destNode);

            /* Do not copy virtual nodes.  Partial virtual nodes like
             * HippoNodeType.NT_FACETSELECT and HippoNodeType.NT_FACETSEARCH
             * should be copied except for their children, even if though they
             * are half virtual. On save(), the virtual part will be
             * ignored.
             */
            if(copyChildren) {
                for(NodeIterator iter = srcNode.getNodes(); iter.hasNext(); ) {
                    Node node = iter.nextNode();
                    canonical = ((HippoNode)node).getCanonicalNode();
                    if(canonical != null && canonical.isSame(node)) {
                        Node child = destNode.addNode(node.getName(), node.getPrimaryNodeType().getName());
                        copy(node, child);
                    }
                }
            }
        } catch(PathNotFoundException ex) {
            throw new RepositoryException("Internal error", ex); // this cannot happen
        } catch(VersionException ex) {
            throw new RepositoryException("Internal error", ex); // this cannot happen
        } catch(ValueFormatException ex) {
            throw new RepositoryException("Internal error", ex); // this cannot happen
        } catch(ConstraintViolationException ex) {
            throw new RepositoryException("Internal error", ex); // this cannot happen
        } catch(NoSuchNodeTypeException ex) {
            throw new RepositoryException("Internal error", ex); // this cannot happen
        }
    }
}
