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

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.state.NodeState;

public class ViewVirtualProvider extends MirrorVirtualProvider
{
    final static private String SVN_ID = "$Id$";

    protected class ViewNodeId extends MirrorNodeId {
        Map<String,String> view; // must be immutable

        ViewNodeId(NodeId parent, NodeId upstream, Map view) {
            super(ViewVirtualProvider.this, parent, upstream);
            this.view = view;
        }
    }

    ViewVirtualProvider(HippoLocalItemStateManager stateMgr) throws RepositoryException {
        super(stateMgr, null, null);
    }

    public NodeState populate(NodeState state) {
        return state;
    }

    protected boolean match(Map<String,String> view, NodeId candidate) {
        for(Map.Entry<String,String> entry : view.entrySet()) {
            String facet = entry.getKey();
            String value = entry.getValue();
            String[] matching = getProperty(candidate, facet);
            if(matching != null && matching.length > 0) {
                if(value != null && !value.equals("") && !value.equals("*")) {
                    int i;
                    for(i=0; i<matching.length; i++)
                        if(matching[i].equals(value))
                            break;
                    if(i == matching.length)
                        return false;
                }
            }
        }
        return true;
    }

    protected void populateChildren(NodeId nodeId, NodeState state, NodeState upstream) {
        ViewNodeId viewId = (ViewNodeId) nodeId;
        for(Iterator iter = upstream.getChildNodeEntries().iterator(); iter.hasNext(); ) {
            NodeState.ChildNodeEntry entry = (NodeState.ChildNodeEntry) iter.next();
            if(match(viewId.view, entry.getId())) {
                ViewNodeId childNodeId = new ViewNodeId(nodeId, entry.getId(), viewId.view);
                state.addChildNodeEntry(entry.getName(), childNodeId);
            }
        }
    }
}
