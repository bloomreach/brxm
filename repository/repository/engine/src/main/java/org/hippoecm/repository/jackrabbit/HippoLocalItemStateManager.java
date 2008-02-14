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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException; 
import java.util.Set;

import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.observation.EventStateCollectionFactory;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateCacheFactory;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.core.state.NodeReferencesId;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.state.SharedItemStateManager;
import org.apache.jackrabbit.core.state.StaleItemStateException;
import org.apache.jackrabbit.core.state.XAItemStateManager;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.hippoecm.repository.FacetedNavigationEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class HippoLocalItemStateManager extends XAItemStateManager {
    protected final Logger log = LoggerFactory.getLogger(HippoLocalItemStateManager.class);

    /** Mask pattern indicating a regular, non-virtual JCR item
     */
    static final int ITEM_TYPE_REGULAR  = 0x00;

    /** Mask pattern indicating an externally defined node, patterns can
     * be OR-ed to indicate both external and virtual nodes.
     */
    static final int ITEM_TYPE_EXTERNAL = 0x01;

    /** Mask pattern indicating a virtual node, patterns can be OR-ed to
     * indicate both external and virtual nodes.
     */
    static final int ITEM_TYPE_VIRTUAL  = 0x02;

    NodeTypeRegistry ntReg;
    NamespaceResolver nsResolver;
    protected HierarchyManager hierMgr;
    FacetedNavigationEngine facetedEngine;
    FacetedNavigationEngine.Context facetedContext;
    protected FilteredChangeLog filteredChangeLog = null;
    protected Map<String,HippoVirtualProvider> virtualProviders;
    protected Map<Name,HippoVirtualProvider> virtualNodeNames;
    protected Set<Name> virtualPropertyNames;
    private Set<ItemState> virtualStates = new HashSet<ItemState>();
    private Map<NodeId,ItemState> virtualNodes = new HashMap<NodeId,ItemState>();

    public HippoLocalItemStateManager(SharedItemStateManager sharedStateMgr, EventStateCollectionFactory factory,
            ItemStateCacheFactory cacheFactory, NodeTypeRegistry ntReg) {
        super(sharedStateMgr, factory, cacheFactory);
        this.ntReg = ntReg;
        virtualProviders = new HashMap<String,HippoVirtualProvider>();
        virtualNodeNames = new HashMap<Name,HippoVirtualProvider>();
        virtualPropertyNames = new HashSet<Name>();
    }

    void registerProvider(Name nodeTypeName, HippoVirtualProvider provider) {
        virtualNodeNames.put(nodeTypeName, provider);
    }

    void registerProviderProperty(Name propName) {
        virtualPropertyNames.add(propName);
    }

    void registerProvider(String moduleName, HippoVirtualProvider provider) {
        virtualProviders.put(moduleName, provider);
    }

    HippoVirtualProvider lookupProvider(String moduleName) {
        return virtualProviders.get(moduleName);
    }

    void initialize(NamespaceResolver nsResolver, HierarchyManager hierMgr,
                    FacetedNavigationEngine facetedEngine,
                    FacetedNavigationEngine.Context facetedContext) {

        this.nsResolver = nsResolver;
        this.hierMgr = hierMgr;
        this.facetedEngine = facetedEngine;
        this.facetedContext = facetedContext;

        String[] providerClassnames = new String[] { "org.hippoecm.repository.jackrabbit.MirrorVirtualProvider", "org.hippoecm.repository.jackrabbit.ViewVirtualProvider", "org.hippoecm.repository.jackrabbit.FacetSelectProvider", "org.hippoecm.repository.jackrabbit.FacetResultSetProvider", "org.hippoecm.repository.jackrabbit.FacetSubSearchProvider", "org.hippoecm.repository.jackrabbit.FacetSearchProvider" };
        HippoVirtualProvider[] providerInstances = new HippoVirtualProvider[providerClassnames.length];

        for(int i=0; i<providerClassnames.length; i++) {
            try {
                Object instance = Class.forName(providerClassnames[i]).newInstance();
                if(instance instanceof HippoVirtualProvider) {
                    providerInstances[i] = (HippoVirtualProvider) instance;
                    registerProvider(providerClassnames[i], providerInstances[i]);
                } else
                    providerInstances[i] = null;
            } catch(ClassNotFoundException ex) {
                log.error("cannot instantiate virtual provider "+providerClassnames[i]+": "+ex);
            } catch(InstantiationException ex) {
                log.error("cannot instantiate virtual provider "+providerClassnames[i]+": "+ex);
            } catch(IllegalAccessException ex) {
                log.error("cannot instantiate virtual provider "+providerClassnames[i]+": "+ex);
            }
        }
        for(int i=0; i<providerInstances.length; i++) {
            try {
                if(providerInstances[i] != null) {
                    providerInstances[i].initialize(this);
                }
            } catch(RepositoryException ex) {
                log.error("cannot initialize virtual provider "+providerClassnames[i]+": "+ex);
            }
        }
    }

    @Override
    public void dispose() {
        facetedEngine.unprepare(facetedContext);
    }

    @Override
    public synchronized void edit() throws IllegalStateException {
        if(inEditMode()) {
            return;
        }
        super.edit();
    }

    @Override
    protected void update(ChangeLog changeLog)
    throws ReferentialIntegrityException, StaleItemStateException, ItemStateException {
        filteredChangeLog = new FilteredChangeLog(changeLog);
        virtualStates.clear();
        virtualNodes.clear();
        filteredChangeLog.invalidate();
        super.update(filteredChangeLog);
    }

    @Override
    public void update()
    throws ReferentialIntegrityException, StaleItemStateException, ItemStateException, IllegalStateException {
        super.update();
        edit();
        FilteredChangeLog tempChangeLog = filteredChangeLog;
        filteredChangeLog = null;
        tempChangeLog.repopulate();
    }

    @Override
    public ItemState getItemState(ItemId id) throws NoSuchItemStateException, ItemStateException {
        ItemState state = super.getItemState(id);
        if(id instanceof HippoNodeId) {
            if(!virtualNodes.containsKey(id)) {
                edit();
                NodeState nodeState;
                if(state != null) {
                    nodeState = (NodeState) state;
                    nodeState = ((HippoNodeId)id).populate(nodeState);
                } else {
                    nodeState = ((HippoNodeId)id).populate();
                }

                Name nodeTypeName = nodeState.getNodeTypeName();
                if(virtualNodeNames.containsKey(nodeTypeName) && !virtualStates.contains(state)) {
                    int type =  isVirtual(nodeState);
                    if( (type  & ITEM_TYPE_EXTERNAL) != 0  && (type  & ITEM_TYPE_VIRTUAL) != 0) {
                        nodeState.removeAllChildNodeEntries();
                    }
                    try {
                        state = virtualNodeNames.get(nodeTypeName).populate(nodeState);
                    } catch(RepositoryException ex) {
                        System.err.println(ex.getMessage());
                        ex.printStackTrace(System.err);
                        return null;
                    }
                }
                virtualNodes.put((HippoNodeId)id, nodeState);
                store(nodeState);
                return nodeState;
            }
        } else {
            if(state instanceof NodeState) {
                NodeState nodeState = (NodeState) state;
                Name nodeTypeName = nodeState.getNodeTypeName();
                if(virtualNodeNames.containsKey(nodeTypeName) && !virtualStates.contains(state)) {
                    edit();
                    int type =  isVirtual(nodeState);
                    if( (type  & ITEM_TYPE_EXTERNAL) != 0  && (type  & ITEM_TYPE_VIRTUAL) != 0) {
                        nodeState.removeAllChildNodeEntries();
                    }
                    try {
                        virtualStates.add(state);
                        state = virtualNodeNames.get(nodeTypeName).populate(nodeState);
                        store(state);
                        return nodeState;
                    } catch(RepositoryException ex) {
                        System.err.println(ex.getMessage());
                        ex.printStackTrace(System.err);
                        return null;
                    }
                }
            }
        }
        return state;
    }

    @Override
    public boolean hasItemState(ItemId id) {
            if(id instanceof HippoNodeId) {
                return true;
            }
            return super.hasItemState(id);
    }

    @Override
    public NodeState getNodeState(NodeId id) throws NoSuchItemStateException, ItemStateException {
        NodeState state = null;
        try {
            state = super.getNodeState(id);
        } catch(NoSuchItemStateException ex) {
            if(!(id instanceof HippoNodeId)) {
                throw ex;
            }
        } catch(ItemStateException ex) {
            if(!(id instanceof HippoNodeId)) {
                throw ex;
            }
        }

        if(virtualNodes.containsKey(id)) {
            state = (NodeState) virtualNodes.get(id);
        } else if(state == null && id instanceof HippoNodeId) {
            edit();
            NodeState nodeState = ((HippoNodeId)id).populate();
            
            virtualNodes.put((HippoNodeId)id, nodeState);
            store(nodeState);

                Name nodeTypeName = nodeState.getNodeTypeName();
                if(virtualNodeNames.containsKey(nodeTypeName)) {
                    int type =  isVirtual(nodeState);
                    /*
                     * If a node is EXTERNAL && VIRTUAL, we are dealing with an already populated nodestate.
                     * Since the parent EXTERNAL node can impose new constaints, like an inherited filter, we
                     * first need to remove all the childNodeEntries, and then populate it again
                     */  
                    if( (type  & ITEM_TYPE_EXTERNAL) != 0  && (type  & ITEM_TYPE_VIRTUAL) != 0) {
                        nodeState.removeAllChildNodeEntries();
                    }
                    try {
                        state = virtualNodeNames.get(nodeTypeName).populate(nodeState);
                    } catch(RepositoryException ex) {
                        System.err.println(ex.getMessage());
                        ex.printStackTrace(System.err);
                        return null;
                    }
                }

            return nodeState;
        }
        return state;
    }

    @Override
    public PropertyState getPropertyState(PropertyId id) throws NoSuchItemStateException, ItemStateException {
        return super.getPropertyState(id);
    }


    int isVirtual(ItemState state) {
        if(state.isNode()) {
            int type = ITEM_TYPE_REGULAR;
            if(state.getId() instanceof HippoNodeId) {
                type |= ITEM_TYPE_VIRTUAL;
            }
            if(virtualNodeNames.containsKey(((NodeState)state).getNodeTypeName())) {
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
            PropertyState propState = (PropertyState) state;
            if(propState.getPropertyId() instanceof HippoPropertyId) {
                return ITEM_TYPE_VIRTUAL;
            } else if(virtualPropertyNames.contains(propState.getName())) {
                return ITEM_TYPE_VIRTUAL;
            } else if(propState.getParentId() instanceof HippoNodeId) {
                return ITEM_TYPE_VIRTUAL;
            } else {
                return ITEM_TYPE_REGULAR;
            }
        }
    }

    class FilteredChangeLog extends ChangeLog {

        private ChangeLog upstream;
        private Set<ItemState> deletedExternals;

        FilteredChangeLog(ChangeLog changelog) {
            upstream = changelog;
            deletedExternals = new HashSet();
        }

        void invalidate() {
            for(Iterator iter = upstream.deletedStates(); iter.hasNext(); ) {
                ItemState state = (ItemState) iter.next();
                if((isVirtual(state) & ITEM_TYPE_EXTERNAL) != 0) {
                    deletedExternals.add(state);
                    ((NodeState)state).removeAllChildNodeEntries();
                    stateDestroyed(state);
                }
            }
            for(Iterator iter = upstream.addedStates(); iter.hasNext(); ) {
                ItemState state = (ItemState) iter.next();
                if((isVirtual(state) & ITEM_TYPE_VIRTUAL) != 0) {
                    if(state.isNode()) {
                        NodeState nodeState = (NodeState) state;
                        try {
                            NodeState parentNodeState = (NodeState) get(nodeState.getParentId());
                            if(parentNodeState != null) {
                                parentNodeState.removeChildNodeEntry(nodeState.getNodeId());
                                stateDestroyed(nodeState);
                            }
                        } catch(NoSuchItemStateException ex) {
                        }
                    } else {
                        stateDestroyed(state);
                    }
                } else if((isVirtual(state) & ITEM_TYPE_EXTERNAL) != 0) {
                    if(!deletedExternals.contains(state)) {
                        ((NodeState)state).removeAllChildNodeEntries();
                        stateDestroyed((NodeState)state);
                    }
                }
            }
            for(Iterator iter = upstream.modifiedStates(); iter.hasNext(); ) {
                ItemState state = (ItemState) iter.next();
                if((isVirtual(state) & ITEM_TYPE_EXTERNAL) != 0) {
                    stateDestroyed((NodeState)state);
                    ((NodeState)state).removeAllChildNodeEntries();
                }
            }
        }
        
        void repopulate() {
            for(Iterator iter = virtualStates.iterator(); iter.hasNext(); ) {
                ItemState state = (ItemState) iter.next();
                // only repopulate ITEM_TYPE_EXTERNAL, not state that are ITEM_TYPE_EXTERNAL && ITEM_TYPE_VIRTUAL
                if(( (isVirtual(state) & ITEM_TYPE_EXTERNAL)) != 0 && ((isVirtual(state) & ITEM_TYPE_VIRTUAL) == 0) ) {
                    try {
                        virtualNodeNames.get(((NodeState)state).getNodeTypeName()).populate((NodeState)state);
                    } catch(RepositoryException ex) {
                        System.err.println(ex.getMessage());
                        ex.printStackTrace(System.err);
                    }
                }
            }
        }

        @Override public ItemState get(ItemId id) throws NoSuchItemStateException {
            return upstream.get(id);
        }
        @Override public boolean has(ItemId id) {
            return upstream.has(id);
        }
        @Override public boolean deleted(ItemId id) {
            return upstream.deleted(id);
        }
        @Override public NodeReferences get(NodeReferencesId id) {
            return upstream.get(id);
        }
        @Override public Iterator addedStates() {
            return new FilteredStateIterator(upstream.addedStates());
        }
        @Override public Iterator modifiedStates() {
            return new FilteredStateIterator(upstream.modifiedStates());
        }
        @Override public Iterator deletedStates() {
            return new FilteredStateIterator(upstream.deletedStates());
        }
        @Override public Iterator modifiedRefs() {
            return upstream.modifiedRefs();
        }
        @Override public void merge(ChangeLog other) {
            upstream.merge(other);
        }
        @Override public void push() {
            upstream.push();
        }
        @Override public void persisted() {
            upstream.persisted();
        }
        @Override public void reset() {
            upstream.reset();
        }
        @Override public void disconnect() {
            upstream.disconnect();
        }
        @Override public void undo(ItemStateManager parent) {
            upstream.undo(parent);
        }
        @Override public String toString() {
            return upstream.toString();
        }

        class FilteredStateIterator implements Iterator {
            Iterator actualIterator;
            ItemState current;
            FilteredStateIterator(Iterator actualIterator) {
                this.actualIterator = actualIterator;
                current = null;
            }
            public boolean hasNext() {
                while(current == null) {
                    if(!actualIterator.hasNext())
                        return false;
                    current = (ItemState) actualIterator.next();
                    if((isVirtual(current) & ITEM_TYPE_VIRTUAL) != 0) {
                        current = null;
                }
                }
                return true;
            }
            public Object next() throws NoSuchElementException {
                Object rtValue = null;
                while(current == null) {
                    if(!actualIterator.hasNext()) {
                        return false;
                    }
                    current = (ItemState) actualIterator.next();
                    if((isVirtual(current) & ITEM_TYPE_VIRTUAL) != 0) {
                        current = null;
                }
                }
                rtValue = current;
                current = null;
                return rtValue;
            }
            public void remove() throws UnsupportedOperationException, IllegalStateException {
                actualIterator.remove();
            }
        }

    }
}