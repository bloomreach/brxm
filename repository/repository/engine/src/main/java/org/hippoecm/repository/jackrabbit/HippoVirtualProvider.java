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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory; 

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
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
import org.apache.jackrabbit.spi.commons.conversion.NameParser;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.spi.commons.conversion.ParsingNameResolver;
import org.apache.jackrabbit.spi.commons.conversion.PathParser;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;

public abstract class HippoVirtualProvider
{
    private HippoLocalItemStateManager stateMgr;
    private NameFactory nameFactory;
    private PathFactory pathFactory;
    private NameResolver nameResolver;

    private Name externalNodeName;
    private Name virtualNodeName;

    protected final Logger log = LoggerFactory.getLogger(HippoVirtualProvider.class); 

    protected final PropDef lookupPropDef(Name nodeTypeName, Name propName) throws RepositoryException {
        PropDef[] propDefs = stateMgr.ntReg.getNodeTypeDef(nodeTypeName).getPropertyDefs();
        int i;
        for(i=0; i<propDefs.length; i++)
            if(propDefs[i].getName().equals(propName)) {
               return propDefs[i];
            }
        throw new RepositoryException("required property "+propName+" in nodetype "+nodeTypeName+" not or badly defined");
    }

    protected final NodeDef lookupNodeDef(NodeState parent, Name nodeTypeName, Name nodeName) throws RepositoryException {
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

    protected HippoVirtualProvider() {
    }

    void initialize(HippoLocalItemStateManager stateMgr) throws RepositoryException {
        this.stateMgr = stateMgr;
        nameFactory = NameFactoryImpl.getInstance();
        pathFactory = PathFactoryImpl.getInstance();
        nameResolver = new ParsingNameResolver(nameFactory, stateMgr.nsResolver);
        initialize();
    }

    protected abstract void initialize() throws RepositoryException;

    protected void register(Name external, Name virtual) {
        externalNodeName = external;
        virtualNodeName = virtual;
        if(external != null)
            stateMgr.registerProvider(externalNodeName, this);
    }

    protected HippoVirtualProvider lookup(String providerName) {
        return stateMgr.lookupProvider(providerName);
    }

    protected final Name resolveName(String name) throws IllegalNameException, NamespaceException {
        return name != null ? NameParser.parse(name, stateMgr.nsResolver, nameFactory) : null;
    }

    protected final Path resolvePath(String path) throws IllegalNameException, NamespaceException, MalformedPathException {
        return path != null ? PathParser.parse((String)path, nameResolver, (PathFactory)pathFactory) : null;
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

    protected final NodeState createNew(NodeId nodeId, Name nodeTypeName, NodeId parentId) {
        return stateMgr.createNew(nodeId, nodeTypeName, parentId);
    }

    protected final PropertyState createNew(Name propName, NodeId parentId) {
        return stateMgr.createNew(propName, parentId);
    }

    protected final String[] getProperty(NodeId nodeId, Name propName) {
        PropertyState propState = getPropertyState(new PropertyId(nodeId, propName));
        if(propState == null) {
            log.warn("expected property state " + propName + " in " + nodeId + " not found");
            return null;
        }
        InternalValue[] values = propState.getValues();
        String[] strings = new String[values.length];
        for(int i=0; i<values.length; i++)
            strings[i] = values[i].getString();
        return strings;
    }

    protected final PropertyState getPropertyState(PropertyId propId) {
        try {
            return (PropertyState) stateMgr.getItemState(propId);
        } catch(NoSuchItemStateException ex) {
            log.warn("expected property state " + propId + " not found: " +
                     ex.getClass().getName() + ": " + ex.getMessage());
            return null;
        } catch(ItemStateException ex) {
            log.warn("expected property state " + propId + " not found: " +
                     ex.getClass().getName()+": "+ex.getMessage());
            return null;
        }
    }

    protected final NodeState getNodeState(NodeId nodeId) {
        try{
            return (NodeState) stateMgr.getItemState(nodeId);
        } catch(NoSuchItemStateException ex) {
            log.warn("expected node state "+nodeId+" not found: " +
                     ex.getClass().getName() + ": " + ex.getMessage());
            return null;
        } catch(ItemStateException ex) {
            log.warn("expected node state " + nodeId + " not found: " +
                     ex.getClass().getName() + ": " + ex.getMessage());
            return null;
        }
    }

    protected final NodeState getNodeState(String absPath) throws RepositoryException {
        try {
            ItemId itemId = stateMgr.hierMgr.resolveNodePath(resolvePath(absPath));
            if(itemId == null || !itemId.denotesNode())
                return null;
            return stateMgr.getNodeState((NodeId)itemId);
        } catch(NoSuchItemStateException ex) {
            log.warn("expected node " + absPath + " not found: " +
                     ex.getClass().getName() + ": " + ex.getMessage());
            return null;
        } catch(ItemStateException ex) {
            log.warn("expected node " + absPath + " not found: " +
                     ex.getClass().getName() + ": " + ex.getMessage());
            throw new RepositoryException(ex.getMessage(), ex);
        }
    }
}
