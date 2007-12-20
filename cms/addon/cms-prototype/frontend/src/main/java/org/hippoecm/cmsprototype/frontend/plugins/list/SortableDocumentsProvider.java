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
import javax.jcr.RepositoryException;

import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.hippoecm.cmsprototype.frontend.model.content.Document;
import org.hippoecm.cmsprototype.frontend.model.content.Folder;
import org.hippoecm.frontend.model.JcrNodeModel;

/**
 * For a given JCR node, provides the document handles among its children.
 *
 * TODO: it's not sortable yet
 */
public class SortableDocumentsProvider extends SortableDataProvider {

    private static final long serialVersionUID = 1L;
    
    Folder folder;
    
    public SortableDocumentsProvider(JcrNodeModel model) {
        this.folder = new Folder(model);
        //setSort("name", true);
    }

    public Iterator<Node> iterator(int first, int count) {
        // TODO replace with a more efficient implementation
        List<Node> list = new ArrayList<Node>();
        int i = 0;
        for (Iterator<Document> documents = folder.getDocuments().iterator(); documents.hasNext(); i++) {
            Document doc = documents.next();
            if (i >= first && i < (first + count)) {
                list.add(doc.getNodeModel().getNode());
            }
        }
        return list.iterator();
    }

    public IModel model(Object object) {
        return new JcrNodeModel(folder.getNodeModel(), (Node) object);
    }

    public int size() {
        return folder.getDocuments().size();
    }

    /*
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
    */

    
}
