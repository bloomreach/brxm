/*
 *  Copyright 2008-2011 Hippo.
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
package org.hippoecm.repository.ocm;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.lock.LockException;

import org.apache.jackrabbit.JcrConstants;
import org.datanucleus.StateManager;
import org.datanucleus.store.fieldmanager.AbstractFieldManager;
import org.hippoecm.repository.api.HierarchyResolver;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FieldManagerImpl extends AbstractFieldManager {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    protected final Logger log = LoggerFactory.getLogger(FieldManagerImpl.class);

    private StateManager sm;
    private Session session;
    private Node node;
    private Node types;

    FieldManagerImpl(StateManager sm, Session session, Node types, Node node) {
        this.sm = sm;
        this.session = session;
        this.node = node;
        this.types = types;
    }

    FieldManagerImpl(StateManager sm, Session session, Node types) {
        this.sm = sm;
        this.session = session;
        this.node = null;
        this.types = types;
    }

    static class Entry {
        Node node;
        String relPath;
    }

    private Node getNode(Node node, String field, String nodetype) throws RepositoryException {
        HierarchyResolver.Entry last = new HierarchyResolver.Entry();
        node = (Node)((HippoWorkspace)session.getWorkspace()).getHierarchyResolver().getItem(node, field, false, last);
        if (node == null && last.node != null) {
            if(!last.node.isCheckedOut()) {
                last.node.checkout();
            }
            if (nodetype != null) {
                node = last.node.addNode(last.relPath, nodetype);
            } else {
                node = last.node.addNode(last.relPath);
            }
            if(node.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                node.addMixin(HippoNodeType.NT_HARDDOCUMENT);
            } else if(node.isNodeType(HippoNodeType.NT_REQUEST)) {
                node.addMixin("mix:referenceable");
            }
        }
        return node;
    }

    private void checkoutNode(Node node) throws UnsupportedRepositoryOperationException, LockException, ItemNotFoundException, AccessDeniedException, RepositoryException {
        Node root = node.getSession().getRootNode();
        Node versionable = node;
        while (!versionable.isSame(root)) {
            if (versionable.isNodeType(JcrConstants.MIX_VERSIONABLE)) {
                versionable.checkout();
                break;
            }
            versionable = versionable.getParent();
        }
    }
}
