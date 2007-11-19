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
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Map;
import java.util.TreeMap;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.conversion.NamePathResolver;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.spi.Name;

import org.hippoecm.repository.FacetedNavigationEngine.HitsRequested;
import org.hippoecm.repository.FacetedNavigationEngine;
import org.hippoecm.repository.api.HippoNodeType;

public class FacetResultSetProvider extends HippoVirtualProvider
{
    final static private String SVN_ID = "$Id$";

    private static Pattern facetPropertyPattern;
    static {
        facetPropertyPattern = Pattern.compile("^@([^=]+)='(.+)'$");
    }

    MirrorVirtualProvider subNodesProvider;
    FacetedNavigationEngine facetedEngine;
    FacetedNavigationEngine.Context facetedContext;

    FacetResultSetProvider(HippoLocalItemStateManager stateMgr, MirrorVirtualProvider subNodesProvider,
                           FacetedNavigationEngine facetedEngine, FacetedNavigationEngine.Context facetedContext) {
        super(stateMgr);
        this.facetedEngine = facetedEngine;
        this.facetedContext = facetedContext;
        this.subNodesProvider = subNodesProvider;
    }

    public NodeState populate(NodeState state) {
        Map<String,String> currentFacetQuery = new TreeMap<String,String>();
        String[] currentFacetPath = getProperty(state.getNodeId(), HippoNodeType.HIPPO_SEARCH);
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
        hitsRequested.setResultRequested(true);
        hitsRequested.setLimit(1000000);
        hitsRequested.setOffset(0);

        FacetedNavigationEngine.Result result = facetedEngine.view(queryname, initialQuery, facetedContext, currentFacetQuery,
                                                                   null, hitsRequested);
        
        for(Iterator<String> iter = result.iterator(); iter.hasNext(); ) {
            String foundNodePath = iter.next();
            try {
                NodeId upstream = getNodeId(foundNodePath);
                /* The next statement is painfull performance wise.
                 * Only to obtain the child node name, we have to retrieve the parent state.
                 */
                Name name = getNodeState(getNodeState(upstream).getParentId()).getChildNodeEntry(upstream).getName();
                state.addChildNodeEntry(name, subNodesProvider . new MirrorNodeId(state.getNodeId(), upstream));
            } catch(RepositoryException ex) {
                System.err.println(ex.getMessage());
                ex.printStackTrace(System.err);
            }
        }

        return state;
    }

    public NodeState populate(NodeId nodeId, NodeId parentId) throws RepositoryException {
        throw new RepositoryException("Illegal internal state");
    }
}
