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
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;

public class SortableDocumentHandlesProvider extends SortableDataProvider {

    private static final long serialVersionUID = 1L;
    
    JcrNodeModel model;
    
    public SortableDocumentHandlesProvider(JcrNodeModel model) {
        this.model = model;
        //setSort("name", true);
    }

    public Iterator<Node> iterator(int first, int count) {
        List<Node> list = new ArrayList<Node>();
        int i = 0;
        for (Iterator<Node> nodes = documents().iterator(); nodes.hasNext(); i++) {
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
        return documents().size();
    }
    
    private List<Node> documents() {
        Node node = model.getNode();
        List<Node> childNodes = new ArrayList<Node>();
        try {
            NodeIterator jcrChildren = node.getNodes();
            while (jcrChildren.hasNext()) {
                Node jcrChild = jcrChildren.nextNode();
                if (jcrChild != null ) {
                    NodeType nodeType = jcrChild.getPrimaryNodeType();
                    if ((HippoNodeType.NT_HANDLE.equals(nodeType.getName()))) {
                        childNodes.add(jcrChild);
                    }
                }
            }
        }
        catch (RepositoryException e) {
            e.printStackTrace();
        }
        return childNodes;
    }
    

    
}
