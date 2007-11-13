/*
  THIS CODE IS UNDER CONSTRUCTION, please leave as is until
  work has proceeded to a stable level, at which time this comment
  will be removed.  -- Berry
*/

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

import org.apache.commons.collections.map.LinkedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.NamespaceException;

import org.apache.jackrabbit.conversion.IllegalNameException;
import org.apache.jackrabbit.conversion.NamePathResolver;
import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.nodetype.NodeDef;
import org.apache.jackrabbit.core.nodetype.NodeDefImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.PropDef;
import org.apache.jackrabbit.core.nodetype.PropDefImpl;
import org.apache.jackrabbit.core.observation.EventStateCollectionFactory;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateCacheFactory;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.ItemStateReferenceCache;
import org.apache.jackrabbit.core.state.LocalItemStateManager;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.core.state.NodeReferencesId;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.state.SharedItemStateManager;
import org.apache.jackrabbit.core.state.StaleItemStateException;
import org.apache.jackrabbit.core.state.XAItemStateManager;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.core.virtual.VirtualNodeState;
import org.apache.jackrabbit.name.NameConstants;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.uuid.UUID;

class HippoLocalItemStateManager extends XAItemStateManager
{
    protected final Logger log = LoggerFactory.getLogger(HippoLocalItemStateManager.class);

    private final ItemStateReferenceCache cache;
    private NodeTypeRegistry ntReg;
    private NamePathResolver namePathResolver;

    public HippoLocalItemStateManager(SharedItemStateManager sharedStateMgr, EventStateCollectionFactory factory,
                                      ItemStateCacheFactory cacheFactory, NodeTypeRegistry ntReg)
    {
        super(sharedStateMgr, factory, cacheFactory);
        this.ntReg = ntReg;
        cache = new ItemStateReferenceCache(cacheFactory);
    }

    @Override
    public synchronized void edit() throws IllegalStateException {
        if(inEditMode())
            return;
        super.edit();
    }

    @Override
    public NodeState createNew(NodeId id, Name nodeTypeName, NodeId parentId) throws IllegalStateException {
        if(nodeTypeName.toString().equals("{http://www.hippoecm.org/nt/1.0}external")) {
            if (log.isDebugEnabled())
                System.err.println("HippoLocalItemStateManager.createNew virtual "+nodeTypeName+" "+id+" "+parentId);
            NodeState nodeState = super.createNew(id, nodeTypeName, parentId);
            return nodeState;
        } else {
            if (log.isDebugEnabled())
                System.err.println("HippoLocalItemStateManager.createNew "+nodeTypeName+" "+id+" "+parentId);
            NodeState nodeState = super.createNew(id, nodeTypeName, parentId);
            return nodeState;
        }
    }

    void setNamePathResolver(NamePathResolver namePathResolver) {
        this.namePathResolver = namePathResolver;
    }

    public PropertyState createNew(Name propName, NodeId parentId) throws IllegalStateException {
        PropertyState propertyState = super.createNew(propName, parentId);
        if (log.isDebugEnabled())
            System.err.println("HippoLocalItemStateManager.createNew "+propName+" "+propertyState+" "+parentId+" "+propertyState.getId());
        return propertyState;
    }
    @Override
    public void store(ItemState state) throws IllegalStateException {
        if (log.isDebugEnabled())
            System.err.println("HippoLocalItemStateManager.store "+state.getId());
        super.store(state);
    }
    @Override
    public void destroy(ItemState state) throws IllegalStateException {
        if (log.isDebugEnabled())
            System.err.println("HippoLocalItemStateManager.destroy "+state.getId());
        super.destroy(state);
    }
    @Override
    public void cancel() throws IllegalStateException {
        if (log.isDebugEnabled())
            System.err.println("HippoLocalItemStateManager.cancel ");
        super.cancel();
    }

    FilteredChangeLog filteredChangeLog = null;

    @Override
    protected void update(ChangeLog changeLog) throws ReferentialIntegrityException,StaleItemStateException,ItemStateException {
        if (log.isDebugEnabled())
            System.err.println("HippoLocalItemStateManager.update start ");
        filteredChangeLog = new FilteredChangeLog(changeLog);
        filteredChangeLog.invalidate();
        super.update(filteredChangeLog);
        if (log.isDebugEnabled())
            System.err.println("HippoLocalItemStateManager.update end ");
    }

    public void update() throws ReferentialIntegrityException, StaleItemStateException,ItemStateException,IllegalStateException {
        if (log.isDebugEnabled())
            System.err.println("HippoLocalItemStateManager.update ");
        virtualChildren.clear();
        super.update();
        edit();
        filteredChangeLog.repopulate();
        filteredChangeLog = null;
    }
    
    @Override
    protected NodeState getNodeState(NodeId id) throws NoSuchItemStateException, ItemStateException {
        if (log.isDebugEnabled())
            System.err.println("HippoLocalItemStateManager.getNodeState "+id);
        if(virtualChildren.containsKey(id)) {
            try {
                return populate(id, virtualChildren.get(id));
            } catch(NamespaceException ex) {
                System.err.println(ex.getMessage());
                ex.printStackTrace(System.err);
            } catch(IllegalNameException ex) {
                System.err.println(ex.getMessage());
                ex.printStackTrace(System.err);
            }
        }
        NodeState state = super.getNodeState(id);
        if(state.getNodeTypeName().toString().equals("{http://www.hippoecm.org/nt/1.0}external")) {
            populate(state);
        }
        return state;
    }
  
    @Override
    protected PropertyState getPropertyState(PropertyId id) throws NoSuchItemStateException, ItemStateException {
        if (log.isDebugEnabled())
            System.err.println("HippoLocalItemStateManager.getPropertyState "+id);

        return super.getPropertyState(id);
    }
  
    @Override
    public ItemState getItemState(ItemId id) throws NoSuchItemStateException, ItemStateException {
        if (log.isDebugEnabled())
            System.err.println("HippoLocalItemStateManager.getItemState "+id);
        return super.getItemState(id);
    }
    
    @Override
    public boolean hasItemState(ItemId id) {
        if (log.isDebugEnabled())
            System.err.println("HippoLocalItemStateManager.hasItemState "+id);
        if(virtualChildren.containsKey(id))
            return true;
        return super.hasItemState(id);
    }
    
    @Override
    public NodeReferences getNodeReferences(NodeReferencesId id) throws NoSuchItemStateException, ItemStateException {
        if (log.isDebugEnabled())
            System.err.println("HippoLocalItemStateManager.getNodeReferences ");
        return super.getNodeReferences(id);
    }
    
    @Override
    public boolean hasNodeReferences(NodeReferencesId id) {
        if (log.isDebugEnabled())
            System.err.println("HippoLocalItemStateManager.hasNodeReferences ");
        return super.hasNodeReferences(id);
    }
   
    
    @Override
    public void stateCreated(ItemState created) {
        if (log.isDebugEnabled())
            System.err.println("HippoLocalItemStateManager.stateCreated "+created.getId());
        super.stateCreated(created);
    }
    
    @Override
    public void stateModified(ItemState modified) {
        if (log.isDebugEnabled())
            System.err.println("HippoLocalItemStateManager.stateModified "+modified.getId());
        super.stateModified(modified);
    }

    @Override
    public void stateDestroyed(ItemState destroyed) {
        if (log.isDebugEnabled())
            System.err.println("HippoLocalItemStateManager.stateDestroyed "+destroyed.getId());
        super.stateDestroyed(destroyed);
    }

    @Override
    public void stateDiscarded(ItemState discarded) {
        if (log.isDebugEnabled())
            System.err.println("HippoLocalItemStateManager.stateDiscarded "+discarded.getId());
        super.stateDiscarded(discarded);
    }


    static PropDefImpl propDef = null;
    static NodeDefImpl nodeDef = null;
    PropDef getPropertyDefinition() {
        if(propDef == null) {
            try {
                propDef = new PropDefImpl();
                propDef.setRequiredType(PropertyType.LONG);
                propDef.setProtected(true);
                propDef.setDeclaringNodeType(namePathResolver.getQName("hippo:external"));
                propDef.setName(namePathResolver.getQName("hippo:property"));
            } catch(NamespaceException ex) {
                System.err.print(ex.getMessage());
                ex.printStackTrace(System.err);
            } catch(IllegalNameException ex) {
                System.err.print(ex.getMessage());
                ex.printStackTrace(System.err);
            }
        }
        return propDef;
    }
    NodeDef getNodeDefinition() {
        if(nodeDef == null) {
            try {
                nodeDef = new NodeDefImpl();
                nodeDef.setDefaultPrimaryType(NameConstants.NT_BASE);
                nodeDef.setAllowsSameNameSiblings(true);
                nodeDef.setProtected(true);
                nodeDef.setDeclaringNodeType(namePathResolver.getQName("hippo:external"));
                nodeDef.setName(namePathResolver.getQName("hippo:virtual"));
            } catch(NamespaceException ex) {
                System.err.print(ex.getMessage());
                ex.printStackTrace(System.err);
            } catch(IllegalNameException ex) {
                System.err.print(ex.getMessage());
                ex.printStackTrace(System.err);
            }
        }
        return nodeDef;
    }

    static NodeId workaroundNodeId;
    static void workaround(NodeId nodeId) {
        workaroundNodeId = nodeId;
    }

    Map<NodeId,NodeId> virtualChildren = new LinkedMap();
    void populate(NodeState state) {
        edit();
        try {
            Name name = namePathResolver.getQName("hippo:property");
            PropertyState propState = createNew(name, state.getNodeId());
            propState.setType(PropertyType.LONG);
            propState.setMultiValued(false);
            propState.setDefinitionId(getPropertyDefinition().getId());
            propState.setValues(new InternalValue[] { InternalValue.create(2) });
            state.addPropertyName(name);

            NodeId childNodeId = new NodeId(UUID.randomUUID());
            state.addChildNodeEntry(namePathResolver.getQName("test"), childNodeId);
            virtualChildren.put(childNodeId, state.getNodeId());

            childNodeId = new NodeId(UUID.randomUUID());
            NodeState realNodeState = getNodeState(workaroundNodeId);
            NodeState realParentState = getNodeState(realNodeState.getParentId());
            NodeState.ChildNodeEntry entry = realParentState.getChildNodeEntry(realNodeState.getNodeId());
            state.addChildNodeEntry(entry.getName(), childNodeId);
            Name nodeTypeName = namePathResolver.getQName("hippo:virtual");
            NodeState childNodeState = createNew(childNodeId, nodeTypeName, state.getNodeId());
            childNodeState.setNodeTypeName(realNodeState.getNodeTypeName());
            childNodeState.setMixinTypeNames(realNodeState.getMixinTypeNames());
            childNodeState.setDefinitionId(realNodeState.getDefinitionId());
            childNodeState.setPropertyNames(realNodeState.getPropertyNames());
            for(Iterator iter = realNodeState.getPropertyNames().iterator(); iter.hasNext(); ) {
                Name propName = (Name) iter.next();
                PropertyId realPropId = new PropertyId(realNodeState.getNodeId(), propName);
                PropertyState realPropState = getPropertyState(realPropId);
                PropertyState copyPropState = createNew(propName, childNodeState.getNodeId());
                copyPropState.setType(realPropState.getType());
                copyPropState.setDefinitionId(realPropState.getDefinitionId());
                copyPropState.setValues(realPropState.getValues());
                copyPropState.setMultiValued(realPropState.isMultiValued());
            }
            //childNodeState.setChildNodeEntries(realNodeState.getChildNodeEntries()); TODO
            stateCreated(childNodeState);

            stateModified(state);
        } catch(NoSuchItemStateException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
        } catch(ItemStateException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
        } catch(NamespaceException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
        } catch(IllegalNameException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
        }
    }

    NodeState populate(NodeId nodeId, NodeId parentId) throws IllegalNameException, NamespaceException {
        NodeState state = createNew(nodeId, namePathResolver.getQName("hippo:virtual"), parentId);
        state.setDefinitionId(getNodeDefinition().getId());
        populate(state);
        return state;
    }

    class FilteredChangeLog extends ChangeLog {
        private ChangeLog upstream;
        private final Set<ItemId> virtuals = new HashSet<ItemId>();
        private final Set<ItemId> externals = new HashSet<ItemId>();

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
                    if(isVirtual(current) == ITEM_TYPE_VIRTUAL)
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
                    if(isVirtual(current) == ITEM_TYPE_VIRTUAL)
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

        FilteredChangeLog(ChangeLog changelog) {
            upstream = changelog;
        }
        FilteredChangeLog() {
            upstream = null;
        }
        public static final int ITEM_TYPE_REGULAR  = 0;
        public static final int ITEM_TYPE_EXTERNAL = 1;
        public static final int ITEM_TYPE_VIRTUAL  = 2;
        private int isVirtual(ItemState state) {
            ItemId parentId = state.getParentId();
            if(parentId != null && (externals.contains(parentId) || virtuals.contains(parentId)))
                return ITEM_TYPE_VIRTUAL;
            if(state.isNode()) {
                String name = ((NodeState)state).getNodeId().toString();
                if(((NodeState)state).getNodeTypeName().toString().equals("{http://www.hippoecm.org/nt/1.0}external")) {
                    if (log.isDebugEnabled())
                        System.err.println("FilterChangeLog.isVirtual#1 not virtual "+state.getId()+" "+name);
                    externals.add(state.getId());
                    return ITEM_TYPE_EXTERNAL;
                } else if(((NodeState)state).getNodeTypeName().toString().equals("{http://www.hippoecm.org/nt/1.0}virtual")) {
                    if (log.isDebugEnabled())
                        System.err.println("FilterChangeLog.isVirtual#2 virtual "+state.getId()+" "+name);
                    return ITEM_TYPE_VIRTUAL;
                } else {
                    if (log.isDebugEnabled())
                        System.err.println("FilterChangeLog.isVirtual#3 not virtual "+state.getId()+" "+name);
                    return ITEM_TYPE_REGULAR;
                }
           } else {
                Name name = ((PropertyState)state).getName();
                if(name.toString().equals("{http://www.hippoecm.org/nt/1.0}property")) {
                    if (log.isDebugEnabled())
                        System.err.println("FilterChangeLog.isVirtual#4 virtual "+state.getId()+" "+name.toString());
                    return ITEM_TYPE_VIRTUAL;
                } else {
                    if (log.isDebugEnabled())
                        System.err.println("FilterChangeLog.isVirtual#5 not virtual "+state.getId()+" "+name.toString());
                    return ITEM_TYPE_REGULAR;
                }
            }
        }

        List<ItemState> states = new LinkedList<ItemState>();

        void invalidate() {
            for(Iterator iter = addedStates(); iter.hasNext(); ) {
                ItemState state = (ItemState) iter.next();
                if(isVirtual(state) == ITEM_TYPE_VIRTUAL) {
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
                if(isVirtual(state) == ITEM_TYPE_EXTERNAL) {
                    states.add(state);
                }
            }
        }

        void repopulate() {
            for(Iterator iter = states.iterator(); iter.hasNext(); ) {
                ItemState state = (ItemState) iter.next();
                if(isVirtual(state) == ITEM_TYPE_EXTERNAL) {
                    populate((NodeState)state);
                }
            }
        }

        /*
        Map newAddedStates = new LinkedMap();
        Map newModifiedStates = new LinkedMap();
        Map newDeletedStates = new LinkedMap();
        Map newModifiedRefs = new LinkedMap();

        @Override public void added(ItemState state) {
            if (log.isDebugEnabled())
                System.err.println("FilteredChangeLog.added "+state);
            newAddedStates.put(state.getId(), state);
        }
        @Override public void modified(ItemState state) {
            if (log.isDebugEnabled())
                System.err.println("FilteredChangeLog.modified "+state);
            newModifiedStates.put(state.getId(), state);
        }
        @Override public void deleted(ItemState state) {
            if (log.isDebugEnabled())
                System.err.println("FilteredChangeLog.deleted "+state);
            newDeletedStates.put(state.getId(), state);
        }
        @Override public void modified(NodeReferences refs) {
            if (log.isDebugEnabled())
                System.err.println("FilteredChangeLog.modifiedRefs");
            newModifiedStates.put(state.getId(), state);
        }
        */

        @Override public ItemState get(ItemId id) throws NoSuchItemStateException {
            /*
            if (log.isDebugEnabled())
                System.err.println("FilteredChangeLog.get "+id);
            */
            if(upstream != null)
                return upstream.get(id);
            else
                return super.get(id);
        }
        @Override public boolean has(ItemId id) {
            /*
            if (log.isDebugEnabled())
                System.err.println("FilteredChangeLog.has "+id);
            */
            if(upstream != null)
                return upstream.has(id);
            else
                return super.has(id);
        }
        @Override public boolean deleted(ItemId id) {
            /*
            if (log.isDebugEnabled())
                System.err.println("FilteredChangeLog.has "+id);
            */
            if(upstream != null)
                return upstream.deleted(id);
            else
                return super.deleted(id);
        }
        @Override public NodeReferences get(NodeReferencesId id) {
            if (log.isDebugEnabled())
                System.err.println("FilteredChangeLog.get#references "+id);
            if(upstream != null)
                return upstream.get(id);
            else
                return super.get(id);
        }
        @Override public Iterator addedStates() {
            if (log.isDebugEnabled())
                System.err.println("FilteredChangeLog.addedStates ");
            if(upstream != null)
                return new FilteredStateIterator(upstream.addedStates());
            else
                return new FilteredStateIterator(super.addedStates());
        }
        @Override public Iterator modifiedStates() {
            /*
            if (log.isDebugEnabled())
                System.err.println("FilteredChangeLog.modifiedStates ");
            */
            if(upstream != null)
                return new FilteredStateIterator(upstream.modifiedStates());
            else
                return new FilteredStateIterator(super.modifiedStates());
        }
        @Override public Iterator deletedStates() {
            /*
            if (log.isDebugEnabled())
                System.err.println("FilteredChangeLog.deletedStates ");
            */
            if(upstream != null)
                return new FilteredStateIterator(upstream.deletedStates());
            else
                return new FilteredStateIterator(super.deletedStates());
        }
        @Override public Iterator modifiedRefs() {
            if (log.isDebugEnabled())
                System.err.println("FilteredChangeLog.modifiedRefs ");
            if(upstream != null)
                return upstream.modifiedRefs();
            else
                return super.modifiedRefs();
        }
        @Override public void merge(ChangeLog other) {
            if (log.isDebugEnabled())
                System.err.println("FilteredChangeLog.merge ");
            if(upstream != null)
                upstream.merge(other);
            else
                super.merge(other);
        }
        @Override public void push() {
            if (log.isDebugEnabled())
                System.err.println("FilteredChangeLog.push ");
            if(upstream != null)
                upstream.push();
            else
                super.push();
        }
        @Override public void persisted() {
            if (log.isDebugEnabled())
                System.err.println("FilteredChangeLog.persisted ");
            if(upstream != null)
                upstream.persisted();
            else
                super.persisted();
        }
        @Override public void reset() {
            if (log.isDebugEnabled())
                System.err.println("FilteredChangeLog.reset ");
            if(upstream != null)
                upstream.reset();
            else
                super.reset();
        }
        @Override public void disconnect() {
            if (log.isDebugEnabled())
                System.err.println("FilteredChangeLog.disconnect ");
            if(upstream != null)
                upstream.disconnect();
            else
                super.disconnect();
        }
        @Override public void undo(ItemStateManager parent) {
            if (log.isDebugEnabled())
                System.err.println("FilteredChangeLog.undo ");
            if(upstream != null)
                upstream.undo(parent);
            else
                super.undo(parent);
        }
        @Override public String toString() {
            if(upstream != null)
                return upstream.toString();
            else
                return super.toString();
        }
    }

}
