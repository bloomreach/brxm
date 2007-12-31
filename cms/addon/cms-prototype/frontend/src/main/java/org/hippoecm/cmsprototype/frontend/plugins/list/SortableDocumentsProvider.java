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
import java.util.Iterator;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.hippoecm.cmsprototype.frontend.model.content.Folder;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;

/**
 * For a given JCR node, provides the document handles among its children.
 *
 * TODO: it's not sortable yet
 */
public class SortableDocumentsProvider extends SortableDataProvider {

    private static final long serialVersionUID = 1L;
    
    Folder folder;
    List<NodeModelWrapper> resources;
    
    public SortableDocumentsProvider(JcrNodeModel model) {
        this.folder = new Folder(model);
        setSort("name", true);
    }

    public Iterator<NodeModelWrapper> iterator(int first, int count) {
        // TODO replace with a more efficient implementation
        List<NodeModelWrapper> list = new ArrayList<NodeModelWrapper>();
        resources = new ArrayList<NodeModelWrapper>();
        resources.addAll(folder.getSubFoldersAndDocuments());
        sortResources();
        int i = 0;
        for (Iterator<NodeModelWrapper> iterator = resources.iterator(); iterator.hasNext(); i++) {
            NodeModelWrapper doc = iterator.next();
            if (i >= first && i < (first + count)) {
                list.add(doc);
            }
        }
        return list.iterator();
    }

    public IModel model(Object object) {
        return (NodeModelWrapper) object;
    }

    public int size() {
        return folder.getSubFoldersAndDocuments().size();
    }
    
    private void sortResources() {
        Collections.sort(resources, new Comparator<NodeModelWrapper>() {

            public int compare(NodeModelWrapper o1, NodeModelWrapper o2) {
                    try {
                        return String.CASE_INSENSITIVE_ORDER.compare(o1.getNodeModel().getNode().getName(), o2.getNodeModel().getNode().getName());
                    } catch (RepositoryException e) {
                        return 0;
                    }
            }
        });
        
        if (getSort().isAscending() == false) {
            Collections.reverse(resources);
        }
    }

}
