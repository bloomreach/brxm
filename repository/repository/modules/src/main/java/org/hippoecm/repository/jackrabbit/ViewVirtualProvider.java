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

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.uuid.UUID;
import org.hippoecm.repository.api.HippoNodeType;

public class ViewVirtualProvider extends MirrorVirtualProvider
{
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    protected class ViewNodeId extends MirrorNodeId {
        private static final long serialVersionUID = 1L;

        /* The following fields MUST be immutable
         */
        boolean singledView;
        LinkedHashMap<Name,String> view;
        LinkedHashMap<Name,String> order;

        ViewNodeId(NodeId parent, NodeId upstream, Name name, LinkedHashMap<Name,String> view, LinkedHashMap<Name,String> order, boolean singledView) {
            super(ViewVirtualProvider.this, parent, name, upstream);
            this.view = view;
            this.order = order;
            this.singledView = singledView;
        }

        class Child implements Comparable<Child> {
            Name name;
            ViewNodeId nodeId;
            Child(Name name, ViewNodeId viewNodeId) {
                this.name = name;
                this.nodeId = viewNodeId;
            }
            public Name getKey() {
                return name;
            }
            public ViewNodeId getValue() {
                return ViewNodeId.this;
            }
            public int compareTo(Child o) {
                if(o == null)
                    throw new NullPointerException();

                if(order == null)
                    return 0;

                for(Map.Entry<Name,String> entry : order.entrySet()) {
                    Name facet = entry.getKey();
                    String value = entry.getValue();

                    int thisFacetValueIndex = -1;
                    String[] thisFacetValues = getProperty(upstream, facet);
                    if(thisFacetValues != null) {
                        for(int i=0; i<thisFacetValues.length; i++) {
                            if(thisFacetValues[i].equals(value)) {
                                thisFacetValueIndex = i;
                                break;
                            }
                        }
                    }

                    int otherFacetValueIndex = -1;
                    String[] otherFacetValues = getProperty(o.getValue().upstream, facet);
                    if(otherFacetValues != null) {
                        for(int i=0; i<otherFacetValues.length; i++) {
                            if(otherFacetValues[i].equals(value)) {
                                otherFacetValueIndex = i;
                                break;
                            }
                        }
                    }

                    if(thisFacetValueIndex != -1 && otherFacetValueIndex == -1) {
                        return -1;
                    } else if(thisFacetValueIndex == -1 && otherFacetValueIndex != -1) {
                        return 1;
                    } else if(value == null || value.equals("") || value.equals("*")) {
                        if(thisFacetValues[thisFacetValueIndex].compareTo(otherFacetValues[otherFacetValueIndex]) != 0) {
                            return thisFacetValues[thisFacetValueIndex].compareTo(otherFacetValues[otherFacetValueIndex]);
                        }
                    }
                }

                return 0;
            }
        }
    }

    public ViewVirtualProvider() throws RepositoryException {
        super();
    }

    private Name handleName;
    private Name requestName;

    @Override
    protected void initialize() throws RepositoryException {
        super.initialize();
        handleName = resolveName(HippoNodeType.NT_HANDLE);
        requestName = resolveName(HippoNodeType.NT_REQUEST);
    }

    @Override
    public NodeState populate(NodeState state) throws RepositoryException {
        String[] docbase = getProperty(state.getNodeId(), docbaseName);
        if(docbase == null || docbase.length == 0) {
            return state;
        }
        if(docbase[0].endsWith("babecafebabe")) {
            // one of the defined (and fixed, so string compare is fine) system areas
            return state;
        }
        NodeState dereference = null;
        try {
            dereference = getNodeState(new NodeId(new UUID(docbase[0])));
        } catch (IllegalArgumentException e) {
            log.warn("invalid docbase '" + docbase[0] + "' because not a valid UUID ");
        }
        if(dereference != null) {
            LinkedHashMap<Name,String> view = new LinkedHashMap<Name,String>();
            for(Iterator iter = dereference.getChildNodeEntries().iterator(); iter.hasNext(); ) {
                ChildNodeEntry entry = (ChildNodeEntry) iter.next();
                if(this.match(view, entry.getId())) {
                    NodeId childNodeId = this . new ViewNodeId(state.getNodeId(),entry.getId(),entry.getName(),view,null,false);
                    state.addChildNodeEntry(entry.getName(), childNodeId);
                }
            }
        }
        return state;
    }

    protected boolean match(Map<Name,String> view, NodeId candidate) {
        for(Map.Entry<Name,String> entry : view.entrySet()) {
            Name facet = entry.getKey();
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
        boolean isHandle = state.getNodeTypeName().equals(handleName);
        Vector<ViewNodeId.Child> children = new Vector<ViewNodeId.Child>();
        for(Iterator iter = upstream.getChildNodeEntries().iterator(); iter.hasNext(); ) {
            ChildNodeEntry entry = (ChildNodeEntry) iter.next();
            if (!isHandle || match(viewId.view, entry.getId())) {
                /*
                 * below we check on the entry's nodestate wether the node type is hippo:request,
                 * because we do not show these nodes in the facetselects in mode single.
                 * Since match() already populates the nodestates of the child entries, this won't impose
                 * extra performance hit
                 */
                if (viewId.singledView && isHandle) {
                    if (getNodeState(entry.getId()).getNodeTypeName().equals(requestName)) {
                        continue;
                    } else {
                        ViewNodeId childNodeId = new ViewNodeId(nodeId, entry.getId(), entry.getName(), viewId.view, viewId.order, viewId.singledView);
                        children.add(childNodeId . new Child(entry.getName(), childNodeId));
                        // stop after first match because single hippo document view, and not using sorted set
                        if(viewId.order == null)
                            break;
                    }
                } else {
                    ViewNodeId childNodeId = new ViewNodeId(nodeId, entry.getId(), entry.getName(), viewId.view, viewId.order, viewId.singledView);
                    children.add(childNodeId . new Child(entry.getName(), childNodeId));
                }
            }
        }
        ViewNodeId.Child[] childrenArray = children.toArray(new ViewNodeId.Child[children.size()]);
        if(viewId.order != null && isHandle) {
            Arrays.sort(childrenArray);
        }
        for(int i=0; i<childrenArray.length && (i==0 || !(viewId.singledView && isHandle)); i++) {
            state.addChildNodeEntry(childrenArray[i].getKey(), childrenArray[i].getValue());
        }
   }
}
