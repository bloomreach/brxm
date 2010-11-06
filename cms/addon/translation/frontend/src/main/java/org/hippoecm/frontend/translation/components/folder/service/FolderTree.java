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
package org.hippoecm.frontend.translation.components.folder.service;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.translation.components.folder.model.T9Node;
import org.hippoecm.frontend.translation.components.folder.model.T9Tree;
import org.wicketstuff.js.ext.tree.ExtTreeLoader;
import org.wicketstuff.js.ext.util.ExtClass;

@ExtClass("Ext.ux.tree.TreeGridLoader")
public final class FolderTree extends ExtTreeLoader {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private final String language;
    private final IModel<T9Tree> data;

    public FolderTree(IModel<T9Tree> data, String language) {
        this.data = data;
        this.language = language;
    }

    @Override
    public List<FolderNode> getChildren(String id) {
        T9Tree tree = data.getObject();
        List<T9Node> children = tree.getChildren(id);
        List<FolderNode> result = new LinkedList<FolderNode>();
        if (children != null) {
            for (T9Node node : children) {
                FolderNode folderNode = new FolderNode(node, language);
                if (folderNode.getT9id() != null) {
                    List<T9Node> siblings = tree.getSiblings(folderNode.getT9id());
                    List<String> languages = new ArrayList<String>();
                    for (T9Node sibling : siblings) {
                        languages.add(sibling.getLang());
                    }
                    folderNode.setT9ns(languages);
                }
                result.add(folderNode);
            }
        }
        return result;
    }

}
