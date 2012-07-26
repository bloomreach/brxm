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
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;

import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.dataprovider.HippoNodeId;
import org.hippoecm.repository.dataprovider.HippoVirtualProvider;
import org.hippoecm.repository.dataprovider.MirrorNodeId;
import org.hippoecm.repository.dataprovider.StateProviderContext;

public abstract class MirrorVirtualProvider extends HippoVirtualProvider
{

    Name docbaseName;
    Name jcrUUIDName;
    Set<Name> omittedProperties;
    Name jcrMixinTypesName;
    Name hippoUUIDName;
    Name hardDocumentName;
    Name hardHandleName;
    Name softDocumentName;
    Name softHandleName;
    Name mixinReferenceableName;
    Name mixinVersionableName;

    //QPropertyDefinition documentHippoUUIDPropDef;
    //QPropertyDefinition handleHippoUUIDPropDef;

    @Override
    protected void initialize() throws RepositoryException {
        docbaseName = resolveName(HippoNodeType.HIPPO_DOCBASE);
        jcrUUIDName = resolveName("jcr:uuid");
        jcrMixinTypesName = resolveName("jcr:mixinTypes");
        hippoUUIDName = resolveName(HippoNodeType.HIPPO_UUID);
        hardDocumentName = resolveName(HippoNodeType.NT_HARDDOCUMENT);
        hardHandleName = resolveName(HippoNodeType.NT_HARDHANDLE);
        softDocumentName = resolveName(HippoNodeType.NT_SOFTDOCUMENT);
        softHandleName = resolveName("hipposys:softhandle");
        mixinReferenceableName = resolveName("mix:referenceable");
        mixinVersionableName = resolveName("mix:versionable");

        omittedProperties = new HashSet<Name>();
        omittedProperties.add(resolveName("hippo:related"));
        omittedProperties.add(resolveName("jcr:isCheckedOut"));
        omittedProperties.add(resolveName("jcr:versionHistory"));
        omittedProperties.add(resolveName("jcr:baseVersion"));
        omittedProperties.add(resolveName("jcr:predecessors"));
        omittedProperties.add(resolveName("jcr:mergeFailed"));
        omittedProperties.add(resolveName("jcr:activity"));
        omittedProperties.add(resolveName("jcr:configuration"));

        //documentHippoUUIDPropDef = lookupPropDef(softDocumentName, hippoUUIDName);
        //handleHippoUUIDPropDef = lookupPropDef(softHandleName, hippoUUIDName);
    }

    @Override
    public NodeState populate(StateProviderContext context, HippoNodeId nodeId, NodeId parentId) throws RepositoryException {
        NodeState dereference = getNodeState(((MirrorNodeId)nodeId).getCanonicalId(), context);
        if(dereference == null) {
            throw new RepositoryException("Cannot populate top mirror node dereferencing "+((MirrorNodeId)nodeId).getCanonicalId());
        }
        NodeState state = createNew(nodeId, dereference.getNodeTypeName(), parentId);
        state.setNodeTypeName(dereference.getNodeTypeName());

        Set<Name> mixins = new HashSet<Name>(((NodeState) dereference).getMixinTypeNames());
        if(mixins.contains(mixinReferenceableName)) {
            mixins.remove(mixinReferenceableName);
        }
        if(mixins.contains(hardHandleName)) {
            mixins.remove(hardHandleName);
            mixins.add(softHandleName);
        }
        if(mixins.contains(mixinVersionableName)) {
            mixins.remove(mixinVersionableName);
        }
        if(mixins.contains(hardDocumentName)) {
            mixins.remove(hardDocumentName);
            mixins.add(softDocumentName);
        }
        state.setMixinTypeNames(mixins);

        for(Iterator iter = dereference.getPropertyNames().iterator(); iter.hasNext(); ) {
            Name propName = (Name) iter.next();
            PropertyId upstreamPropId = new PropertyId(dereference.getNodeId(), propName);
            PropertyState upstreamPropState = getPropertyState(upstreamPropId);
            if(propName.equals(jcrUUIDName)) {
                if(mixins.contains(softDocumentName) || mixins.contains(softHandleName)) {
                    propName = hippoUUIDName;
                } else {
                    continue;
                }
            }
            if(omittedProperties.contains(propName))
                continue;
            state.addPropertyName(propName);
            PropertyState propState = createNew(propName, state.getNodeId());
            propState.setType(upstreamPropState.getType());
            if(propName.equals(jcrMixinTypesName)) {
                // replace the jcr:mixinTypes properties with the possibly changed mixin types
                propState.setValues(InternalValue.create((Name[])state.getMixinTypeNames().toArray(new Name[state.getMixinTypeNames().size()])));
            } else {
                propState.setValues(upstreamPropState.getValues());
            }
            propState.setMultiValued(upstreamPropState.isMultiValued());
        }

        populateChildren(context, nodeId, state, dereference);
        return state;
    }

    protected abstract void populateChildren(StateProviderContext context, NodeId nodeId, NodeState state, NodeState upstream) throws RepositoryException;
    
}
