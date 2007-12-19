/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.cmsprototype.frontend.plugins.list;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;

/**
 * For a given JCR node, provides the document handles among its children.
 *
 * TODO: it's not sortable yet
 */
public class SortableDocumentHandlesProvider extends SortableDataProvider {

    private static final long serialVersionUID = 1L;
    
    JcrNodeModel model;
    transient List<Node> documents;
    
    public SortableDocumentHandlesProvider(JcrNodeModel model) {
        this.model = model;
        documents = documents();
        setSort("name", true);
    }

    public Iterator<Node> iterator(int first, int count) {
        // TODO replace with a more efficient implementation
        ensureDocumentsAreLoaded();
        sortDocuments();
        List<Node> list = new ArrayList<Node>();
        int i = 0;
        for (Iterator<Node> nodes = documents.iterator(); nodes.hasNext(); i++) {
            Node node = nodes.next();
            if (i >= first && i < (first + count)) {
                list.add(node);
            }
        }
        return list.iterator();
    }

    public IModel model(Object object) {
        return new JcrNodeModel(model, (HippoNode) object);
    }

    public int size() {
        ensureDocumentsAreLoaded();
        return documents.size();
    }
    
    private void ensureDocumentsAreLoaded() {
        if (documents == null) {
            documents = documents();
        }
    }
    
    private List<Node> documents() {
        HippoNode node = model.getNode();
        List<Node> childNodes = new ArrayList<Node>();
        try {
            NodeIterator jcrChildren = node.getNodes();
            while (jcrChildren.hasNext()) {
                HippoNode jcrChild = (HippoNode) jcrChildren.nextNode();
                if (jcrChild != null ) {
                    if (jcrChild.isNodeType(HippoNodeType.NT_HANDLE)) {
                        childNodes.add(jcrChild);
                        System.out.println("adding " + jcrChild.getPath());
                    }
                    
                    // handle facet result nodes
                    else if (jcrChild.isNodeType(HippoNodeType.NT_FACETRESULT)) {
                        NodeIterator fsChildren = jcrChild.getNodes();
                        while (fsChildren.hasNext()) {
                            HippoNode fsChild = (HippoNode) fsChildren.next();
                            if (fsChild != null && fsChild.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                                Node canonicalNode = fsChild.getCanonicalNode();
                                Node parentNode = canonicalNode.getParent();
                                if (parentNode != null && parentNode.isNodeType(HippoNodeType.NT_HANDLE)) {
                                    childNodes.add(parentNode);
                                }
                            }
                        }
                    }
                }
            }
        }
        catch (RepositoryException e) {
            e.printStackTrace();
        }

        // remove duplicates (if any) :-\
        Set<Node> set = new HashSet<Node>();
        set.addAll(childNodes);
        childNodes.clear();
        childNodes.addAll(set);
        
        return childNodes;
    }
    
    private void sortDocuments() {
        Collections.sort(documents, new Comparator<Node>() {

            public int compare(Node o1, Node o2) {
                try {
                    return String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName());
                } catch (RepositoryException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    return 0;
                }
            }
        });
        
        if (getSort().isAscending() == false) {
            Collections.reverse(documents);
        }
    }

    
}
