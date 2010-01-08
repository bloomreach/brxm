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
package org.hippoecm.frontend.plugins.cms.browse.list;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;

import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.browse.list.comparators.StateComparator;
import org.hippoecm.frontend.plugins.standards.browse.BrowserSearchResult;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.SearchDocumentsProvider;
import org.hippoecm.frontend.plugins.standards.list.TableDefinition;
import org.hippoecm.frontend.plugins.standards.list.comparators.NameComparator;
import org.hippoecm.frontend.plugins.standards.list.comparators.TypeComparator;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListPagingDefinition;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable.TableSelectionListener;
import org.hippoecm.frontend.plugins.standards.list.resolvers.DocumentAttributeModifier;
import org.hippoecm.frontend.plugins.standards.list.resolvers.EmptyRenderer;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IconAttributeModifier;
import org.hippoecm.frontend.plugins.standards.list.resolvers.StateIconAttributeModifier;
import org.hippoecm.frontend.plugins.yui.YuiPluginHelper;
import org.hippoecm.frontend.plugins.yui.tables.TableHelperBehavior;
import org.hippoecm.frontend.service.render.RenderPlugin;

public class SearchDocumentListingPlugin extends RenderPlugin<BrowserSearchResult> implements
        TableSelectionListener<Node> {
    private static final long serialVersionUID = 1L;

    private ListPagingDefinition pagingDefinition;
    private ListDataTable<Node> dataTable;

    public SearchDocumentListingPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        pagingDefinition = new ListPagingDefinition(config);

        dataTable = getListDataTable("table", getTableDefinition(), newDataProvider(), this, pagingDefinition);
        add(dataTable);
        dataTable.init(context);

        BrowserSearchResult bsr = getModelObject();
        if (bsr.getSelectedNode() != null) {
            dataTable.setDefaultModel(new JcrNodeModel(bsr.getSelectedNode()));
        }
    }

    @Override
    public void onModelChanged() {
        dataTable.destroy();

        dataTable = getListDataTable("table", getTableDefinition(), newDataProvider(), this, pagingDefinition);
        replace(dataTable);
        dataTable.init(getPluginContext());
        BrowserSearchResult bsr = getModelObject();
        if (bsr.getSelectedNode() != null) {
            dataTable.setDefaultModel(new JcrNodeModel(bsr.getSelectedNode()));
        }
        redraw();
    }

    public void selectionChanged(IModel<Node> model) {
        BrowserSearchResult bsr = getModelObject();
        bsr.setSelectedNode(model.getObject());
        dataTable.setDefaultModel(model);
    }

    protected TableDefinition<Node> getTableDefinition() {
        List<ListColumn<Node>> columns = new ArrayList<ListColumn<Node>>();

        ListColumn<Node> column = new ListColumn<Node>(new Model(""), "icon");
        column.setComparator(new TypeComparator());
        column.setRenderer(new EmptyRenderer());
        column.setAttributeModifier(new IconAttributeModifier());
        columns.add(column);

        column = new ListColumn<Node>(new StringResourceModel("doclisting-name", this, null), "name");
        column.setComparator(new NameComparator());
        column.setAttributeModifier(new DocumentAttributeModifier());
        columns.add(column);

        column = new ListColumn<Node>(new StringResourceModel("doclisting-state", this, null), "state");
        column.setComparator(new StateComparator());
        column.setRenderer(new EmptyRenderer());
        column.setAttributeModifier(new StateIconAttributeModifier());
        columns.add(column);

        return new TableDefinition<Node>(columns);
    }

    protected ISortableDataProvider<Node> newDataProvider() {
        return new SearchDocumentsProvider(getModel(), getTableDefinition().getComparators());
    }

    protected ListDataTable<Node> getListDataTable(String id, TableDefinition<Node> tableDefinition,
            ISortableDataProvider<Node> dataProvider, TableSelectionListener<Node> selectionListener,
            ListPagingDefinition pagingDefinition) {
        ListDataTable<Node> table = new ListDataTable<Node>(id, tableDefinition, dataProvider, selectionListener,
                false, pagingDefinition);
        table.add(new TableHelperBehavior(YuiPluginHelper.getManager(getPluginContext())));
        return table;
    }

}
