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
package org.hippoecm.frontend.translation.components.folder;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.hippoecm.frontend.translation.components.TestLocaleProvider;
import org.hippoecm.frontend.translation.components.folder.model.T9Node;
import org.hippoecm.frontend.translation.components.folder.model.T9Tree;

public class FolderTranslationPage extends WebPage {

    public FolderTranslationPage(PageParameters parameters) {
        StringValue folderValue = parameters.get("folder");
        if (folderValue.isNull()) {
            folderValue = StringValue.valueOf("evenements");
        }
        LoadableDetachableModel<T9Tree> treeModel = new LoadableDetachableModel<T9Tree>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected T9Tree load() {
                return new JsonT9Tree();
            }
        };
        IModel<T9Node> t9NodeModel = new Model<T9Node>(treeModel.getObject().getNode(folderValue.toString()));
        add(new FolderTranslationView("grid", treeModel, t9NodeModel, new TestLocaleProvider()));
    }
}
