/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.addon.workflow.AbstractWorkflowDialog;
import org.hippoecm.addon.workflow.IWorkflowInvoker;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.i18n.model.NodeTranslator;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.DocumentListFilter;
import org.hippoecm.frontend.plugins.standards.list.DocumentsProvider;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.TableDefinition;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable.TableSelectionListener;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListPagingDefinition;
import org.hippoecm.frontend.plugins.standards.list.datatable.SortableDataProvider;
import org.hippoecm.frontend.plugins.standards.list.resolvers.AbstractListAttributeModifier;
import org.hippoecm.frontend.plugins.standards.list.resolvers.DocumentTypeIconAttributeModifier;
import org.hippoecm.frontend.plugins.standards.list.resolvers.EmptyRenderer;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IListCellRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ReorderDialog extends AbstractWorkflowDialog {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ReorderDialog.class);

    static final CssResourceReference REORDER_CSS = new CssResourceReference(ReorderDialog.class, "reorder.css");

    private ReorderPanel panel;
    private List<String> mapping;

    static class ListItem implements IDetachable {
        private static final long serialVersionUID = 1L;

        private String name;
        private IModel<String> displayName;
        private AttributeModifier cellModifier;
        private JcrNodeModel nodeModel;

        ListItem(JcrNodeModel nodeModel, DocumentTypeIconAttributeModifier attributeModifier) {
            this.nodeModel = nodeModel;
            try {
                name = nodeModel.getNode().getName();
                displayName = new NodeTranslator(nodeModel).getNodeName();
                cellModifier = attributeModifier.getCellAttributeModifier(nodeModel.getNode());
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

        private final List<ListItem> listItems;

        ReorderDataProvider(DocumentsProvider documents) {
            listItems = new LinkedList<ListItem>();
            DocumentTypeIconAttributeModifier attributeModifier = new DocumentTypeIconAttributeModifier();
            Iterator<Node> it = documents.iterator(0, documents.size());
            while (it.hasNext()) {
                IModel<Node> entry = documents.model(it.next());
                if (entry instanceof JcrNodeModel) {
                    listItems.add(new ListItem((JcrNodeModel) entry, attributeModifier));
                }
            }
        }

        @Override
        public Iterator<ListItem> iterator(long first, long count) {
            return listItems.subList((int) first, (int) (first + count)).iterator();
        }

        @Override
        public IModel<ListItem> model(ListItem object) {
            return new Model<ListItem>(object);
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
        private AjaxLink<Void> top;
        private AjaxLink<Void> bottom;

        public ReorderPanel(String id, JcrNodeModel model, DocumentListFilter filter) {
            super(id);
            setOutputMarkupId(true);

            List<ListColumn<ListItem>> columns = new ArrayList<ListColumn<ListItem>>();
            final DocumentTypeIconAttributeModifier attributeModifier = new DocumentTypeIconAttributeModifier();

            ListColumn<ListItem> column = new ListColumn<ListItem>(new Model<String>(""), "icon");
            column.setRenderer(new EmptyRenderer<ListItem>());
            column.setAttributeModifier(new AbstractListAttributeModifier<ListItem>() {
                private static final long serialVersionUID = 1L;

                @Override
                public AttributeModifier[] getCellAttributeModifiers(IModel<ListItem> model) {
                    ListItem item = model.getObject();
                    return new AttributeModifier[] { item.getCellModifier() };
                }

                @Override
                public AttributeModifier[] getColumnAttributeModifiers() {
                    return new AttributeModifier[] { attributeModifier.getColumnAttributeModifier() };
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

            tableDefinition = new TableDefinition<ListItem>(columns, false);
            DocumentsProvider documents = new DocumentsProvider(model, filter, new HashMap<String, Comparator<Node>>());
            dataProvider = new ReorderDataProvider(documents);

            pagingDefinition = new ListPagingDefinition();
            pagingDefinition.setPageSize(dataProvider.size() > 0 ? (int) dataProvider.size() : 1);
            dataTable = new ListDataTable<ListItem>("table", tableDefinition, dataProvider, this, false, pagingDefinition);
            add(dataTable);

            top = new AjaxLink<Void>("top") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    IModel<ListItem> selection = dataTable.getModel();
                    dataProvider.shiftTop(selection.getObject());

                    ReorderPanel thisPanel = ReorderPanel.this;
                    dataTable = new ListDataTable<ListItem>("table", tableDefinition, dataProvider, thisPanel, false,
                            pagingDefinition);
                    dataTable.setScrollSelectedIntoView(true, true);
                    thisPanel.replace(dataTable);
                    selectionChanged(selection);
                }
            };
            add(top);

            up = new AjaxLink<Void>("up") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    IModel<ListItem> selection = dataTable.getModel();
                    dataProvider.shiftUp(selection.getObject());

                    ReorderPanel thisPanel = ReorderPanel.this;
                    dataTable = new ListDataTable<ListItem>("table", tableDefinition, dataProvider, thisPanel, false,
                            pagingDefinition);
                    dataTable.setScrollSelectedIntoView(true, true);
                    thisPanel.replace(dataTable);
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

                    ReorderPanel thisPanel = ReorderPanel.this;
                    dataTable = new ListDataTable<ListItem>("table", tableDefinition, dataProvider, thisPanel, false,
                            pagingDefinition);
                    dataTable.setScrollSelectedIntoView(true, false);
                    thisPanel.replace(dataTable);
                    selectionChanged(selection);
                }
            };
            add(down);

            bottom = new AjaxLink<Void>("bottom") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    IModel<ListItem> selection = dataTable.getModel();
                    dataProvider.shiftBottom(selection.getObject());

                    ReorderPanel thisPanel = ReorderPanel.this;
                    dataTable = new ListDataTable<ListItem>("table", tableDefinition, dataProvider, thisPanel, false,
                            pagingDefinition);
                    dataTable.setScrollSelectedIntoView(true, false);
                    thisPanel.replace(dataTable);
                    selectionChanged(selection);
                }
            };
            add(bottom);

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
                up.setEnabled(position > 1);
                down.setEnabled(position < size);
            }

            dataTable.setModel(model);
            AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
            if (target != null) {
                target.add(this);
            }
        }

        public List<String> getMapping() {
            return dataProvider.getMapping();
        }
    }

    ReorderDialog(IWorkflowInvoker action, IPluginConfig pluginConfig,
            WorkflowDescriptorModel model, List<String> mapping) {
        super(model, action);
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
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(REORDER_CSS));
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
