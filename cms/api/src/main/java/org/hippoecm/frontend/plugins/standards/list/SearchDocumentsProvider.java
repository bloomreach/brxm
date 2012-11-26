/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.frontend.plugins.standards.list;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.query.RowIterator;

import org.apache.wicket.Session;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.standards.browse.BrowserSearchResult;
import org.hippoecm.frontend.plugins.standards.list.comparators.NodeComparator;
import org.hippoecm.frontend.plugins.standards.list.datatable.SortState;
import org.hippoecm.frontend.plugins.standards.list.datatable.SortableDataProvider;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchDocumentsProvider extends SortableDataProvider<Node> {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(SearchDocumentsProvider.class);

    private IModel<BrowserSearchResult> bsrModel;
    private Map<String, Comparator<Node>> comparators;
    private transient List<Node> entries;

    public SearchDocumentsProvider(IModel<BrowserSearchResult> model, Map<String, Comparator<Node>> comparators) {
        this.bsrModel = model;
        this.comparators = comparators;
    }

    private void load() {
        if (entries == null) {
            Map<String, Node> handles = new HashMap<String, Node>();
            entries = new LinkedList<Node>();
            BrowserSearchResult result = bsrModel.getObject();
            if (result != null && result.getQueryResult() != null) {
                javax.jcr.Session session = UserSession.get().getJcrSession();
                try {
                    RowIterator rows = result.getQueryResult().getRows();
                    while (rows.hasNext()) {
                        String path = rows.nextRow().getValue("jcr:path").getString();
                        try {
                            Node node = (Node) session.getItem(path);
                            if (node.getDepth() > 0) {
                                Node parent = node.getParent();
                                if (parent.isNodeType(HippoNodeType.NT_HANDLE)) {
                                    if (parent.isNodeType("mix:referenceable")) {
                                        handles.put(parent.getUUID(), parent);
                                    } else {
                                        log.info("Skipping unreferenceable handle " + parent.getPath());
                                    }
                                } else {
                                    entries.add(node);
                                }
                            }
                        } catch (PathNotFoundException ex) {
                            log.info("Could not resolve node from search " + path);
                            continue;
                        }
                    }
                } catch (RepositoryException ex) {
                    log.error(ex.getMessage());
                }
            }
            for (Node handle : handles.values()) {
                entries.add(handle);
            }
            SortState sortState = getSortState();
            if (sortState != null && sortState.isSorted()) {
                String sortProperty = sortState.getProperty();
                if (sortProperty != null) {
                    Comparator<Node> comparator = comparators.get(sortProperty);
                    if (comparator != null) {
                        Collections.sort(entries, comparator);
                        if (sortState.isDescending()) {
                            Collections.reverse(entries);
                        }
                        Collections.sort(entries, new FoldersFirstComparator());
                    }
                }
            }
        }
    }

    public Iterator<Node> iterator(int first, int count) {
        load();
        return entries.subList(first, first + count).iterator();
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
    }

    private static class FoldersFirstComparator extends NodeComparator {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare(JcrNodeModel o1, JcrNodeModel o2) {
            try {
                Node n1 = o1.getNode();
                Node n2 = o2.getNode();
                if (n1 == null) {
                    if (n2 == null) {
                        return 0;
                    }
                    return 1;
                } else if (n2 == null) {
                    return -1;
                }
                String label1 = folderOrDocument(n1);
                String label2 = folderOrDocument(n2);
                return String.CASE_INSENSITIVE_ORDER.compare(label2, label1);
            } catch (RepositoryException e) {
                return 0;
            }
        }

        private String folderOrDocument(Node node) throws RepositoryException {
            String type = "";
            if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                type = node.getPrimaryNodeType().getName();
                NodeIterator nodeIt = node.getNodes();
                while (nodeIt.hasNext()) {
                    Node childNode = nodeIt.nextNode();
                    if (childNode.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                        type = "document";
                        break;
                    }
                }
                if (type.indexOf(":") > -1) {
                    type = "document";
                }
            } else {
                type = "folder";
            }
            return type;
        }
    }
}
