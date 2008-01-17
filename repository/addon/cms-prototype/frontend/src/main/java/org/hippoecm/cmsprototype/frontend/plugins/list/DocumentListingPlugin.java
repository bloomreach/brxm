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

import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IStyledColumn;
import org.apache.wicket.model.Model;
import org.hippoecm.cmsprototype.frontend.model.content.Document;
import org.hippoecm.cmsprototype.frontend.model.content.Folder;
import org.hippoecm.cmsprototype.frontend.model.exception.ModelWrapException;
import org.hippoecm.cmsprototype.frontend.plugins.list.datatable.CustomizableDocumentListingDataTable;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Channel;

public class DocumentListingPlugin extends AbstractListingPlugin {

    private static final long serialVersionUID = 1L;

    public static final String USER_PREF_NODENAME = "hippo:browseperspective-listingview";

    public DocumentListingPlugin(PluginDescriptor pluginDescriptor, JcrNodeModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);
    }

    @Override
    protected void addTable(JcrNodeModel nodeModel, int pageSize, int viewSize) {
        Folder folder = null;
        try {
            folder = new Folder(nodeModel);
        } catch (ModelWrapException e) {
            // node is not a folder or in a folder
        }
        
        Document selectedDocument = null;
        try {
            selectedDocument = new Document(nodeModel);
        } catch (ModelWrapException e) {
            // node is not a document or document variant
        }
        
        dataTable = new CustomizableDocumentListingDataTable("table", columns, new SortableDocumentsProvider(folder), pageSize, false);
        dataTable.addBottomPaging(viewSize);
        dataTable.addTopColumnHeaders();
        if (selectedDocument != null) {
            dataTable.setSelectedNode(selectedDocument.getNodeModel());
        }
        add((Component)dataTable);
    }

    @Override
    protected String getPluginUserPrefNodeName() {
        return USER_PREF_NODENAME;
    }

    @Override
    protected IStyledColumn getNodeColumn(Model model, String propertyName, Channel incoming) {
        return new DocumentListingNodeColumn(model, propertyName, incoming);
    }


}
