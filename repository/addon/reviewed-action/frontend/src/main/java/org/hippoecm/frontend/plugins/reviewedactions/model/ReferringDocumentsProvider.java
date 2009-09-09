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
package org.hippoecm.frontend.plugins.reviewedactions.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
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

    public Iterator iterator(int first, int count) {
        load();
        return entries.subList(first, first + count).iterator();
    }

    public IModel model(Object object) {
        return (IModel) object;
    }

    public int getNumResults() {
        load();
        return numResults;
    }
    
    public int getLimit() {
        return 20;
    }
    
    public int size() {
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
                entries = new ArrayList<JcrNodeModel>();
                numResults = 0;
                Set<Node> nodes = getReferrers(getNodeModel().getNode());
                if (nodes != null) {
                    Set<Node> referrers = new TreeSet<Node>(new Comparator<Node>() {
                        public int compare(Node node1, Node node2) {
                            try {
                                int result = node1.getName().compareTo(node2.getName());
                                if (result != 0) {
                                    return result;
                                }
                                return node1.getUUID().compareTo(node2.getUUID());
                            } catch (UnsupportedRepositoryOperationException ex) {
                                // cannot happen
                                return 0;
                            } catch (RepositoryException ex) {
                                return 0;
                            }
                        }
                    });
                    referrers.addAll(nodes);
                    for(Node node : referrers) {
                        entries.add(new JcrNodeModel(node));
                    }
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
            if (numResults < 0) {
                numResults = entries.size();
            }
        }
    }

    protected Set<Node> getReferrers(Node document) throws RepositoryException {
        Set<Node> referrers = new TreeSet<Node>(new Comparator<Node>() {
            public int compare(Node node1, Node node2) {
                try {
                    return node1.getUUID().compareTo(node2.getUUID());
                } catch (UnsupportedRepositoryOperationException ex) {
                    // cannot happen
                    return 0;
                } catch (RepositoryException ex) {
                    return 0;
                }
            }
        });
        if (!document.isNodeType(HippoNodeType.NT_DOCUMENT)) {
            return null;
        }
        document = ((HippoNode) document).getCanonicalNode();
        Node handle = document.getParent();
        if (!handle.isNodeType(HippoNodeType.NT_HANDLE) || !handle.isNodeType(HippoNodeType.NT_HARDHANDLE)) {
            return null;
        }
        String uuid = handle.getUUID();
        QueryManager queryManager = document.getSession().getWorkspace().getQueryManager();
        String statement = "//*[@hippo:docbase='" + uuid + "']";
        Query query = queryManager.createQuery(statement, Query.XPATH);
        QueryResult result = query.execute();
        numResults = (int) result.getNodes().getSize();
        for (NodeIterator iter = result.getNodes(); iter.hasNext();) {
            Node node = iter.nextNode();
            while (node != null && !node.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                node = (node.getDepth() > 0 ? node.getParent() : null);
            }
            if (node != null) {
                if (node.isNodeType("hippostd:publishable") && node.hasProperty("hippostd:state")) {
                    String state = node.getProperty("hippostd:state").getString();
                    if ("published".equals(state) || (retrieveUnpublished && "unpublished".equals(state))) {
                        referrers.add(node);
                        if (referrers.size() >= getLimit()) {
                            break;
                        }
                    }
                }
            }
        }
        return referrers;
    }

}
