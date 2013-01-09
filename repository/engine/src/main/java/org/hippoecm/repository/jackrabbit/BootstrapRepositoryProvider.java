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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import javax.jcr.InvalidItemStateException;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.spi.Name;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.dataprovider.HippoNodeId;
import org.hippoecm.repository.dataprovider.HippoVirtualProvider;
import org.hippoecm.repository.dataprovider.StateProviderContext;

public class BootstrapRepositoryProvider extends HippoVirtualProvider
{

    protected class BootstrapNodeId extends HippoNodeId {
        private static final long serialVersionUID = 1L;
        NodeId upstream;

        protected BootstrapNodeId(HippoVirtualProvider provider, NodeId parent, StateProviderContext context, Name name, NodeId upstream) {
            super(provider, parent, context, name);
            this.upstream = upstream;
        }

        BootstrapNodeId(NodeId parent, NodeId upstream, StateProviderContext context, Name name) {
            super(BootstrapRepositoryProvider.this, parent, context, name);
            this.upstream = upstream;
        }
    }

    Name docbaseName;
    Name jcrUUIDdocbaseName;
    Name mixinReferenceableName;

    @Override
    protected void initialize() throws RepositoryException {
        register(resolveName(HippoNodeType.NT_MOUNT), null);
        jcrUUIDdocbaseName = resolveName("jcr:uuid");
        mixinReferenceableName = resolveName("mix:referenceable");
    }

    BootstrapRepositoryProvider() throws RepositoryException {
        super();
    }

    public NodeState populate(StateProviderContext context, NodeState state) throws RepositoryException {
        NodeId nodeId = state.getNodeId();
        String docbase = getProperty(nodeId, docbaseName)[0];
        NodeState upstream = getNodeState(new NodeId(UUID.fromString(docbase)), context);
        if (upstream != null) {
            for (Iterator<ChildNodeEntry> iter = upstream.getChildNodeEntries().iterator(); iter.hasNext(); ) {
                ChildNodeEntry entry = iter.next();
                NodeId childNodeId = new BootstrapNodeId(nodeId, entry.getId(), context, entry.getName());
                state.addChildNodeEntry(entry.getName(), childNodeId);
            }
        }
        return state;
    }

    @Override
    public NodeState populate(StateProviderContext context, HippoNodeId nodeId, NodeId parentId) throws RepositoryException {
        NodeState upstream = getNodeState(((BootstrapNodeId)nodeId).upstream, context);
        if (upstream == null)
            throw new InvalidItemStateException("cannot populate top bootstrap node");
        NodeState state = createNew(nodeId, upstream.getNodeTypeName(), parentId);
        state.setNodeTypeName(upstream.getNodeTypeName());

        Set mixins = ((NodeState) state).getMixinTypeNames();
        if(mixins.contains(mixinReferenceableName)) {
            mixins = new HashSet(mixins);
            mixins.remove(mixinReferenceableName);
            state.setMixinTypeNames(mixins);
        } else {
            state.setMixinTypeNames(mixins);
        }

        for(Iterator<Name> iter = upstream.getPropertyNames().iterator(); iter.hasNext(); ) {
            Name propName = iter.next();
            PropertyId upstreamPropId = new HippoPropertyId(upstream.getNodeId(), upstream.getNodeId(), propName);
            PropertyState upstreamPropState = getPropertyState(upstreamPropId);
            if(propName.equals(jcrUUIDdocbaseName)) {
                continue;
            }
            state.addPropertyName(propName);
            PropertyState propState = createNew(propName, state.getNodeId());
            propState.setType(upstreamPropState.getType());
            propState.setValues(upstreamPropState.getValues());
            propState.setMultiValued(upstreamPropState.isMultiValued());
        }

        populateChildren(context, nodeId, state, upstream);
        return state;
    }

    protected void populateChildren(StateProviderContext context, NodeId nodeId, NodeState state, NodeState upstream) {
        for(Iterator<ChildNodeEntry> iter = upstream.getChildNodeEntries().iterator(); iter.hasNext(); ) {
            ChildNodeEntry entry = iter.next();
            BootstrapNodeId childNodeId = new BootstrapNodeId(nodeId, entry.getId(), context, entry.getName());
            state.addChildNodeEntry(entry.getName(), childNodeId);
        }
    }
}
