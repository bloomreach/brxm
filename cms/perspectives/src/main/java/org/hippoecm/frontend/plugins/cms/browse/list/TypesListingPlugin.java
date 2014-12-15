/*
 *  Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.editor.type.JcrDraftLocator;
import org.hippoecm.editor.type.JcrTypeLocator;
import org.hippoecm.frontend.editor.list.resolvers.TemplateTypeRenderer;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.reviewedactions.list.comparators.StateComparator;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;
import org.hippoecm.frontend.plugins.standards.DocumentListFilter;
import org.hippoecm.frontend.plugins.standards.list.DocumentsProvider;
import org.hippoecm.frontend.plugins.standards.list.ExpandCollapseListingPlugin;
import org.hippoecm.frontend.plugins.standards.list.IListColumnProvider;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.TableDefinition;
import org.hippoecm.frontend.plugins.standards.list.comparators.NameComparator;
import org.hippoecm.frontend.plugins.standards.list.datatable.IPagingDefinition;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable.TableSelectionListener;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListPagingDefinition;
import org.hippoecm.frontend.skin.DocumentListColumn;
import org.hippoecm.frontend.types.ITypeLocator;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TypesListingPlugin extends ExpandCollapseListingPlugin<Node> {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(TypesListingPlugin.class);

    private ITypeLocator typeLocator;

    public TypesListingPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        setClassName("hippo-typeslist " + DocumentListColumn.DOCUMENT_LIST_CSS_CLASS);

        IModel<Node> nodeModel = getModel();
        if (nodeModel != null && nodeModel.getObject() != null) {
            try {
                Node node = nodeModel.getObject();
                if (node.isNodeType(HippoNodeType.NT_NAMESPACE)) {
                    typeLocator = new JcrDraftLocator(node.getName());
                }
            } catch (RepositoryException ex) {
                log.error("Error determining node type for listing", ex);
            }
        }
        if (typeLocator == null) {
            typeLocator = new JcrTypeLocator();
        }
    }

    @Override
    protected ListDataTable<Node> newListDataTable(String id,
                                                   TableDefinition<Node> tableDefinition,
                                                   ISortableDataProvider<Node, String> dataProvider,
                                                   TableSelectionListener<Node> selectionListener,
                                                   final boolean triState,
                                                   ListPagingDefinition pagingDefinition) {
        return new TypesDataTable(id, tableDefinition, dataProvider, selectionListener, triState, pagingDefinition);
    }

    @Override
    protected ISortableDataProvider<Node, String> newDataProvider() {
        return new DocumentsProvider(getModel(), new DocumentListFilter(getPluginConfig()),
                getTableDefinition().getComparators());
    }

    @Override
    protected IListColumnProvider getDefaultColumnProvider() {
        return new TypesListColumnProvider();
    }

    @Override
    protected void onDetach() {
        typeLocator.detach();
        super.onDetach();
    }

    private class TypesDataTable extends ListDataTable<Node> {
        private static final long serialVersionUID = 1L;

        private TypesDataTable(String id,
                               TableDefinition<Node> tableDefinition,
                               ISortableDataProvider<Node, String> dataProvider,
                               TableSelectionListener<Node> selectionListener,
                               boolean triState,
                               IPagingDefinition pagingDefinition) {
            super(id, tableDefinition, dataProvider, selectionListener, triState, pagingDefinition);
        }

        void redraw(Item<Node> item) {
            redrawItem(item);
        }

        @Override
        protected IObserver<?> newObserver(final Item<Node> item, IModel<Node> model) {
            if (model instanceof JcrNodeModel) {
                Node node = ((JcrNodeModel) model).getNode();
                try {
                    if (node != null && node.isNodeType(HippoNodeType.NT_TEMPLATETYPE)) {
                        JcrNodeModel nodeModel = new JcrNodeModel(node
                                .getNode(HippoNodeType.HIPPOSYSEDIT_NODETYPE));
                        return new TypeObserver(this, item, nodeModel);
                    }
                } catch (RepositoryException ex) {
                    log.error(ex.getMessage(), ex);
                }
            }
            return super.newObserver(item, model);
        }
    }

    private class TypeObserver implements IObserver<JcrNodeModel>, IDetachable {
        private static final long serialVersionUID = 7399282716376782006L;

        private TypesDataTable table;
        private Item<Node> item;
        private JcrNodeModel nodeModel;

        TypeObserver(TypesDataTable table, Item<Node> item, JcrNodeModel model) {
            this.table = table;
            this.item = item;
            this.nodeModel = model;
        }

        public JcrNodeModel getObservable() {
            return nodeModel;
        }

        public void onEvent(Iterator<? extends IEvent<JcrNodeModel>> events) {
            table.redraw(item);
        }

        public void detach() {
            nodeModel.detach();
        }

    }

    private class TypesListColumnProvider implements IListColumnProvider {

        @Override
        public IHeaderContributor getHeaderContributor() {
            return null;
        }

        @Override
        public List<ListColumn<Node>> getColumns() {
            List<ListColumn<Node>> columns = new ArrayList<ListColumn<Node>>();
            columns.add(createIconColumn());
            columns.add(createNameColumn());
            return columns;
        }

        private ListColumn<Node> createIconColumn() {
            final Model<String> iconHeader = Model.of(StringUtils.EMPTY);
            final ListColumn<Node> column = new ListColumn<>(iconHeader, "icon");
            column.setComparator(new StateComparator());
            column.setRenderer(new TypeIconAndStateRenderer());
            column.setCssClass(DocumentListColumn.ICON.getCssClass());
            return column;
        }

        private ListColumn<Node> createNameColumn() {
            final ClassResourceModel nameHeader = new ClassResourceModel("typeslisting-name", TypesListingPlugin.class);
            final ListColumn<Node> column = new ListColumn<>(nameHeader, "name");
            column.setCssClass(DocumentListColumn.NAME.getCssClass());
            column.setComparator(new NameComparator());
            return column;
        }

        @Override
        public List<ListColumn<Node>> getExpandedColumns() {
            final List<ListColumn<Node>> columns = getColumns();
            columns.add(createTypeColumn());
            return columns;
        }

        private ListColumn<Node> createTypeColumn() {
            final ClassResourceModel typeHeader = new ClassResourceModel("typeslisting-type", TypesListingPlugin.class);
            final ListColumn<Node> column = new ListColumn<Node>(typeHeader, null);
            column.setRenderer(new TemplateTypeRenderer(typeLocator));
            column.setCssClass(DocumentListColumn.TYPE.getCssClass());
            return column;
        }
    }
}
