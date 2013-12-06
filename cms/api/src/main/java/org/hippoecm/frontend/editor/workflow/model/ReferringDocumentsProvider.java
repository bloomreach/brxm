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
package org.hippoecm.frontend.editor.workflow.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoQuery;
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
                List<Node> nodes = getReferrersSortedByName(model.getObject());
                for(Node node : nodes) {
                    entries.add(new JcrNodeModel(node));
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        }
    }

    protected List<Node> getReferrersSortedByName(Node document) throws RepositoryException {
        List<Node> referrers = new ArrayList<>();
        if (!document.isNodeType(HippoNodeType.NT_DOCUMENT)) {
            return null;
        }
        document = ((HippoNode) document).getCanonicalNode();
        Node handle = document.getParent();
        if (!handle.isNodeType(HippoNodeType.NT_HANDLE)) {
            return null;
        }
        String uuid = handle.getIdentifier();
        QueryManager queryManager = document.getSession().getWorkspace().getQueryManager();
        String statement = createReferrersStatement(retrieveUnpublished, uuid, 5);
        HippoQuery query = (HippoQuery) queryManager.createQuery(statement, Query.XPATH);
        query.setLimit(1000);
        QueryResult result = query.execute();
        numResults = (int) result.getNodes().getSize();
        if (numResults >= 1000) {
            numResults = -1;
        }
        for (NodeIterator iter = result.getNodes(); iter.hasNext();) {
            if(referrers.size() >= getLimit()) {
                break;
            }
            referrers.add(iter.nextNode());
        }
        return referrers;
    }

    static String createReferrersStatement(final boolean retrieveUnpublished, final String uuid, final int maxDocbaseDepth) {
        if (maxDocbaseDepth == 0) {
            throw new IllegalArgumentException("maxDocbaseDepth must be at least 1");
        }
        StringBuilder statement;
        if (retrieveUnpublished) {
            statement = new StringBuilder("//element(*,hippo:handle)[*/hippo:availability='preview' and (");
        } else {
            statement = new StringBuilder("//element(*,hippo:handle)[*/hippo:availability='live' and (");
        }
        // we check at most maxDocbaseDepth levels deep for referring links
        for (int i = 0; i < maxDocbaseDepth; i++) {
            if (i > 0) {
                statement.append(" or ");
            }
            for (int j = 0 ; j <= i; j++) {
                statement.append("*/");
            }
            statement.append("@hippo:docbase='").append(uuid).append("'");
        }
        statement.append(")] order by @jcr:name ascending");
        return statement.toString();
    }

}
