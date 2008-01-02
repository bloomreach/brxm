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
import org.hippoecm.cmsprototype.frontend.plugins.list.datatable.CustomizableDocumentListingDataTable;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;

public class DocumentListingPlugin extends AbstractListingPlugin {

    private static final long serialVersionUID = 1L;
    

    public DocumentListingPlugin(PluginDescriptor pluginDescriptor, JcrNodeModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);
    }

    @Override
    public void addTable(JcrNodeModel nodeModel) {
        dataTable = new CustomizableDocumentListingDataTable("table", columns, new SortableDocumentsProvider(
                nodeModel), DEFAULT_PAGE_SIZE, false);
        dataTable.addBottomPaging(3);
        dataTable.addTopColumnHeaders();
        add((Component)dataTable);
    }

    
    
    
    
    
    
    
}
