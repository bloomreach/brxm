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
        Node target = org.hippoecm.repository.decorating.NodeDecorator.unwrap(node);
        return ((org.apache.jackrabbit.jcr2spi.HippoSessionImpl)session).pendingChanges(target, nodeType, prune);
    }

    public NodeIterator pendingChanges(Node node, String nodeType) throws NamespaceException, NoSuchNodeTypeException,
                                                                          RepositoryException {
        Node target = org.hippoecm.repository.decorating.NodeDecorator.unwrap(node);
        return ((org.apache.jackrabbit.jcr2spi.HippoSessionImpl)session).pendingChanges(target, nodeType, false);
    }

    public NodeIterator pendingChanges() throws RepositoryException {
        return ((org.apache.jackrabbit.jcr2spi.HippoSessionImpl)session).pendingChanges(null, null, false);
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
