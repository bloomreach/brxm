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
package org.hippoecm.frontend.plugins.cms.browse.list;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IStyledColumn;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.hippoecm.frontend.model.tree.AbstractTreeNode;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugins.standards.list.generic.AbstractListingPlugin;
import org.hippoecm.frontend.plugins.standards.list.generic.datatable.CustomizableDocumentListingDataTable;
import org.hippoecm.frontend.plugins.standards.list.generic.datatable.ICustomizableDocumentListingDataTable;

public class DocumentListingPlugin extends AbstractListingPlugin {

    private static final long serialVersionUID = 1L;

    public static final String USER_PREF_NODENAME = "browseperspective-listingview";

    public DocumentListingPlugin(PluginDescriptor pluginDescriptor, IPluginModel model, Plugin parentPlugin) {
        super(pluginDescriptor, new JcrNodeModel(model), parentPlugin);
    }

    @Override
    protected ICustomizableDocumentListingDataTable getTable(IPluginModel model) {
        List<NodeModelWrapper> listEntries = new ArrayList<NodeModelWrapper>();
        if (model instanceof AbstractTreeNode) {
            AbstractTreeNode treeNode = (AbstractTreeNode) model;
            Enumeration<AbstractTreeNode> children = treeNode.children();
            while (children.hasMoreElements()) {
                AbstractTreeNode child = children.nextElement();
                listEntries.add(child);
            }
        }
        SortableDocumentsProvider documentsProvider = new SortableDocumentsProvider(listEntries);
        dataTable = new CustomizableDocumentListingDataTable("table", columns, documentsProvider, pageSize, false);
        dataTable.addBottomPaging(viewSize);
        dataTable.addTopColumnHeaders();
        return dataTable;
    }

    @Override
    protected String getPluginUserPrefNodeName() {
        return USER_PREF_NODENAME;
    }

    @Override
    protected IStyledColumn getNodeColumn(Model model, String propertyName, Channel channel) {
        return new DocumentListingNodeColumn(model, propertyName, channel);
    }

}
