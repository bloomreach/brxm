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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.conversion.NamePathResolver;
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
import org.hippoecm.repository.FacetedNavigationEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class HippoLocalItemStateManager extends XAItemStateManager {
    protected final Logger log = LoggerFactory.getLogger(HippoLocalItemStateManager.class);
    
    protected NodeTypeRegistry ntReg;
    protected FilteredChangeLog filteredChangeLog = null;
    protected Map<Name,HippoVirtualProvider> virtualProviders;
    protected Set<Name> virtualProperties;
    protected HierarchyManager hierMgr;
    protected NamePathResolver resolver;

    public HippoLocalItemStateManager(SharedItemStateManager sharedStateMgr, EventStateCollectionFactory factory,
            ItemStateCacheFactory cacheFactory, NodeTypeRegistry ntReg) {
        super(sharedStateMgr, factory, cacheFactory);
        this.ntReg = ntReg;
        virtualProviders = new HashMap<Name,HippoVirtualProvider>();
        virtualProperties = new HashSet<Name>();
    }
    
    void register(Name nodeTypeName, HippoVirtualProvider provider) {
        virtualProviders.put(nodeTypeName, provider);
    }

    void initialize(NamePathResolver resolver, HierarchyManager hierMgr,
                    FacetedNavigationEngine facetedEngine,
                    FacetedNavigationEngine.Context facetedContext) {

        this.resolver = resolver;
        this.hierMgr = hierMgr;

        /* Include the following for issue HREPTWO-179:

        MirrorVirtualProvider  mirrorProvider;
        ViewVirtualProvider    viewProvider;
        FacetResultSetProvider resultSetProvider;
        FacetSelectProvider    facetSelectProvider;
        FacetSearchProvider    facetSearchProvider;

        mirrorProvider      = new MirrorVirtualProvider(this);
        viewProvider        = new ViewVirtualProvider(this);
        resultSetProvider   = new FacetResultSetProvider(this, mirrorProvider);

        try {
            facetSelectProvider = new FacetSelectProvider(this, viewProvider);
            facetSelectProvider.register(this);
        } catch(IllegalNameException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
        } catch(NamespaceException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
        } catch(RepositoryException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
        }

        try {
            facetSearchProvider = new FacetSearchProvider(this, resultSetProvider, facetedEngine, facetedContext);
            register(resolver.getQName(HippoNodeType.NT_FACETSEARCH), facetSearchProvider);
        } catch(IllegalNameException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
        } catch(NamespaceException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
        } catch(RepositoryException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
        }

        */

    }

    @Override
    public synchronized void edit() throws IllegalStateException {
        if(inEditMode())
            return;
        super.edit();
    }
    
    @Override
    protected void update(ChangeLog changeLog)
    throws ReferentialIntegrityException, StaleItemStateException, ItemStateException {
        filteredChangeLog = new FilteredChangeLog(changeLog);
        filteredChangeLog.invalidate();
        super.update(filteredChangeLog);
    }
    
    @Override
    public void update()
    throws ReferentialIntegrityException, StaleItemStateException, ItemStateException, IllegalStateException {
        super.update();
        edit();
        filteredChangeLog.repopulate();
        filteredChangeLog = null;
    }
    
    @Override
    public NodeState getNodeState(NodeId id) throws NoSuchItemStateException, ItemStateException {
        if(id instanceof HippoNodeId) {
            edit();
            return ((HippoNodeId)id).populate();
        }
        NodeState state = super.getNodeState(id);
        Name nodeTypeName = state.getNodeTypeName();
        if(virtualProviders.containsKey(nodeTypeName)) {
            edit();
            try {
                return virtualProviders.get(nodeTypeName).populate(state);
            } catch(RepositoryException ex) {
                System.err.println(ex.getMessage());
                ex.printStackTrace(System.err);
                return null;
            }
        } else {
            return state;
        }
    }
    
    @Override
    public PropertyState getPropertyState(PropertyId id) throws NoSuchItemStateException, ItemStateException {
        return super.getPropertyState(id);
    }

    @Override
    public boolean hasItemState(ItemId id) {
        if(id instanceof HippoNodeId)
            return true;
        return super.hasItemState(id);
    }
    
    class FilteredChangeLog extends ChangeLog {
        
        /** Mask pattern indicating a regular, non-virtual JCR item
         */
        private static final int ITEM_TYPE_REGULAR  = 0x00;
        
        /** Mask pattern indicating an externally defined node, patterns can
         * be OR-ed to indicate both external and virtual nodes.
         */
        private static final int ITEM_TYPE_EXTERNAL = 0x01;
        
        /** Mask pattern indicating a virtual node, patterns can be OR-ed to
         * indicate both external and virtual nodes.
         */
        private static final int ITEM_TYPE_VIRTUAL  = 0x02;
        
        private ChangeLog upstream;
        private List<ItemState> virtualStates = new LinkedList<ItemState>();
        
        FilteredChangeLog(ChangeLog changelog) {
            upstream = changelog;
        }
        
        private int isVirtual(ItemState state) {
            if(state.isNode()) {
                int type = ITEM_TYPE_REGULAR;
                if(state.getId() instanceof HippoNodeId)
                    type |= ITEM_TYPE_VIRTUAL;
                if(virtualProviders.containsKey(((NodeState)state).getNodeTypeName()))
                    type |= ITEM_TYPE_EXTERNAL;
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
                if(propState.getPropertyId() instanceof HippoPropertyId)
                    return ITEM_TYPE_VIRTUAL;
                else if(virtualProperties.contains(propState.getName()))
                    return ITEM_TYPE_VIRTUAL;
                else
                    return ITEM_TYPE_REGULAR;
            }
        }
        
        void invalidate() {
            for(Iterator iter = addedStates(); iter.hasNext(); ) {
                ItemState state = (ItemState) iter.next();
                if((isVirtual(state) & ITEM_TYPE_VIRTUAL) != 0) {
                    if(state.isNode()) {
                        NodeState nodeState = (NodeState) state;
                        try {
                            nodeState = (NodeState) get(nodeState.getParentId());
                            nodeState.removeAllChildNodeEntries();
                        } catch(org.apache.jackrabbit.core.state.NoSuchItemStateException ex) {
                            System.err.println("FilteredChangeLog.invalidate exception "+ex.getMessage());
                        }
                    }
                    state.setStatus(ItemState.STATUS_STALE_MODIFIED);
                    stateModified(state);
                }
            }
            
            for(Iterator iter = addedStates(); iter.hasNext(); ) {
                ItemState state = (ItemState) iter.next();
                if((isVirtual(state) & ITEM_TYPE_EXTERNAL) != 0) {
                    virtualStates.add(state);
                }
            }
        }
        
        void repopulate() {
            for(Iterator iter = virtualStates.iterator(); iter.hasNext(); ) {
                ItemState state = (ItemState) iter.next();
                if((isVirtual(state) & ITEM_TYPE_EXTERNAL) != 0) {
                    try {
                        virtualProviders.get(((NodeState)state).getNodeTypeName()).populate((NodeState)state);
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
            }
            public boolean hasNext() {
                while(current == null) {
                    if(!actualIterator.hasNext())
                        return false;
                    current = (ItemState) actualIterator.next();
                    if((isVirtual(current) & ITEM_TYPE_VIRTUAL) != 0)
                        current = null;
                }
                return true;
            }
            public Object next() throws NoSuchElementException {
                Object rtValue = null;
                while(current == null) {
                    if(!actualIterator.hasNext())
                        return false;
                    current = (ItemState) actualIterator.next();
                    if((isVirtual(current) & ITEM_TYPE_VIRTUAL) != 0)
                        current = null;
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
