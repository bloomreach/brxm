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
package org.hippoecm.repository.dataprovider;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import javax.jcr.InvalidItemStateException;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.nodetype.NodeTypeConflictException;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class HippoVirtualProvider implements DataProviderModule
{

    private DataProviderContext stateMgr;

    private Name externalNodeName;
    private Name virtualNodeName;
    private Map<String, Name> nameCache = new HashMap<String, Name>();

    private final Logger log = LoggerFactory.getLogger(HippoVirtualProvider.class);

    protected final QPropertyDefinition lookupPropDef(Name nodeTypeName, Name propName) throws RepositoryException {
        QPropertyDefinition[] propDefs = stateMgr.getNodeTypeRegistry().getNodeTypeDef(nodeTypeName).getPropertyDefs();
        int i;
        for(i=0; i<propDefs.length; i++)
            if(propDefs[i].getName().equals(propName)) {
               return propDefs[i];
            }
        throw new RepositoryException("required property "+propName+" in nodetype "+nodeTypeName+" not or badly defined");
    }

    protected final QNodeDefinition lookupNodeDef(NodeState parent, org.apache.jackrabbit.spi.Name nodeTypeName, org.apache.jackrabbit.spi.Name nodeName) throws RepositoryException {
        org.apache.jackrabbit.core.nodetype.EffectiveNodeType effNodeType;
        try {
            HashSet set = new HashSet(parent.getMixinTypeNames());
            effNodeType = stateMgr.getNodeTypeRegistry().getEffectiveNodeType(parent.getNodeTypeName(), set);
            try {
                return effNodeType.getApplicableChildNodeDef(nodeName, nodeTypeName, stateMgr.getNodeTypeRegistry());
            } catch (RepositoryException re) {
                // FIXME? hack, use nt:unstructured as parent
                effNodeType = stateMgr.getNodeTypeRegistry().getEffectiveNodeType(NameConstants.NT_UNSTRUCTURED);
                return effNodeType.getApplicableChildNodeDef(nodeName, nodeTypeName, stateMgr.getNodeTypeRegistry());
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

    public void initialize(DataProviderContext stateMgr) throws RepositoryException {
        this.stateMgr = stateMgr;
        initialize();
    }

    protected abstract void initialize() throws RepositoryException;

    protected void register(Name external, Name virtual) {
        externalNodeName = external;
        virtualNodeName = virtual;
        if(external != null)
            stateMgr.registerProvider(externalNodeName, this);
    }

    protected final DataProviderContext getDataProviderContext() {
        return stateMgr;
    }
    
    protected HippoVirtualProvider lookup(String providerName) {
        return stateMgr.lookupProvider(providerName);
    }
    
    public HippoVirtualProvider lookupProvider(Name nodeTypeName) {
        return stateMgr.lookupProvider(nodeTypeName);
    }

    public final Name resolveName(String name) throws IllegalNameException, NamespaceException {
        if (!nameCache.containsKey(name)) {
            nameCache.put(name, stateMgr.getQName(name));
        }
        return nameCache.get(name);
    }

    public final Path resolvePath(String path) throws IllegalNameException, NamespaceException, MalformedPathException {
        return stateMgr.getQPath(path);
    }

    public NodeState populate(StateProviderContext context, NodeState state) throws RepositoryException {
        return state;
    }

    public NodeState populate(StateProviderContext context, HippoNodeId nodeId, NodeId parentId) throws RepositoryException {
        try {
            NodeState state = createNew(nodeId, virtualNodeName, parentId);
            NodeState parentState = stateMgr.getNodeState(parentId);
            populate(context, state);
            return state;
        } catch(NoSuchItemStateException ex) {
            throw new RepositoryException("impossible state");
        } catch(ItemStateException ex) {
            throw new RepositoryException("item state exception", ex);
        }
    }

    protected final NodeState createNew(NodeId nodeId, Name nodeTypeName, NodeId parentId) throws RepositoryException {
        return stateMgr.createNew(nodeId, nodeTypeName, parentId);
    }

    protected final PropertyState createNew(Name propName, NodeId parentId) throws RepositoryException {
        return stateMgr.createNew(propName, parentId);
    }

    protected final String[] getProperty(NodeId nodeId, Name propName) throws InvalidItemStateException, RepositoryException {
        PropertyState propState = getPropertyState(new PropertyId(nodeId, propName));
        if(propState == null) {
            if(log.isDebugEnabled()) {
                log.debug("possible expected property state " + propName + " in " + nodeId + " not found");
            }
            return null;
        }
        InternalValue[] values = propState.getValues();
        String[] strings = new String[values.length];
        for(int i=0; i<values.length; i++) {
            strings[i] = values[i].getString();
        }
        return strings;
    }

    protected final String[] getProperty(NodeId nodeId, Name propName, String[] defaultValue) throws InvalidItemStateException, RepositoryException {
        PropertyId propId = new PropertyId(nodeId, propName);
        try {
            PropertyState propState = (PropertyState) stateMgr.getItemState(propId);
            InternalValue[] values = propState.getValues();
            String[] strings = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                strings[i] = values[i].getString();
            }
            return strings;
        } catch (NoSuchItemStateException ex) {
            if(log.isDebugEnabled()) {
                log.debug("possible expected property state " + propId + " not found: " +
                          ex.getClass().getName() + ": " + ex.getMessage());
            }
            return defaultValue;
        } catch(ItemStateException ex) {
            if(log.isDebugEnabled()) {
                log.debug("possible expected property state " + propId + " not found: " +
                          ex.getClass().getName()+": "+ex.getMessage());
            }
            throw new InvalidItemStateException(ex);
        }
    }


    protected final PropertyState getPropertyState(PropertyId propId) throws InvalidItemStateException {
        try {
            return (PropertyState) stateMgr.getItemState(propId);
        } catch(NoSuchItemStateException ex) {
            if(log.isDebugEnabled()) {
                log.debug("possible expected property state " + propId + " not found: " +
                          ex.getClass().getName() + ": " + ex.getMessage());
            }
            throw new InvalidItemStateException(ex);
        } catch(ItemStateException ex) {
            if(log.isDebugEnabled()) {
                log.debug("possible expected property state " + propId + " not found: " +
                          ex.getClass().getName()+": "+ex.getMessage());
            }
            throw new InvalidItemStateException(ex);
        }
    }

    protected final NodeState getCanonicalNodeState(NodeId nodeId) {
        try {
            return (NodeState)stateMgr.getCanonicalItemState(nodeId);
        } catch (NoSuchItemStateException ex) {
            return null;
        } catch (ItemStateException ex) {
            return null;
        }
    }

    protected final NodeState getNodeState(NodeId nodeId, StateProviderContext context) {
        try {
            if(!(nodeId instanceof ParameterizedNodeId) && (context != null && context.getParameterString() != null)) {
                // we need to parameterize the node id still. This happens when for example parameterized faceted navigation is below the
                // virtual hierarchy of a facetselect or mirror.
                nodeId = new ParameterizedNodeId(nodeId, context.getParameterString());
                return (NodeState) stateMgr.getItemState(nodeId);
            } else {
                return (NodeState) stateMgr.getItemState(nodeId);
            }
        } catch(NoSuchItemStateException ex) {
            if(log.isDebugEnabled()) {
                log.debug("possible expected node state "+nodeId+" not found: " +
                          ex.getClass().getName() + ": " + ex.getMessage());
            }
            return null;
        } catch(ItemStateException ex) {
            if(log.isDebugEnabled()) {
                log.debug("possible expected node state " + nodeId + " not found: " +
                          ex.getClass().getName() + ": " + ex.getMessage());
            }
            return null;
        }
    }
}
