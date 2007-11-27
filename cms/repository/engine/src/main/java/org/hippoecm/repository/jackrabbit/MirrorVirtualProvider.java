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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.conversion.NamePathResolver;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.nodetype.PropDef;
import org.apache.jackrabbit.core.nodetype.PropDefId;
import org.apache.jackrabbit.core.nodetype.PropDefImpl;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.spi.Name;

import org.hippoecm.repository.api.HippoNodeType;

public class MirrorVirtualProvider extends HippoVirtualProvider
{
    final static private String SVN_ID = "$Id$";

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

    Name docbaseName;
    Name jcrUUIDdocbaseName;
    Name hippoUUIDName;
    Name hippoReferenceableName;
    Name mixinReferenceableName;

    PropDef hippoUUIDPropDef;

    private void initialize() throws RepositoryException {
        docbaseName = stateMgr.resolver.getQName(HippoNodeType.HIPPO_DOCBASE);
        jcrUUIDdocbaseName = stateMgr.resolver.getQName("jcr:uuid");
        hippoUUIDName = stateMgr.resolver.getQName(HippoNodeType.HIPPO_UUID);
        hippoReferenceableName = stateMgr.resolver.getQName(HippoNodeType.NT_REFERENCEABLE);
        mixinReferenceableName = stateMgr.resolver.getQName("mix:referenceable");

        hippoUUIDPropDef = lookupPropDef(hippoReferenceableName, hippoUUIDName);

    }

    MirrorVirtualProvider(HippoLocalItemStateManager stateMgr) throws RepositoryException {
        super(stateMgr, stateMgr.resolver.getQName(HippoNodeType.NT_MIRROR), null);
        initialize();
    }

    protected MirrorVirtualProvider(HippoLocalItemStateManager stateMgr, Name external, Name virtual) throws RepositoryException {
        super(stateMgr, external, virtual);
        initialize();
    }

    public NodeState populate(NodeState state) throws RepositoryException {
        NodeId nodeId = state.getNodeId();
        String docbase = getProperty(nodeId, docbaseName)[0];
        NodeState upstream = getNodeState(docbase);
        for(Iterator iter = upstream.getChildNodeEntries().iterator(); iter.hasNext(); ) {
            NodeState.ChildNodeEntry entry = (NodeState.ChildNodeEntry) iter.next();
            NodeId childNodeId = new MirrorNodeId(nodeId, entry.getId());
            state.addChildNodeEntry(entry.getName(), childNodeId);
        }
        return state;
    }

    public NodeState populate(HippoNodeId nodeId, NodeId parentId) throws RepositoryException {
        NodeState upstream = getNodeState(((MirrorNodeId)nodeId).upstream);
        NodeState state = createNew(nodeId, upstream.getNodeTypeName(), parentId);
        state.setNodeTypeName(upstream.getNodeTypeName());

        Set mixins = ((NodeState) state).getMixinTypeNames();
        if(mixins.contains(mixinReferenceableName)) {
            mixins = new HashSet(mixins);
            mixins.remove(mixinReferenceableName);
            mixins.add(hippoReferenceableName);
            state.setMixinTypeNames(mixins);
        } else {
            state.setMixinTypeNames(mixins);
        }

        state.setDefinitionId(upstream.getDefinitionId());
        for(Iterator iter = upstream.getPropertyNames().iterator(); iter.hasNext(); ) {
            Name propName = (Name) iter.next();
            PropertyId upstreamPropId = new HippoPropertyId(upstream.getNodeId(), propName);
            PropertyState upstreamPropState = getPropertyState(upstreamPropId);
            PropDefId propDefId = upstreamPropState.getDefinitionId();
            if(propName.equals(jcrUUIDdocbaseName)) {
                propName = hippoUUIDName;
                propDefId = hippoUUIDPropDef.getId();
            }
            state.addPropertyName(propName);
            PropertyState propState = createNew(propName, state.getNodeId());
            propState.setType(upstreamPropState.getType());
            propState.setDefinitionId(propDefId);
            propState.setValues(upstreamPropState.getValues());
            propState.setMultiValued(upstreamPropState.isMultiValued());
        }

        populateChildren(nodeId, state, upstream);
        return state;
    }

    protected void populateChildren(NodeId nodeId, NodeState state, NodeState upstream) {
        for(Iterator iter = upstream.getChildNodeEntries().iterator(); iter.hasNext(); ) {
            NodeState.ChildNodeEntry entry = (NodeState.ChildNodeEntry) iter.next();
            MirrorNodeId childNodeId = new MirrorNodeId(nodeId, entry.getId());
            state.addChildNodeEntry(entry.getName(), childNodeId);
        }
    }
}
