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

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.hippoecm.cmsprototype.frontend.model.content.Document;
import org.hippoecm.cmsprototype.frontend.model.content.Folder;
import org.hippoecm.cmsprototype.frontend.model.exception.ModelWrapException;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides the {@link Folder}s and {@link Document}s contained within a given {@link Folder}.
 *
 */
public class SortableDocumentsProvider extends SortableDataProvider {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(SortableDocumentsProvider.class);

    Folder folder;
    List<NodeModelWrapper> resources;

    public SortableDocumentsProvider(Folder folder) {
        this.folder = folder;
        setSort("name", true);
        
        resources = folder.getSubFoldersAndDocuments();
        Folder parentFolder = folder.getParentFolder();
        if (parentFolder != null) {
            try {
                resources.add(new DocumentListingParentFolder(parentFolder.getNodeModel()));
            } catch (ModelWrapException e) { }
        }
    }
    
    public Iterator<NodeModelWrapper> iterator(int first, int count) {
        sortResources();
        List<NodeModelWrapper> list = Collections.unmodifiableList(resources.subList(first, first + count));
        return list.iterator();
    }

    public IModel model(Object object) {
        return (NodeModelWrapper) object;
    }

    public int size() {
        return resources.size();
    }

    private void sortResources() {
        Collections.sort(resources, new Comparator<NodeModelWrapper>() {

            public int compare(NodeModelWrapper o1, NodeModelWrapper o2) {
                
                // parent folder always on top
                if ((o1 instanceof DocumentListingParentFolder 
                            && SortableDocumentsProvider.this.getSort().isAscending())
                        || (o2 instanceof DocumentListingParentFolder 
                                && !SortableDocumentsProvider.this.getSort().isAscending())) {
                    return Integer.MIN_VALUE;
                }
                else if ((o2 instanceof DocumentListingParentFolder 
                                && SortableDocumentsProvider.this.getSort().isAscending())
                            || (o1 instanceof DocumentListingParentFolder 
                                    && !SortableDocumentsProvider.this.getSort().isAscending())) {
                    return Integer.MAX_VALUE;
                }
                
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
