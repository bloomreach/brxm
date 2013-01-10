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

import javax.jcr.RepositoryException;
import org.apache.jackrabbit.core.id.NodeId;

import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.spi.Name;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.dataprovider.HippoNodeId;
import org.hippoecm.repository.dataprovider.StateProviderContext;

public class FacetSelectProvider extends ViewVirtualProvider
{

    ViewVirtualProvider subNodesProvider;
    
    Name facetsName;
    Name valuesName;
    Name modesName;
    
    protected void initialize() throws RepositoryException {
        this.subNodesProvider = (ViewVirtualProvider) lookup(ViewVirtualProvider.class.getName());
        facetSelectName = resolveName(HippoNodeType.NT_FACETSELECT);
        register(resolveName(HippoNodeType.NT_FACETSELECT), null);
        docbaseName = resolveName(HippoNodeType.HIPPO_DOCBASE);
        facetsName = resolveName(HippoNodeType.HIPPO_FACETS);
        valuesName = resolveName(HippoNodeType.HIPPO_VALUES);
        modesName = resolveName(HippoNodeType.HIPPO_MODES);
        handleName = resolveName(HippoNodeType.NT_HANDLE);
        requestName = resolveName(HippoNodeType.NT_REQUEST);
        translationName = resolveName(HippoNodeType.NT_TRANSLATION);
    }

    @Override
    public NodeState populate(StateProviderContext context, NodeState state) throws RepositoryException {

        String[] docbase = getProperty(state.getNodeId(), docbaseName);
        String[] newFacets = getProperty(state.getNodeId(), facetsName);
        String[] newValues = getProperty(state.getNodeId(), valuesName);
        String[] newModes  = getProperty(state.getNodeId(), modesName);

        return super.populate(context, subNodesProvider, state, docbase, newFacets, newValues, newModes, true);
        
    }

    @Override
    public NodeState populate(StateProviderContext context, HippoNodeId nodeId, NodeId parentId) throws RepositoryException {
        throw new RepositoryException("Illegal internal state");
    }
}
