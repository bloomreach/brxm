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

import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;

import org.hippoecm.repository.FacetedNavigationEngine;
import org.hippoecm.repository.FacetedNavigationEngine.Context;

public interface DataProviderContext {
    public HippoVirtualProvider lookupProvider(String moduleName);

    public void registerProvider(Name moduleName, HippoVirtualProvider provider);

    public void registerProviderProperty(Name propName);

    public NodeTypeRegistry getNodeTypeRegistry();

    public NamespaceResolver getNamespaceResolver();

    public HierarchyManager getHierarchyManager();

    public FacetedNavigationEngine<FacetedNavigationEngine.Query, Context> getFacetedEngine();

    public FacetedNavigationEngine.Context getFacetedContext();

    public ItemState getItemState(ItemId id) throws NoSuchItemStateException, ItemStateException;

    public NodeState getNodeState(NodeId id) throws NoSuchItemStateException, ItemStateException;

    public NodeState createNew(NodeId nodeId, Name nodeTypeName, NodeId parentId);

    public PropertyState createNew(Name propName, NodeId parentId);
}
