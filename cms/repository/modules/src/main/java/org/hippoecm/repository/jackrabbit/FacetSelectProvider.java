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
import java.util.Vector;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.uuid.UUID;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.jackrabbit.ViewVirtualProvider.ViewNodeId;
import org.hippoecm.repository.jackrabbit.ViewVirtualProvider.ViewNodeId.Child;

public class FacetSelectProvider extends HippoVirtualProvider
{
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    ViewVirtualProvider subNodesProvider;
    Name docbaseName;
    Name facetsName;
    Name valuesName;
    Name modesName;
    Name handleName;
    Name requestName;

    public FacetSelectProvider() {
        super();
    }

    protected void initialize() throws RepositoryException {
        this.subNodesProvider = (ViewVirtualProvider) lookup("org.hippoecm.repository.jackrabbit.ViewVirtualProvider");
        register(resolveName(HippoNodeType.NT_FACETSELECT), null);
        docbaseName = resolveName(HippoNodeType.HIPPO_DOCBASE);
        facetsName = resolveName(HippoNodeType.HIPPO_FACETS);
        valuesName = resolveName(HippoNodeType.HIPPO_VALUES);
        modesName = resolveName(HippoNodeType.HIPPO_MODES);
        handleName = resolveName(HippoNodeType.NT_HANDLE);
        requestName = resolveName(HippoNodeType.NT_REQUEST);
    }

    @Override
    public NodeState populate(NodeState state) throws RepositoryException {

        String[] docbase = getProperty(state.getNodeId(), docbaseName);
        String[] newFacets = getProperty(state.getNodeId(), facetsName);
        String[] newValues = getProperty(state.getNodeId(), valuesName);
        String[] newModes  = getProperty(state.getNodeId(), modesName);

        if(docbase == null || docbase.length == 0 || newFacets == null || newValues == null || newModes == null) {
            return state;
        }
System.err.println("BERRY BERRY BERRY "+docbase[0]+" "+docbase[0].endsWith("babecafebabe"));
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
            boolean singledView = false;
            LinkedHashMap<Name,String> view = new LinkedHashMap<Name,String>();
            LinkedHashMap<Name,String> order = null;

            /*
             * If state.getParentId() instanceof ViewNodeId, we know for sure we are dealing with
             * a facetselect below a facetselect. We need to take into account the view of the parent select.
             * In principle, a state of type HippoNodeId always has a parent. To be sure, check for null
             */

            if (state.getParentId()!=null && state.getParentId() instanceof ViewNodeId) {
               ViewNodeId viewNodeId = ((ViewNodeId)state.getParentId());
               if(viewNodeId.view != null) {
                   view.putAll(viewNodeId.view);
               }
               if(viewNodeId.order != null) {
                   if(order == null) {
                       order = new LinkedHashMap<Name,String>();
                   }
                   order.putAll(viewNodeId.order);
               }
               singledView = viewNodeId.singledView;
            }

            if(newFacets.length != newValues.length || newFacets.length != newModes.length) {
                throw new RepositoryException("Malformed definition of faceted selection: all must be of same length.");
            }
            for(int i=0; i<newFacets.length; i++) {
                if(newModes[i].equalsIgnoreCase("stick") || newModes[i].equalsIgnoreCase("select") ||
                   newModes[i].equalsIgnoreCase("single")) {
                    view.put(resolveName(newFacets[i]), newValues[i]);
                    if(newModes[i].equalsIgnoreCase("single")) {
                        singledView = true;
                    }
                } else if(newModes[i].equalsIgnoreCase("prefer") || newModes[i].equalsIgnoreCase("prefer-single")) {
                    if(order == null) {
                        order = new LinkedHashMap<Name,String>();
                    }
                    order.put(resolveName(newFacets[i]), newValues[i]);
                    if(newModes[i].endsWith("prefer-single")) {
                        singledView = true;
                    }
                } else if(newModes[i].equalsIgnoreCase("clear")) {
                    view.remove(resolveName(newFacets[i]));
                }
            }
            boolean isHandle = dereference.getNodeTypeName().equals(handleName);
            if(order != null && isHandle) {
                // since the order is not null, we first have to sort all childs according the order. We only order below a handle
                Vector<ViewNodeId.Child> children = new Vector<ViewNodeId.Child>();
                for(Iterator iter = dereference.getChildNodeEntries().iterator(); iter.hasNext(); ){
                    ChildNodeEntry entry = (ChildNodeEntry) iter.next();
                    ViewNodeId childNodeId = subNodesProvider. new ViewNodeId(state.getNodeId(), entry.getId(), entry.getName(), view, order, singledView);
                    children.add(childNodeId . new Child(entry.getName(), childNodeId));
                }
                ViewNodeId.Child[] childrenArray = children.toArray(new ViewNodeId.Child[children.size()]);
                Arrays.sort(childrenArray);
                for(int i = 0; i < childrenArray.length && (i == 0 || !singledView) ; i ++) {
                    state.addChildNodeEntry(childrenArray[i].getKey(), childrenArray[i].getValue());
                }

            } else {
                for(Iterator iter = dereference.getChildNodeEntries().iterator(); iter.hasNext(); ) {
                    ChildNodeEntry entry = (ChildNodeEntry) iter.next();
                    if(subNodesProvider.match(view, entry.getId())) {
                        /*
                         * below we check on the entry's nodestate wether the node type is hippo:request,
                         * because we do not show these nodes in the facetselects in mode single.
                         * Since match() already populates the nodestates of the child entries, this won't impose
                         * extra performance hit
                         */
                        if(isHandle && singledView && getNodeState(entry.getId()).getNodeTypeName().equals(requestName)) {
                            continue;
                        } else {
                            NodeId childNodeId = subNodesProvider . new ViewNodeId(state.getNodeId(), entry.getId(), entry.getName(), view, order, singledView);
                            state.addChildNodeEntry(entry.getName(), childNodeId);
                            if(isHandle && singledView) {
                               // stop after first match because single hippo document view
                               break;
                            }
                        }
                    }
                }
            }
        }

        return state;
    }

    @Override
    public NodeState populate(HippoNodeId nodeId, NodeId parentId) throws RepositoryException {
        throw new RepositoryException("Illegal internal state");
    }
}
