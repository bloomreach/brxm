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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.state.NodeState;
import org.hippoecm.repository.api.HippoNodeType;

public class FacetSelectProvider extends HippoVirtualProvider
{
    final static private String SVN_ID = "$Id$";

    ViewVirtualProvider subNodesProvider;

    FacetSelectProvider(HippoLocalItemStateManager stateMgr, ViewVirtualProvider subNodesProvider) throws RepositoryException {
        super(stateMgr, stateMgr.resolver.getQName(HippoNodeType.NT_FACETSELECT), null);
        this.subNodesProvider = subNodesProvider;
    }

    public NodeState populate(NodeState state) throws RepositoryException {
        NodeId nodeId = state.getNodeId();
        String docbase = getProperty(nodeId, HippoNodeType.HIPPO_DOCBASE)[0];
        String[] newFacets = getProperty(nodeId, HippoNodeType.HIPPO_FACETS);
        String[] newValues = getProperty(nodeId, HippoNodeType.HIPPO_VALUES);
        String[] newModes  = getProperty(nodeId, HippoNodeType.HIPPO_MODES);

        NodeState dereference = getNodeState(docbase);

        if(dereference != null) {

            Map<String,String> view = new HashMap<String,String>();
            if(newFacets.length != newValues.length || newFacets.length != newModes.length)
                throw new RepositoryException("Malformed definition of faceted selection: all must be of same length.");
            for(int i=0; i<newFacets.length; i++) {
                if(newModes[i].equalsIgnoreCase("stick") || newModes[i].equalsIgnoreCase("select")) {
                    view.put(newFacets[i], newValues[i]);
                } else if(newModes[i].equalsIgnoreCase("clear")) {
                    view.remove(newFacets[i]);
                }
            }
            for(Iterator iter = dereference.getChildNodeEntries().iterator(); iter.hasNext(); ) {
                NodeState.ChildNodeEntry entry = (NodeState.ChildNodeEntry) iter.next();
                if(subNodesProvider.match(view, entry.getId())) {
                    NodeId childNodeId = subNodesProvider . new ViewNodeId(state.getNodeId(),entry.getId(),entry.getName(),view);
                    state.addChildNodeEntry(entry.getName(), childNodeId);
                }
            }
        }

        return state;
    }

    public NodeState populate(NodeId nodeId, NodeId parentId) throws RepositoryException {
        throw new RepositoryException("Illegal internal state");
    }
}
