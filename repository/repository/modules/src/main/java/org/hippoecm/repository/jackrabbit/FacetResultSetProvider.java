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

import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.nodetype.PropDef;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.hippoecm.repository.FacetedNavigationEngine;
import org.hippoecm.repository.FacetedNavigationEngine.HitsRequested;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FacetResultSetProvider extends HippoVirtualProvider
{
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    protected final Logger log = LoggerFactory.getLogger(HippoVirtualProvider.class);

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
    PropDef primaryTypePropDef;

    public FacetResultSetProvider()
        throws IllegalNameException, NamespaceException, RepositoryException {
        super();
    }

    @Override
    public void initialize(DataProviderContext stateMgr) throws RepositoryException {
        super.initialize(stateMgr);
        this.facetedEngine = stateMgr.getFacetedEngine();
        this.facetedContext = stateMgr.getFacetedContext();
    }

    @Override
    protected void initialize() throws RepositoryException {
        subNodesProvider = (MirrorVirtualProvider) lookup("org.hippoecm.repository.jackrabbit.MirrorVirtualProvider");
        countName = resolveName(HippoNodeType.HIPPO_COUNT);
        countPropDef = lookupPropDef(resolveName(HippoNodeType.NT_FACETRESULT), countName);
        primaryTypePropDef = lookupPropDef(resolveName(HippoNodeType.NT_FACETRESULT), countName);
        this.subNodesProvider = (MirrorVirtualProvider) lookup("org.hippoecm.repository.jackrabbit.MirrorVirtualProvider");
        register(null, resolveName(HippoNodeType.NT_FACETRESULT));
    }

    @Override
    public NodeState populate(NodeState state) throws IllegalNameException, NamespaceException {
        FacetResultSetNodeId nodeId = (FacetResultSetNodeId) state.getNodeId();
        String queryname = nodeId.queryname;
        String docbase = nodeId.docbase;
        String[] search = nodeId.search;
        long count = nodeId.count;

        Map<String,String> currentFacetQuery = new TreeMap<String,String>();
        for(int i=0; search != null && i < search.length; i++) {
            Matcher matcher = facetPropertyPattern.matcher(search[i]);
            if(matcher.matches() && matcher.groupCount() == 2) {
                try {
                    currentFacetQuery.put(resolvePath(matcher.group(1)).toString(), matcher.group(2));
                } catch(IllegalNameException ex) {
                    // FIXME: log a very serious error
                    return state;
                } catch(NamespaceException ex) {
                    // FIXME: log a very serious error
                    return state;
                } catch(MalformedPathException ex) {
                    // FIXME: log a very serious error
                    return state;
                }
            }
        }
        FacetedNavigationEngine.Query initialQuery;
        initialQuery = facetedEngine.parse(docbase);

        /* Current implementation, especialy within JackRabbit itself,
         * has serious performance problems when a large number (>1000)
         * of child nodes are direct decendents of a single parent.
         * We have chosen that there is a hard limit on the retrieval of
         * the result set at this time.
         * As nearly all user applications have serious problems with
         * displaying a large number of decendants, this is currently not
         * seen as a serious drawback to have this hard limit AT THIS TIME.
         * User applications which provide a display should not do paging
         * themselves, but use a --to be developed-- interface in the
         * facet search.  The user applications should also provide an
         * internal hard limit, in general lower than the limit set below,
         * in order to provide feedback to the user that the resultset
         * has been truncated.
         */
        HitsRequested hitsRequested = new HitsRequested();
        hitsRequested.setResultRequested(true);
        hitsRequested.setLimit(1000);
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

        PropertyState propState = createNew(NameConstants.JCR_PRIMARYTYPE, state.getNodeId());
        propState.setType(PropertyType.NAME);
        propState.setDefinitionId(primaryTypePropDef.getId());
        propState.setValues(new InternalValue[] { InternalValue.create(resolveName(HippoNodeType.NT_FACETRESULT))} );
        propState.setMultiValued(false);
        state.addPropertyName(NameConstants.JCR_PRIMARYTYPE);

        propState = createNew(countName, state.getNodeId());
        propState.setType(PropertyType.LONG);
        propState.setDefinitionId(countPropDef.getId());
        propState.setValues(new InternalValue[] { InternalValue.create(count) });
        propState.setMultiValued(false);
        state.addPropertyName(countName);

        for(NodeId upstream : facetedResult) {
            if(upstream == null)
                continue;
            /* The next statements are painfull performance wise.
             * Only to obtain the child node name, we have to retrieve the parent state.
             */
            NodeState upstreamState = getNodeState(upstream);
            if(upstreamState == null)
                continue;
            NodeId parentId = upstreamState.getParentId();
            if(parentId == null)
                continue;
            Name name = getNodeState(parentId).getChildNodeEntry(upstream).getName();
            state.addChildNodeEntry(name, subNodesProvider . new MirrorNodeId(state.getNodeId(), upstream, name));
        }

        return state;
    }
}
