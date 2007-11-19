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

import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.conversion.IllegalNameException;
import org.apache.jackrabbit.conversion.MalformedPathException;
import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.nodetype.NodeDef;
import org.apache.jackrabbit.core.nodetype.NodeDefImpl;
import org.apache.jackrabbit.core.nodetype.PropDef;
import org.apache.jackrabbit.core.nodetype.PropDefImpl;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;

public abstract class HippoVirtualProvider
{
    protected HippoLocalItemStateManager stateMgr;

    protected Name externalNodeName;
    protected Name virtualNodeName;
    protected NodeDef virtualNodeDef;
    protected PropDef propDef;

    NodeDef defineNodeDef(Name name, Name declaringName) {
        NodeDefImpl nodeDef;
        nodeDef = new NodeDefImpl();
        nodeDef.setDefaultPrimaryType(name);
        nodeDef.setAllowsSameNameSiblings(true);
        nodeDef.setProtected(true);
        nodeDef.setDeclaringNodeType(declaringName);
        nodeDef.setName(name);
        return nodeDef;
    }

    PropDef definePropDef() {
        PropDefImpl propDef = null;
        try {
            propDef = new PropDefImpl();
            propDef.setRequiredType(PropertyType.LONG);
            propDef.setProtected(true);
            propDef.setDeclaringNodeType(stateMgr.resolver.getQName("hippo:external"));
            propDef.setName(stateMgr.resolver.getQName("hippo:property"));
        } catch(NamespaceException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
        } catch(IllegalNameException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
        }
        return propDef;
    }

    private HippoVirtualProvider() {
    }

    HippoVirtualProvider(HippoLocalItemStateManager stateMgr) {
        this.stateMgr = stateMgr;
        externalNodeName = null;
        virtualNodeName = null;
        virtualNodeDef = null;
    }

    HippoVirtualProvider(HippoLocalItemStateManager stateMgr, Name external, Name virtual) {
        this.stateMgr = stateMgr;
        externalNodeName = external;
        virtualNodeName = virtual;
        virtualNodeDef = defineNodeDef(virtualNodeName, externalNodeName);
    }

    public void register(HippoLocalItemStateManager manager) {
        manager.register(externalNodeName, this);
    }

    public NodeState populate(NodeState state) throws RepositoryException {
        return state;
    }

    public NodeState populate(HippoNodeId nodeId, NodeId parentId) throws RepositoryException {
        NodeState state = createNew(nodeId, virtualNodeName, parentId);
        state.setDefinitionId(virtualNodeDef.getId());
        populate(state);
        return state;
    }

    public NodeState createNew(NodeId nodeId, Name nodeTypeName, NodeId parentId) {
        return stateMgr.createNew(nodeId, nodeTypeName, parentId);
    }

    public PropertyState createNew(Name propName, NodeId parentId) {
        return stateMgr.createNew(propName, parentId);
    }

    public PropertyState getPropertyState(PropertyId propId) {
        try {
            return stateMgr.getPropertyState(propId);
        } catch(NoSuchItemStateException ex) {
            return null;
        } catch(ItemStateException ex) {
            return null;
        }
    }

    public NodeState getNodeState(NodeId nodeId) {
        try{
            return stateMgr.getNodeState(nodeId);
        } catch(NoSuchItemStateException ex) {
            return null;
        } catch(ItemStateException ex) {
            return null;
        }
    }

    public String[] getProperty(NodeId nodeId, String name) {
        try {
            Name propName = stateMgr.resolver.getQName(name);
            InternalValue[] values = getPropertyState(new PropertyId(nodeId, propName)).getValues();
            String[] strings = new String[values.length];
            for(int i=0; i<values.length; i++)
                strings[i] = values[i].getString();
            return strings;
        } catch(IllegalNameException ex) {
            return null;
        } catch(NamespaceException ex) {
            return null;
        }
    }

    public NodeState getNodeState(String absPath) throws RepositoryException {
        try {
            return stateMgr.getNodeState(getNodeId(absPath));
        } catch(NoSuchItemStateException ex) {
            return null;
        } catch(ItemStateException ex) {
            throw new RepositoryException(ex.getMessage(), ex);
        } catch(MalformedPathException ex) {
            throw new RepositoryException(ex.getMessage(), ex);
        }
    }

    public NodeId getNodeId(String absPath) throws RepositoryException {
        ItemId itemId = stateMgr.hierMgr.resolvePath(stateMgr.resolver.getQPath(absPath));
        if(itemId.denotesNode())
            return (NodeId) itemId;
        else
            return null;
    }
}
