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
package org.hippoecm.frontend.plugins.standards.list;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.EventCollection;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugins.standards.DocumentListFilter;
import org.hippoecm.frontend.plugins.standards.list.comparators.NodeComparator;
import org.hippoecm.frontend.plugins.standards.list.datatable.SortState;
import org.hippoecm.frontend.plugins.standards.list.datatable.SortableDataProvider;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentsProvider extends SortableDataProvider<Node> implements IObservable {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(DocumentsProvider.class);

    private IModel<Node> folder;
    private DocumentListFilter filter;
    private Map<String, Comparator<Node>> comparators;

    private boolean observing = false;
    private IObservationContext obContext;
    private Map<IModel<Node>, IObserver> observers;
    private transient List<Node> entries = null;

    public DocumentsProvider(IModel<Node> model, DocumentListFilter filter, Map<String, Comparator<Node>> comparators) {
        this.folder = model;
        this.filter = filter;
        this.comparators = comparators;
        this.observers = new HashMap<IModel<Node>, IObserver>();
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
        for (Map.Entry<IModel<Node>, IObserver> entry : observers.entrySet()) {
            entry.getKey().detach();
        }
        entries = null;
    }

    private void load() {
        if (entries != null) {
            return;
        }

        Set<JcrNodeModel> observed = new HashSet<JcrNodeModel>();
        entries = new ArrayList<Node>();
        Node node = folder.getObject();
        if (node != null) {
            try {
                NodeIterator subNodes = filter.filter(node, node.getNodes());
                while (subNodes.hasNext()) {
                    Node subNode = subNodes.nextNode();
                    // Skip deleted documents
                    if (subNode.isNodeType(HippoNodeType.NT_HANDLE)) {
                        if (!subNode.hasNode(subNode.getName())) {
                            observed.add(new JcrNodeModel(subNode));
                            continue;
                        }
                    }
                    // skip translations
                    if (subNode.isNodeType(HippoNodeType.NT_TRANSLATION)) {
                        continue;
                    }
                    entries.add(subNode);
                }
            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }
        } else {
            log.info("Jcr node in JcrNodeModel is null, returning empty list");
        }

        for (Iterator<Map.Entry<IModel<Node>, IObserver>> iter = observers.entrySet().iterator(); iter.hasNext();) {
            Map.Entry<IModel<Node>, IObserver> entry = iter.next();
            if (!observed.contains(entry.getKey())) {
                iter.remove();
            }
        }
        for (final JcrNodeModel model : observed) {
            if (!observers.containsKey(model)) {
                IObserver observer = new IObserver() {
                    private static final long serialVersionUID = 1L;

                    public IObservable getObservable() {
                        return model;
                    }

                    public void onEvent(Iterator events) {
                        if (obContext != null) {
                            obContext.notifyObservers(new EventCollection(events));
                        }
                    }

                };
                if (observing) {
                    obContext.registerObserver(observer);
                }
                observers.put(model, observer);
            }
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

    public void setObservationContext(IObservationContext<? extends IObservable> context) {
        this.obContext = context;
    }

    public void startObservation() {
        observing = true;
        for (Map.Entry<IModel<Node>, IObserver> entry : observers.entrySet()) {
            obContext.registerObserver(entry.getValue());
        }
    }

    public void stopObservation() {
        for (Map.Entry<IModel<Node>, IObserver> entry : observers.entrySet()) {
            obContext.unregisterObserver(entry.getValue());
        }
        observing = false;
    }

}
