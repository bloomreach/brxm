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

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.conversion.IllegalNameException;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.nodetype.PropDef;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;

import org.hippoecm.repository.FacetedNavigationEngine;
import org.hippoecm.repository.FacetedNavigationEngine.HitsRequested;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.jackrabbit.AbstractFacetSearchProvider.FacetSearchNodeId;

public class FacetResultSetProvider extends HippoVirtualProvider
{
    final static private String SVN_ID = "$Id$";
    protected final Logger log = LoggerFactory.getLogger(HippoLocalItemStateManager.class);

    private static Pattern facetPropertyPattern;
    static {
        facetPropertyPattern = Pattern.compile("^@([^=]+)='(.+)'$");
    }

    protected class FacetResultSetNodeId extends HippoNodeId {
        String queryname;
        String docbase;
        String[] search;
        long count;
        FacetResultSetNodeId(NodeId parent, Name name) {
            super(FacetResultSetProvider.this, parent, name);
        }
        FacetResultSetNodeId(NodeId parent, Name name, String queryname, String docbase, String[] search, long count) {
            super(FacetResultSetProvider.this, parent, name);
            this.queryname = queryname;
            this.docbase = docbase;
            this.search = search;
            this.count = count;
        }
    }

    MirrorVirtualProvider subNodesProvider;
    FacetedNavigationEngine facetedEngine;
    FacetedNavigationEngine.Context facetedContext;

    Name countName;

    PropDef countPropDef;

    FacetResultSetProvider(HippoLocalItemStateManager stateMgr, MirrorVirtualProvider subNodesProvider,
                           FacetedNavigationEngine facetedEngine, FacetedNavigationEngine.Context facetedContext)
        throws IllegalNameException, NamespaceException, RepositoryException {
        super(stateMgr, null, stateMgr.resolver.getQName(HippoNodeType.NT_FACETRESULT));

        this.facetedEngine = facetedEngine;
        this.facetedContext = facetedContext;
        this.subNodesProvider = subNodesProvider;

        countName = stateMgr.resolver.getQName(HippoNodeType.HIPPO_COUNT);
        countPropDef = lookupPropDef(stateMgr.resolver.getQName(HippoNodeType.NT_FACETRESULT), countName);
    }

    public NodeState populate(NodeState state) {
        FacetResultSetNodeId nodeId = (FacetResultSetNodeId) state.getNodeId();
        String queryname = nodeId.queryname;
        String docbase = nodeId.docbase;
        String[] search = nodeId.search;
        long count = nodeId.count;

        Map<String,String> currentFacetQuery = new TreeMap<String,String>();
        for(int i=0; search != null && i < search.length; i++) {
            Matcher matcher = facetPropertyPattern.matcher(search[i]);
            if(matcher.matches() && matcher.groupCount() == 2) {
                currentFacetQuery.put(matcher.group(1), matcher.group(2));
            }
        }
        FacetedNavigationEngine.Query initialQuery;
        initialQuery = facetedEngine.parse(docbase);
      
        HitsRequested hitsRequested = new HitsRequested();
        hitsRequested.setResultRequested(true);
        hitsRequested.setLimit(1000000);
        hitsRequested.setOffset(0);

        FacetedNavigationEngine.Result facetedResult;
        long t1 = 0, t2;
        if(log.isDebugEnabled())
            t1 = System.currentTimeMillis();
        facetedResult = facetedEngine.view(queryname, initialQuery, facetedContext, currentFacetQuery, null,
                                           hitsRequested);
        if(log.isDebugEnabled()) {
            t2 = System.currentTimeMillis();
            log.debug("facetsearch turnaround="+(t2-t1));
        }
        count = facetedResult.length();

        PropertyState propState = createNew(countName, state.getNodeId());
        propState.setType(PropertyType.LONG);
        propState.setDefinitionId(countPropDef.getId());
        propState.setValues(new InternalValue[] { InternalValue.create(count) });
        propState.setMultiValued(false);
        state.addPropertyName(countName);

        for(String foundNodePath : facetedResult) {
            try {
                NodeId upstream = getNodeId(foundNodePath);
                if(upstream == null)
                    continue;
                /* The next statement is painfull performance wise.
                 * Only to obtain the child node name, we have to retrieve the parent state.
                 */
                Name name = getNodeState(getNodeState(upstream).getParentId()).getChildNodeEntry(upstream).getName();
                state.addChildNodeEntry(name, subNodesProvider . new MirrorNodeId(state.getNodeId(), upstream, name));
            } catch(RepositoryException ex) {
                System.err.println(ex.getMessage());
                ex.printStackTrace(System.err);
            }
        }

        return state;
    }
}
