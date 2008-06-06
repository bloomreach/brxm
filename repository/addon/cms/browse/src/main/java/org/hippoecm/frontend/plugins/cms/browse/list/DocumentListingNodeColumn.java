/*
 * Copyright 2008 Hippo
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
package org.hippoecm.frontend.plugins.cms.browse.list;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.standards.sa.list.NodeCell;
import org.hippoecm.frontend.plugins.standards.sa.list.NodeColumn;

public class DocumentListingNodeColumn extends NodeColumn {
    private static final long serialVersionUID = 1L;

    private DocumentListingPlugin plugin;
    
    public DocumentListingNodeColumn(IModel displayModel, String nodePropertyName, DocumentListingPlugin plugin) {
        super(displayModel, nodePropertyName);
        this.plugin = plugin;
    }

    @Override
    protected NodeCell getNodeCell(String componentId, IModel model, String nodePropertyName) {
        return new DocumentListingNodeCell(componentId, (JcrNodeModel) model, nodePropertyName, plugin);
    }

}
