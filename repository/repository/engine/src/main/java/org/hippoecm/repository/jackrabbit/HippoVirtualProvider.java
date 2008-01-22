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

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;

import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.nodetype.NodeDef;
import org.apache.jackrabbit.core.nodetype.NodeTypeConflictException;
import org.apache.jackrabbit.core.nodetype.PropDef;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.conversion.NameParser;
import org.apache.jackrabbit.spi.commons.conversion.PathParser;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;

//import org.apache.jackrabbit.name.NameResolver;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;

public abstract class HippoVirtualProvider
{
    private HippoLocalItemStateManager stateMgr;
    private NameFactory nameResolver;
    private PathFactory pathResolver;

    protected Name externalNodeName;
    protected Name virtualNodeName;

    PropDef lookupPropDef(Name nodeTypeName, Name propName) throws RepositoryException {
        PropDef[] propDefs = stateMgr.ntReg.getNodeTypeDef(nodeTypeName).getPropertyDefs();
        int i;
        for(i=0; i<propDefs.length; i++)
            if(propDefs[i].getName().equals(propName)) {
               return propDefs[i];
            }
        throw new RepositoryException("required property "+propName+" in nodetype "+nodeTypeName+" not or badly defined");
    }

    NodeDef lookupNodeDef(NodeState parent, Name nodeTypeName, Name nodeName) throws RepositoryException {
        EffectiveNodeType effNodeType;
        try {
            HashSet set = new HashSet(parent.getMixinTypeNames());
            set.add(parent.getNodeTypeName());
            effNodeType = stateMgr.ntReg.getEffectiveNodeType((Name[]) set.toArray(new Name[set.size()]));
            try {
                return effNodeType.getApplicableChildNodeDef(nodeName, nodeTypeName, stateMgr.ntReg);
            } catch (RepositoryException re) {
                // FIXME? hack, use nt:unstructured as parent
                effNodeType = stateMgr.ntReg.getEffectiveNodeType(NameConstants.NT_UNSTRUCTURED);
                return effNodeType.getApplicableChildNodeDef(nodeName, nodeTypeName, stateMgr.ntReg);
            }
        } catch (NoSuchNodeTypeException ex) {
            throw new RepositoryException("internal error: failed to build effective node type for node " + parent.getNodeId(),
                                          ex);
        } catch (NodeTypeConflictException ex) {
            throw new RepositoryException("internal error: failed to build effective node type for node " + parent.getNodeId(),
                                          ex);
        }
    }

    private HippoVirtualProvider() {
    }

    HippoVirtualProvider(HippoLocalItemStateManager stateMgr) {
        this(stateMgr, (Name)null, (Name)null);
        nameResolver = NameFactoryImpl.getInstance();
        pathResolver = PathFactoryImpl.getInstance();
    }

    HippoVirtualProvider(HippoLocalItemStateManager stateMgr, Name external, Name virtual) {
        this.stateMgr = stateMgr;
        nameResolver = NameFactoryImpl.getInstance();
        pathResolver = PathFactoryImpl.getInstance();
        externalNodeName = external;
        virtualNodeName = virtual;
        if(external != null)
            stateMgr.register(externalNodeName, this);
    }

    HippoVirtualProvider(HippoLocalItemStateManager stateMgr, String external, String virtual) throws IllegalNameException, NamespaceException {
        this.stateMgr = stateMgr;
        nameResolver = NameFactoryImpl.getInstance();
        pathResolver = PathFactoryImpl.getInstance();
        externalNodeName = resolveName(external);
        virtualNodeName = resolveName(virtual);
        if(external != null)
            stateMgr.register(externalNodeName, this);
    }

    public Name resolveName(String name) throws IllegalNameException, NamespaceException {
        return name != null ? NameParser.parse(name, stateMgr.nsResolver, nameResolver) : null;
    }

    public Path resolvePath(String path) throws IllegalNameException, NamespaceException, MalformedPathException {
        NameResolver nr = new org.apache.jackrabbit.spi.commons.conversion.ParsingNameResolver(nameResolver, stateMgr.nsResolver);
        return path != null ? PathParser.parse((String)path, nr, (PathFactory)pathResolver)
            : null;
    }

    public NodeState populate(NodeState state) throws RepositoryException {
        return state;
    }

    public NodeState populate(HippoNodeId nodeId, NodeId parentId) throws RepositoryException {
        try {
            NodeState state = createNew(nodeId, virtualNodeName, parentId);
            NodeState parentState = stateMgr.getNodeState(parentId);
            state.setDefinitionId(lookupNodeDef(parentState, virtualNodeName, nodeId.name).getId());
            populate(state);
            return state;
        } catch(NoSuchItemStateException ex) {
            throw new RepositoryException("impossible state");
        } catch(ItemStateException ex) {
            throw new RepositoryException("item state exception", ex);
        }
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
            return getProperty(nodeId, resolveName(name));
        } catch(IllegalNameException ex) {
            return null;
        } catch(NamespaceException ex) {
            return null;
        }
    }

    public String[] getProperty(NodeId nodeId, Name propName) {
        PropertyState propState = getPropertyState(new PropertyId(nodeId, propName));
        if(propState == null)
            return null;
        InternalValue[] values = propState.getValues();
        String[] strings = new String[values.length];
        for(int i=0; i<values.length; i++)
            strings[i] = values[i].getString();
        return strings;
    }

    public NodeState getNodeState(String absPath) throws RepositoryException {
        try {
            NodeId nodeId = getNodeId(absPath);
            if(nodeId != null)
                return stateMgr.getNodeState(nodeId);
            else
                return null;
        } catch(NoSuchItemStateException ex) {
            return null;
        } catch(ItemStateException ex) {
            throw new RepositoryException(ex.getMessage(), ex);
        }
    }

    public NodeId getNodeId(String absPath) throws RepositoryException {
        ItemId itemId = stateMgr.hierMgr.resolveNodePath(resolvePath(absPath));
        if(itemId != null && itemId.denotesNode())
            return (NodeId) itemId;
        else
            return null;
    }
}
