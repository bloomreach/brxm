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

import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.nodetype.PropDef;
import org.apache.jackrabbit.core.nodetype.NodeDef;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;

import org.hippoecm.repository.FacetedNavigationEngine.HitsRequested;
import org.hippoecm.repository.FacetedNavigationEngine;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.ISO9075Helper;

import org.apache.jackrabbit.core.nodetype.NodeDefImpl;

public class FacetSearchProvider extends AbstractFacetSearchProvider
{
    final static private String SVN_ID = "$Id$";

    FacetSearchProvider()
        throws RepositoryException
    {
        super();
    }

    @Override
    protected void initialize() throws RepositoryException {
        super.initialize();
        subSearchProvider = (FacetSubSearchProvider) lookup("org.hippoecm.repository.jackrabbit.FacetSubSearchProvider");
        subNodesProvider  = (FacetResultSetProvider) lookup("org.hippoecm.repository.jackrabbit.FacetResultSetProvider");
        virtualNodeName = resolveName(HippoNodeType.NT_FACETSUBSEARCH);
        register(resolveName(HippoNodeType.NT_FACETSEARCH), virtualNodeName);
    }

    @Override
    public NodeState populate(HippoNodeId nodeId, NodeId parentId) throws RepositoryException {
        throw new RepositoryException("Cannot populate top facetsearch node");
    }
}
