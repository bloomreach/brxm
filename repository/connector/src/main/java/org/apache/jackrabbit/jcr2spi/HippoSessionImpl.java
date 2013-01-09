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
package org.apache.jackrabbit.jcr2spi;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.NamespaceException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;

import org.apache.jackrabbit.jcr2spi.config.RepositoryConfig;
import org.apache.jackrabbit.jcr2spi.hierarchy.HierarchyManager;
import org.apache.jackrabbit.jcr2spi.state.HippoSessionItemStateManager;
import org.apache.jackrabbit.jcr2spi.state.ItemState;
import org.apache.jackrabbit.jcr2spi.state.ItemStateFactory;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.state.SessionItemStateManager;
import org.apache.jackrabbit.jcr2spi.state.UpdatableItemStateManager;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.spi.XASessionInfo;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>HippoSessionImpl</code>...
 */
public class HippoSessionImpl extends SessionImpl implements NamespaceResolver, ManagerProvider {

    private static Logger log = LoggerFactory.getLogger(SessionImpl.class);

    private Repository repository;

    private SessionInfo sessionInfo;

    private RepositoryConfig config;

    private HippoSessionItemStateManager sessionItemStateManager;

    private ItemManager itemManager;
    
    HippoSessionImpl(SessionInfo sessionInfo, Repository repository, RepositoryConfig config)
        throws RepositoryException {
        super(sessionInfo, repository, config);
        this.repository = repository;
        this.config = config;
        this.sessionInfo = sessionInfo;
    }


    @Override
    protected SessionItemStateManager createSessionItemStateManager(UpdatableItemStateManager wsStateManager,
                                                                    ItemStateFactory isf) throws RepositoryException {
        sessionItemStateManager = new HippoSessionItemStateManager(wsStateManager, getValidator(), getQValueFactory(), isf, this);
        return sessionItemStateManager;
    }

    @Override
    protected ItemManager createItemManager(HierarchyManager hierarchyManager) {
        itemManager = super.createItemManager(hierarchyManager);
        return itemManager;
    }


    @Override
    HippoSessionImpl switchWorkspace(String workspaceName) throws AccessDeniedException, NoSuchWorkspaceException,
                                                                  RepositoryException {
        checkAccessibleWorkspace(workspaceName);
        SessionInfo info = config.getRepositoryService().obtain(sessionInfo, workspaceName);
        if (info instanceof XASessionInfo) {
            return new HippoXASessionImpl((XASessionInfo) info, repository, config);
        } else {
            return new HippoSessionImpl(info, repository, config);
        }
    }

    public NodeIterator pendingChanges(Node node, String nodeType, boolean prune) throws NamespaceException,
                                                                            NoSuchNodeTypeException, RepositoryException {
        NodeState target;
        if(node != null) {
            target = (NodeState) ((NodeImpl)node).getItemState();
        } else {
            target = getHierarchyManager().getRootEntry().getNodeState();
        }
        Set affected = sessionItemStateManager.pendingChanges(target);
        Set<Node> nodes = new LinkedHashSet<Node>();
        for(Iterator iter = affected.iterator(); iter.hasNext(); ) {
            ItemState state = (ItemState) iter.next();
            if (!state.isValid()) {
                continue;
            }
            boolean propChange = false;
            if(!state.isNode()) {
                state = state.getParent();
                propChange = true;
            }
            Node candidate = (Node) itemManager.getItem(state.getHierarchyEntry());
            if(nodeType != null && !candidate.isNodeType(nodeType))
                continue;
            if(!propChange && node != null && node.isSame(candidate))
                continue;

            nodes.add(candidate);
        }
        if (prune) {
            for (Iterator<Node> iter = nodes.iterator(); iter.hasNext();) {
                Node ancestor = iter.next();
                while (ancestor.getDepth() > 0) {
                    ancestor = ancestor.getParent();
                    if (node != null && node.equals(ancestor)) {
                        break;
                    }
                    if (nodes.contains(ancestor)) {
                        iter.remove();
                        break;
                    }
                }
            }
        }
        return new SetNodeIterator(nodes);
    }

    static class SetNodeIterator implements NodeIterator {
        Iterator<Node> iter;
        long position = 0;
        long size;

        public SetNodeIterator(Set<Node> nodes) {
            iter = nodes.iterator();
            position = -1;
            size = nodes.size();
        }

        public Object next() {
            ++position;
            return iter.next();
        }

        public Node nextNode() {
            ++position;
            return iter.next();
        }

        public long getPosition() {
            return position;
        }

        public long getSize() {
            return size;
        }

        public void skip(long count) {
            while (count > 0)
                next();
        }

        public void remove() {
            iter.remove();
            --size;
        }

        public boolean hasNext() {
            return iter.hasNext();
        }
    }
}
