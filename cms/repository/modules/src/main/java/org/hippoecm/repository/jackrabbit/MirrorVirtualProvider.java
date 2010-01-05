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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.nodetype.PropDef;
import org.apache.jackrabbit.core.nodetype.PropDefId;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.hippoecm.repository.api.HippoNodeType;

public abstract class MirrorVirtualProvider extends HippoVirtualProvider
{
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    protected abstract class MirrorNodeId extends HippoNodeId {

        private static final long serialVersionUID = 1L;

        NodeId upstream;

        protected MirrorNodeId(HippoVirtualProvider provider, NodeId parent, Name name, NodeId upstream) {
            super(provider, parent, name);
            this.upstream = upstream;
        }

        MirrorNodeId(NodeId parent, NodeId upstream, Name name) {
            super(MirrorVirtualProvider.this, parent, name);
            this.upstream = upstream;
        }
    }

    Name docbaseName;
    Name jcrUUIDName;
    Name jcrMixinTypesName;
    Name hippoUUIDName;
    Name hardDocumentName;
    Name hardHandleName;
    Name softDocumentName;
    Name mixinReferenceableName;
    Name mixinVersionableName;

    PropDef hippoUUIDPropDef;

    @Override
    protected void initialize() throws RepositoryException {
        docbaseName = resolveName(HippoNodeType.HIPPO_DOCBASE);
        jcrUUIDName = resolveName("jcr:uuid");
        jcrMixinTypesName = resolveName("jcr:mixinTypes");
        hippoUUIDName = resolveName(HippoNodeType.HIPPO_UUID);
        hardDocumentName = resolveName(HippoNodeType.NT_HARDDOCUMENT);
        hardHandleName = resolveName(HippoNodeType.NT_HARDHANDLE);
        softDocumentName = resolveName(HippoNodeType.NT_SOFTDOCUMENT);
        mixinReferenceableName = resolveName("mix:referenceable");
        mixinVersionableName = resolveName("mix:versionable");

        hippoUUIDPropDef = lookupPropDef(softDocumentName, hippoUUIDName);
    }

    @Override
    public NodeState populate(HippoNodeId nodeId, NodeId parentId) throws RepositoryException {
        NodeState dereference = getNodeState(((MirrorNodeId)nodeId).upstream);
        if(dereference == null) {
            throw new RepositoryException("Cannot populate top mirror node");
        }
        NodeState state = createNew(nodeId, dereference.getNodeTypeName(), parentId);
        state.setNodeTypeName(dereference.getNodeTypeName());

        Set<Name> mixins = new HashSet<Name>(((NodeState) dereference).getMixinTypeNames());
        if(mixins.contains(mixinReferenceableName)) {
            mixins.remove(mixinReferenceableName);
        }
        if(mixins.contains(hardHandleName)) {
            mixins.remove(hardHandleName);
        }
        if(mixins.contains(mixinVersionableName)) {
            mixins.remove(mixinVersionableName);
        }
        if(mixins.contains(hardDocumentName)) {
            mixins.remove(hardDocumentName);
            mixins.add(softDocumentName);
        }
        state.setMixinTypeNames(mixins);

        state.setDefinitionId(dereference.getDefinitionId());
        for(Iterator iter = dereference.getPropertyNames().iterator(); iter.hasNext(); ) {
            Name propName = (Name) iter.next();
            PropertyId upstreamPropId = new PropertyId(dereference.getNodeId(), propName);
            PropertyState upstreamPropState = getPropertyState(upstreamPropId);
            PropDefId propDefId = upstreamPropState.getDefinitionId();
            if(propName.equals(jcrUUIDName)) {
                propName = hippoUUIDName;
                propDefId = hippoUUIDPropDef.getId();
            }
            state.addPropertyName(propName);
            PropertyState propState = createNew(propName, state.getNodeId());
            propState.setType(upstreamPropState.getType());
            propState.setDefinitionId(propDefId);
            if(propName.equals(jcrMixinTypesName)) {
                // replace the jcr:mixinTypes properties with the possibly changed mixin types
                propState.setValues(InternalValue.create((Name[])state.getMixinTypeNames().toArray(new Name[state.getMixinTypeNames().size()])));
            } else {
                propState.setValues(upstreamPropState.getValues());
            }
            propState.setMultiValued(upstreamPropState.isMultiValued());
        }

        populateChildren(nodeId, state, dereference);
        return state;
    }

    protected abstract void populateChildren(NodeId nodeId, NodeState state, NodeState upstream);
    
}
