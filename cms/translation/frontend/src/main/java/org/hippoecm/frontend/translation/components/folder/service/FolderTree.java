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
package org.hippoecm.frontend.translation.components.folder.service;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.translation.ILocaleProvider;
import org.hippoecm.frontend.translation.ILocaleProvider.HippoLocale;
import org.hippoecm.frontend.translation.components.folder.model.T9Node;
import org.hippoecm.frontend.translation.components.folder.model.T9Tree;
import org.wicketstuff.js.ext.tree.ExtTreeLoader;
import org.wicketstuff.js.ext.util.ExtClass;

@ExtClass("Ext.ux.tree.TreeGridLoader")
public final class FolderTree extends ExtTreeLoader {

    private static final long serialVersionUID = 1L;

    private final ILocaleProvider provider;
    private final String language;
    private final IModel<T9Tree> data;

    public FolderTree(IModel<T9Tree> data, String language, ILocaleProvider provider) {
        this.data = data;
        this.language = language;
        this.provider = provider;
    }

    @Override
    public List<FolderNode> getChildren(String id) {
        T9Tree tree = data.getObject();

        String parentLang = null;
        T9Node parent = tree.getNode(id);
        while (parent != null) {
            if (parent.getLang() != null) {
                parentLang = parent.getLang();
                break;
            }
            parent = parent.getParent();
        }

        List<T9Node> children = tree.getChildren(id);
        List<FolderNode> result = new LinkedList<FolderNode>();
        if (children != null) {
            for (T9Node node : children) {
                FolderNode folderNode = new FolderNode(node, language);
                if (folderNode.getT9id() != null) {
                    String nodeLanguage = folderNode.getLang();

                    List<T9Node> siblings = tree.getSiblings(folderNode.getT9id());
                    List<String> languages = new ArrayList<String>();
                    for (T9Node sibling : siblings) {
                        String siblingLang = sibling.getLang();
                        if (siblingLang.equals(nodeLanguage)) {
                            continue;
                        }
                        languages.add(siblingLang);
                    }
                    folderNode.setT9ns(languages);

                    if (parentLang == null) {
                        HippoLocale locale = provider.getLocale(nodeLanguage);
                        if (!"".equals(locale.getLocale().getCountry())) {
                            folderNode.setIconCls("hippo-translation-country-" + locale.getLocale().getCountry().toLowerCase());
                        }
                    }
                }
                result.add(folderNode);
            }
        }
        return result;
    }

}
