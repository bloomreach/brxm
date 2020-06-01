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
package org.hippoecm.repository.jackrabbit;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NamespaceException;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.observation.EventStateCollectionFactory;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateCacheFactory;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.state.SharedItemStateManager;
import org.apache.jackrabbit.core.state.StaleItemStateException;
import org.apache.jackrabbit.core.state.XAItemStateManager;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
import org.hippoecm.repository.FacetedNavigationEngine;
import org.hippoecm.repository.FacetedNavigationEngine.Context;
import org.hippoecm.repository.FacetedNavigationEngine.Query;
import org.hippoecm.repository.Modules;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.dataprovider.DataProviderContext;
import org.hippoecm.repository.dataprovider.DataProviderModule;
import org.hippoecm.repository.dataprovider.HippoNodeId;
import org.hippoecm.repository.dataprovider.HippoVirtualProvider;
import org.hippoecm.repository.dataprovider.ParameterizedNodeId;
import org.hippoecm.repository.dataprovider.StateProviderContext;
import org.hippoecm.repository.security.HippoAccessManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HippoLocalItemStateManager extends XAItemStateManager implements DataProviderContext, HandleListener {

    protected final Logger log = LoggerFactory.getLogger(HippoLocalItemStateManager.class);

    /** Mask pattern indicating a regular, non-virtual JCR item
     */
    static final int ITEM_TYPE_REGULAR = 0x00;

    /** Mask pattern indicating an externally defined node, patterns can
     * be OR-ed to indicate both external and virtual nodes.
     */
    static final int ITEM_TYPE_EXTERNAL = 0x01;

    /** Mask pattern indicating a virtual node, patterns can be OR-ed to
     * indicate both external and virtual nodes.
     */
    static final int ITEM_TYPE_VIRTUAL = 0x02;

    private NodeTypeRegistry ntReg;
    private org.apache.jackrabbit.core.SessionImpl session;
    private HierarchyManager hierMgr;
    private FacetedNavigationEngine<Query, Context> facetedEngine;
    private FacetedNavigationEngine.Context facetedContext;
    private HippoLocalItemStateManager.FilteredChangeLog filteredChangeLog = null;
    private boolean noUpdateChangeLog = false;
    private Map<String, HippoVirtualProvider> virtualProviders;
    private Map<Name, HippoVirtualProvider> virtualNodeNames;
    private Set<Name> virtualPropertyNames;
    private Set<ItemState> virtualStates = new HashSet<ItemState>();
    private Set<ItemId> modifiedExternals = new HashSet<ItemId>();
    private Map<NodeId, ItemState> virtualNodes = new HashMap<NodeId, ItemState>();
    private Map<ItemId, Object> deletedExternals = new WeakHashMap<ItemId, Object>();
    private NodeId rootNodeId;
    private final boolean virtualLayerEnabled;
    private int virtualLayerEnabledCount = 0;
    private boolean virtualLayerRefreshing = true;
    private boolean parameterizedView = false;
    private StateProviderContext currentContext = null;
    private static Modules<DataProviderModule> dataProviderModules = null;
    private boolean editFakeMode = false;
    private boolean editRealMode = false;
    private AccessManager accessManager;
    private Name handleNodeName;

    public HippoLocalItemStateManager(SharedItemStateManager sharedStateMgr, EventStateCollectionFactory factory, ItemStateCacheFactory cacheFactory, String attributeName, NodeTypeRegistry ntReg, boolean enabled, NodeId rootNodeId) {
        super(sharedStateMgr, factory, attributeName, cacheFactory);
        this.ntReg = ntReg;
        virtualLayerEnabled = enabled;
        this.rootNodeId = rootNodeId;
        virtualProviders = new HashMap<String, HippoVirtualProvider>();
        virtualNodeNames = new HashMap<Name, HippoVirtualProvider>();
        virtualPropertyNames = new HashSet<Name>();
    }

    public boolean isEnabled() {
        return virtualLayerEnabled && virtualLayerEnabledCount == 0;
    }

    public void setEnabled(boolean enabled) {
        if (enabled) {
            --virtualLayerEnabledCount;
        } else {
            ++virtualLayerEnabledCount;
        }
    }

    public void setRefreshing(boolean enabled) {
        virtualLayerRefreshing = enabled;
    }

    public NodeTypeRegistry getNodeTypeRegistry() {
        return ntReg;
    }

    public HierarchyManager getHierarchyManager() {
        return hierMgr;
    }

    public FacetedNavigationEngine<FacetedNavigationEngine.Query, Context> getFacetedEngine() {
        return facetedEngine;
    }

    public FacetedNavigationEngine.Context getFacetedContext() {
        return facetedContext;
    }

    public void registerProvider(Name nodeTypeName, HippoVirtualProvider provider) {
        virtualNodeNames.put(nodeTypeName, provider);
    }

    public void registerProviderProperty(Name propName) {
        virtualPropertyNames.add(propName);
    }

    public void registerProvider(String moduleName, HippoVirtualProvider provider) {
        virtualProviders.put(moduleName, provider);
    }

    public HippoVirtualProvider lookupProvider(String moduleName) {
        return virtualProviders.get(moduleName);
    }

    public HippoVirtualProvider lookupProvider(Name nodeTypeName) {
        return virtualNodeNames.get(nodeTypeName);
    }

    private long virtualNodeIdLsb = 1L;
    private final long virtualNodeIdMsb = NodeId.valueOf("cafeface-0000-0000-0000-000000000000").getMostSignificantBits();

    public UUID generateUuid(StateProviderContext context, NodeId canonical) {
        /* There are alternative implementations possible here.  The default implementation, that would be
         * similar to Jackrabbit is simply to return UUID.randomUUID();  However this can be slow at times.
         * Another implemetnation is to use a global AtomicLong and return "new UUID(known-start-value,
         * AtomicLong.getAndIncrement())".  However an atomic long isn't needed since these nodes only live
         * during a single session.  Therefor a simple long is sufficient since no concurrent access is allowed.
         */
        return new UUID(virtualNodeIdMsb, virtualNodeIdLsb++);
    }

    public Name getQName(String name) throws IllegalNameException, NamespaceException {
        return session.getQName(name);
    }

    public Path getQPath(String path) throws MalformedPathException, IllegalNameException, NamespaceException {
        return session.getQPath(path);
    }

    private static synchronized Modules<DataProviderModule> getDataProviderModules(ClassLoader loader) {
        if (dataProviderModules == null) {
            dataProviderModules = new Modules<DataProviderModule>(loader, DataProviderModule.class);
        }
        return new Modules(dataProviderModules);
    }

    void initialize(org.apache.jackrabbit.core.SessionImpl session,
                    FacetedNavigationEngine<Query, Context> facetedEngine,
                    FacetedNavigationEngine.Context facetedContext) throws IllegalNameException, NamespaceException {
        this.session = session;
        this.accessManager = session.getAccessManager();
        this.hierMgr = session.getHierarchyManager();
        this.facetedEngine = facetedEngine;
        this.facetedContext = facetedContext;
        this.handleNodeName = session.getQName(HippoNodeType.NT_HANDLE);

        LinkedHashSet<DataProviderModule> providerInstances = new LinkedHashSet<DataProviderModule>();
        if (virtualLayerEnabled) {
            Modules<DataProviderModule> modules = getDataProviderModules(getClass().getClassLoader());
            for (DataProviderModule module : modules) {
                log.info("Provider module " + module.toString());
                providerInstances.add(module);
            }
        }

        for (DataProviderModule provider : providerInstances) {
            if (provider instanceof HippoVirtualProvider) {
                registerProvider(provider.getClass().getName(), (HippoVirtualProvider)provider);
            }
        }
        for (DataProviderModule provider : providerInstances) {
            try {
                provider.initialize(this);
            } catch (RepositoryException ex) {
                log.error("cannot initialize virtual provider " + provider.getClass().getName() + ": " + ex.getMessage(), ex);
            }
        }
    }

    @Override
    public void dispose() {
        if (facetedEngine != null) {
            facetedEngine.unprepare(facetedContext);
        }
        super.dispose();
    }

    @Override
    public synchronized void edit() throws IllegalStateException {
        if (!editFakeMode)
            editRealMode = true;
        boolean editPreviousMode = editFakeMode;
        editFakeMode = false;
        if (super.inEditMode()) {
            editFakeMode = editPreviousMode;
            return;
        }
        editFakeMode = editPreviousMode;
        super.edit();
    }

    @Override
    public boolean inEditMode() {
        if (editFakeMode)
            return false;
        return editRealMode;
    }

    void clearChangeLog() {
        virtualStates.clear();
        virtualNodes.clear();
        filteredChangeLog = null;
        modifiedExternals.clear();
    }


    @Override
    protected void update(ChangeLog changeLog) throws ReferentialIntegrityException, StaleItemStateException,
                                                      ItemStateException {
        filteredChangeLog = new FilteredChangeLog(changeLog);

        virtualStates.clear();
        virtualNodes.clear();
        filteredChangeLog.invalidate();
        if (!noUpdateChangeLog) {
            super.update(filteredChangeLog);
        }
        modifiedExternals.clear();
        deletedExternals.putAll(filteredChangeLog.deletedExternals);
    }

    @Override
    public void update()
            throws ReferentialIntegrityException, StaleItemStateException, ItemStateException, IllegalStateException {
        super.update();
        editRealMode = false;
        try {
            editFakeMode = true;
            edit();
            FilteredChangeLog tempChangeLog = filteredChangeLog;
            filteredChangeLog = null;
            parameterizedView = false;
            if (tempChangeLog != null) {
                tempChangeLog.repopulate();
            }
        } finally {
            editFakeMode = false;
        }
    }

    void refresh() throws ReferentialIntegrityException, StaleItemStateException, ItemStateException {
        if (!inEditMode()) {
            edit();
        }
        noUpdateChangeLog = true;
        update();
        noUpdateChangeLog = false;
        editRealMode = false;
    }

    public ItemState getCanonicalItemState(ItemId id) throws NoSuchItemStateException, ItemStateException {
        try {
            if (!accessManager.isGranted(id, AccessManager.READ)) {
                return null;
            }
        } catch (RepositoryException ex) {
            return null;
        }
        return super.getItemState(id);
    }

    @Override
    public ItemState getItemState(ItemId id) throws NoSuchItemStateException, ItemStateException {
        currentContext = null;
        ItemState state;
        boolean editPreviousMode = editFakeMode;
        editFakeMode = true;
        try {
            if (id instanceof ParameterizedNodeId) {
                currentContext = new StateProviderContext(((ParameterizedNodeId)id).getParameterString());
                id = ((ParameterizedNodeId)id).getUnparameterizedNodeId();
                parameterizedView = true;
            }
            state = super.getItemState(id);
            if (deletedExternals.containsKey(id))
                return state;
            if (id instanceof HippoNodeId) {
                if (!virtualNodes.containsKey(id)) {
                    edit();
                    NodeState nodeState = (NodeState)state;
                    if (isEnabled()) {
                        nodeState = ((HippoNodeId)id).populate(currentContext, nodeState);
                        Name nodeTypeName = nodeState.getNodeTypeName();
                        if (virtualNodeNames.containsKey(nodeTypeName) && !virtualStates.contains(state)) {
                            int type = isVirtual(nodeState);
                            if ((type & ITEM_TYPE_EXTERNAL) != 0 && (type & ITEM_TYPE_VIRTUAL) != 0) {
                                nodeState.removeAllChildNodeEntries();
                            }
                            nodeState = ((HippoNodeId)id).populate(virtualNodeNames.get(nodeTypeName), nodeState);
                        }
                        virtualNodes.put((HippoNodeId)id, nodeState);
                        forceStore(nodeState);
                    }
                    return nodeState;
                }
            } else if (state instanceof NodeState) {
                NodeState nodeState = (NodeState)state;
                Name nodeTypeName = nodeState.getNodeTypeName();
                if (virtualNodeNames.containsKey(nodeTypeName) && !virtualStates.contains(state)) {
                    edit();
                    int type = isVirtual(nodeState);
                    if ((type & ITEM_TYPE_EXTERNAL) != 0) {
                        nodeState.removeAllChildNodeEntries();
                    }
                    try {
                        if (virtualLayerEnabled) {
                            if (id instanceof ParameterizedNodeId) {
                                if (isEnabled()) {
                                    state = virtualNodeNames.get(nodeTypeName).populate(new StateProviderContext(((ParameterizedNodeId)id).getParameterString()), nodeState);
                                    parameterizedView = true;
                                }
                            } else if (id instanceof HippoNodeId) {
                                if (isEnabled()) {
                                    state = ((HippoNodeId)id).populate(virtualNodeNames.get(nodeTypeName), nodeState);
                                }
                            } else {
                                if (isEnabled()) {
                                    state = virtualNodeNames.get(nodeTypeName).populate(currentContext, nodeState);
                                } else {
                                    state = virtualNodeNames.get(nodeTypeName).populate(currentContext, nodeState);
                                    ((NodeState)state).removeAllChildNodeEntries();
                                }
                            }
                        } else {
                            log.error("Populating while virtual layer disabled", new Exception());
                        }
                        virtualStates.add(state);
                        forceStore(state);
                        return nodeState;
                    } catch(InvalidItemStateException ex) {
                        log.debug("InvalidItemStateException for nodeTypeName '"+nodeTypeName+"'. ", ex);
                        return nodeState;
                    } catch (RepositoryException ex) {
                        log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
                        throw new ItemStateException("Failed to populate node state", ex);
                    }
                }
            }
        } catch(InvalidItemStateException ex) {
            throw new ItemStateException("Source location has changed", ex);
        } finally {
            currentContext = null;
            editFakeMode = editPreviousMode;
        }
        return state;
    }

    @Override
    public boolean hasItemState(ItemId id) {
        if (id instanceof HippoNodeId || id instanceof ParameterizedNodeId) {
            return true;
        } else if (id instanceof PropertyId && ((PropertyId)id).getParentId() instanceof HippoNodeId) {
            return true;
        }
        return super.hasItemState(id);
    }

    @Override
    public NodeState getNodeState(NodeId id) throws NoSuchItemStateException, ItemStateException {
        NodeState state = null;
        if (!(id instanceof HippoNodeId)) {
            try {
                state = super.getNodeState(id);
            } catch (NoSuchItemStateException ex) {
                if (!(id instanceof ParameterizedNodeId)) {
                    throw ex;
                }
            }
        }

        if (virtualNodes.containsKey(id)) {
            state = (NodeState)virtualNodes.get(id);
        } else if (state == null && id instanceof HippoNodeId) {
            boolean editPreviousMode = editFakeMode;
            editFakeMode = true;
            NodeState nodeState;
            try {
                edit();
                if (isEnabled()) {
                    nodeState = ((HippoNodeId)id).populate(currentContext);
                    if (nodeState == null) {
                        throw new NoSuchItemStateException("Populating node failed");
                    }
                } else {
                    nodeState = populate((HippoNodeId)id);
                }

                virtualNodes.put((HippoNodeId)id, nodeState);
                forceStore(nodeState);

                Name nodeTypeName = nodeState.getNodeTypeName();
                if (virtualNodeNames.containsKey(nodeTypeName)) {
                    int type = isVirtual(nodeState);
                    /*
                     * If a node is EXTERNAL && VIRTUAL, we are dealing with an already populated nodestate.
                     * Since the parent EXTERNAL node can impose new constaints, like an inherited filter, we
                     * first need to remove all the childNodeEntries, and then populate it again
                     */
                    if ((type & ITEM_TYPE_EXTERNAL) != 0 && (type & ITEM_TYPE_VIRTUAL) != 0) {
                        nodeState.removeAllChildNodeEntries();
                    }
                    try {
                        state = ((HippoNodeId)id).populate(virtualNodeNames.get(nodeTypeName), nodeState);
                    } catch(InvalidItemStateException ex) {
                        throw new ItemStateException("Node has been modified", ex);
                    }
                }
            } finally {
                editFakeMode = editPreviousMode;
            }
            return nodeState;
        } else if (isHandle(state)) {
            reorderHandleChildNodeEntries(state);
        }
        return state;
    }

    private void reorderHandleChildNodeEntries(final NodeState state) {
        if (accessManager == null) {
            return;
        }

        // returns a copy of the list
        List<ChildNodeEntry> cnes = state.getChildNodeEntries();
        LinkedList<ChildNodeEntry> updatedList = new LinkedList<ChildNodeEntry>();
        int readableIndex = 0;
        for (ChildNodeEntry current : cnes) {
            boolean added = false;

            // if there is a same-name-sibling with a bigger index, check authorization
            // there is no need to check last one, because it's already last
            int index = current.getIndex();
            ChildNodeEntry next = state.getChildNodeEntry(current.getName(), index + 1);
            if (next != null) {
                try {
                    // this is SNS number 2, so check previous one,
                    if (!accessManager.isGranted(current.getId(), AccessManager.READ)) {
                        updatedList.addLast(current);
                        added = true;
                    }
                } catch (ItemNotFoundException t) {
                    log.error("Unable to order documents below handle " + state.getId(), t);
                } catch (RepositoryException t) {
                    log.error("Unable to determine access rights for " + current.getId());
                }
            }

            if (!added) {
                updatedList.add(readableIndex, current);
                readableIndex++;
            }
        }

        // always invoke {@link NodeState#setChildNodeEntries} (even when there are no changes)
        // so that the hierarchy manager cache is verified and updated.
        state.setChildNodeEntries(updatedList);
    }

    @Override
    public PropertyState getPropertyState(PropertyId id) throws NoSuchItemStateException, ItemStateException {
        if (id.getParentId() instanceof HippoNodeId) {
            throw new NoSuchItemStateException("Property of a virtual node cannot be retrieved from shared ISM");
        }
        return super.getPropertyState(id);
    }

    private NodeState populate(HippoNodeId nodeId) throws NoSuchItemStateException, ItemStateException {
        try {
            NodeState dereference = getNodeState(rootNodeId);
            NodeState state = createNew(nodeId, dereference.getNodeTypeName(), nodeId.parentId);
            state.setNodeTypeName(dereference.getNodeTypeName());
            return state;
        } catch(RepositoryException ex) {
            throw new NoSuchItemStateException(ex.getMessage(), ex);
        }
    }

    boolean isPureVirtual(ItemId id) {
        if (id.denotesNode()) {
            if (id instanceof HippoNodeId) {
                return true;
            }
        } else {
            try {
                PropertyState propState = (PropertyState)getItemState(id);
                return (propState.getParentId() instanceof HippoNodeId);
            } catch (NoSuchItemStateException ex) {
                return true;
            } catch (ItemStateException ex) {
                return true;
            }
        }
        return false;
    }

    int isVirtual(ItemState state) {
        if (state.isNode()) {
            int type = ITEM_TYPE_REGULAR;
            if (state.getId() instanceof HippoNodeId) {
                type |= ITEM_TYPE_VIRTUAL;
            }
            if (virtualNodeNames.containsKey(((NodeState)state).getNodeTypeName())) {
                type |= ITEM_TYPE_EXTERNAL;
            }
            return type;
        } else {
            /* it is possible to do a check on type name of the property
             * using Name name = ((PropertyState)state).getName().toString().equals(...)
             * to check and return whether a property is virtual.
             *
             * FIXME: this would be better if these properties would not be
             * named for all node types, but bound to a specific node type
             * for which there is already a provider defined.
             */
            PropertyState propState = (PropertyState)state;
            if (propState.getPropertyId() instanceof HippoPropertyId) {
                return ITEM_TYPE_VIRTUAL;
            } else if (virtualPropertyNames.contains(propState.getName())) {
                return ITEM_TYPE_VIRTUAL;
            } else if (propState.getParentId() instanceof HippoNodeId) {
                return ITEM_TYPE_VIRTUAL;
            } else {
                return ITEM_TYPE_REGULAR;
            }
        }
    }

    boolean isHandle(ItemState state) {
        if (handleNodeName != null && state.isNode()) {
            return handleNodeName.equals(((NodeState) state).getNodeTypeName());
        }
        return false;
    }

    class FilteredChangeLog extends ChangeLog {
        private ChangeLog upstream;
        Map<ItemId, Object> deletedExternals = new HashMap<ItemId, Object>();

        FilteredChangeLog(ChangeLog changelog) {
            upstream = changelog;
        }

        void invalidate() {
            if (!virtualLayerRefreshing) {
                for (ItemState state : upstream.modifiedStates()) {
                    if ((isVirtual(state) & ITEM_TYPE_EXTERNAL) != 0) {
                        forceUpdate(state);
                    }
                }
                return;
            }
            List<ItemState> deletedStates = new LinkedList<ItemState>();
            for (ItemState state : upstream.deletedStates()) {
                deletedStates.add(state);
            }
            List<ItemState> addedStates = new LinkedList<ItemState>();
            for (ItemState state : upstream.addedStates()) {
                addedStates.add(state);
            }
            List<ItemState> modifiedStates = new LinkedList<ItemState>();
            for (ItemState state : upstream.modifiedStates()) {
                modifiedStates.add(state);
            }
            for (ItemState state : deletedStates) {
                if ((isVirtual(state) & ITEM_TYPE_EXTERNAL) != 0) {
                    deletedExternals.put(state.getId(), null);
                    ((NodeState)state).removeAllChildNodeEntries();
                    forceUpdate(state);
                }
            }
            for (ItemState state : addedStates) {
                if ((isVirtual(state) & ITEM_TYPE_VIRTUAL) != 0) {
                    if (state.isNode()) {
                        NodeState nodeState = (NodeState)state;
                        try {
                            NodeState parentNodeState = (NodeState)get(nodeState.getParentId());
                            if (parentNodeState != null) {
                                parentNodeState.removeChildNodeEntry(nodeState.getNodeId());
                                forceUpdate(nodeState);
                            }
                        } catch (NoSuchItemStateException ex) {
                        }
                    } else {
                        forceUpdate(state);
                    }
                } else if ((isVirtual(state) & ITEM_TYPE_EXTERNAL) != 0) {
                    if (!deletedExternals.containsKey(state.getId())
                            && !HippoLocalItemStateManager.this.deletedExternals.containsKey(state.getId())) {
                        ((NodeState)state).removeAllChildNodeEntries();
                        forceUpdate((NodeState)state);
                    }
                }
            }
            for (ItemState state : modifiedStates) {
                if ((isVirtual(state) & ITEM_TYPE_EXTERNAL) != 0) {
                    if (!deletedExternals.containsKey(state.getId())
                            && !HippoLocalItemStateManager.this.deletedExternals.containsKey(state.getId())) {
                        forceUpdate((NodeState)state);
                        ((NodeState)state).removeAllChildNodeEntries();
                    }
                }
            }
        }

        private void repopulate() {
            for (Iterator iter = new HashSet<ItemState>(virtualStates).iterator(); iter.hasNext(); ) {
                ItemState state = (ItemState)iter.next();
                // only repopulate ITEM_TYPE_EXTERNAL, not state that are ITEM_TYPE_EXTERNAL && ITEM_TYPE_VIRTUAL
                if (((isVirtual(state) & ITEM_TYPE_EXTERNAL)) != 0 && ((isVirtual(state) & ITEM_TYPE_VIRTUAL) == 0)
                        && !deleted(state.getId())
                        && !deletedExternals.containsKey(state.getId())
                        && !HippoLocalItemStateManager.this.deletedExternals.containsKey(state.getId())) {
                    try {
                        if (state.getId() instanceof ParameterizedNodeId) {
                            virtualNodeNames.get(((NodeState)state).getNodeTypeName()).populate(new StateProviderContext(((ParameterizedNodeId)state.getId()).getParameterString()), (NodeState)state);
                            parameterizedView = true;
                        } else if (state.getId() instanceof HippoNodeId) {
                            ((HippoNodeId)state.getId()).populate(virtualNodeNames.get(((NodeState)state).getNodeTypeName()), (NodeState)state);
                        } else {
                            virtualNodeNames.get(((NodeState)state).getNodeTypeName()).populate(null, (NodeState)state);
                        }
                    } catch (InvalidItemStateException ex) {
                        log.info(ex.getClass().getName() + ": " + ex.getMessage(), ex);
                    } catch (ItemStateException ex) {
                        log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
                    } catch (RepositoryException ex) {
                        log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
                    }
                }
            }
        }

        @Override
        public void added(ItemState state) {
            upstream.added(state);
        }

        @Override
        public void modified(ItemState state) {
            upstream.modified(state);
        }

        @Override
        public void deleted(ItemState state) {
            upstream.deleted(state);
        }

        @Override
        public void modified(NodeReferences refs) {
            upstream.modified(refs);
        }

        @Override
        public boolean isModified(ItemId id) {
            return upstream.isModified(id);
        }

        @Override
        public ItemState get(ItemId id) throws NoSuchItemStateException {
            return upstream.get(id);
        }

        @Override
        public boolean has(ItemId id) {
            return upstream.has(id) && !HippoLocalItemStateManager.this.deletedExternals.containsKey(id);
        }

        @Override
        public boolean deleted(ItemId id) {
            return upstream.deleted(id) && !HippoLocalItemStateManager.this.deletedExternals.containsKey(id);
        }

        @Override
        public NodeReferences getReferencesTo(NodeId id) {
            return upstream.getReferencesTo(id);
        }

        @Override
        public Iterable<ItemState> addedStates() {
            return new FilteredStateIterator(upstream.addedStates(), false);
        }

        @Override
        public Iterable<ItemState> modifiedStates() {
            return new FilteredStateIterator(upstream.modifiedStates(), true);
        }

        @Override
        public Iterable<ItemState> deletedStates() {
            return new FilteredStateIterator(upstream.deletedStates(), false);
        }

        @Override
        public Iterable<NodeReferences> modifiedRefs() {
            return new FilteredReferencesIterator(upstream.modifiedRefs());
        }

        @Override
        public void merge(ChangeLog other) {
            upstream.merge(other);
        }

        @Override
        public void push() {
            upstream.push();
        }

        @Override
        public void persisted() {
            upstream.persisted();
        }

        @Override
        public void reset() {
            upstream.reset();
        }

        @Override
        public void disconnect() {
            upstream.disconnect();
        }

        @Override
        public void undo(ItemStateManager parent) {
            upstream.undo(parent);
        }

        @Override
        public String toString() {
            return upstream.toString();
        }

        class FilteredStateIterator implements Iterable<ItemState> {
            Iterable<ItemState> actualIterable;
            ItemState current;
            boolean modified;

            FilteredStateIterator(Iterable<ItemState> actualIterable, boolean modified) {
                this.actualIterable = actualIterable;
                current = null;
                this.modified = modified;
            }

            public Iterator<ItemState> iterator() {
                final Iterator<ItemState> actualIterator = actualIterable.iterator();
                return new Iterator<ItemState>() {
                    public boolean hasNext() {
                        while (current == null) {
                            if (!actualIterator.hasNext())
                                return false;
                            current = (ItemState)actualIterator.next();
                            if (needsSkip(current)) {
                                current = null;
                            }
                        }
                        return true;
                    }

                    public boolean needsSkip(ItemState current) {
                        if (HippoLocalItemStateManager.this.deletedExternals.containsKey(current.getId())) {
                            return true;
                        }
                        if ((isVirtual(current) & ITEM_TYPE_VIRTUAL) != 0) {
                            if (!current.isNode()) {
                                PropertyState propState = (PropertyState)current;
                                if (modifiedExternals.contains(propState.getParentId())) {
                                    return false;
                                }
                            }
                            return true;
                        }
                        if (modified) {
                            if ((isVirtual(current) & ITEM_TYPE_EXTERNAL) != 0) {
                                return !modifiedExternals.contains(current.getId());
                            }
                        }
                        return false;
                    }

                    public ItemState next() throws NoSuchElementException {
                        while (current == null) {
                            if (!actualIterator.hasNext()) {
                                throw new NoSuchElementException();
                            }
                            current = actualIterator.next();
                            if (needsSkip(current)) {
                                current = null;
                            }
                        }
                        ItemState rtValue = current;
                        current = null;
                        return rtValue;
                    }

                    public void remove() throws UnsupportedOperationException, IllegalStateException {
                        actualIterator.remove();
                    }
                };
            }
        }

        class FilteredReferencesIterator implements Iterable<NodeReferences> {
            Iterable<NodeReferences> actualIterable;
            NodeReferences current;

            FilteredReferencesIterator(Iterable<NodeReferences> actualIterable) {
                this.actualIterable = actualIterable;
                current = null;
            }

            public Iterator<NodeReferences> iterator() {
                final Iterator<NodeReferences> actualIterator = actualIterable.iterator();
                return new Iterator<NodeReferences>() {
                    public boolean hasNext() {
                        while (current == null) {
                            if (!actualIterator.hasNext())
                                return false;
                            current = (NodeReferences)actualIterator.next();
                            if (needsSkip(current)) {
                                current = null;
                            }
                        }
                        return (current != null);
                    }

                    public boolean needsSkip(NodeReferences current) {
                        return isPureVirtual(current.getTargetId());
                    }

                    public NodeReferences next() throws NoSuchElementException {
                        NodeReferences rtValue = null;
                        while (current == null) {
                            if (!actualIterator.hasNext()) {
                                throw new NoSuchElementException();
                            }
                            current = (NodeReferences)actualIterator.next();
                            if (needsSkip(current)) {
                                current = null;
                            }
                        }
                        rtValue = new NodeReferences(current.getTargetId());
                        for (PropertyId propId : (List<PropertyId>)current.getReferences()) {
                            if (!isPureVirtual(propId)) {
                                rtValue.addReference(propId);
                            }
                        }
                        current = null;
                        if (rtValue == null)
                            throw new NoSuchElementException();
                        return rtValue;
                    }

                    public void remove() throws UnsupportedOperationException, IllegalStateException {
                        actualIterator.remove();
                    }
                };
            }
        }
    }

    @Override
    public void handleModified(final NodeState sharedState) {
        NodeId handleId = sharedState.getNodeId();
        ItemState localState = cache.retrieve(handleId);
        if (localState != null) {
            reorderHandleChildNodeEntries((NodeState) localState);
        } else {
            clearHierarchyManagerCacheForHandle(sharedState);
        }
    }

    private void clearHierarchyManagerCacheForHandle(final NodeState sharedState) {
        final NodeId handleId = sharedState.getNodeId();
        final Name nodeTypeName = sharedState.getNodeTypeName();
        final NodeId parentId = sharedState.getParentId();
        NodeState wrappedState = new NodeState(handleId, nodeTypeName, parentId, ItemState.STATUS_EXISTING, false);
        nodesReplaced(wrappedState);
    }

    @Override
    public void stateModified(final ItemState modified) {
        super.stateModified(modified);
        if (accessManager != null
                && modified.getContainer() != this
                && !cache.isCached(modified.getId())) {
            ((HippoAccessManager) accessManager).stateModified(modified);
        }
    }

    @Override
    public void stateDestroyed(ItemState destroyed) {
        if (destroyed.getContainer() != this) {
            if ((isVirtual(destroyed) & ITEM_TYPE_EXTERNAL) != 0) {
                deletedExternals.put(destroyed.getId(), null);
            }
        }
        super.stateDestroyed(destroyed);
    }

    private void forceUpdate(ItemState state) {
        stateDiscarded(state);
    }

    @Override
    public void store(ItemState state) {
        if ((isVirtual(state) & ITEM_TYPE_EXTERNAL) != 0) {
            modifiedExternals.add(state.getId());
        }
        super.store(state);
    }

    private void forceStore(ItemState state) {
        super.store(state);
    }
}
