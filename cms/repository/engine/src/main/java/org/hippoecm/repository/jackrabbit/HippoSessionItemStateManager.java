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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.LocalItemStateManager;
import org.apache.jackrabbit.core.state.SessionItemStateManager;

import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.StaleItemStateException;
import javax.jcr.ReferentialIntegrityException;

import org.apache.jackrabbit.spi.Name;

class HippoSessionItemStateManager extends SessionItemStateManager {
    protected final Logger log = LoggerFactory.getLogger(HippoSessionItemStateManager.class);

    HippoHierarchyManager wrappedHierMgr = null;
    LocalItemStateManager localStateMgr;

    HippoSessionItemStateManager(NodeId rootNodeId, LocalItemStateManager manager, SessionImpl session) {
        super(rootNodeId, manager, session);
        localStateMgr = manager;
        if(wrappedHierMgr == null)
            wrappedHierMgr = new HippoHierarchyManager(this, super.getHierarchyMgr());
        // stateMgr = new AugmentingStateManager(stateMgr);
    }

    @Override
    public HierarchyManager getHierarchyMgr() {
        if(wrappedHierMgr == null)
            wrappedHierMgr = new HippoHierarchyManager(this, super.getHierarchyMgr());
        return wrappedHierMgr;
    }

    public HierarchyManager getAtticAwareHierarchyMgr() {
        return new HippoHierarchyManager(this, super.getAtticAwareHierarchyMgr());
    }

    HippoSessionItemStateManager(NodeId rootNodeId, LocalItemStateManager manager, XASessionImpl session) {
        super(rootNodeId, manager, session);
    }

    @Override
    public NodeState createNew(NodeState transientState) throws IllegalStateException {
        if (log.isDebugEnabled())
            System.err.println("HippoSessionItemStateManager.createNew#1 "+transientState.getNodeTypeName());
        Name nodeTypeName = transientState.getNodeTypeName();
        /*
        if(nodeTypeName.toString().equals("{http://www.hippoecm.org/nt/1.0}base")) {
            HippoNodeState virtualState = new HippoNodeState(transientState.getNodeId(), transientState.getNodeTypeName(), transientState.getParentId());
            transientState.connect(virtualState);
            NodeState persistentState = super.createNew(virtualState);
            //return virtualState;
            return persistentState;
            //} else if(nodeTypeName.toString().equals("{http://www.hippoecm.org/nt/1.0}item")) {
        } else
        */
           return super.createNew(transientState);
    }

    @Override
    public PropertyState createNew(Name propName, NodeId parentId) throws IllegalStateException {
        if (log.isDebugEnabled())
            System.err.println("HippoSessionItemStateManager.createNew#2 "+parentId+" "+propName);
        return super.createNew(propName, parentId);
    }

    @Override
    public PropertyState createNew(PropertyState transientState) throws IllegalStateException {
        if (log.isDebugEnabled())
            System.err.println("HippoSessionItemStateManager.createNew#3 "+transientState.getId());
        return super.createNew(transientState);
    }

    @Override
    public void store(ItemState state) throws IllegalStateException {
        if (log.isDebugEnabled())
            System.err.println("HippoSessionItemStateManager.store "+state.getId());
        super.store(state);
    }

    @Override
    public ItemState getItemState(ItemId id) throws NoSuchItemStateException, ItemStateException {
        if (log.isDebugEnabled())
            System.err.println("HippoSessionItemStateManager.getItemState ");
        return super.getItemState(id);
    }

    @Override
    public boolean hasItemState(ItemId id) {
        if (log.isDebugEnabled())
            System.err.println("HippoSessionItemStateManager.hasItemState ");
        return super.hasItemState(id);
    }

    @Override
    public ItemState getTransientItemState(ItemId id) throws NoSuchItemStateException, ItemStateException {
        if (log.isDebugEnabled())
            System.err.println("HippoSessionItemStateManager.getTransientItemState ");
        return super.getTransientItemState(id);
    }

    @Override
    public boolean hasTransientItemState(ItemId id) {
        if (log.isDebugEnabled())
            System.err.println("HippoSessionItemStateManager.hasTransientItemState ");
        return super.hasTransientItemState(id);
    }

    @Override
    public void update() throws ReferentialIntegrityException, StaleItemStateException, ItemStateException, IllegalStateException {
        if (log.isDebugEnabled())
            System.err.println("HippoSessionItemStateManager.update ");
        super.update();
    }

}
