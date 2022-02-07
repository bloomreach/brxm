/*
 *  Copyright 2008-2022 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.standardworkflow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.DataGridView;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.hippoecm.addon.workflow.IWorkflowInvoker;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.addon.workflow.WorkflowDialog;
import org.hippoecm.frontend.ajax.BrLink;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeNameModel;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.DocumentListFilter;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.plugins.standards.list.DocumentsProvider;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.TableDefinition;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable.TableSelectionListener;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListPagingDefinition;
import org.hippoecm.frontend.plugins.standards.list.datatable.SortableDataProvider;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IListCellRenderer;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IconRenderer;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>This class implements the functionality to reorder subfolders.</p>
 *
 * <p>The {@link FolderWorkflowPlugin} adds this dialog to the "reorder" action.</p>
 *
 * <p>The {@link #mapping} can be used as argument to
 * {@link org.hippoecm.repository.standardworkflow.FolderWorkflow#reorder(List)}
 * to perform the actual ordering.</p>
 */
class ReorderDialog extends WorkflowDialog<WorkflowDescriptor> {

    private static final Logger log = LoggerFactory.getLogger(ReorderDialog.class);
    private static final String TABLE = "table";

    private ReorderPanel panel;
    private List<String> mapping;

    static class ListItem implements IDetachable {

        private String name;
        private IModel<String> displayName;
        private JcrNodeModel nodeModel;
        private int index;

        ListItem(final JcrNodeModel nodeModel) {
            this.nodeModel = nodeModel;
            try {
                name = nodeModel.getNode().getName();
                index = nodeModel.getNode().getIndex();
                displayName = new NodeNameModel(nodeModel);
            } catch (RepositoryException e) {
                log.error(e.getMessage(), e);
            }
        }

        public IModel<String> getDisplayName() {
            return displayName;
        }

        public String getName() {
            return name;
        }

        public IModel<Node> getNodeModel() {
            return nodeModel;
        }

        public String getPathName() {
            return name + (index > 1 ? "[" + index + "]" : "");
        }

        public void detach() {
            nodeModel.detach();
        }

        @Override
        public boolean equals(final Object other) {
            if (other instanceof ListItem) {
                ListItem otherItem = (ListItem) other;
                try {
                    return otherItem.nodeModel.getNode().isSame(nodeModel.getNode());
                } catch (RepositoryException e) {
                    return false;
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 67 * hash + (nodeModel != null && nodeModel.getNode() != null ? nodeModel.getNode().hashCode() : 0);
            return hash;
        }
    }

    static class ReorderDataProvider extends SortableDataProvider<ListItem> {

        private final List<ListItem> listItems;

        ReorderDataProvider(final DocumentsProvider documents) {
            listItems = new LinkedList<>();
            Iterator<Node> it = documents.iterator(0, documents.size());
            while (it.hasNext()) {
                IModel<Node> entry = documents.model(it.next());
                if (entry instanceof JcrNodeModel) {
                    listItems.add(new ListItem((JcrNodeModel) entry));
                }
            }
        }

        @Override
        public Iterator<ListItem> iterator(final long first, final long count) {
            return listItems.subList((int) first, (int) (first + count)).iterator();
        }

        @Override
        public IModel<ListItem> model(final ListItem object) {
            return new Model<>(object);
        }

        @Override
        public long size() {
            return listItems.size();
        }

        @Override
        public void detach() {
            for (ListItem item : listItems) {
                item.detach();
            }
        }

        public void shiftTop(final ListItem item) {
            int index = listItems.indexOf(item);
            if (index > 0) {
                listItems.remove(index);
                listItems.add(0, item);
            }
        }

        public void shiftUp(final ListItem item) {
            int index = listItems.indexOf(item);
            if (index > 0) {
                listItems.remove(index);
                listItems.add(--index, item);
            }
        }

        public void shiftDown(final ListItem item) {
            int index = listItems.indexOf(item);
            if (index < listItems.size()) {
                listItems.remove(index);
                listItems.add(++index, item);
            }
        }

        public void shiftBottom(final ListItem item) {
            int index = listItems.indexOf(item);
            if (index < listItems.size()) {
                listItems.remove(index);
                listItems.add(item);
            }
        }

        public List<String> getMapping() {
            LinkedList<String> newOrder = new LinkedList<>();
            for (ListItem item : listItems) {
                newOrder.add(item.getPathName());
            }
            return newOrder;
        }
    }

    private static class ReorderPanel extends WebMarkupContainer implements TableSelectionListener<ListItem> {

        private TableDefinition<ListItem> tableDefinition;
        private ReorderDataProvider dataProvider;
        private ListDataTable<ListItem> dataTable;
        private ListPagingDefinition pagingDefinition;
        private AjaxLink<Void> up;
        private AjaxLink<Void> down;
        private AjaxLink<Void> top;
        private AjaxLink<Void> bottom;

        ReorderPanel(final String id, final JcrNodeModel model, final DocumentListFilter filter) {
            super(id);
            setOutputMarkupId(true);

            List<ListColumn<ListItem>> columns = new ArrayList<>();

            ListColumn<ListItem> column = new ListColumn<>(Model.of(""), "icon");
            IconRenderer iconRenderer = new IconRenderer(IconSize.M);
            column.setRenderer(new IListCellRenderer<ListItem>() {
                @Override
                public Component getRenderer(final String id, final IModel<ListItem> model) {
                    final IModel<Node> nodeModel = model.getObject().getNodeModel();
                    return iconRenderer.getRenderer(id, nodeModel);
                }

                @Override
                public IObservable getObservable(final IModel<ListItem> model) {
                    final IModel<Node> nodeModel = model.getObject().getNodeModel();
                    return iconRenderer.getObservable(nodeModel);
                }
            });
            columns.add(column);

            column = new ListColumn<>(Model.of(""), "name");
            column.setRenderer(new IListCellRenderer<ListItem>() {

                public Component getRenderer(final String id, final IModel<ListItem> model) {
                    ListItem item = model.getObject();
                    return new Label(id, item.getDisplayName());
                }

                public IObservable getObservable(IModel<ListItem> model) {
                    ListItem item = model.getObject();
                    IModel<String> displayName = item.getDisplayName();
                    if (displayName instanceof IObservable) {
                        return (IObservable) displayName;
                    }
                    return null;
                }
            });
            columns.add(column);

            tableDefinition = new TableDefinition<>(columns, false);
            DocumentsProvider documents = new DocumentsProvider(model, filter, new HashMap<>());
            dataProvider = new ReorderDataProvider(documents);

            pagingDefinition = new ListPagingDefinition();
            pagingDefinition.setPageSize(dataProvider.size() > 0 ? (int) dataProvider.size() : 1);
            updateListDataTable();
            add(dataTable);

            top = new BrLink<Void>("top") {
                @Override
                public void onClick(final AjaxRequestTarget target) {
                    final IModel<ListItem> selection = dataTable.getModel();
                    dataProvider.shiftTop(selection.getObject());

                    updateListDataTable();
                    selectionChanged(selection);
                }
            };
            add(top);
            final HippoIcon topIcon = HippoIcon.fromSprite("icon", Icon.STEP_BACKWARD);
            topIcon.addCssClass("hi-rotate-90");
            top.add(topIcon);

            up = new BrLink<Void>("up") {
                @Override
                public void onClick(final AjaxRequestTarget target) {
                    final IModel<ListItem> selection = dataTable.getModel();
                    dataProvider.shiftUp(selection.getObject());

                    updateListDataTable();
                    selectionChanged(selection);
                }
            };
            add(up);
            up.add(HippoIcon.fromSprite("icon", Icon.CHEVRON_UP));

            down = new BrLink<Void>("down") {
                @Override
                public void onClick(final AjaxRequestTarget target) {
                    final IModel<ListItem> selection = dataTable.getModel();
                    dataProvider.shiftDown(selection.getObject());

                    updateListDataTable();
                    selectionChanged(selection);
                }
            };
            add(down);
            down.add(HippoIcon.fromSprite("icon", Icon.CHEVRON_DOWN));

            bottom = new BrLink<Void>("bottom") {
                @Override
                public void onClick(final AjaxRequestTarget target) {
                    final IModel<ListItem> selection = dataTable.getModel();
                    dataProvider.shiftBottom(selection.getObject());

                    updateListDataTable();
                    selectionChanged(selection);
                }
            };
            add(bottom);
            final HippoIcon bottomIcon = HippoIcon.fromSprite("icon", Icon.STEP_FORWARD);
            bottomIcon.addCssClass("hi-rotate-90");
            bottom.add(bottomIcon);

           if (dataProvider.size() > 0) {
                ListItem selection = dataProvider.iterator(0, 1).next();
                selectionChanged(dataProvider.model(selection));
            } else {
                top.setEnabled(false);
                up.setEnabled(false);
                down.setEnabled(false);
                bottom.setEnabled(false);
            }
        }

        private void updateListDataTable() {
            dataTable = new ListDataTable<ListItem>(TABLE, tableDefinition, dataProvider
                    , ReorderPanel.this::selectionChanged, false, pagingDefinition) {
                @Override
                protected DataGridView newDataGridView(final String id, final List list
                        , final IDataProvider dataProvider) {
                    final DataGridView dataGridView = super.newDataGridView(id, list, dataProvider);
                    if (pagingDefinition.getPageSize() == dataProvider.size()) {
                        dataGridView.setVersioned(false);
                    }
                    return dataGridView;
                }
            };
            dataTable.setScrollSelectedIntoView(true, true);
            ReorderPanel.this.addOrReplace(dataTable);
        }

        public void selectionChanged(final IModel<ListItem> model) {
            final ListItem item = model.getObject();
            final long size = dataProvider.size();
            final int position = dataProvider.listItems.indexOf(item);

            top.setEnabled(position > 0);
            up.setEnabled(position > 0);
            down.setEnabled(position < (size - 1));
            bottom.setEnabled(position < (size - 1));

            dataTable.setModel(model);
            final Optional<AjaxRequestTarget> target = RequestCycle.get().find(AjaxRequestTarget.class);
            target.ifPresent(ajaxRequestTarget -> ajaxRequestTarget.add(top, up, down, bottom, dataTable));
        }

        List<String> getMapping() {
            return dataProvider.getMapping();
        }
    }

    /**
     * <p>Shows a dialog to reorder subfolders of the backing node of the supplied model.</p>
     *
     * <p>The {@link #onOk()} calls the invoker.</p>
     *
     * <p>Mind that the {@link #mapping} is both input and output of this dialog.</p>
     *
     * @param invoker {@link IWorkflowInvoker}
     * @param pluginConfig
     * @param model {@link WorkflowDescriptorModel} for the backing folder node
     * @param order The order of the subfolders of the backing folder node, that this dialog modifies.
     */
    ReorderDialog(final IWorkflowInvoker invoker, final IPluginConfig pluginConfig, final WorkflowDescriptorModel model,
                  final List<String> order) {
        super(invoker, model);

        setTitleKey("reorder");
        setSize(DialogConstants.MEDIUM_AUTO);

        this.mapping = order;

        String name;
        try {
            JcrNodeModel folderModel = new JcrNodeModel(model.getNode());
            panel = new ReorderPanel("reorder-panel", folderModel, new DocumentListFilter(pluginConfig));
            add(panel);
            name = folderModel.getNode().getName();
        } catch (RepositoryException e) {
            log.error(e.getMessage(), e);
            name = "";
        }
        add(new Label("message", new StringResourceModel("reorder-message", this).setParameters(name)));
    }

    @Override
    protected void onOk() {
        mapping.clear();
        mapping.addAll(panel.getMapping());
        super.onOk();
    }
}
