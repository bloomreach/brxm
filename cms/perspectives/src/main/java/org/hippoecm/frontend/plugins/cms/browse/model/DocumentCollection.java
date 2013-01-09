/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.cms.browse.model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.IChangeListener;
import org.hippoecm.frontend.plugins.standards.browse.BrowserSearchResult;

public class DocumentCollection implements IDetachable {
    private static final long serialVersionUID = 1L;

    public enum DocumentCollectionType {
        FOLDER, SEARCHRESULT, UNKNOWN
    }

    private List<IChangeListener> listeners;
    private IModel<Node> folder;
    private IModel<BrowserSearchResult> searchResult;

    public DocumentCollection() {
        this.listeners = new LinkedList<IChangeListener>();
    }

    public DocumentCollectionType getType() {
        if (getSearchResult() != null && getSearchResult().getObject() != null) {
            return DocumentCollectionType.SEARCHRESULT;
        }
        if (getFolder() != null && getFolder().getObject() != null) {
            return DocumentCollectionType.FOLDER;
        }
        return DocumentCollectionType.UNKNOWN;
    }

    public String getCategory() {
        switch (getType()) {
        case FOLDER:
            try {
                return getFolder().getObject().getPrimaryNodeType().getName();
            } catch (RepositoryException e) {
                return "unknown";
            }
        case SEARCHRESULT:
            return getSearchResult().getObject().getQueryName();
        default:
            return "unknown";
        }
    }

    public IModel<Node> getFolder() {
        return folder;
    }

    public void setFolder(IModel<Node> folder) {
        this.folder = folder;
        notifyListeners();
    }

    public IModel<BrowserSearchResult> getSearchResult() {
        return searchResult;
    }

    public void setSearchResult(IModel<BrowserSearchResult> searchResult) {
        this.searchResult = searchResult;
        notifyListeners();
    }

    public boolean isOrdered() {
        return true;
    }

    public void addListener(IChangeListener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(IChangeListener listener) {
        this.listeners.remove(listener);
    }

    private void notifyListeners() {
        for (IChangeListener listener : new ArrayList<IChangeListener>(listeners)) {
            listener.onChange();
        }
    }

    public void detach() {
        if (folder != null) {
            folder.detach();
        }
        if (searchResult != null) {
            searchResult.detach();
        }
    }

}
