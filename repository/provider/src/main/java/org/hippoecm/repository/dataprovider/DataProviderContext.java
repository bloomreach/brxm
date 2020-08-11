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

import java.util.UUID;
import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;

import org.hippoecm.repository.FacetedNavigationEngine;
import org.hippoecm.repository.FacetedNavigationEngine.Context;

public interface DataProviderContext {

    public HippoVirtualProvider lookupProvider(String moduleName);

    public HippoVirtualProvider lookupProvider(Name nodeTypeName);

    public UUID generateUuid(StateProviderContext context, NodeId canonical);

    public void registerProvider(Name nodeTypeName, HippoVirtualProvider provider);

    public void registerProviderProperty(Name propName);

    public NodeTypeRegistry getNodeTypeRegistry();;

    public HierarchyManager getHierarchyManager();

    public FacetedNavigationEngine<FacetedNavigationEngine.Query, Context> getFacetedEngine();

    public FacetedNavigationEngine.Context getFacetedContext();

    public ItemState getCanonicalItemState(ItemId id) throws NoSuchItemStateException, ItemStateException;

    public ItemState getItemState(ItemId id) throws NoSuchItemStateException, ItemStateException;

    public NodeState getNodeState(NodeId id) throws NoSuchItemStateException, ItemStateException;

    public NodeState createNew(NodeId nodeId, Name nodeTypeName, NodeId parentId) throws RepositoryException;

    public PropertyState createNew(Name propName, NodeId parentId) throws RepositoryException;
    
    public Name getQName(String name) throws IllegalNameException, NamespaceException;
    
    public Path getQPath(String path) throws MalformedPathException, IllegalNameException, NamespaceException;
}
