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
package org.hippoecm.frontend.plugins.cms.browse.list;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;

import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.browse.list.comparators.StateComparator;
import org.hippoecm.frontend.plugins.standards.list.AbstractListingPlugin;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentListingPlugin extends AbstractListingPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    static final Logger log = LoggerFactory.getLogger(DocumentListingPlugin.class);
    private static final long serialVersionUID = 1L;

    public DocumentListingPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
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

    @Override
    protected ListDataTable<Node> getListDataTable(String id, TableDefinition<Node> tableDefinition,
            ISortableDataProvider<Node> dataProvider, TableSelectionListener<Node> selectionListener, boolean triState,
            ListPagingDefinition pagingDefinition) {
        return new DraggebleListDataTable(id, tableDefinition, dataProvider, selectionListener, triState,
                pagingDefinition);
    }

    class DraggebleListDataTable extends ListDataTable<Node> {
        private static final long serialVersionUID = 1L;

        public DraggebleListDataTable(String id, TableDefinition<Node> tableDefinition, ISortableDataProvider<Node> dataProvider,
                TableSelectionListener<Node> selectionListener, boolean triState, ListPagingDefinition pagingDefinition) {
            super(id, tableDefinition, dataProvider, selectionListener, triState, pagingDefinition);

            //Don't use drag&drop on documents because 300+ documents in a folder on IE* is slow
            //            add(new DragBehavior(YuiPluginHelper.getManager(getPluginContext()), new DragSettings(YuiPluginHelper
            //                    .getConfig(getPluginConfig()))) {
            //                private static final long serialVersionUID = 1L;
            //
            //                @Override
            //                protected IModel getDragModel() {
            //                    return null;
            //                }
            //            });

            add(new TableHelperBehavior());
        }
    }

}
