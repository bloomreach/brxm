/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.frontend.plugins.cms.browse.list;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.plugins.standards.list.datatable.SortState;
import org.hippoecm.frontend.plugins.standards.list.datatable.SortableDataProvider;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provider of JcrNodeModels for nodes in a facet(sub)search resultset.
 * Multiple variants of the same document are collapsed to a single entry.  
 */
public class FacetSearchProvider extends SortableDataProvider<Node> implements IObservable {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: $";

    static final Logger log = LoggerFactory.getLogger(FacetSearchProvider.class);

    private JcrNodeModel model;
    private transient List<Node> entries = null;
    private Map<String, Comparator<Node>> comparators;
    private IObservationContext obContext;
    private JcrNodeModel resultSetModel;

    public FacetSearchProvider(JcrNodeModel model, Map<String, Comparator<Node>> comparators) {
        this.model = model;
        this.comparators = comparators;
    }

    /**
     * Load a single representative for each set of variants of a document.
     * Different documents can share a name, so the comparison is based on the
     * handle that contains the variants.  The first document (lowest sns index
     * in resultset) is used. 
     */
    void load() {
        if (entries == null) {
            Map<String, Node> primaryNodes = new HashMap<String, Node>();
            Node node = model.getNode();
            // workaround: node may disappear without notification
            if (node != null) {
                try {
                    NodeIterator subNodes = node.getNode(HippoNodeType.HIPPO_RESULTSET).getNodes();
                    while (subNodes.hasNext()) {
                        Node subNode = subNodes.nextNode();
                        if (subNode == null || !(subNode instanceof HippoNode)) {
                            continue;
                        }
                        try {
                            Node canonicalNode = ((HippoNode) subNode).getCanonicalNode();
                            if (canonicalNode == null) {
                                // no physical equivalent exists
                                continue;
                            }
                            if (canonicalNode.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                                Node parentNode = canonicalNode.getParent();
                                if (parentNode.isNodeType(HippoNodeType.NT_HANDLE)) {
                                    if (primaryNodes.containsKey(parentNode.getUUID())) {
                                        Node currentNode = primaryNodes.get(parentNode.getUUID());
                                        if (subNode.getIndex() < currentNode.getIndex()) {
                                            primaryNodes.put(parentNode.getUUID(), subNode);
                                        }
                                    } else {
                                        primaryNodes.put(parentNode.getUUID(), subNode);
                                    }
                                }
                            }
                        } catch (ItemNotFoundException ex) {
                            // physical item no longer exists
                            continue;
                        }
                    }
                } catch (RepositoryException ex) {
                    log.error(ex.getMessage());
                }
            }
            entries = new LinkedList<Node>();
            for (Node subNode : primaryNodes.values()) {
                entries.add(subNode);
            }
        }
    }

    // impl IDataProvider

    public Iterator<Node> iterator(int first, int count) {
        load();
        List<Node> displayedList = new ArrayList<Node>(entries);
        SortState sortState = getSortState();
        if (sortState != null && sortState.isSorted()) {
            String sortProperty = sortState.getProperty();
            if (sortProperty != null) {
                Comparator<Node> comparator = comparators.get(sortProperty);
                if (comparator != null) {
                    Collections.sort(displayedList, comparator);
                    if (sortState.isDescending()) {
                        Collections.reverse(displayedList);
                    }
                }
            }
        }
        return displayedList.subList(first, first + count).iterator();
    }

    public IModel<Node> model(Node object) {
        return new JcrNodeModel(object);
    }

    public int size() {
        load();
        return entries.size();
    }

    public void detach() {
        entries = null;
        model.detach();
        if (resultSetModel != null) {
            resultSetModel.detach();
        }
    }

    // observation

    public void setObservationContext(IObservationContext context) {
        this.obContext = context;
    }

    public void startObservation() {
        // workaround: facetsearch does not generate events, so the node may disappear at any time
        Node node = model.getNode();
        if (node != null) {
            // simply forward any events on the resultset node to our listeners
            try {
                resultSetModel = new JcrNodeModel(node.getNode(HippoNodeType.HIPPO_RESULTSET));
                resultSetModel.setObservationContext(obContext);
                resultSetModel.startObservation();
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        }
    }

    public void stopObservation() {
        if (resultSetModel != null) {
            resultSetModel.stopObservation();
            resultSetModel = null;
        }
    }

    // override Object

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FacetSearchProvider) {
            FacetSearchProvider that = (FacetSearchProvider) obj;
            return that.model.equals(model);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return model.hashCode() ^ 1234537;
    }

}
