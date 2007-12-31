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

import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IStyledColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Notification;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;

public class DocumentListingPlugin extends Plugin {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private static final long serialVersionUID = 1L;
    
    AjaxFallbackDefaultDataTable dataTable;
    List<IStyledColumn> columns;

    public DocumentListingPlugin(PluginDescriptor pluginDescriptor, JcrNodeModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);

        columns = new ArrayList<IStyledColumn>();
        columns.add(new NodeColumn(new Model("Name"), "name", "name", pluginDescriptor.getIncoming()));
        columns.add(new PropertyColumn(new Model("Type"), "name"));
        columns.add(new PropertyColumn(new Model("Date"), "name"));
        columns.add(new PropertyColumn(new Model("State"), "name"));
        
        // TODO replace with CustomizableDocumentListingDataTable  
        dataTable = new AjaxFallbackDefaultDataTable("table", columns, new SortableDocumentsProvider(model), DEFAULT_PAGE_SIZE);
        add(dataTable);
    }
    
    @Override
    public void receive(Notification notification) {
        if ("select".equals(notification.getOperation())) {
            JcrNodeModel nodeModel = new JcrNodeModel(notification.getData());
            HippoNode node = nodeModel.getNode();
            try {
	            if (!nodeModel.equals(getModel())
	            		&& !node.isNodeType(HippoNodeType.NT_DOCUMENT)
	            		&& !node.isNodeType(HippoNodeType.NT_HANDLE)) {
	                setModel(nodeModel);
	                remove(dataTable);
	                dataTable = new AjaxFallbackDefaultDataTable("table", columns, new SortableDocumentsProvider(
	                        nodeModel), 10);
	                add(dataTable);
	                notification.getContext().addRefresh(this);
	            }
            } catch(RepositoryException ex) {
            	ex.printStackTrace();
            }
        }
        // don't propagate the notification to children
    }
    
}
