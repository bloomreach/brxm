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
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IStyledColumn;
import org.apache.wicket.model.Model;
import org.hippoecm.cmsprototype.frontend.plugins.generic.list.AbstractListingPlugin;
import org.hippoecm.cmsprototype.frontend.plugins.generic.list.datatable.CustomizableDocumentListingDataTable;
import org.hippoecm.cmsprototype.frontend.plugins.generic.list.datatable.ICustomizableDocumentListingDataTable;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.Notification;

public class DocumentListingPlugin extends AbstractListingPlugin {

    private static final long serialVersionUID = 1L;

    public static final String USER_PREF_NODENAME = "browseperspective-listingview";

    public DocumentListingPlugin(PluginDescriptor pluginDescriptor, IPluginModel model, Plugin parentPlugin) {
        super(pluginDescriptor, new JcrNodeModel(model), parentPlugin);
    }

    @Override
    protected ICustomizableDocumentListingDataTable getTable(IPluginModel model) {
        Map map = model.getMapRepresentation();
        
        List<String> children = (List<String>) map.get("children");
        List<NodeModelWrapper> listEntries = new ArrayList<NodeModelWrapper>();
        if (children != null) {
            for (String child : children) {
                listEntries.add(new ListEntry(new JcrNodeModel(child)));
            }
        }

        String parentPath = (String) map.get("parent");
        ListEntry parent = new ListEntry(new JcrNodeModel(parentPath));

        SortableDocumentsProvider documentsProvider = new SortableDocumentsProvider(parent, listEntries); 
        dataTable = new CustomizableDocumentListingDataTable("table", columns, documentsProvider, pageSize, false);
        dataTable.addBottomPaging(viewSize);
        dataTable.addTopColumnHeaders();
        return dataTable;
    }

    @Override
    public void receive(Notification notification) {
        if ("relatives".equals(notification.getOperation())) {
            IPluginModel pluginModel = notification.getModel();
            
            remove((Component) dataTable);
            add((Component) getTable(pluginModel));
            
            notification.getContext().addRefresh(this);
        }
    }

    @Override
    protected String getPluginUserPrefNodeName() {
        return USER_PREF_NODENAME;
    }

    @Override
    protected IStyledColumn getNodeColumn(Model model, String propertyName, Channel channel) {
        return new DocumentListingNodeColumn(model, propertyName, channel);
    }

    private class ListEntry extends NodeModelWrapper {
        private static final long serialVersionUID = 1L;

        public ListEntry(JcrNodeModel nodeModel) {
            super(nodeModel);
        }
    }

}
