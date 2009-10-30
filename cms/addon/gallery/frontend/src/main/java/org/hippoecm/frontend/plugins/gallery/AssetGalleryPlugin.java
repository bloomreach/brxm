/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.plugins.gallery;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.list.AbstractListingPlugin;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.TableDefinition;
import org.hippoecm.frontend.plugins.standards.list.comparators.NameComparator;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListPagingDefinition;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable.TableSelectionListener;
import org.hippoecm.frontend.plugins.standards.list.resolvers.EmptyRenderer;
import org.hippoecm.frontend.plugins.yui.YuiPluginHelper;
import org.hippoecm.frontend.plugins.yui.dragdrop.DragSettings;
import org.hippoecm.frontend.plugins.yui.dragdrop.NodeDragBehavior;
import org.hippoecm.frontend.plugins.yui.tables.TableHelperBehavior;

public class AssetGalleryPlugin extends AbstractListingPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    public AssetGalleryPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    public TableDefinition getTableDefinition() {
        List<ListColumn> columns = new ArrayList<ListColumn>();

        ListColumn column = new ListColumn(new StringResourceModel("assetgallery-type", this, null), "type");
        column.setRenderer(new EmptyRenderer());
        column.setAttributeModifier(new MimeTypeAttributeModifier());
        column.setComparator(new MimeTypeComparator());
        columns.add(column);

        column = new ListColumn(new StringResourceModel("assetgallery-name", this, null), "name");
        column.setComparator(new NameComparator());
        columns.add(column);

        column = new ListColumn(new StringResourceModel("assetgallery-size", this, null), "size");
        column.setRenderer(new SizeRenderer());
        column.setComparator(new SizeComparator());
        columns.add(column);

        return new TableDefinition(columns);
    }

    @Override
    protected ListDataTable getListDataTable(String id, TableDefinition tableDefinition,
            ISortableDataProvider dataProvider, TableSelectionListener selectionListener, boolean triState,
            ListPagingDefinition pagingDefinition) {
        return new DraggableListDataTable(id, tableDefinition, dataProvider, selectionListener, triState,
                pagingDefinition);
    }

    class DraggableListDataTable extends ListDataTable {
        private static final long serialVersionUID = 1L;

        public DraggableListDataTable(String id, TableDefinition tableDefinition, ISortableDataProvider dataProvider,
                TableSelectionListener selectionListener, boolean triState, ListPagingDefinition pagingDefinition) {
            super(id, tableDefinition, dataProvider, selectionListener, triState, pagingDefinition);
            
            add(new TableHelperBehavior(YuiPluginHelper.getManager(getPluginContext())));
        }

        @Override
        protected Item newRowItem(String id, int index, IModel model) {
            Item item = super.newRowItem(id, index, model);
            if (model instanceof JcrNodeModel) {
                JcrNodeModel nodeModel = (JcrNodeModel) model;
                item.add(new NodeDragBehavior(YuiPluginHelper.getManager(getPluginContext()), new DragSettings(
                        YuiPluginHelper.getConfig(getPluginConfig())), nodeModel));
            }
            return item;
        }
    }
}
