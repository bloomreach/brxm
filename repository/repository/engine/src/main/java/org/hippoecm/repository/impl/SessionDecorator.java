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
package org.hippoecm.repository.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
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
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.VersionException;
import javax.transaction.xa.XAResource;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import org.apache.jackrabbit.api.XASession;
import org.apache.jackrabbit.spi.Path;

import org.hippoecm.repository.DerivedDataEngine;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.decorating.DecoratorFactory;
import org.hippoecm.repository.decorating.NodeIteratorDecorator;
import org.hippoecm.repository.jackrabbit.HippoLocalItemStateManager;
import org.hippoecm.repository.jackrabbit.SessionImpl;
import org.hippoecm.repository.jackrabbit.XASessionImpl;
import org.hippoecm.repository.jackrabbit.xml.DereferencedSysViewSAXEventGenerator;
import org.hippoecm.repository.jackrabbit.xml.PhysicalSysViewSAXEventGenerator;
import org.hippoecm.repository.updater.UpdaterNode;
import org.hippoecm.repository.updater.UpdaterProperty;

public class SessionDecorator extends org.hippoecm.repository.decorating.SessionDecorator implements XASession, HippoSession {

    private static Logger log = LoggerFactory.getLogger(SessionDecorator.class);

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    protected DerivedDataEngine derivedEngine;

    SessionDecorator(DecoratorFactory factory, Repository repository, Session session) {
        super(factory, repository, session);
        derivedEngine = new DerivedDataEngine(this);
    }

    SessionDecorator(DecoratorFactory factory, Repository repository, XASession session) throws RepositoryException {
        super(factory, repository, session);
        derivedEngine = new DerivedDataEngine(this);
    }

    void postSave(Node node) throws VersionException, LockException, ConstraintViolationException, RepositoryException {
        if(derivedEngine != null) {
            derivedEngine.save(node);
        }
    }

    public void postValidation() throws ConstraintViolationException, RepositoryException {
        derivedEngine.validate();
    }

    public void postDerivedData(boolean enabled) {
        if (enabled) {
            if (derivedEngine == null) {
                derivedEngine = new DerivedDataEngine(this);
            }
        } else {
            derivedEngine = null;
        }
    }

    public void postMountEnabled(boolean enabled) {
        ((HippoLocalItemStateManager)((org.apache.jackrabbit.core.WorkspaceImpl)session.getWorkspace()).getItemStateManager()).setEnabled(enabled);
    }

    public void postRefreshEnabled(boolean enabled) {
        ((HippoLocalItemStateManager)((org.apache.jackrabbit.core.WorkspaceImpl)session.getWorkspace()).getItemStateManager()).setRefreshing(enabled);
    }

    Node getCanonicalNode(Node node) throws RepositoryException {
        if (session instanceof XASession) {
            return ((XASessionImpl)session).getCanonicalNode(node);
        } else {
            return ((SessionImpl)session).getCanonicalNode(node);
        }
    }

    String[] getQPath(String absPath) throws NamespaceException, RepositoryException {
        NamespaceRegistry nsreg = session.getWorkspace().getNamespaceRegistry();
        Path.Element[] elements = (session instanceof XASession ? (XASessionImpl)session : (SessionImpl)session) .
            getQPath(absPath.startsWith("/") ? absPath.substring(1) : absPath).getElements();
        String[] rtelements = new String[elements.length];
        for (int i = 0; i < elements.length; i++) {
            if (nsreg.getPrefix(elements[i].getName().getNamespaceURI()).equals("")) {
                rtelements[i] = elements[i].getName().getLocalName();
            } else {
                rtelements[i] = nsreg.getPrefix(elements[i].getName().getNamespaceURI()) + ":"
                        + elements[i].getName().getLocalName();
            }
        }
        return rtelements;
    }

    public XAResource getXAResource() {
        return ((XASession) session).getXAResource();
    }

    @Override
    public Session impersonate(Credentials credentials) throws LoginException, RepositoryException {
        Session newSession = session.impersonate(credentials);
        return DecoratorFactoryImpl.getSessionDecorator(newSession);
    }

    public void save() throws AccessDeniedException, ConstraintViolationException, InvalidItemStateException,
            VersionException, LockException, RepositoryException {
        if (derivedEngine != null) {
            try {
                derivedEngine.save();
            } catch (VersionException ex) {
                log.warn(ex.getClass().getName() + ": " + ex.getMessage(), ex);
                throw ex;
            } catch (LockException ex) {
                log.warn(ex.getClass().getName() + ": " + ex.getMessage(), ex);
                throw ex;
            } catch (ConstraintViolationException ex) {
                log.warn(ex.getClass().getName() + ": " + ex.getMessage(), ex);
                throw ex;
            } catch (RepositoryException ex) {
                log.warn(ex.getClass().getName() + ": " + ex.getMessage(), ex);
                throw ex;
            }
        }
        try {
            postMountEnabled(false);
            super.save();
        } finally {
             postMountEnabled(true);
        }
    }

    public ContentHandler getDereferencedImportContentHandler(String parentAbsPath, int uuidBehavior,
            int referenceBehavior, int mergeBehavior) throws PathNotFoundException, ConstraintViolationException,
            VersionException, LockException, RepositoryException {
        if (session instanceof XASession) {
            return ((XASessionImpl) session).getDereferencedImportContentHandler(parentAbsPath, uuidBehavior, referenceBehavior, mergeBehavior);
        } else {
            return ((SessionImpl) session).getDereferencedImportContentHandler(parentAbsPath, uuidBehavior, referenceBehavior, mergeBehavior);
        }
    }

    public void importDereferencedXML(String parentAbsPath, InputStream in, int uuidBehavior, int referenceBehavior,
            int mergeBehavior) throws IOException, PathNotFoundException, ItemExistsException,
            ConstraintViolationException, VersionException, InvalidSerializedDataException, LockException,
            RepositoryException {

        if (session instanceof XASession) {
            ((XASessionImpl) session).importDereferencedXML(parentAbsPath, in, uuidBehavior, referenceBehavior, mergeBehavior);
        } else {
            ((SessionImpl) session).importDereferencedXML(parentAbsPath, in, uuidBehavior, referenceBehavior, mergeBehavior);
        }
        // run derived data engine
        derivedEngine.save();
        //session.save();
    }


    public void exportDereferencedView(String absPath, ContentHandler contentHandler, boolean binaryAsLink, boolean noRecurse)
            throws PathNotFoundException, SAXException, RepositoryException {
        Item item = getItem(absPath);
        if (!item.isNode()) {
            // there's a property, though not a node at the specified path
            throw new PathNotFoundException(absPath);
        }
        try {
            postMountEnabled(false);
            new DereferencedSysViewSAXEventGenerator((Node) item, noRecurse, binaryAsLink, contentHandler).serialize();
        } finally {
            postMountEnabled(true);
        }
    }

    public void exportDereferencedView(String absPath, OutputStream out, boolean binaryAsLink, boolean noRecurse)
            throws IOException, PathNotFoundException, RepositoryException {
        try {
            ContentHandler handler = getExportContentHandler(out);
            this.exportDereferencedView(absPath, handler, binaryAsLink, noRecurse);
        } catch (SAXException e) {
            Exception exception = e.getException();
            if (exception instanceof RepositoryException) {
                throw (RepositoryException) exception;
            } else if (exception instanceof IOException) {
                throw (IOException) exception;
            } else {
                throw new RepositoryException("Error serializing system view XML", e);
            }
        }
    }

    public void exportSystemView(String absPath, ContentHandler contentHandler, boolean binaryAsLink, boolean noRecurse)
            throws PathNotFoundException, SAXException, RepositoryException {

        // check sanity of this session
        session.isLive();

        Item item = getItem(absPath);
        if (!item.isNode()) {
            // there's a property, though not a node at the specified path
            throw new PathNotFoundException(absPath);
        }
        try {
            postMountEnabled(false);
            new PhysicalSysViewSAXEventGenerator((Node) item, noRecurse, binaryAsLink, contentHandler).serialize();
        } finally {
            postMountEnabled(true);
        }
    }

    public void exportSystemView(String absPath, OutputStream out, boolean binaryAsLink, boolean noRecurse)
            throws IOException, PathNotFoundException, RepositoryException {
        try {
            ContentHandler handler = getExportContentHandler(out);
            this.exportSystemView(absPath, handler, binaryAsLink, noRecurse);
        } catch (SAXException e) {
            Exception exception = e.getException();
            if (exception instanceof RepositoryException) {
                throw (RepositoryException) exception;
            } else if (exception instanceof IOException) {
                throw (IOException) exception;
            } else {
                throw new RepositoryException("Error serializing system view XML", e);
            }
        }
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
        if (destAbsNodePath.startsWith(srcNode.getPath()+"/")) {
            String msg = srcNode.getPath() + ": Invalid destination path (cannot be descendant of source path)";
            throw new RepositoryException(msg);
        }

        while (destAbsNodePath.startsWith("/")) {
            destAbsNodePath = destAbsNodePath.substring(1);
        }
        Node destNode = getRootNode();
        int p = destAbsNodePath.lastIndexOf("/");
        if (p > 0) {
            destNode = destNode.getNode(destAbsNodePath.substring(0, p));
            destAbsNodePath = destAbsNodePath.substring(p + 1);
        }
        try {
            destNode = destNode.addNode(destAbsNodePath, srcNode.getPrimaryNodeType().getName());
            copy(srcNode, destNode);
            return destNode;
        } catch (ConstraintViolationException ex) {
            throw new RepositoryException("Internal error", ex); // this cannot happen
        } catch (NoSuchNodeTypeException ex) {
            throw new RepositoryException("Internal error", ex); // this cannot happen
        }
    }

    public static void copy(Node srcNode, Node destNode) throws ItemExistsException, LockException, RepositoryException {
        try {
            Node canonical;
            boolean copyChildren = true;
            if (srcNode instanceof HippoNode && ((canonical = ((HippoNode) srcNode).getCanonicalNode()) == null || !canonical.isSame(srcNode))) {
                copyChildren = false;
            }

            NodeType[] mixinNodeTypes = srcNode.getMixinNodeTypes();
            for (int i = 0; i < mixinNodeTypes.length; i++) {
                destNode.addMixin(mixinNodeTypes[i].getName());
            }

            for (PropertyIterator iter = NodeDecorator.unwrap(srcNode).getProperties(); iter.hasNext();) {
                Property property = iter.nextProperty();
                if (property instanceof UpdaterProperty) {
                    if (((UpdaterProperty)property).isMultiple()) {
                        destNode.setProperty(property.getName(), property.getValues());
                    } else {
                        destNode.setProperty(property.getName(), property.getValue());
                    }
                } else {
                    PropertyDefinition definition = property.getDefinition();
                    if (!definition.isProtected()) {
                        if (definition.isMultiple())
                            destNode.setProperty(property.getName(), property.getValues());
                        else
                            destNode.setProperty(property.getName(), property.getValue());
                    }
                }
            }

            /* Do not copy virtual nodes.  Partial virtual nodes like
             * HippoNodeType.NT_FACETSELECT and HippoNodeType.NT_FACETSEARCH
             * should be copied except for their children, even if though they
             * are half virtual. On save(), the virtual part will be
             * ignored.
             */
            if (copyChildren) {
                for (NodeIterator iter = srcNode.getNodes(); iter.hasNext();) {
                    Node node = iter.nextNode();
                    if (!(node instanceof HippoNode) || ((canonical = ((HippoNode) node).getCanonicalNode()) != null && canonical.isSame(node))) {
                        Node child;
                        // check if the subnode is autocreated
                        if (!(node instanceof UpdaterNode) && node.getDefinition().isAutoCreated() && destNode.hasNode(node.getName())) {
                            child = destNode.getNode(node.getName());
                        } else {
                            child = destNode.addNode(node.getName(), node.getPrimaryNodeType().getName());
                        }
                        copy(node, child);
                    }
                }
            }
        } catch (PathNotFoundException ex) {
            throw new RepositoryException("Internal error", ex); // this cannot happen
        } catch (VersionException ex) {
            throw new RepositoryException("Internal error", ex); // this cannot happen
        } catch (ValueFormatException ex) {
            throw new RepositoryException("Internal error", ex); // this cannot happen
        } catch (ConstraintViolationException ex) {
            throw new RepositoryException("Internal error", ex); // this cannot happen
        } catch (NoSuchNodeTypeException ex) {
            throw new RepositoryException("Internal error", ex); // this cannot happen
        }
    }

    public NodeIterator pendingChanges(Node node, String nodeType, boolean prune) throws NamespaceException,
                                                                            NoSuchNodeTypeException, RepositoryException {
        NodeIterator changesIter;
        changesIter = session instanceof XASessionImpl ? ((XASessionImpl)session).pendingChanges(node, nodeType, prune)
                                                       : ((SessionImpl)session).pendingChanges(node, nodeType, prune);
        return new NodeIteratorDecorator(factory, this, changesIter);
    }

    public NodeIterator pendingChanges(Node node, String nodeType) throws NamespaceException, NoSuchNodeTypeException,
                                                                          RepositoryException {
        NodeIterator changesIter;
        changesIter = session instanceof XASessionImpl ? ((XASessionImpl)session).pendingChanges(node, nodeType, false)
                                                       : ((SessionImpl)session).pendingChanges(node, nodeType, false);
        return new NodeIteratorDecorator(factory, this, changesIter);
    }

    public NodeIterator pendingChanges(String nodeType, boolean prune) throws NamespaceException,
                                                                            NoSuchNodeTypeException, RepositoryException {
        NodeIterator changesIter;
        changesIter = session instanceof XASessionImpl ? ((XASessionImpl)session).pendingChanges(null, nodeType, prune)
                                                       : ((SessionImpl)session).pendingChanges(null, nodeType, prune);
        return new NodeIteratorDecorator(factory, this, changesIter);
    }

    public NodeIterator pendingChanges(String nodeType) throws NamespaceException, NoSuchNodeTypeException,
                                                                          RepositoryException {
        NodeIterator changesIter;
        changesIter = session instanceof XASessionImpl ? ((XASessionImpl)session).pendingChanges(null, nodeType, false)
                                                       : ((SessionImpl)session).pendingChanges(null, nodeType, false);
        return new NodeIteratorDecorator(factory, this, changesIter);
    }

    public NodeIterator pendingChanges() throws RepositoryException {
        NodeIterator changesIter;
        changesIter = session instanceof XASessionImpl ? ((XASessionImpl)session).pendingChanges(null, null, false)
                                                       : ((SessionImpl)session).pendingChanges(null, null, false);
        return new NodeIteratorDecorator(factory, this, changesIter);
    }

    private ContentHandler getExportContentHandler(OutputStream stream) throws RepositoryException {
        try {
            SAXTransformerFactory stf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
            TransformerHandler handler = stf.newTransformerHandler();

            Transformer transformer = handler.getTransformer();
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "no");

            handler.setResult(new StreamResult(stream));
            return handler;
        } catch (TransformerFactoryConfigurationError e) {
            throw new RepositoryException("SAX transformer implementation not available", e);
        } catch (TransformerException e) {
            throw new RepositoryException("Error creating an XML export content handler", e);
        }
    }
}
