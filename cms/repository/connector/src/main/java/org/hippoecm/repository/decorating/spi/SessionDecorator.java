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
package org.hippoecm.repository.decorating.spi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
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
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;

import org.apache.jackrabbit.api.XASession;

import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.decorating.DecoratorFactory;

public class SessionDecorator extends org.hippoecm.repository.decorating.SessionDecorator {

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    HippoSession remoteSession;

    protected SessionDecorator(DecoratorFactory factory, Repository repository, Session session, HippoSession remoteSession) {
        super(factory, repository, session);
        this.remoteSession = remoteSession;
    }

    protected SessionDecorator(DecoratorFactory factory, Repository repository, XASession session, HippoSession remoteSession) throws RepositoryException {
        super(factory, repository, session);
        this.remoteSession = remoteSession;
    }

    public Node copy(Node srcNode, String destAbsNodePath) throws PathNotFoundException, ItemExistsException,
            LockException, VersionException, RepositoryException {
        Node node = remoteSession.copy(remoteSession.getRootNode().getNode(srcNode.getPath().substring(1)), destAbsNodePath);
        String path = node.getPath();
        Node parent = session.getRootNode().getNode(path.substring(1,path.lastIndexOf("/")));
        parent.refresh(false);
        return factory.getNodeDecorator(this, parent.getNode(path.substring(path.lastIndexOf("/")+1)));
    }

    public void save() throws AccessDeniedException, ConstraintViolationException, InvalidItemStateException,
            VersionException, LockException, RepositoryException {
        super.save();
        remoteSession.save();
    }

    public void refresh(boolean keepChanges) throws RepositoryException {
        remoteSession.refresh(keepChanges);
        super.refresh(keepChanges);
    }

    public NodeIterator pendingChanges(Node node, String nodeType, boolean prune) throws NamespaceException,
                                                                            NoSuchNodeTypeException, RepositoryException {
        // FIXME probably broken
        return new NodeIteratorPendingChanges(factory, this, remoteSession.pendingChanges(remoteSession.getRootNode().getNode(node.getPath().substring(1)), nodeType, prune));
    }

    public NodeIterator pendingChanges(Node node, String nodeType) throws NamespaceException, NoSuchNodeTypeException,
                                                                          RepositoryException {
        // FIXME probably broken
        return new NodeIteratorPendingChanges(factory, this, remoteSession.pendingChanges(remoteSession.getRootNode().getNode(node.getPath().substring(1)), nodeType));
    }

    public NodeIterator pendingChanges() throws RepositoryException {
        // FIXME probably broken
        return new NodeIteratorPendingChanges(factory, this, remoteSession.pendingChanges());
    }

    private class NodeIteratorPendingChanges extends org.hippoecm.repository.decorating.RangeIteratorDecorator implements NodeIterator {
        public NodeIteratorPendingChanges(DecoratorFactory factory, Session session, NodeIterator iterator) {
            super(factory, session, iterator);
        }

        public Node nextNode() {
            try {
                Node node = (Node) next();
                String path = node.getPath();
                return factory.getNodeDecorator(session, session.getRootNode().getNode(path.substring(1)));
            } catch(RepositoryException ex) {
                return null;
            }
        }

        public Object next() {
            try {
                Object object = iterator.next();
                if (object instanceof Version) {
                    return factory.getVersionDecorator(session, (Version) session.getRootNode().getNode(((Version) object).getPath().substring(1)));
                } else if (object instanceof VersionHistory) {
                    return factory.getVersionHistoryDecorator(session, (VersionHistory) session.getRootNode().getNode(((VersionHistory) object).getPath().substring(1)));
                } else if (object instanceof Node) {
                    return factory.getNodeDecorator(session, session.getRootNode().getNode(((Node) object).getPath().substring(1)));
                } else if (object instanceof Property) {
                    return factory.getPropertyDecorator(session, session.getRootNode().getProperty(((Property) object).getPath().substring(1)));
                } else if (object instanceof Item) {
                    throw new UnsupportedOperationException("No decorator available for " + object);
                } else {
                    throw new UnsupportedOperationException("No decorator available for " + object);
                }

            } catch(RepositoryException ex) {
                return null;
            }
        }
    }

    public void exportDereferencedView(String absPath, OutputStream out, boolean binaryAsLink, boolean noRecurse)
        throws IOException, PathNotFoundException, RepositoryException {
        remoteSession.exportDereferencedView(absPath, out, binaryAsLink, noRecurse);
    }

    public void importDereferencedXML(String parentAbsPath, InputStream in, int uuidBehavior, int referenceBehavior,
            int mergeBehavior) throws IOException, PathNotFoundException, ItemExistsException,
            ConstraintViolationException, VersionException, InvalidSerializedDataException, LockException,
                                      RepositoryException {
        remoteSession.importDereferencedXML(parentAbsPath, in, uuidBehavior, referenceBehavior, mergeBehavior);
    }

    HippoSession getRemoteSession() {
        return remoteSession;
    }
}
