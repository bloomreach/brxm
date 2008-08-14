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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.IClusterable;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.standards.list.comparators.NameComparator;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentsProvider extends SortableDataProvider {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(DocumentsProvider.class);
    
    private List<JcrNodeModel> entries = new ArrayList<JcrNodeModel>();
    private Map<String, Comparator> comparators;

    public DocumentsProvider(JcrNodeModel model, Map<String, Comparator> comparators) {
        this.comparators = comparators;
        Node node = model.getNode();
        try {
            while (node != null) {
                if (!(node.isNodeType(HippoNodeType.NT_DOCUMENT) && !node.isNodeType("hippostd:folder"))
                        && !node.isNodeType(HippoNodeType.NT_HANDLE) && !node.isNodeType(HippoNodeType.NT_TEMPLATETYPE)
                        && !node.isNodeType(HippoNodeType.NT_REQUEST) && !node.isNodeType("rep:root")) {
                    NodeIterator childNodesIterator = node.getNodes();
                    while (childNodesIterator.hasNext()) {
                        entries.add(new JcrNodeModel(childNodesIterator.nextNode()));
                    }
                    break;
                }
                if (!node.isNodeType("rep:root")) {
                    model = model.getParentModel();
                    node = model.getNode();
                } else {
                    break;
                }
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
        Collections.sort(entries, new NameComparator());
        Collections.sort(entries, new FoldersFirstComparator());
    }

    public Iterator iterator(int first, int count) {
        SortParam sortParam = getSort();
        if (sortParam != null) {
            String sortProperty = sortParam.getProperty();
            if (sortProperty != null) {
                Comparator comparator = comparators.get(sortProperty);
                if (comparator != null) {
                    Collections.sort(entries, comparator);
                    if (getSort().isAscending() == false) {
                        Collections.reverse(entries);
                    }
                    Collections.sort(entries, new FoldersFirstComparator());
                }
            }
        }
        return Collections.unmodifiableList(entries.subList(first, first + count)).iterator();
    }

    public IModel model(Object object) {
        return (JcrNodeModel) object;
    }

    public int size() {
        return entries.size();
    }

    @Override
    public void detach() {
        for (IModel entry : entries) {
            entry.detach();
        }
        super.detach();
    }
    
    
    private class FoldersFirstComparator implements Comparator<JcrNodeModel>, IClusterable {
        @SuppressWarnings("unused")
        private final static String SVN_ID = "$Id$";

        private static final long serialVersionUID = 1L;

        public int compare(JcrNodeModel o1, JcrNodeModel o2) {
            try {
                HippoNode n1 = o1.getNode();
                HippoNode n2 = o2.getNode();
                if (n1 == null) {
                    if (n2 == null) {
                        return 0;
                    }
                    return 1;
                } else if (o2 == null) {
                    return -1;
                }
                String label1 = folderOrDocument(n1);
                String label2 = folderOrDocument(n2);
                return String.CASE_INSENSITIVE_ORDER.compare(label2, label1);
            } catch (RepositoryException e) {
                return 0;
            }
        }
        
        private String folderOrDocument(HippoNode node) throws RepositoryException {
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

