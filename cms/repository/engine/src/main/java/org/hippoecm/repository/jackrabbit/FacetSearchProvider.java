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

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.spi.Name;
import org.hippoecm.repository.FacetedNavigationEngine;
import org.hippoecm.repository.FacetedNavigationEngine.HitsRequested;
import org.hippoecm.repository.api.HippoNodeType;

public class FacetSearchProvider extends HippoVirtualProvider
{
    final static private String SVN_ID = "$Id$";

    protected class FacetSearchNodeId extends HippoNodeId {
        FacetSearchNodeId(NodeId parent) {
            super(FacetSearchProvider.this, parent);
        }
        void setProperty(Name propName, String[] propValue) {
        }
    }

    private static Pattern facetPropertyPattern;
    static {
        facetPropertyPattern = Pattern.compile("^@([^=]+)='(.+)'$");
    }

    FacetResultSetProvider subNodesProvider;
    FacetedNavigationEngine facetedEngine;
    FacetedNavigationEngine.Context facetedContext;

    FacetSearchProvider(HippoLocalItemStateManager stateMgr, FacetResultSetProvider subNodesProvider,
                        FacetedNavigationEngine facetedEngine, FacetedNavigationEngine.Context facetedContext)
        throws RepositoryException
    {
        super(stateMgr, stateMgr.resolver.getQName(HippoNodeType.NT_FACETSEARCH), null);
        this.facetedEngine = facetedEngine;
        this.facetedContext = facetedContext;
        this.subNodesProvider = subNodesProvider;
    }

    public NodeState populate(NodeState state) throws RepositoryException {
        NodeId nodeId = state.getNodeId();
        String docbase = getProperty(nodeId, HippoNodeType.HIPPO_DOCBASE)[0];
        String[] newFacets = getProperty(nodeId, HippoNodeType.HIPPO_FACETS);
        String[] newValues = getProperty(nodeId, HippoNodeType.HIPPO_SEARCH);
        String[] newModes  = getProperty(nodeId, HippoNodeType.HIPPO_VALUES);

        String[] currentFacetPath = getProperty(state.getNodeId(), HippoNodeType.HIPPO_SEARCH);

        Map<String,Map<String,org.hippoecm.repository.FacetedNavigationEngine.Count>> facetSearchResultMap;
        facetSearchResultMap = new TreeMap<String,Map<String,FacetedNavigationEngine.Count>>();
        Map<String,FacetedNavigationEngine.Count> facetSearchResult;
        facetSearchResult = new TreeMap<String,FacetedNavigationEngine.Count>();
        facetSearchResultMap.put(currentFacetPath[0], facetSearchResult);

        Map<String,String> currentFacetQuery = new TreeMap<String,String>();
        for(int i=0; currentFacetPath != null && i < currentFacetPath.length; i++) {
            Matcher matcher = facetPropertyPattern.matcher(currentFacetPath[i]);
            if(matcher.matches() && matcher.groupCount() == 2) {
                currentFacetQuery.put(matcher.group(1), matcher.group(2));
            }
        }
        FacetedNavigationEngine.Query initialQuery;
        initialQuery = facetedEngine.parse(getProperty(state.getNodeId(), HippoNodeType.HIPPO_DOCBASE)[0]);
        String queryname = getProperty(state.getNodeId(), HippoNodeType.HIPPO_QUERYNAME)[0];
      
        HitsRequested hitsRequested = new HitsRequested();
        hitsRequested.setResultRequested(false);

        facetedEngine.view(queryname, initialQuery, facetedContext, currentFacetQuery, null, facetSearchResultMap, null, hitsRequested);


        for(Map.Entry<String,FacetedNavigationEngine.Count> facetValue : facetSearchResult.entrySet()) {
            FacetSearchNodeId childNodeId = new FacetSearchNodeId(state.getNodeId());
            state.addChildNodeEntry(stateMgr.resolver.getQName(facetValue.getKey()), childNodeId);

            String[] newFacetPath = new String[Math.max(0, currentFacetPath.length - 1)];
            if(currentFacetPath.length > 0)
                System.arraycopy(currentFacetPath, 1, newFacetPath, 0, currentFacetPath.length - 1);
            String[] oldSearchPath = getProperty(nodeId, HippoNodeType.HIPPO_SEARCH);
            String[] newSearchPath = new String[oldSearchPath.length + 1];
            System.arraycopy(oldSearchPath, 0, newSearchPath, 0, oldSearchPath.length);
            // check for xpath separator
            if(currentFacetPath[0].indexOf("#") == -1)
                newSearchPath[oldSearchPath.length] = "@" + currentFacetPath[0] + "='" + facetValue.getKey() + "'";
            else
                newSearchPath[oldSearchPath.length] = "@" + currentFacetPath[0].substring(0,currentFacetPath[0].indexOf("#")) + "='" + facetValue.getKey() + "'" + currentFacetPath[0].substring(currentFacetPath[0].indexOf("#"));

            childNodeId.setProperty(stateMgr.resolver.getQName(HippoNodeType.HIPPO_QUERYNAME), getProperty(nodeId, HippoNodeType.HIPPO_QUERYNAME));
            childNodeId.setProperty(stateMgr.resolver.getQName(HippoNodeType.HIPPO_DOCBASE), getProperty(nodeId, HippoNodeType.HIPPO_DOCBASE));
            childNodeId.setProperty(stateMgr.resolver.getQName(HippoNodeType.HIPPO_FACETS), newFacetPath);
            childNodeId.setProperty(stateMgr.resolver.getQName(HippoNodeType.HIPPO_SEARCH), newSearchPath);
            childNodeId.setProperty(stateMgr.resolver.getQName(HippoNodeType.HIPPO_COUNT), new String[] { Integer.toString(facetValue.getValue().count) } ); // FIXME
        }

        return state;
    }

    public NodeState populate(NodeId nodeId, NodeId parentId) throws RepositoryException {
        throw new RepositoryException("Illegal internal state");
    }
}
