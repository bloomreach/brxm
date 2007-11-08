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

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.commons.collections.map.LinkedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.ReferentialIntegrityException;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.LocalItemStateManager;
import org.apache.jackrabbit.core.state.XAItemStateManager;

import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.name.QName;

import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.observation.EventStateCollectionFactory;
import org.apache.jackrabbit.core.state.ItemStateCacheFactory;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateReferenceCache;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.core.state.NodeReferencesId;
import org.apache.jackrabbit.core.state.SharedItemStateManager;
import org.apache.jackrabbit.core.state.StaleItemStateException;

import org.apache.jackrabbit.core.virtual.VirtualNodeState;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;

import org.apache.jackrabbit.core.state.ChangeLog;

import org.apache.jackrabbit.spi.Name;

class HippoLocalItemStateManager extends XAItemStateManager
{
    protected final Logger log = LoggerFactory.getLogger(HippoLocalItemStateManager.class);

    private final ItemStateReferenceCache cache;
    private NodeTypeRegistry ntReg;

    public HippoLocalItemStateManager(SharedItemStateManager sharedStateMgr, EventStateCollectionFactory factory,
                                      ItemStateCacheFactory cacheFactory, NodeTypeRegistry ntReg)
    {
        super(sharedStateMgr, factory, cacheFactory);
        this.ntReg = ntReg;
        cache = new ItemStateReferenceCache(cacheFactory);
    }

    public NodeState createNew(NodeId id, Name nodeTypeName, NodeId parentId) throws IllegalStateException {
        if(nodeTypeName.toString().equals("{http://www.hippoecm.org/nt/1.0}external")) {
            if (log.isDebugEnabled())
                System.err.println("HippoLocalItemStateManager.createNew virtual "+nodeTypeName+" "+id+" "+parentId);
            NodeState nodeState = super.createNew(id, nodeTypeName, parentId);
            NodeState virtualNodeState = new NodeState(id, nodeTypeName, parentId, ItemState.STATUS_NEW, true);
            virtualNodeState.setContainer(this);
            return virtualNodeState;
        } else {
            if (log.isDebugEnabled())
                System.err.println("HippoLocalItemStateManager.createNew "+nodeTypeName+" "+id+" "+parentId);
            NodeState nodeState = super.createNew(id, nodeTypeName, parentId);
            return nodeState;
        }
    }

    public PropertyState createNew(Name propName, NodeId parentId) throws IllegalStateException {
        PropertyState propertyState = super.createNew(propName, parentId);
        if (log.isDebugEnabled())
            System.err.println("HippoLocalItemStateManager.createNew "+propName+" "+propertyState+" "+parentId+" "+propertyState.getId());
        return propertyState;
    }
    public void store(ItemState state) throws IllegalStateException {
        if (log.isDebugEnabled())
            System.err.println("HippoLocalItemStateManager.store "+state.getId());
        super.store(state);
    }
    public void destroy(ItemState state) throws IllegalStateException {
        if (log.isDebugEnabled())
            System.err.println("HippoLocalItemStateManager.destroy "+state.getId());
        super.destroy(state);
    }
    public void cancel() throws IllegalStateException {
        if (log.isDebugEnabled())
            System.err.println("HippoLocalItemStateManager.cancel ");
        super.cancel();
    }

    protected void update(ChangeLog changeLog) throws ReferentialIntegrityException, StaleItemStateException, ItemStateException {
        if (log.isDebugEnabled())
            System.err.println("HippoLocalItemStateManager.update start ");
        super.update(new FilterChangeLog(changeLog));
        if (log.isDebugEnabled())
            System.err.println("HippoLocalItemStateManager.update end ");
    }

    public void update() throws ReferentialIntegrityException, StaleItemStateException, ItemStateException, IllegalStateException {
        if (log.isDebugEnabled())
            System.err.println("HippoLocalItemStateManager.update ");
        super.update();
    }
    
    protected NodeState getNodeState(NodeId id) throws NoSuchItemStateException, ItemStateException {
        if (log.isDebugEnabled())
            System.err.println("HippoLocalItemStateManager.getNodeState "+id);
        return super.getNodeState(id);
    }
  
    protected PropertyState getPropertyState(PropertyId id) throws NoSuchItemStateException, ItemStateException {
        if (log.isDebugEnabled())
            System.err.println("HippoLocalItemStateManager.getPropertyState "+id);

        return super.getPropertyState(id);
    }
  
    public ItemState getItemState(ItemId id) throws NoSuchItemStateException, ItemStateException {
        if (log.isDebugEnabled())
            System.err.println("HippoLocalItemStateManager.getItemState "+id);
        return super.getItemState(id);
    }
    
    public boolean hasItemState(ItemId id) {
        if (log.isDebugEnabled())
            System.err.println("HippoLocalItemStateManager.hasItemState "+id);
        return super.hasItemState(id);
    }
    
    public NodeReferences getNodeReferences(NodeReferencesId id) throws NoSuchItemStateException, ItemStateException {
        if (log.isDebugEnabled())
            System.err.println("HippoLocalItemStateManager.getNodeReferences ");
        return super.getNodeReferences(id);
    }
    
    public boolean hasNodeReferences(NodeReferencesId id) {
        if (log.isDebugEnabled())
            System.err.println("HippoLocalItemStateManager.hasNodeReferences ");
        return super.hasNodeReferences(id);
    }
   
    
    public void stateCreated(ItemState created) {
        if (log.isDebugEnabled())
            System.err.println("HippoLocalItemStateManager.stateCreated "+created.getId());
        super.stateCreated(created);
    }
    
    public void stateModified(ItemState modified) {
        if (log.isDebugEnabled())
            System.err.println("HippoLocalItemStateManager.stateModified "+modified.getId());
        super.stateModified(modified);
    }

    public void stateDestroyed(ItemState destroyed) {
        if (log.isDebugEnabled())
            System.err.println("HippoLocalItemStateManager.stateDestroyed "+destroyed.getId());
        super.stateDestroyed(destroyed);
    }

    public void stateDiscarded(ItemState discarded) {
        if (log.isDebugEnabled())
            System.err.println("HippoLocalItemStateManager.stateDiscarded "+discarded.getId());
        super.stateDiscarded(discarded);
    }
    

    class FilterChangeLog extends ChangeLog {
        private ChangeLog upstream;
        private final Map virtualAddedStates = new LinkedMap();
        private final Map virtualDeletedStates = new LinkedMap();
        private final Map virtualModifiedStates = new LinkedMap();
        FilterChangeLog(ChangeLog changelog) {
            upstream = changelog;
        }
        FilterChangeLog() {
            upstream = null;
        }
        private boolean isVirtual(ItemState state) {
            if(state.isNode()) {
                String name = ((NodeState)state).getNodeId().toString();
                if(((NodeState)state).getNodeTypeName().toString().equals("{http://www.hippoecm.org/nt/1.0}virtual") ||
                   (((NodeState)state).getNodeTypeName().toString().equals("{http://www.hippoecm.org/nt/1.0}external")
                    && ((NodeState)state).isTransient())) {
                    if (log.isDebugEnabled())
                        System.err.println("FilterChangeLog virtual "+state.getId()+" "+name);
                    return true;
                } else {
                    if (log.isDebugEnabled())
                        System.err.println("FilterChangeLog not virtual "+state.getId()+" "+name);
                    return false;
                }
           } else {
                Name name = ((PropertyState)state).getName();
                if(name.toString().equals("{http://www.hippoecm.org/nt/1.0}property")) {
                    if (log.isDebugEnabled())
                        System.err.println("FilterChangeLog virtual "+state.getId()+" "+name.toString());
                    return true;
                } else {
                    if (log.isDebugEnabled())
                        System.err.println("FilterChangeLog not virtual "+state.getId()+" "+name.toString());
                    return false;
                }
            }
        }
        @Override public void added(ItemState state) {
            if (log.isDebugEnabled())
                System.err.println("FilterChangeLog.added "+state.getId());
            if(isVirtual(state)) {
                virtualAddedStates.put(state.getId(), state);
            } else {
                if(upstream != null)
                    upstream.added(state);
                else
                    super.added(state);
            }
        }
        @Override public void modified(ItemState state) {
            if (log.isDebugEnabled())
                System.err.println("FilterChangeLog.modified "+state);
            if(isVirtual(state)) {
                virtualModifiedStates.put(state.getId(), state);
            } else {
                if(upstream != null)
                    upstream.modified(state);
                else
                    super.modified(state);
            }
        }
        @Override public void deleted(ItemState state) {
            if (log.isDebugEnabled())
                System.err.println("FilterChangeLog.deleted "+state);
            if(isVirtual(state)) {
                virtualDeletedStates.put(state.getId(), state);
            } else {
                if(upstream != null)
                    upstream.deleted(state);
                else
                    super.deleted(state);
            }
        }
        @Override public void modified(NodeReferences refs) {
            if (log.isDebugEnabled())
                System.err.println("FilterChangeLog.modified");
            //if(!virtualItems.contains(state))
            if(upstream != null)
                upstream.modified(refs);
            else
                super.modified(refs);
        }

        @Override public ItemState get(ItemId id) throws NoSuchItemStateException {
            if (log.isDebugEnabled())
                System.err.println("FilterChangeLog.get "+id);
            ItemState state = (ItemState) virtualAddedStates.get(id);
            if(state != null) {
                //if(state.isNode())
                //populate(state);
                return state;
            }
            if(upstream != null)
                return upstream.get(id);
            else
                return super.get(id);
        }
        @Override public boolean has(ItemId id) {
            if (log.isDebugEnabled())
                System.err.println("FilterChangeLog.has "+id);
            if(upstream != null)
                return upstream.has(id);
            else
                return super.has(id);
        }
        @Override public boolean deleted(ItemId id) {
            if (log.isDebugEnabled())
                System.err.println("FilterChangeLog.has "+id);
            if(upstream != null)
                return upstream.deleted(id);
            else
                return super.deleted(id);
        }
        @Override public NodeReferences get(NodeReferencesId id) {
            if (log.isDebugEnabled())
                System.err.println("FilterChangeLog.get#references "+id);
            if(upstream != null)
                return upstream.get(id);
            else
                return super.get(id);
        }
        @Override public Iterator addedStates() {
            if (log.isDebugEnabled())
                System.err.println("FilterChangeLog.addedStates ");
            if(upstream != null)
                return upstream.addedStates();
            else
                return super.addedStates();
        }
        @Override public Iterator modifiedStates() {
            if (log.isDebugEnabled()) {
                System.err.println("FilterChangeLog.modifiedStates ");
                Iterator iter;
                if(upstream != null)
                    iter = upstream.addedStates();
                else
                    iter = super.addedStates();
                while(iter.hasNext()) {
                    ItemState item = (ItemState) iter.next();
                    System.err.println("FilterChangeLog.modifiedStates "+item.getId());
                }
            }
            if(upstream != null)
                return upstream.modifiedStates();
            else
                return super.modifiedStates();
        }
        @Override public Iterator deletedStates() {
            if (log.isDebugEnabled()) {
                System.err.println("FilterChangeLog.deletedStates ");
                Iterator iter;
                if(upstream != null)
                    iter = upstream.addedStates();
                else
                    iter = super.addedStates();
                while(iter.hasNext()) {
                    ItemState item = (ItemState) iter.next();
                    System.err.println("FilterChangeLog.deletedStates "+item.getId());
                }
            }
            if(upstream != null)
                return upstream.deletedStates();
            else
                return super.deletedStates();
        }
        @Override public Iterator modifiedRefs() {
            if (log.isDebugEnabled())
                System.err.println("FilterChangeLog.modifiedRefs ");
            if(upstream != null)
                return upstream.modifiedRefs();
            else
                return super.modifiedRefs();
        }
        @Override public void merge(ChangeLog other) {
            if (log.isDebugEnabled())
                System.err.println("FilterChangeLog.merge ");
            if(upstream != null)
                upstream.merge(other);
            else
                super.merge(other);
        }
        @Override public void push() {
            if (log.isDebugEnabled())
                System.err.println("FilterChangeLog.push ");
            if(upstream != null)
                upstream.push();
            else
                super.push();
        }
        @Override public void persisted() {
            if (log.isDebugEnabled())
                System.err.println("FilterChangeLog.persisted ");
            for(Iterator iter = virtualAddedStates.values().iterator(); iter.hasNext(); ) {
                ItemState state = (ItemState) iter.next();
                if(state.isNode()) {
                    if(state.hasOverlayedState()) {
                        state.getOverlayedState().getContainer().stateModified(state);
                    }
                    state.setStatus(ItemState.STATUS_STALE_MODIFIED);
                    state.notifyStateUpdated();
                }
            }
            if(upstream != null)
                upstream.persisted();
            else
                super.persisted();
        }
        @Override public void reset() {
            if (log.isDebugEnabled())
                System.err.println("FilterChangeLog.reset ");
            if(upstream != null)
                upstream.reset();
            else
                super.reset();
        }
        @Override public void disconnect() {
            if (log.isDebugEnabled())
                System.err.println("FilterChangeLog.disconnect ");
            if(upstream != null)
                upstream.disconnect();
            else
                super.disconnect();
        }
        @Override public void undo(ItemStateManager parent) {
            if (log.isDebugEnabled())
                System.err.println("FilterChangeLog.undo ");
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
