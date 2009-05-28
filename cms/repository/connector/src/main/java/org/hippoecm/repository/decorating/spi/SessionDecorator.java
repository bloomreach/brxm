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
import javax.jcr.ItemExistsException;
import javax.jcr.NamespaceException;
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

import org.apache.jackrabbit.api.XASession;
import org.hippoecm.repository.api.HippoNode;
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


    static void copy(Node srcNode, Node destNode) throws ItemExistsException, LockException, RepositoryException {
        try {
            Node canonical = ((HippoNode) srcNode).getCanonicalNode();
            boolean copyChildren = (canonical != null && canonical.isSame(srcNode));

            NodeType[] mixinNodeTypes = srcNode.getMixinNodeTypes();
            for (int i = 0; i < mixinNodeTypes.length; i++) {
                destNode.addMixin(mixinNodeTypes[i].getName());
            }

            for (PropertyIterator iter = NodeDecorator.unwrap(srcNode).getProperties(); iter.hasNext();) {
                Property property = iter.nextProperty();
                PropertyDefinition definition = property.getDefinition();
                if (!definition.isProtected()) {
                    if (definition.isMultiple())
                        destNode.setProperty(property.getName(), property.getValues());
                    else
                        destNode.setProperty(property.getName(), property.getValue());
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
                    canonical = ((HippoNode) node).getCanonicalNode();
                    if (canonical != null && canonical.isSame(node)) {
                        Node child;
                        // check if the subnode is autocreated
                        if (node.getDefinition().isAutoCreated() && destNode.hasNode(node.getName())) {
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

}

