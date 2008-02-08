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
package org.hippoecm.repository.jackrabbit;

import java.util.Set;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NamespaceException;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.ItemImpl;
import org.apache.jackrabbit.core.ItemLifeCycleListener;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.nodetype.NodeDefId;
import org.apache.jackrabbit.core.nodetype.NodeDefinitionImpl;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.state.SessionItemStateManager;
import org.apache.jackrabbit.core.util.Dumpable;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.name.NameConstants;

public class ItemManager extends org.apache.jackrabbit.core.ItemManager {
    private static Logger log = LoggerFactory.getLogger(ItemManager.class);

    private NodeDefinition rootNodeDef;
    private NodeId rootNodeId;
    private HierarchyManager hierMgr;
    private ItemStateManager itemStateProvider;
    Set itemCache;

    protected ItemManager(SessionItemStateManager itemStateProvider, HierarchyManager hierMgr,
                          SessionImpl session, NodeDefinition rootNodeDef,
                          NodeId rootNodeId) {
        super(itemStateProvider, hierMgr, session, rootNodeDef, rootNodeId);
        this.rootNodeDef = rootNodeDef;
        this.rootNodeId = rootNodeId;
        this.hierMgr = hierMgr;
        this.itemStateProvider = itemStateProvider;
        itemCache = new HashSet();
    }

    /*
    @Override
    org.apache.jackrabbit.core.NodeImpl createNodeInstance(NodeState state, NodeDefinition def) throws RepositoryException {
        if (state.getNodeTypeName().equals(HippoNameConstants.NT_VERSION) ||
            state.getNodeTypeName().equals(HippoNameConstants.NT_VERSIONHISTORY)) {
            return super.createNodeInstance(state, def);
        } else {
            NodeId id = state.getNodeId();
            ItemLifeCycleListener[] listeners = new ItemLifeCycleListener[]{this};
            return new NodeImpl(this, session, id, state, def, listeners);
        }
    }
    */

    private boolean inCache(ItemId id) {
        synchronized (itemCache) {
            return itemCache.contains(id);
        }
    }

    private NodeDefinition getDefinition(NodeState state)
            throws RepositoryException {
        NodeDefId defId = state.getDefinitionId();
        NodeDefinitionImpl def = session.getNodeTypeManager().getNodeDefinition(defId);
        if (def == null) {
            /**
             * todo need proper way of handling inconsistent/corrupt definition
             * e.g. 'flag' items that refer to non-existent definitions
             */
            log.warn("node at " + state.getNodeId() + " has invalid definitionId (" + defId + ")");

            // fallback: try finding applicable definition
            NodeImpl parent = (NodeImpl) getItem(state.getParentId());
            NodeState parentState = (NodeState) parent.getItemState();
            NodeState.ChildNodeEntry cne = parentState.getChildNodeEntry(state.getNodeId());
            def = parent.getApplicableChildNodeDefinition(cne.getName(), state.getNodeTypeName());
            state.setDefinitionId(def.unwrap().getId());
        }
        return def;
    }

    org.apache.jackrabbit.core.NodeImpl getRootNode() throws RepositoryException {
        return (org.apache.jackrabbit.core.NodeImpl) getItem(rootNodeId);
    }

    public ItemImpl getItem(Path path)
            throws PathNotFoundException, AccessDeniedException, RepositoryException {
        ItemId id = hierMgr.resolvePath(path);
        if (id == null) {
            throw new PathNotFoundException(safeGetJCRPath(path));
        }
        try {
            return getItem(id);
        } catch (ItemNotFoundException infe) {
            throw new PathNotFoundException(safeGetJCRPath(path));
        }
    }

    public synchronized ItemImpl getItem(ItemId id)
            throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        ItemImpl item = null;
        // check cache
        if(inCache(id))
            return super.getItem(id);
        if (item == null) {
            // not yet in cache, need to create instance:
            // check privileges
            if (!session.getAccessManager().isGranted(id, AccessManager.READ)) {
                throw new AccessDeniedException("cannot read item " + id);
            }
            // create instance of item
            item = createItemInstance(id);
            if (item == null) {
                return super.getItem(id);
            }
        }
        return item;
    }

    public synchronized ItemImpl getItem(ItemState state)
            throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        ItemImpl item = null;
        ItemId id = state.getId();
        // check cache
        if(inCache(id))
            return super.getItem(id);
        if (item == null) {
            // not yet in cache, need to create instance:
            // only check privileges if state is not new
            if (state.getStatus() != ItemState.STATUS_NEW
                    && !session.getAccessManager().isGranted(id, AccessManager.READ)) {
                throw new AccessDeniedException("cannot read item " + id);
            }
            // create instance of item
            item = createItemInstance(id);
            if (item == null) {
                return super.getItem(state);
            }
        }
        return item;
    }

    //-------------------------------------------------< item factory methods >
    private ItemImpl createItemInstance(ItemId id)
            throws ItemNotFoundException, RepositoryException {
        // create instance of item using its state object
        ItemImpl item;
        ItemState state;
        try {
            state = itemStateProvider.getItemState(id);
        } catch (NoSuchItemStateException nsise) {
            throw new ItemNotFoundException(id.toString());
        } catch (ItemStateException ise) {
            String msg = "failed to retrieve item state of item " + id;
            log.error(msg, ise);
            throw new RepositoryException(msg, ise);
        }

        if (id.equals(rootNodeId)) {
            // special handling required for root node
            item = createNodeInstance((NodeState) state, rootNodeDef);
        } else if (state.isNode()) {
            item = createNodeInstance((NodeState) state);
        } else {
            item = null;
        }
        return item;
    }

    org.apache.jackrabbit.core.NodeImpl createNodeInstance(NodeState state, NodeDefinition def)
            throws RepositoryException {
        NodeId id = state.getNodeId();
        // we want to be informed on life cycle changes of the new node object
        // in order to maintain item cache consistency
        ItemLifeCycleListener[] listeners = new ItemLifeCycleListener[]{this};

        // check special nodes
        if (state.getNodeTypeName().equals(NameConstants.NT_VERSION)) {
            return createVersionInstance(id, state, def, listeners);

        } else if (state.getNodeTypeName().equals(NameConstants.NT_VERSIONHISTORY)) {
            return createVersionHistoryInstance(id, state, def, listeners);

        } else {
            // create node object
            return new NodeImpl(this, session, id, state, def, listeners);
        }
    }

    org.apache.jackrabbit.core.NodeImpl createNodeInstance(NodeState state) throws RepositoryException {
        // 1. get definition of the specified node
        NodeDefinition def = getDefinition(state);
        // 2. create instance
        return createNodeInstance(state, def);
    }

    String safeGetJCRPath(Path path) {
        try {
            return session.getJCRPath(path);
        } catch (NamespaceException e) {
            log.error("failed to convert " + path.toString() + " to JCR path.");
            // return string representation of internal path as a fallback
            return path.toString();
        }
    }

    public void itemCreated(ItemImpl item) {
        synchronized (itemCache) {
            ItemId id = item.getId();
            itemCache.add(id);
            super.itemCreated(item);
        }
    }

    public void itemInvalidated(ItemId id, ItemImpl item) {
        synchronized (itemCache) {
            itemCache.remove(id);
            super.itemInvalidated(id, item);
        }
    }

    public void itemDestroyed(ItemId id, ItemImpl item) {
        synchronized (itemCache) {
            itemCache.remove(id);
            super.itemDestroyed(id, item);
        }
    }
}
