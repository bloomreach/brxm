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
package org.hippoecm.frontend.plugins.standardworkflow;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.IRequestTarget;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.i18n.model.NodeTranslator;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.DocumentListFilter;
import org.hippoecm.frontend.plugins.standards.list.DocumentsProvider;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.TableDefinition;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListPagingDefinition;
import org.hippoecm.frontend.plugins.standards.list.datatable.SortableDataProvider;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable.TableSelectionListener;
import org.hippoecm.frontend.plugins.standards.list.resolvers.EmptyRenderer;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IListAttributeModifier;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IListCellRenderer;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IconAttributeModifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ReorderDialog extends CompatibilityWorkflowPlugin.WorkflowAction.WorkflowDialog {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ReorderDialog.class);

    private ReorderPanel panel;
    private WorkflowDescriptorModel model;
    private List<String> mapping;

    static class ListItem implements IDetachable {
        private static final long serialVersionUID = 1L;

        private String name;
        private IModel<String> displayName;
        private AttributeModifier cellModifier;
        private AttributeModifier columnModifier;
        private JcrNodeModel nodeModel;

        ListItem(JcrNodeModel nodeModel) {
            this.nodeModel = nodeModel;
            try {
                name = nodeModel.getNode().getName();
                displayName = new NodeTranslator(nodeModel).getNodeName();
                IconAttributeModifier attributeModifier = new IconAttributeModifier();
                cellModifier = attributeModifier.getCellAttributeModifier(nodeModel.getNode());
                columnModifier = attributeModifier.getColumnAttributeModifier(nodeModel.getNode());
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

        public AttributeModifier getCellModifier() {
            return cellModifier;
        }

        public AttributeModifier getColumnModifier() {
            return columnModifier;
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
        private static final long serialVersionUID = 1L;

        private LinkedList<ListItem> listItems;

        ReorderDataProvider(DocumentsProvider documents) {
            listItems = new LinkedList<ListItem>();
            Iterator<Node> it = documents.iterator(0, documents.size());
            while (it.hasNext()) {
                IModel<Node> entry = documents.model(it.next());
                if (entry instanceof JcrNodeModel) {
                    listItems.add(new ListItem((JcrNodeModel) entry));
                }
            }
        }

        public Iterator<ListItem> iterator(int first, int count) {
            return listItems.subList(first, first + count).iterator();
        }

        public IModel<ListItem> model(ListItem object) {
            return new Model<ListItem>(object);
        }

        public int size() {
            return listItems.size();
        }

        public void detach() {
            for (ListItem item : listItems) {
                item.detach();
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

        public List<String> getMapping() {
            LinkedList<String> newOrder = new LinkedList<String>();
            for (ListItem item : listItems) {
                newOrder.add(item.getName());
            }
            return newOrder;
        }
    }

    class ReorderPanel extends WebMarkupContainer implements TableSelectionListener<ListItem> {
        private static final long serialVersionUID = 1L;

        private TableDefinition<ListItem> tableDefinition;
        private ReorderDataProvider dataProvider;
        private ListDataTable<ListItem> dataTable;
        private ListPagingDefinition pagingDefinition;
        private AjaxLink<Void> up;
        private AjaxLink<Void> down;

        public ReorderPanel(String id, JcrNodeModel model, DocumentListFilter filter) {
            super(id);
            setOutputMarkupId(true);

            List<ListColumn<ListItem>> columns = new ArrayList<ListColumn<ListItem>>();

            ListColumn<ListItem> column = new ListColumn<ListItem>(new Model<String>(""), "icon");
            column.setRenderer(new EmptyRenderer<ListItem>());
            column.setAttributeModifier(new IListAttributeModifier<ListItem>() {
                private static final long serialVersionUID = 1L;

                public AttributeModifier[] getCellAttributeModifiers(IModel<ListItem> model) {
                    ListItem item = model.getObject();
                    return new AttributeModifier[] { item.getCellModifier() };
                }

                public AttributeModifier[] getColumnAttributeModifiers(IModel<ListItem> model) {
                    ListItem item = model.getObject();
                    return new AttributeModifier[] { item.getColumnModifier() };
                }
            });
            columns.add(column);

            column = new ListColumn<ListItem>(new Model<String>(""), "name");
            column.setRenderer(new IListCellRenderer<ListItem>() {
                private static final long serialVersionUID = 1L;

                public Component getRenderer(String id, IModel<ListItem> model) {
                    ListItem item = model.getObject();
                    return new Label(id, item.getDisplayName());
                }
            });
            columns.add(column);

            tableDefinition = new TableDefinition<ListItem>(columns, false);
            DocumentsProvider documents = new DocumentsProvider(model, filter, new HashMap<String, Comparator<Node>>());
            dataProvider = new ReorderDataProvider(documents);

            pagingDefinition = new ListPagingDefinition();
            pagingDefinition.setPageSize(dataProvider.size() > 0 ? dataProvider.size() : 1);
            add(dataTable = new ListDataTable<ListItem>("table", tableDefinition, dataProvider, this, false,
                    pagingDefinition));

            up = new AjaxLink<Void>("up") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    IModel<ListItem> selection = dataTable.getModel();
                    dataProvider.shiftUp(selection.getObject());

                    ReorderPanel panel = ReorderPanel.this;
                    dataTable = new ListDataTable<ListItem>("table", tableDefinition, dataProvider, panel, false,
                            pagingDefinition);
                    panel.replace(dataTable);
                    selectionChanged(selection);
                }
            };
            add(up);

            down = new AjaxLink<Void>("down") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    IModel<ListItem> selection = dataTable.getModel();
                    dataProvider.shiftDown(selection.getObject());

                    ReorderPanel panel = ReorderPanel.this;
                    dataTable = new ListDataTable<ListItem>("table", tableDefinition, dataProvider, panel, false,
                            pagingDefinition);
                    panel.replace(dataTable);
                    selectionChanged(selection);
                }
            };
            add(down);

            if (dataProvider.size() > 0) {
                ListItem selection = dataProvider.iterator(0, 1).next();
                selectionChanged(dataProvider.model(selection));
            } else {
                up.setEnabled(false);
                down.setEnabled(false);
            }
        }

        public void selectionChanged(IModel<ListItem> model) {
            ListItem item = model.getObject();
            long position = -1;
            int size = dataProvider.size();
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
                up.setEnabled(position > 1);
                down.setEnabled(position < size);
            }

            dataTable.setModel(model);
            IRequestTarget target = RequestCycle.get().getRequestTarget();
            if (AjaxRequestTarget.class.isAssignableFrom(target.getClass())) {
                ((AjaxRequestTarget) target).addComponent(this);
            }
        }

        public List<String> getMapping() {
            return dataProvider.getMapping();
        }
    }

    ReorderDialog(CompatibilityWorkflowPlugin.WorkflowAction action, IPluginConfig pluginConfig,
            WorkflowDescriptorModel model, List<String> mapping) {
        action.super();
        this.model = model;
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
        add(new Label("message", new StringResourceModel("reorder-message", this, null, new Object[] { name })));
    }

    @Override
    public IModel getTitle() {
        return new StringResourceModel("reorder", this, null);
    }

    @Override
    protected void onOk() {
        mapping.clear();
        mapping.addAll(panel.getMapping());
        super.onOk();
    }

    @Override
    public IValueMap getProperties() {
        IValueMap props = new ValueMap(super.getProperties());
        props.put("width", 520);
        props.put("height", 400);
        return props;
    }
}
