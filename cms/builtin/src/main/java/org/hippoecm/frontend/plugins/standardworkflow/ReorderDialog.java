/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.hippoecm.addon.workflow.IWorkflowInvoker;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.addon.workflow.WorkflowDialog;
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

class ReorderDialog extends WorkflowDialog<WorkflowDescriptor> {

    private static final Logger log = LoggerFactory.getLogger(ReorderDialog.class);

    private ReorderPanel panel;
    private List<String> mapping;

    static class ListItem implements IDetachable {

        private String name;
        private IModel<String> displayName;
        private JcrNodeModel nodeModel;
        private int index;

        ListItem(JcrNodeModel nodeModel) {
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
            return name + (index > 1 ? "["+index+"]" : "");
        }

        public void detach() {
            nodeModel.detach();
        }

        @Override
        public boolean equals(Object other) {
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

        ReorderDataProvider(DocumentsProvider documents) {
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
        public Iterator<ListItem> iterator(long first, long count) {
            return listItems.subList((int) first, (int) (first + count)).iterator();
        }

        @Override
        public IModel<ListItem> model(ListItem object) {
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

        public void shiftTop(ListItem item) {
            int index = listItems.indexOf(item);
            if (index > 0) {
                listItems.remove(index);
                listItems.add(0, item);
            }
        }

        public void shiftUp(ListItem item) {
            int index = listItems.indexOf(item);
            if (index > 0) {
                listItems.remove(index);
                listItems.add(--index, item);
            }
        }

        public void shiftDown(ListItem item) {
            int index = listItems.indexOf(item);
            if (index < listItems.size()) {
                listItems.remove(index);
                listItems.add(++index, item);
            }
        }

        public void shiftBottom(ListItem item) {
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

    class ReorderPanel extends WebMarkupContainer implements TableSelectionListener<ListItem> {

        private TableDefinition<ListItem> tableDefinition;
        private ReorderDataProvider dataProvider;
        private ListDataTable<ListItem> dataTable;
        private ListPagingDefinition pagingDefinition;
        private AjaxLink<Void> up;
        private AjaxLink<Void> down;
        private AjaxLink<Void> top;
        private AjaxLink<Void> bottom;

        public ReorderPanel(String id, JcrNodeModel model, DocumentListFilter filter) {
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

                public Component getRenderer(String id, IModel<ListItem> model) {
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
            dataTable = new ListDataTable<>("table", tableDefinition, dataProvider, this, false, pagingDefinition);
            add(dataTable);

            top = new AjaxLink<Void>("top") {
                @Override
                public void onClick(AjaxRequestTarget target) {
                    IModel<ListItem> selection = dataTable.getModel();
                    dataProvider.shiftTop(selection.getObject());

                    ReorderPanel thisPanel = ReorderPanel.this;
                    dataTable = new ListDataTable<>("table", tableDefinition, dataProvider, thisPanel, false,
                            pagingDefinition);
                    dataTable.setScrollSelectedIntoView(true, true);
                    thisPanel.replace(dataTable);
                    selectionChanged(selection);
                }
            };
            add(top);
            final HippoIcon topIcon = HippoIcon.fromSprite("icon", Icon.STEP_BACKWARD);
            topIcon.addCssClass("hi-rotate-90");
            top.add(topIcon);

            up = new AjaxLink<Void>("up") {
                @Override
                public void onClick(AjaxRequestTarget target) {
                    IModel<ListItem> selection = dataTable.getModel();
                    dataProvider.shiftUp(selection.getObject());

                    ReorderPanel thisPanel = ReorderPanel.this;
                    dataTable = new ListDataTable<>("table", tableDefinition, dataProvider, thisPanel, false,
                            pagingDefinition);
                    dataTable.setScrollSelectedIntoView(true, true);
                    thisPanel.replace(dataTable);
                    selectionChanged(selection);
                }
            };
            add(up);
            up.add(HippoIcon.fromSprite("icon", Icon.CHEVRON_UP));

            down = new AjaxLink<Void>("down") {
                @Override
                public void onClick(AjaxRequestTarget target) {
                    IModel<ListItem> selection = dataTable.getModel();
                    dataProvider.shiftDown(selection.getObject());

                    ReorderPanel thisPanel = ReorderPanel.this;
                    dataTable = new ListDataTable<>("table", tableDefinition, dataProvider, thisPanel, false,
                            pagingDefinition);
                    dataTable.setScrollSelectedIntoView(true, false);
                    thisPanel.replace(dataTable);
                    selectionChanged(selection);
                }
            };
            add(down);
            down.add(HippoIcon.fromSprite("icon", Icon.CHEVRON_DOWN));

            bottom = new AjaxLink<Void>("bottom") {
                @Override
                public void onClick(AjaxRequestTarget target) {
                    IModel<ListItem> selection = dataTable.getModel();
                    dataProvider.shiftBottom(selection.getObject());

                    ReorderPanel thisPanel = ReorderPanel.this;
                    dataTable = new ListDataTable<>("table", tableDefinition, dataProvider, thisPanel, false,
                            pagingDefinition);
                    dataTable.setScrollSelectedIntoView(true, false);
                    thisPanel.replace(dataTable);
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

        public void selectionChanged(IModel<ListItem> model) {
            ListItem item = model.getObject();
            long position = -1;
            long size = dataProvider.size();
            Iterator<ListItem> siblings = dataProvider.iterator(0, size);
            int i = 0;
            while (siblings.hasNext()) {
                i++;
                ListItem sibling = siblings.next();
                if (sibling.equals(item)) {
                    position = i;
                    break;
                }
            }
            if (position != -1) {
                top.setEnabled(position > 1);
                up.setEnabled(position > 1);
                down.setEnabled(position < size);
                bottom.setEnabled(position < size);
            }

            dataTable.setModel(model);
            AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
            if (target != null) {
                target.add(this);
            }
        }

        List<String> getMapping() {
            return dataProvider.getMapping();
        }
    }

    ReorderDialog(IWorkflowInvoker invoker, IPluginConfig pluginConfig, WorkflowDescriptorModel model,
                  List<String> mapping) {
        super(invoker, model);

        setTitleKey("reorder");
        setSize(DialogConstants.MEDIUM_AUTO);

        this.mapping = mapping;

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
