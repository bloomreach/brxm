/*
 *  Copyright 2008-2016 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.MergeException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;

import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.decorating.DecoratorFactory;
import org.hippoecm.repository.deriveddata.DerivedDataEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.repository.util.JcrConstants.ROOT_NODE_ID;

public class NodeDecorator extends org.hippoecm.repository.decorating.NodeDecorator implements HippoNode {

    private static final Logger log = LoggerFactory.getLogger(NodeDecorator.class);

    protected NodeDecorator(DecoratorFactory factory, Session session, Node node) {
        super(factory, session, node);
    }

    public Node getCanonicalNode() throws RepositoryException {
        // Note that HREPTWO-2127 is still unresolved, even though the
        // previous implementation did have problems with it, but the current
        // implementation hasn't.  The point is that if you try to perform a
        // hasPRoperty you do not have the same view as with getProperty,
        // which is wrong.
        Node canonical = ((SessionDecorator)getSession()).getCanonicalNode(node);
        if(canonical != null) {
            return factory.getNodeDecorator(session, canonical);
        } else {
            return null;
        }
    }

    @Override
    public boolean isVirtual() throws RepositoryException {
        return getIdentifier().startsWith("cafeface");
    }

    @Override
    public boolean recomputeDerivedData() throws RepositoryException {
        if(item.isNode()) {
            return ((SessionDecorator)getSession()).computeDerivedData((Node) item);
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void save() throws AccessDeniedException, ConstraintViolationException, InvalidItemStateException,
            ReferentialIntegrityException, VersionException, LockException, RepositoryException {
        if(item.isNode()) {
            ((SessionDecorator)getSession()).postSave((Node)item);
        }
        super.save();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove() throws VersionException, LockException, RepositoryException {
        try {
            ((SessionDecorator)getSession()).postMountEnabled(false);
            if(isNode()) {
                DerivedDataEngine.removal(this);
            }
            super.remove();
        } finally {
            ((SessionDecorator)getSession()).postMountEnabled(true);
        }
    }

    /**
     * internal function to access the display name for a normal, Version or VersionHistory node.
     * @param node the <em>underlying</em> node
     * @return a symbolic name of the node
     */
    static String getDisplayName(Node node) throws RepositoryException {
        //if (node.hasProperty(HippoNodeType.HIPPO_UUID) && node.hasProperty(HippoNodeType.HIPPO_SEARCH)) {
        if (node.hasProperty(HippoNodeType.HIPPO_SEARCH)) {

            // just return the resultset
            if (node.getName().equals(HippoNodeType.HIPPO_RESULTSET)) {
                return HippoNodeType.HIPPO_RESULTSET;
            }

            // the last search is the current one
            Value[] searches = node.getProperty(HippoNodeType.HIPPO_SEARCH).getValues();
            if (searches.length == 0) {
                return node.getName();
            }
            String search = searches[searches.length-1].getString();

            // check for search seperator
            if (search.indexOf("#") == -1) {
                return node.getName();
            }

            // check for sql parameter '?'
            String xpath = search.substring(search.indexOf("#")+1);
            if (xpath.indexOf('?') == -1) {
                return node.getName();
            }

            // construct query
            xpath = xpath.substring(0,xpath.indexOf('?')) + node.getName() + xpath.substring(xpath.indexOf('?')+1);

            Query query = node.getSession().getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);

            // execute
            QueryResult result = query.execute();
            RowIterator iter = result.getRows();
            if (iter.hasNext()) {
                return iter.nextRow().getValues()[0].getString();
            } else {
                return node.getName();
            }
        } else {
            return node.getName();
        }
    }

    /**
     * @inheritDoc
     */
    public Version checkin() throws VersionException, UnsupportedRepositoryOperationException,
            InvalidItemStateException, LockException, RepositoryException {
        try {
            ((SessionDecorator)getSession()).postMountEnabled(false);
            return super.checkin();
        } finally {
            ((SessionDecorator)getSession()).postMountEnabled(true);
        }
    }

    /**
     * @inheritDoc
     */
    public void checkout() throws UnsupportedRepositoryOperationException, LockException, RepositoryException {
        if(!isCheckedOut()) {
            try {
                ((SessionDecorator)getSession()).postMountEnabled(false);
                ((SessionDecorator)getSession()).postRefreshEnabled(false);
                super.checkout();
            } finally {
                ((SessionDecorator)getSession()).postMountEnabled(true);
                ((SessionDecorator)getSession()).postRefreshEnabled(true);
            }
        }
    }

    /**
     * @inheritDoc
     */
    public void removeMixin(String mixinName) throws NoSuchNodeTypeException, VersionException,
                                                     ConstraintViolationException, LockException, RepositoryException {
        try {
            ((SessionDecorator)getSession()).postMountEnabled(false);
            super.removeMixin(mixinName);
        } finally {
            ((SessionDecorator)getSession()).postMountEnabled(true);
        }
    }

    /**
     * @inheritDoc
     */
    public void orderBefore(String srcChildRelPath, String destChildRelPath)
            throws UnsupportedRepositoryOperationException, VersionException, ConstraintViolationException,
            ItemNotFoundException, LockException, RepositoryException {
        try {
            ((SessionDecorator)getSession()).postMountEnabled(false);
            super.orderBefore(srcChildRelPath, destChildRelPath);
        } finally {
            ((SessionDecorator)getSession()).postMountEnabled(true);
        }
    }

    /**
     * @inheritDoc
     */
    public NodeIterator merge(String srcWorkspace, boolean bestEffort) throws NoSuchWorkspaceException,
            AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
        try {
            ((SessionDecorator)getSession()).postMountEnabled(false);
            return super.merge(srcWorkspace, bestEffort);
        } finally {
            ((SessionDecorator)getSession()).postMountEnabled(true);
        }
    }

    /**
     * @inheritDoc
     */
    public void restore(String versionName, boolean removeExisting) throws VersionException, ItemExistsException,
            UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        try {
            ((SessionDecorator)getSession()).postMountEnabled(false);
            super.restore(versionName, removeExisting);
        } finally {
            ((SessionDecorator)getSession()).postMountEnabled(true);
        }
    }

    /**
     * @inheritDoc
     */
    public void restore(Version version, boolean removeExisting) throws VersionException, ItemExistsException,
            UnsupportedRepositoryOperationException, LockException, RepositoryException {
        try {
            ((SessionDecorator)getSession()).postMountEnabled(false);
            super.restore(version, removeExisting);
        } finally {
            ((SessionDecorator)getSession()).postMountEnabled(true);
        }
    }

    /**
     * @inheritDoc
     */
    public void restore(Version version, String relPath, boolean removeExisting) throws PathNotFoundException,
            ItemExistsException, VersionException, ConstraintViolationException,
            UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        try {
            ((SessionDecorator)getSession()).postMountEnabled(false);
            super.restore(version, relPath, removeExisting);
        } finally {
            ((SessionDecorator)getSession()).postMountEnabled(true);
        }
    }

    /**
     * @inheritDoc
     */
    public void restoreByLabel(String versionLabel, boolean removeExisting) throws VersionException,
            ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException,
            RepositoryException {
        try {
            ((SessionDecorator)getSession()).postMountEnabled(false);
            super.restoreByLabel(versionLabel, removeExisting);
        } finally {
            ((SessionDecorator)getSession()).postMountEnabled(true);
        }
    }

    @Override
    public String getDisplayName() throws RepositoryException {
        Node node = this;
        if (isVirtual()) {
            node = getCanonicalNode();
            if (node == null) {
                return getName();
            }
        }
        if (!node.isNodeType(HippoNodeType.NT_NAMED)) {
            if (ROOT_NODE_ID.equals(node.getIdentifier())) {
                return getName();
            }
            final Node parent = node.getParent();
            if (parent.isNodeType(HippoNodeType.NT_HANDLE) && parent.isNodeType(HippoNodeType.NT_NAMED)) {
                node = parent;
            }
            else {
                return getName();
            }
        }
        return node.getProperty(HippoNodeType.HIPPO_NAME).getString();
    }

}
