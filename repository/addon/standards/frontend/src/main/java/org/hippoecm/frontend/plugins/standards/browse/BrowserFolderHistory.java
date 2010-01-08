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
package org.hippoecm.frontend.plugins.standards.browse;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;

import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;

class BrowserFolderHistory implements IDetachable {
    private static final long serialVersionUID = 1L;

    private final IModel<Node> folder;
    private IModel<BrowserSearchResult> searchResult;
    private IModel<Node> active;
    private Set<IModel<Node>> documents;
    private Map<IModel<Node>, IModel<Node>> handles;

    BrowserFolderHistory(IModel<Node> folder) {
        this.folder = folder;
        this.documents = new HashSet<IModel<Node>>();
        this.setSearchResult(new AbstractReadOnlyModel<BrowserSearchResult>() {
            private static final long serialVersionUID = 1L;

            @Override
            public BrowserSearchResult getObject() {
                return null;
            }
        });
        this.setActiveDocument(new JcrNodeModel((Node) null));
    }

    public void detach() {
        folder.detach();
        searchResult.detach();
        active.detach();
        for (IModel<Node> document : documents) {
            document.detach();
        }
        if (handles != null) {
            for (Map.Entry<IModel<Node>, IModel<Node>> entry :handles.entrySet()) {
                entry.getKey().detach();
                entry.getValue().detach();
            }
        }
    }

    public void setActiveDocument(IModel<Node> document) {
        internalSetDocument(document);
        if (searchResult.getObject() != null && handles != null && handles.containsKey(document)) {
            searchResult.getObject().setSelectedNode(handles.get(document).getObject());
        }
    }

    public IModel<Node> getActiveDocument() {
        return active;
    }

    public boolean containsDocument(IModel<Node> document) {
        return documents.contains(document);
    }

    public void setSearchResult(IModel<BrowserSearchResult> searchResult) {
        if (this.handles == null && searchResult.getObject() != null) {
            handles = new HashMap<IModel<Node>, IModel<Node>>();
        } else if (this.handles != null && searchResult.getObject() == null) {
            handles = null;
        }
        BrowserSearchResult bsr = searchResult.getObject();
        if (bsr != null) {
            IModel<Node> document = new JcrNodeModel(bsr.getSelectedNode());
            if (document.getObject() != null) {
                JcrNodeModel parentModel = BrowserHelper.getParent(document);
                if (BrowserHelper.isHandle(parentModel)) {
                    handles.put(parentModel, document);
                    internalSetDocument(parentModel);
                } else {
                    internalSetDocument(document);
                }
            } else {
                internalSetDocument(document);
            }
        }
        this.searchResult = searchResult;
    }

    public IModel<BrowserSearchResult> getSearchResult() {
        return searchResult;
    }

    public IModel<Node> getFolder() {
        return folder;
    }

    private void internalSetDocument(IModel<Node> document) {
        this.active = document;
        if (document.getObject() != null) {
            this.documents.add(document);
        }
    }
}