/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.plugins.standards.list.SingleVariantProvider;
import org.hippoecm.frontend.plugins.standards.list.datatable.SortState;
import org.hippoecm.frontend.plugins.standards.list.datatable.SortableDataProvider;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provider of JcrNodeModels for nodes in a facet(sub)search resultset.
 * Multiple variants of the same document are collapsed to a single entry.  
 */
public final class FacetSearchProvider extends SortableDataProvider<Node> implements IObservable {
    private static final long serialVersionUID = 1L;


    static final Logger log = LoggerFactory.getLogger(FacetSearchProvider.class);

    private JcrNodeModel model;
    private SingleVariantProvider handleProvider;
    private Map<String, Comparator<Node>> comparators;
    private IObservationContext obContext;
    private JcrNodeModel resultSetModel;

    public FacetSearchProvider(JcrNodeModel model, Map<String, Comparator<Node>> comparators) {
        this.model = model;
        this.comparators = comparators;
        this.handleProvider = new SingleVariantProvider(new LoadableDetachableModel<Iterator>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected NodeIterator load() {
                Node node = FacetSearchProvider.this.model.getNode();
                // workaround: node may disappear without notification
                if (node != null) {
                    try {
                        return node.getNode(HippoNodeType.HIPPO_RESULTSET).getNodes();
                    } catch (RepositoryException e) {
                        log.info("Could not get NodeIterator from resultset for FacetSearchProvider, will return null.",
                                e);
                    }
                }
                return null;
            }
        });
    }

    // impl IDataProvider

    public Iterator<Node> iterator(int first, int count) {
        List<Node> displayedList = new ArrayList<Node>(handleProvider.getObject());
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
        return handleProvider.getObject().size();
    }

    public void detach() {
        handleProvider.detach();
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
