/*
 *  Copyright 2009-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.workflow.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.commons.lang.ArrayUtils;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.RepositoryRuntimeException;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.hippoecm.frontend.plugins.standards.list.datatable.SortState;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoQuery;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReferringDocumentsProvider extends NodeModelWrapper implements ISortableDataProvider {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ReferringDocumentsProvider.class);

    private boolean retrieveUnpublished;
    private SortState state = new SortState();
    private transient int numResults;
    private transient List<JcrNodeModel> entries;

    public ReferringDocumentsProvider(final JcrNodeModel nodeModel) {
        this(nodeModel, false);
    }

    public ReferringDocumentsProvider(final JcrNodeModel nodeModel, final boolean retrieveUnpublished) {
        super(nodeModel);
        this.retrieveUnpublished = retrieveUnpublished;
    }

    public Iterator iterator(final long first, final long count) {
        load();
        return entries.subList((int) first, (int) (first + count)).iterator();
    }

    public IModel model(final Object object) {
        return (IModel) object;
    }

    /**
     * An estimate of the total number of results; when this number is unknown or equal to
     * the hard limit of 1000, -1 is returned.  This should be interpreted as "thousands
     * of hits".
     */
    public int getNumResults() {
        load();
        return numResults;
    }
    
    public int getLimit() {
        return 100;
    }
    
    public long size() {
        load();
        return entries.size();
    }

    @Override
    public void detach() {
        entries = null;
        super.detach();
    }

    public ISortState getSortState() {
        return state;
    }

    public void setSortState(final ISortState state) {
        this.state = (SortState) state;
    }

    protected void load() {
        if (entries == null) {
            try {
                entries = new ArrayList<>();
                numResults = 0;
                final IModel<Node> model = getChainedModel();
                SortedSet<Node> nodes = getReferrersSortedByName(model.getObject(), retrieveUnpublished, getLimit());
                numResults = nodes.size();
                for(Node node : nodes) {
                    entries.add(new JcrNodeModel(node));
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        }
    }

    static SortedSet<Node> getReferrersSortedByName(Node handle, final boolean retrieveUnpublished, final int resultMaxCount) throws RepositoryException {
        if (handle.isNodeType(HippoNodeType.NT_DOCUMENT)) {
            handle = handle.getParent();
        }
        if (!handle.isNodeType(HippoNodeType.NT_HANDLE)) {
            return Collections.emptySortedSet();
        }

        final Map<String, Node> referrers = new HashMap<>();
        final String uuid = handle.getIdentifier();

        final String requiredAvailability;
        if (retrieveUnpublished) {
            requiredAvailability = "preview";
        } else {
            requiredAvailability = "live";
        }

        StringBuilder queryBuilder =
                new StringBuilder("//element(*,hippo:facetselect)[@hippo:docbase='").append(uuid).append("']");
        addReferrers(handle, requiredAvailability, resultMaxCount, queryBuilder.toString(), referrers);

        queryBuilder =
                new StringBuilder("//element(*,hippo:mirror)[@hippo:docbase='").append(uuid).append("']");
        addReferrers(handle, requiredAvailability, resultMaxCount, queryBuilder.toString(), referrers);

        return getSortedReferrers(referrers.values());
    }


    static void addReferrers(final Node handle, final String requiredAvailability,
                             final int resultMaxCount, final String queryStatement, final Map<String, Node> referrers) throws RepositoryException {
        final QueryManager queryManager = handle.getSession().getWorkspace().getQueryManager();
        final HippoQuery query = (HippoQuery) queryManager.createQuery(queryStatement, Query.XPATH);
        query.setLimit(1000);
        final QueryResult result = query.execute();

        for (Node hit : new NodeIterable(result.getNodes())) {
            if (referrers.size() >= resultMaxCount) {
                break;
            }

            Node current = hit;
            while (current.getDepth() > 0) {
                Node parent = current.getParent();
                if (parent.isNodeType(HippoNodeType.NT_HANDLE) &&
                        current.isNodeType(HippoNodeType.NT_DOCUMENT) &&
                        hasCorrectAvailability(current, requiredAvailability)) {
                    referrers.put(parent.getIdentifier(), parent);
                    break;
                }
                current = current.getParent();
            }
        }
    }

    static boolean hasCorrectAvailability(final Node node, final String requiredAvailability) throws RepositoryException {
        String[] availabilityArray = JcrUtils.getMultipleStringProperty(node, HippoNodeType.HIPPO_AVAILABILITY, null);
        // availabilityArray can be null in ArrayUtils.contains
        return ArrayUtils.contains(availabilityArray, requiredAvailability);
    }

    private static SortedSet<Node> getSortedReferrers(final Collection<Node> nodes) throws RepositoryException {
        final TreeSet<Node> sorted = new TreeSet<>((o1, o2) -> {
            try {
                return o1.getName().compareTo(o2.getName());
            } catch (RepositoryException e) {
                throw new RepositoryRuntimeException(e);
            }
        });
        sorted.addAll(nodes);
        return sorted;
    }


}
