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

import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.MergeException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;

import org.apache.jackrabbit.core.NodeImpl;

import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.jackrabbit.HippoNodeId;
import org.hippoecm.repository.decorating.DecoratorFactory;
import org.hippoecm.repository.DerivedDataEngine;

public class NodeDecorator extends org.hippoecm.repository.decorating.NodeDecorator implements HippoNode {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    protected Node node;
    protected HippoSession session;

    protected NodeDecorator(DecoratorFactory factory, Session session, Node node) {
        super(factory, session, node);
        this.session = (HippoSession) session;
        this.node = node;
    }

    public Node getCanonicalNode() throws RepositoryException {
        if (hasProperty("hippo:uuid")) {
            return getSession().getNodeByUUID(getProperty("hippo:uuid").getString());
        } else if(((NodeImpl)node).getId() instanceof HippoNodeId) {
            return null;
        } else {
            return this;
        }
    }

    /** {@inheritDoc} */
    public void save() throws AccessDeniedException, ConstraintViolationException, InvalidItemStateException,
            ReferentialIntegrityException, VersionException, LockException, RepositoryException {
        if(item.isNode()) {
            ((SessionDecorator)getSession()).derivedEngine.save((Node)item);
        }
        super.save();
    }

    /** {@inheritDoc} */
    public void remove() throws VersionException, LockException, RepositoryException {
        if(isNode()) {
            DerivedDataEngine.removal((Node)this);
        }
        super.remove();
    }

    public String getDisplayName() throws RepositoryException {
        //if (hasProperty(HippoNodeType.HIPPO_UUID) && hasProperty(HippoNodeType.HIPPO_SEARCH)) {
        if (hasProperty(HippoNodeType.HIPPO_SEARCH)) {

            // just return the resultset
            if (getName().equals(HippoNodeType.HIPPO_RESULTSET)) {
                return HippoNodeType.HIPPO_RESULTSET;
            }

            // the last search is the current one
            Value[] searches = getProperty(HippoNodeType.HIPPO_SEARCH).getValues();
            if (searches.length == 0) {
                return getName();
            }
            String search = searches[searches.length-1].getString();

            // check for search seperator
            if (search.indexOf("#") == -1) {
                return getName();
            }

            // check for sql parameter '?'
            String xpath = search.substring(search.indexOf("#")+1);
            if (xpath.indexOf('?') == -1) {
                return getName();
            }

            // construct query
            xpath = xpath.substring(0,xpath.indexOf('?')) + getName() + xpath.substring(xpath.indexOf('?')+1);

            Query query = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);

            // execute
            QueryResult result = query.execute();
            RowIterator iter = result.getRows();
            if (iter.hasNext()) {
                return iter.nextRow().getValues()[0].getString();
            } else {
                return getName();
            }
        } else {
            return getName();
        }
    }

    /**
     * internal function to access the canonical node for a normal, Version or VersionHistory node.
     * @param unwrapped the <em>underlying</em> node
     * @param wrapped the <em>decorated</em> node
     * @return an decorated canonical node
     */
    static Node getCanonicalNode(Node wrapped, Node unwrapped) throws RepositoryException {
        if (unwrapped.hasProperty("hippo:uuid")) {
            return unwrapped.getSession().getNodeByUUID(unwrapped.getProperty("hippo:uuid").getString());
        } else if(((NodeImpl)unwrapped).getId() instanceof HippoNodeId) {
            return null;
        } else {
            return wrapped;
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
}
