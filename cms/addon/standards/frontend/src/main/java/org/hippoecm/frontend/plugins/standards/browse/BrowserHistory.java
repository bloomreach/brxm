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

import java.util.LinkedHashMap;
import java.util.Map;

import javax.jcr.Node;

import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;

class BrowserHistory implements IDetachable {
    private static final long serialVersionUID = 1L;

    static final int MAX_HISTORY = 100;

    // map folder -> state
    private Map<IModel<Node>, BrowserFolderHistory> folderHistories;

    public BrowserHistory() {
        folderHistories = new LinkedHashMap<IModel<Node>, BrowserFolderHistory>();
    }

    public BrowserFolderHistory getFolderHistory(IModel<Node> folder) {
        BrowserFolderHistory folderHistory;
        if (folderHistories.containsKey(folder)) {
            folderHistory = folderHistories.remove(folder);
        } else {
            folderHistory = new BrowserFolderHistory(folder);
        }
        folderHistories.put(folder, folderHistory);
        if (folderHistories.size() > MAX_HISTORY) {
            folderHistories.entrySet().iterator().remove();
        }
        return folderHistories.get(folder);
    }

    public BrowserFolderHistory getFolderHistoryForDocument(IModel<Node> document) {
        BrowserFolderHistory state = null;
        for (Map.Entry<IModel<Node>, BrowserFolderHistory> entry : folderHistories.entrySet()) {
            if (entry.getValue().containsDocument(document)) {
                state = entry.getValue();
            }
        }
        return state;
    }

    public void detach() {
        for (Map.Entry<IModel<Node>, BrowserFolderHistory> entry : folderHistories.entrySet()) {
            entry.getKey().detach();
            entry.getValue().detach();
        }
    }

}