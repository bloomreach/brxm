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

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.dataprovider.StateProviderContext;

/**
 * deprecated since 2.26.00
 */
@Deprecated
public class FacetSubSearchProvider extends AbstractFacetSearchProvider
{

    QPropertyDefinition primaryTypePropDef;

    public FacetSubSearchProvider()
        throws RepositoryException
    {
    }

    @Override
    protected void initialize() throws RepositoryException {
        super.initialize();
        subSearchProvider = this;
        subNodesProvider  = (FacetResultSetProvider) lookup(FacetResultSetProvider.class.getName());
        primaryTypePropDef = lookupPropDef(resolveName(HippoNodeType.NT_FACETBASESEARCH), countName);
        virtualNodeName = resolveName(HippoNodeType.NT_FACETSUBSEARCH);
        register(null, virtualNodeName);
    }

    @Override
    public NodeState populate(StateProviderContext context, NodeState state) throws RepositoryException {
        super.populate(context, state);

        PropertyState propState = createNew(NameConstants.JCR_PRIMARYTYPE, state.getNodeId());
        propState.setType(PropertyType.NAME);
        propState.setValues(new InternalValue[] { InternalValue.create(resolveName(HippoNodeType.NT_FACETSUBSEARCH)) });
        propState.setMultiValued(false);
        state.addPropertyName(NameConstants.JCR_PRIMARYTYPE);

        return state;
    }
}
