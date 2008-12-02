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
package org.hippoecm.repository.jackrabbit;

import java.util.Iterator;

import javax.jcr.ReferentialIntegrityException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.LocalItemStateManager;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.SessionItemStateManager;
import org.apache.jackrabbit.core.state.StaleItemStateException;
import org.apache.jackrabbit.spi.commons.conversion.PathResolver;

public class HippoSessionItemStateManager extends SessionItemStateManager {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    protected final Logger log = LoggerFactory.getLogger(HippoSessionItemStateManager.class);

    HippoHierarchyManager wrappedHierMgr = null;
    HippoLocalItemStateManager localStateMgr;
    NodeId rootNodeId;

    HippoSessionItemStateManager(NodeId rootNodeId, LocalItemStateManager mgr, PathResolver resolver, NodeTypeRegistry ntReg) {
        super(rootNodeId, mgr, resolver, ntReg);
        this.rootNodeId = rootNodeId;
        this.localStateMgr = (HippoLocalItemStateManager) mgr;
        if (wrappedHierMgr == null) {
            wrappedHierMgr = new HippoHierarchyManager(this, super.getHierarchyMgr());
        }
    }

    public void disposeAllTransientItemStates() {
        /* It is imperative that the stateMgr.refresh() method is ONLY called after a
         * super.disposeAllTransientItemStates().  This is the only way to guarantee
         * that there are in fact no changes in the changelog of the local ISM.
         */
        super.disposeAllTransientItemStates();
        try {
            edit();
            localStateMgr.refresh();
        } catch(ReferentialIntegrityException ex) {
            // cannot occur
        } catch(StaleItemStateException ex) {
            // cannot occur
        } catch(ItemStateException ex) {
            // cannot occur
        }
    }

    @Override
    public void dispose() {
        localStateMgr.dispose();
        super.dispose();
    }

    @Override
    public HierarchyManager getHierarchyMgr() {
        if(wrappedHierMgr == null)
            wrappedHierMgr = new HippoHierarchyManager(this, super.getHierarchyMgr());
        return wrappedHierMgr;
    }

    @Override
    public HierarchyManager getAtticAwareHierarchyMgr() {
        return new HippoHierarchyManager(this, super.getAtticAwareHierarchyMgr());
    }

    @Override
    public void stateDiscarded(ItemState discarded) {
        super.stateDiscarded(discarded);
    }

    /**
     * This is a hack for the missing method in the o.a.j.c.SessionItemStateManager.
     * "return atticStore.get(id);" should all that's needed, see getTransientItemState.
     * @param id the item id
     * @return the ItemState
     * @throws NoSuchItemStateException when the item is not in the attic store
     * @throws ItemStateException when the item should be in the attic but cannot be found
     */
    public ItemState getAtticItemState(ItemId id) throws NoSuchItemStateException, ItemStateException {
        ItemState itemState = null;

        // If the item id is in the attic it should be in the localStateManager as well
        if (localStateMgr.hasItemState(id)) {
            return localStateMgr.getItemState(id);
        }

        if (!hasTransientItemStateInAttic(id)) {
            throw new NoSuchItemStateException("Item state not found in attic: " + id);
        }

        // FIXME: FIX_IN_JACKRABBIT! This is extremely expensive on large deletes.
        // somehow we do have getTransientState but not getAtticState
        // it is possible to get a list of all attic states from the root node
        Iterator iter = getDescendantTransientItemStatesInAttic(rootNodeId);
        while (iter.hasNext()) {
            ItemState state = (ItemState) iter.next();
            if (state.getId().equals(id)) {
                itemState = state;
                break;
            }
        }
        if (itemState == null) {
            throw new ItemStateException("Item state not found in attic, but the itemMgr thinks it is: " + id);
        }
        return itemState;
    }
}
