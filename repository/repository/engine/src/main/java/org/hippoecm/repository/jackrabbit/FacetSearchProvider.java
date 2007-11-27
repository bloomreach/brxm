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
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;

import org.hippoecm.repository.FacetedNavigationEngine.HitsRequested;
import org.hippoecm.repository.FacetedNavigationEngine;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.ISO9075Helper;

public class FacetSearchProvider extends HippoVirtualProvider
{
    final static private String SVN_ID = "$Id$";
    protected final Logger log = LoggerFactory.getLogger(HippoLocalItemStateManager.class);

    class FacetSearchNodeId extends HippoNodeId {
        String queryname;
        String docbase;
        String[] facets;
        String[] search;
        long count;
        protected FacetSearchNodeId(HippoVirtualProvider provider, NodeId parent) {
            super(provider, parent);
        }
        FacetSearchNodeId(NodeId parent) {
            super(FacetSearchProvider.this, parent);
        }
    }

    private static Pattern facetPropertyPattern;
    static {
        facetPropertyPattern = Pattern.compile("^@([^=]+)='(.+)'$");
    }

    FacetResultSetProvider subNodesProvider;
    FacetedNavigationEngine facetedEngine;
    FacetedNavigationEngine.Context facetedContext;

    Name querynameName;
    Name docbaseName;
    Name facetsName;
    Name searchName;
    Name countName;

    PropDef querynamePropDef;
    PropDef docbasePropDef;
    PropDef facetsPropDef;
    PropDef searchPropDef;
    PropDef countPropDef;

    FacetSearchProvider(HippoLocalItemStateManager stateMgr, FacetResultSetProvider subNodesProvider,
                        FacetedNavigationEngine facetedEngine, FacetedNavigationEngine.Context facetedContext)
        throws RepositoryException
    {
        super(stateMgr, stateMgr.resolver.getQName(HippoNodeType.NT_FACETSEARCH), stateMgr.resolver.getQName(HippoNodeType.NT_FACETSEARCH));
        this.facetedEngine = facetedEngine;
        this.facetedContext = facetedContext;
        this.subNodesProvider = subNodesProvider;

        querynameName = stateMgr.resolver.getQName(HippoNodeType.HIPPO_QUERYNAME);
        docbaseName = stateMgr.resolver.getQName(HippoNodeType.HIPPO_DOCBASE);
        facetsName = stateMgr.resolver.getQName(HippoNodeType.HIPPO_FACETS);
        searchName = stateMgr.resolver.getQName(HippoNodeType.HIPPO_SEARCH);
        countName = stateMgr.resolver.getQName(HippoNodeType.HIPPO_COUNT);

        stateMgr.registerProperty(countName);

        querynamePropDef = lookupPropDef(stateMgr.resolver.getQName(HippoNodeType.NT_FACETSEARCH), querynameName);
        docbasePropDef = lookupPropDef(stateMgr.resolver.getQName(HippoNodeType.NT_FACETSEARCH), docbaseName);
        facetsPropDef = lookupPropDef(stateMgr.resolver.getQName(HippoNodeType.NT_FACETSEARCH), facetsName);
        searchPropDef = lookupPropDef(stateMgr.resolver.getQName(HippoNodeType.NT_FACETSEARCH), searchName);
        countPropDef = lookupPropDef(stateMgr.resolver.getQName(HippoNodeType.NT_FACETSEARCH), countName);
    }

    public NodeState populate(NodeState state) throws RepositoryException {
        NodeId nodeId = state.getNodeId();
        String queryname;
        String docbase;
        String[] facets;
        String[] search;
        long count = 0;
        if(nodeId instanceof FacetSearchNodeId) {
            FacetSearchNodeId facetSearchNodeId = (FacetSearchNodeId) nodeId;
            queryname = facetSearchNodeId.queryname;
            docbase = facetSearchNodeId.docbase;
            facets = facetSearchNodeId.facets;
            search = facetSearchNodeId.search;
            count = facetSearchNodeId.count;
        } else {
            queryname = getProperty(nodeId, querynameName)[0];
            docbase = getProperty(nodeId, docbaseName)[0];
            facets = getProperty(nodeId, facetsName);
            search = getProperty(nodeId, searchName);
        }

        if(facets != null && facets.length > 0) {
            Map<String,Map<String,org.hippoecm.repository.FacetedNavigationEngine.Count>> facetSearchResultMap;
            facetSearchResultMap = new TreeMap<String,Map<String,FacetedNavigationEngine.Count>>();
            Map<String,FacetedNavigationEngine.Count> facetSearchResult;
            facetSearchResult = new TreeMap<String,FacetedNavigationEngine.Count>();
            facetSearchResultMap.put(facets[0], facetSearchResult);

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
            hitsRequested.setResultRequested(false);

            FacetedNavigationEngine.Result facetedResult;
            long t1 = 0, t2;
            if(log.isDebugEnabled())
                t1 = System.currentTimeMillis();
            facetedResult = facetedEngine.view(queryname, initialQuery, facetedContext, currentFacetQuery, null,
                                               facetSearchResultMap, null, hitsRequested);
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

            for(Map.Entry<String,FacetedNavigationEngine.Count> facetValue : facetSearchResult.entrySet()) {
                String[] newFacets = new String[Math.max(0, facets.length - 1)];
                if(facets.length > 1)
                    System.arraycopy(facets, 1, newFacets, 0, facets.length - 1);
                String[] newSearch = new String[search != null ? search.length + 1 : 1];
                if(search != null && search.length > 0)
                    System.arraycopy(search, 0, newSearch, 0, search.length);
                if(facets[0].indexOf("#") == -1)
                    newSearch[newSearch.length-1] = "@" + facets[0] + "='" + facetValue.getKey() + "'";
                else
                    newSearch[newSearch.length-1] = "@" + facets[0].substring(0,facets[0].indexOf("#")) + "='" + facetValue.getKey() + "'" + facets[0].substring(facets[0].indexOf("#"));
                
                FacetSearchNodeId childNodeId = new FacetSearchNodeId(state.getNodeId());
                Name childName = stateMgr.resolver.getQName(ISO9075Helper.encodeLocalName(facetValue.getKey()));
                state.addChildNodeEntry(childName, childNodeId);
                childNodeId.queryname = queryname;
                childNodeId.docbase = docbase;
                childNodeId.facets = newFacets;
                childNodeId.search = newSearch;
                childNodeId.count = facetValue.getValue().count;
            }
        }

        FacetResultSetProvider.FacetResultSetNodeId childNodeId;
        childNodeId = subNodesProvider . new FacetResultSetNodeId(state.getNodeId(),queryname,docbase,search,count);
        state.addChildNodeEntry(stateMgr.resolver.getQName(HippoNodeType.HIPPO_RESULTSET), childNodeId);

        return state;
    }

    public NodeState populate(HippoNodeId nodeId, NodeId parentId) throws RepositoryException {
        FacetSearchNodeId searchNodeId = (FacetSearchNodeId) nodeId;
        NodeState state = createNew(nodeId, externalNodeName, parentId);
        state.setDefinitionId(virtualNodeDef.getId());

        PropertyState propState = createNew(querynameName, nodeId);
        propState.setType(PropertyType.STRING);
        propState.setDefinitionId(querynamePropDef.getId());
        propState.setValues(new InternalValue[] { InternalValue.create(searchNodeId.queryname) });
        propState.setMultiValued(false);
        state.addPropertyName(querynameName);

        propState = createNew(docbaseName, nodeId);
        propState.setType(PropertyType.STRING);
        propState.setDefinitionId(docbasePropDef.getId());
        propState.setValues(new InternalValue[] { InternalValue.create(searchNodeId.docbase) });
        propState.setMultiValued(false);
        state.addPropertyName(docbaseName);

        propState = createNew(facetsName, nodeId);
        propState.setType(PropertyType.STRING);
        propState.setDefinitionId(facetsPropDef.getId());
        propState.setValues(InternalValue.create(searchNodeId.facets));
        propState.setMultiValued(true);
        state.addPropertyName(facetsName);

        propState = createNew(searchName, nodeId);
        propState.setType(PropertyType.STRING);
        propState.setDefinitionId(searchPropDef.getId());
        propState.setValues(InternalValue.create(searchNodeId.search));
        propState.setMultiValued(true);
        state.addPropertyName(searchName);

        return populate(state);
    }
}
