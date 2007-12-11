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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeDefinition;

import org.apache.jackrabbit.core.ItemLifeCycleListener;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.nodetype.NodeDefinitionImpl;
import org.apache.jackrabbit.core.nodetype.PropertyDefinitionImpl;
import org.apache.jackrabbit.core.ItemImpl;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.spi.Name;

public class NodeImpl extends org.apache.jackrabbit.core.NodeImpl {
    private static Logger log = LoggerFactory.getLogger(NodeImpl.class);

    protected NodeImpl(ItemManager itemMgr, SessionImpl session, NodeId id,
                       NodeState state, NodeDefinition definition,
                       ItemLifeCycleListener[] listeners) {
        super(itemMgr, session, id, state, definition, listeners);
    }

    @Override
    protected void onRemove() throws RepositoryException {
        HippoLocalItemStateManager localISM;
        localISM = (HippoLocalItemStateManager)(((HippoSessionItemStateManager)stateMgr).localStateMgr);
        if((localISM.isVirtual(state) & HippoLocalItemStateManager.ITEM_TYPE_EXTERNAL) != 0) {
	    ((NodeState)state).removeAllChildNodeEntries();
        }
        super.onRemove();
    }

    // FIXME: Only necessary because of current ItemManager implementation
    ItemState getItemState() {
        return state;
    }

    // FIXME: Only necessary because of current ItemManager implementation
    protected NodeDefinitionImpl getApplicableChildNodeDefinition(Name nodeName, Name nodeTypeName)
        throws ConstraintViolationException, RepositoryException {
        return super.getApplicableChildNodeDefinition(nodeName, nodeTypeName);
    }
}
