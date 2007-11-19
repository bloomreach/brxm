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

import java.util.Iterator;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.spi.Name;

public class MirrorVirtualProvider extends HippoVirtualProvider
{
    protected class MirrorNodeId extends HippoNodeId {
        NodeId upstream;
        protected MirrorNodeId(HippoVirtualProvider provider, NodeId parent, NodeId upstream) {
            super(provider, parent);
            this.upstream = upstream;
        }
        MirrorNodeId(NodeId parent, NodeId upstream) {
            super(MirrorVirtualProvider.this, parent);
            this.upstream = upstream;
        }
    }

    MirrorVirtualProvider(HippoLocalItemStateManager stateMgr) {
        super(stateMgr);
    }

    public NodeState populate(NodeState state) {
        return state;
    }

    public NodeState populate(NodeId nodeId, NodeId parentId) {
        NodeState upstream = getNodeState(((MirrorNodeId)nodeId).upstream);
        NodeState state = createNew(nodeId, upstream.getNodeTypeName(), parentId);
        state.setNodeTypeName(upstream.getNodeTypeName());
        state.setMixinTypeNames(upstream.getMixinTypeNames());
        state.setDefinitionId(upstream.getDefinitionId());
        state.setPropertyNames(upstream.getPropertyNames());
        for(Iterator iter = upstream.getPropertyNames().iterator(); iter.hasNext(); ) {
            Name propName = (Name) iter.next();
            PropertyId upstreamPropId = new HippoPropertyId(upstream.getNodeId(), propName);
            PropertyState upstreamPropState = getPropertyState(upstreamPropId);
            PropertyState propState = createNew(propName, state.getNodeId());
            propState.setType(upstreamPropState.getType());
            propState.setDefinitionId(upstreamPropState.getDefinitionId());
            propState.setValues(upstreamPropState.getValues());
            propState.setMultiValued(upstreamPropState.isMultiValued());
        }
        populateChildren(nodeId, state, upstream);
        return state;
    }

    protected void populateChildren(NodeId nodeId, NodeState state, NodeState upstream) {
        for(Iterator iter = upstream.getChildNodeEntries().iterator(); iter.hasNext(); ) {
            NodeState.ChildNodeEntry entry = (NodeState.ChildNodeEntry) iter.next();
            state.addChildNodeEntry(entry.getName(), new MirrorNodeId(nodeId, entry.getId()));
        }
    }
}
