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
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.hippoecm.frontend.plugins.standards.list.datatable.SortState;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoQuery;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReferringDocumentsProvider extends NodeModelWrapper implements ISortableDataProvider {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ReferringDocumentsProvider.class);

    private boolean retrieveUnpublished;
    private SortState state = new SortState();
    private transient int numResults;
    private transient List<JcrNodeModel> entries;

    public ReferringDocumentsProvider(JcrNodeModel nodeModel) {
        this(nodeModel, false);
    }

    public ReferringDocumentsProvider(JcrNodeModel nodeModel, boolean retrieveUnpublished) {
        super(nodeModel);
        this.retrieveUnpublished = retrieveUnpublished;
    }

    public Iterator iterator(long first, long count) {
        load();
        return entries.subList((int) first, (int) (first + count)).iterator();
    }

    public IModel model(Object object) {
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

    public void setSortState(ISortState state) {
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

    static SortedSet<Node> getReferrersSortedByName(Node handle, boolean retrieveUnpublished, int resultMaxCount) throws RepositoryException {
        if (handle.isNodeType(HippoNodeType.NT_DOCUMENT)) {
            handle = handle.getParent();
        }
        if (!handle.isNodeType(HippoNodeType.NT_HANDLE)) {
            return Collections.emptySortedSet();
        }

        Comparator<Node> comparator = (Node node1, Node node2) -> {
            try {
                if (node1.getIdentifier().equals(node2.getIdentifier())) {
                    return 0;
                }

                int nameCompareResult = node1.getName().compareTo(node2.getName());
                if (nameCompareResult != 0) {
                    return nameCompareResult;
                } else {
                    return node1.getIdentifier().compareTo(node2.getIdentifier());
                }
            } catch (RepositoryException e) {
                // ignore, should not happen
                return 0;
            }
        };
        TreeSet<Node> referrers = new TreeSet<>(comparator);
        String uuid = handle.getIdentifier();

        StringBuilder queryBuilder =
                new StringBuilder("//element(*,hippo:facetselect)[@hippo:docbase='").append(uuid).append("']");
        addReferrers(handle, retrieveUnpublished, resultMaxCount, queryBuilder.toString(), referrers);

        queryBuilder =
                new StringBuilder("//element(*,hippo:mirror)[@hippo:docbase='").append(uuid).append("']");
        addReferrers(handle, retrieveUnpublished, resultMaxCount, queryBuilder.toString(), referrers);

        return referrers;
    }

    static void addReferrers(Node handle, boolean retrieveUnpublished, int resultMaxCount, String queryStatement, TreeSet<Node> referrers) throws RepositoryException {
        QueryManager queryManager = handle.getSession().getWorkspace().getQueryManager();
        HippoQuery query = (HippoQuery) queryManager.createQuery(queryStatement, Query.XPATH);
        query.setLimit(1000);
        QueryResult result = query.execute();
        String rootUuid = handle.getSession().getRootNode().getIdentifier();

        for (NodeIterator iter = result.getNodes(); iter.hasNext();) {
            if (referrers.size() >= resultMaxCount) {
                break;
            }

            Node current = iter.nextNode();
            while (!current.getIdentifier().equals(rootUuid)) {
                if (current.isNodeType(HippoNodeType.NT_DOCUMENT) && hasCorrectAvailability(current, retrieveUnpublished)) {
                    Node parent = current.getParent();
                    if (parent.isNodeType(HippoNodeType.NT_HANDLE)) {
                        referrers.add(parent);
                        break;
                    }
                }

                current = current.getParent();
            }
        }
    }

    static boolean hasCorrectAvailability(Node node, boolean retrieveUnpublished) throws RepositoryException {
        String[] availabilityArray = JcrUtils.getMultipleStringProperty(node, "hippo:availability", null);
        if (availabilityArray == null) {
            return false;
        }

        // iterating over all strings in case the node is a not-yet-converted 7.8 style document
        for (String availability: availabilityArray) {
            if (availability.equals("live")) {
                return true;
            }
            if (retrieveUnpublished && availability.equals("preview")) {
                return true;
            }
        }

        return false;
    }

}
