/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.replication.replicators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.ItemNotFoundException;

import org.apache.jackrabbit.core.cluster.ChangeLogRecord;
import org.apache.jackrabbit.core.config.ConfigurationException;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.spi.Name;
import org.hippoecm.repository.replication.FatalReplicationException;
import org.hippoecm.repository.replication.Filter;
import org.hippoecm.repository.replication.RecoverableReplicationException;
import org.hippoecm.repository.replication.Replicator;
import org.hippoecm.repository.replication.ReplicatorContext;
import org.hippoecm.repository.replication.ReplicatorHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A base class for {@link Replicator}s. It handles all local parsing, logic and {@link Filter}s. and calls the remote 
 * methods from the implementations.
 * <p/>
 * Note that the whole class is synchronized because the single way this class is used is 
 * through the synchronized {@link #replicate(ChangeLogRecord)} method.
 */
abstract class AbstractReplicator implements Replicator {

    /** Logger. */
    private static final Logger log = LoggerFactory.getLogger(AbstractReplicator.class);

    /** The replicator context. */
    protected ReplicatorContext context;

    /** The replicator helper. */
    protected ReplicatorHelper helper;

    /** The filters that are configured for this replicator. */
    protected List<Filter> filters = new ArrayList<Filter>();

    /** A cache to for filter exclusion, gets cleared at start of replication of a revision */
    private final Map<NodeId, Boolean> exclusionCache = new HashMap<NodeId, Boolean>();

    /** Stack to collect all changed {@link ItemState}s in {@link StateCollection}s. */
    protected StateCollectionStack stack = new StateCollectionStack();

    /**
     * {@inheritDoc}
     * <p/>
     * Calls {@link #postInit()} for implementation specific initialization.
     */
    final public void init(ReplicatorContext context, List<Filter> filters) throws ConfigurationException {
        this.context = context;
        this.filters = filters;
        this.helper = new ReplicatorHelper(context);
        initFilters();
        postInit();
        log.info("Replicator '{}': initialized.", getId());
    }

    protected void initFilters() throws ConfigurationException {
        for (Filter filter : filters) {
            filter.init(context);
        }
    }

    /**
     * Post initialization hook for implementations.
     */
    protected void postInit() {
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Calls {@link #preDestroy()} for implementation specific destroy.
     */
    final public void destroy() {
        preDestroy();
        destroyFilters();
        log.info("Replicator '{}': destroyed.", getId());
    }

    /**
     * Pre-destroy hook for implementations.
     */
    protected void preDestroy() {
    }

    /**
     * Stop filters.
     */
    protected void destroyFilters() {
        for (Filter filter : filters) {
            filter.destroy();
        }
    }

    /**
     * {@inheritDoc}
     * This method is synchronized, effectively synchronizing the whole class. This way it is
     * guaranteed that all {@link ChangeLogRecord}s are replicated in the correct order.
     */
    final synchronized public void replicate(ChangeLogRecord record) throws RecoverableReplicationException,
            FatalReplicationException {
        log.debug("Replicator '{}': start replication of revision {}.", getId(), record.getRevision());

        stack.clear();
        exclusionCache.clear();

        parseChanges(record.getChanges());

        connect();

        replicateChanges();

        saveChanges();

        disconnect();

        log.debug("Replicator '{}': replicated revision {}.", getId(), record.getRevision());
    }

    /**
     * Parse the {@link ChangeLog} and create the {@link StateCollection}s.
     * 
     * 
     * @param changeLog the {@link ChangeLog}
     */
    @SuppressWarnings("unchecked")
    private void parseChanges(ChangeLog changeLog) {
        addStatesToStateCollections(changeLog.addedStates(), StateCollection.ADD_NODE);
        addStatesToStateCollections(changeLog.modifiedStates(), StateCollection.MODIFY_NODE);
        addStatesToStateCollections(changeLog.deletedStates(), StateCollection.DELETE_NODE);
        // TODO: what about changeLog.modifiedRefs() ?
    }

    /**
     * Helper method to all {@link ItemState}s of an {@link Iterator} to a {@link StateCollection}.
     * 
     * @param iter the {@link Iterator} of {@link ItemState}s
     * @param operation the type of node operation. 
     * @see StateCollection
     */
    private void addStatesToStateCollections(Iterable<ItemState> iterable, int operation) {
        for(ItemState state : iterable) {
            StateCollection collection = getOrCreateStateCollection(state);
            collection.addState(state, operation);
        }
    }

    /**
     * Replicate the {@link StateCollection}s in the {@link #stack}. This method is
     * implicitly synchronized by the {@link #replicate(ChangeLogRecord)} method.
     */
    private void replicateChanges() {
        while (stack.hasStateCollections()) {
            StateCollection collection = stack.popStateCollection();
            NodeId id = collection.getId();
            if (collection.getNodeOperation() == StateCollection.DELETE_NODE) {
                removeNodeIfExists(id);
            } else if (collection.getNodeOperation() == StateCollection.ADD_NODE) {
                try {
                    addNode(id);
                } catch (ItemNotFoundException e) {
                    log.warn("Unable to add node, node not found locally with id '{}'", id);
                }
            } else if (collection.getNodeOperation() == StateCollection.MODIFY_NODE) {
                try {
                    modifyNode(collection);
                } catch (ItemNotFoundException e) {
                    log.warn("Unable to modify node, node not found locally with id '{}'", id);
                }
            } else {
                log.error("Replicator '{}': Ignoring invalid node operation {}.", getId(), collection
                        .getNodeOperation());
            }
        }
    }

    private void saveChanges() {
        saveRemoteChanges();
    }

    private void addNode(NodeId id) throws ItemNotFoundException {
        if (isNodeExcludedAndHasToBeRemoved(id)) {
            removeNodeIfExists(id);
        } else if (isNodeExcludedByFilters(id)) {
            return;
        } else {
            removeAndAddNode(id);
        }
    }
    
    private void removeAndAddNode(NodeId id) throws ItemNotFoundException {
        removeNodeIfExists(id);
        NodeId missingId = findFirstNonExistingParent(id);
        addNodeAndChilderen(missingId);
    }

    private void removeNodeIfExists(NodeId id) {
        if (helper.isRootNodeId(id)) {
            log.debug("Root node cannot be removed.");
            return;
        }
        if (hasRemoteNode(id)) {
            removeRemoteNode(id);
        }
    }

    private NodeId findFirstNonExistingParent(NodeId id) throws ItemNotFoundException {
        NodeState state = helper.getNodeState(id);
        if (state == null) {
            throw new ItemNotFoundException(id.toString());
        }
        NodeId parentId = state.getParentId();
        while (!hasRemoteNode(parentId)) {
            state = helper.getNodeState(parentId);
            parentId = state.getParentId();
        }
        return state.getNodeId();
    }

    private void modifyNode(StateCollection collection) throws ItemNotFoundException {
        NodeId id = collection.getId();
        if (isNodeExcludedAndHasToBeRemoved(id)) {
            removeNodeAndCheckParent(id);
            return;
        }
        if (isNodeExcludedByFilters(id)) {
            return;
        }

        if (hasRemoteNode(id)) {
            if (hasNodeMoved(id)) {
                // TODO: this could be optimized for moves
                removeAndAddNode(id);
                return;
            } else {
                modifyProperties(collection);
                return;
            }
        } else {
            NodeId missingId = findFirstNonExistingParent(id);
            addNodeAndChilderen(missingId);
            return;
        }
    }

    private void removeNodeAndCheckParent(NodeId id) {
        removeNodeIfExists(id);
        // because the node is removed the parent is modified and has to be checked.
        NodeState state = helper.getNodeState(id);
        if (isNodeExcludedAndHasToBeRemoved(state.getParentId())) {
            removeNodeAndCheckParent(state.getParentId());
        }
    }

    private boolean hasNodeMoved(NodeId id) {
        if (!hasRemoteNode(id)) {
            return false;
        }
        String localPath = helper.getJCRPath(id);
        String remotePath = getRemoteJCRPath(id);
        if (localPath.equals(remotePath)) {
            return false;
        } else {
            return true;
        }
    }

    private void modifyProperties(StateCollection collection) {
        setRemoteProperties(collection.getId(), collection.addedStates());
        setRemoteProperties(collection.getId(), collection.modifiedStates());
        removeRemoteProperties(collection.getId(), collection.deletedStates());
    }

    @SuppressWarnings("unchecked")
    private void addChilderen(NodeId parentId) {
        NodeState state = helper.getNodeState(parentId);
        Iterator<ChildNodeEntry> iter = state.getChildNodeEntries().iterator();
        while (iter.hasNext()) {
            ChildNodeEntry entry = iter.next();
            if (!hasRemoteNode(entry.getId())) {
                addNodeAndChilderen(entry.getId());
            }
        }
    }

    private void addNodeAndChilderen(NodeId id) {
        if (isNodeExcludedByFilters(id)) {
            return;
        }
        NodeState state = helper.getNodeState(id);
        addRemoteNode(state.getParentId(), id);
        addChilderen(id);
    }

    protected boolean isNodeExcludedByFilters(NodeId id) {
        if (exclusionCache.containsKey(id)) {
            return exclusionCache.get(id);
        }
        for (Filter filter : filters) {
            if (filter.isNodeExcluded(id)) {
                exclusionCache.put(id, Boolean.valueOf(true));
                return true;
            }
        }
        if (isParentExcludedByFilters(id)) {
            exclusionCache.put(id, Boolean.valueOf(true));
            return true;
        }
        exclusionCache.put(id, Boolean.valueOf(false));
        return false;
    }

    private boolean isParentExcludedByFilters(NodeId id) {
        NodeState state = helper.getNodeState(id);
        if (state != null && !helper.isRootNodeId(state.getParentId())) {
            if (isNodeExcludedByFilters(state.getParentId())) {
                return true;
            } else {
                return isParentExcludedByFilters(state.getParentId());
            }
        }
        return false;
    }

    protected boolean isNodeExcludedAndHasToBeRemoved(NodeId id) {
        for (Filter filter : filters) {
            if (filter.removeExistingExcludedNodes() && filter.isNodeExcluded(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Helper method to get from or create in the {@link #stack} the {@link StateCollection} for the {@link ItemState}.
     * 
     * @param state the {@link ItemState}
     */
    private StateCollection getOrCreateStateCollection(ItemState state) {
        NodeId id = null;
        if (state.isNode()) {
            id = (NodeId) state.getId();
        } else {
            id = (NodeId) state.getParentId();
        }
        if (!stack.hasStateCollection(id)) {
            stack.pushStateCollection(new StateCollection(id));
        }
        return stack.peek(id);
    }

    protected boolean propertyIsVirtual(Name propName) {
        if ("hippo:count".equals(helper.getJCRName(propName))) {
            return true;
        }
        return false;
    }

    /**
     * The implementations must provide an id string. This method is NOT called before the
     * {@link #postInit()} hook.
     * @return the id
     */
    abstract protected String getId();

    abstract protected void connect();

    abstract protected void disconnect();

    abstract protected void saveRemoteChanges();

    abstract protected boolean hasRemoteNode(NodeId id);

    abstract protected String getRemoteJCRPath(NodeId id);

    abstract protected void removeRemoteNode(NodeId id);

    abstract protected void addRemoteNode(NodeId parentId, NodeId id);

    abstract protected void setRemoteProperties(NodeId id, Iterator<ItemState> stateIter);

    abstract protected void removeRemoteProperties(NodeId id, Iterator<ItemState> stateIter);

}
