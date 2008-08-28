/*
 * Copyright 2008 Hippo
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
package org.hippoecm.frontend.plugins.standardworkflow.reorder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.IRequestTarget;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.standards.list.DocumentsProvider;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.TableDefinition;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable.TableSelectionListener;
import org.hippoecm.frontend.plugins.standards.list.resolvers.EmptyRenderer;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IListAttributeModifier;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IListCellRenderer;
import org.hippoecm.repository.api.ISO9075Helper;

public class ReorderPanel extends Panel implements TableSelectionListener {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;

    private TableDefinition tableDefinition;
    private ReorderDataProvider dataProvider;
    private ListDataTable dataTable;
    private int pagesize;
    private AjaxLink up;
    private AjaxLink down;

    public ReorderPanel(String id, JcrNodeModel model) {
        super(id);
        setOutputMarkupId(true);

        List<ListColumn> columns = new ArrayList<ListColumn>();

        ListColumn column = new ListColumn(new Model(""), "icon");
        column.setRenderer(new EmptyRenderer());
        column.setAttributeModifier(new IListAttributeModifier() {
            private static final long serialVersionUID = 1L;

            public AttributeModifier getCellAttributeModifier(IModel model) {
                if (model instanceof ListItem) {
                    ListItem item = (ListItem) model.getObject();
                    return item.getCellModifier();
                }
                return null;
            }

            public AttributeModifier getColumnAttributeModifier(IModel model) {
                if (model instanceof ListItem) {
                    ListItem item = (ListItem) model.getObject();
                    return item.getColumnModifier();
                }
                return null;
            }

        });
        columns.add(column);

        column = new ListColumn(new Model(""), "name");
        column.setRenderer(new IListCellRenderer() {
            private static final long serialVersionUID = 1L;

            public Component getRenderer(String id, IModel model) {
                if (model instanceof ListItem) {
                    ListItem item = (ListItem) model;
                    return new Label(id, ISO9075Helper.decodeLocalName(item.getName()));
                }
                return new Label(id);
            }
        });
        columns.add(column);

        tableDefinition = new TableDefinition(columns, false);
        DocumentsProvider documents = new DocumentsProvider(model, new HashMap<String, Comparator<IModel>>());
        dataProvider = new ReorderDataProvider(documents);
        pagesize = dataProvider.size() > 0 ? dataProvider.size() : 1;
        add(dataTable = new ListDataTable("table", tableDefinition, dataProvider, this, pagesize, false));

        up = new AjaxLink("up") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                ListItem selection = (ListItem) dataTable.getModel();
                dataProvider.shiftUp(selection);

                ReorderPanel panel = ReorderPanel.this;
                dataTable = new ListDataTable("table", tableDefinition, dataProvider, panel, pagesize, false);
                panel.replace(dataTable);
                selectionChanged(selection);
            }
        };
        add(up);

        down = new AjaxLink("down") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                ListItem selection = (ListItem) dataTable.getModel();
                dataProvider.shiftDown(selection);

                ReorderPanel panel = ReorderPanel.this;
                dataTable = new ListDataTable("table", tableDefinition, dataProvider, panel, pagesize, false);
                panel.replace(dataTable);
                selectionChanged(selection);
            }
        };
        add(down);

        if (dataProvider.size() > 0) {
            ListItem selection = dataProvider.iterator(0, 1).next();
            selectionChanged(selection);
        } else {
            up.setEnabled(false);
            down.setEnabled(false);
        }
    }

    public void selectionChanged(IModel model) {
        ListItem item = (ListItem) model;
        long position = -1;
        int size = dataProvider.size();
        Iterator<ListItem> siblings = dataProvider.iterator(0, size);
        int i = 0;
        while (siblings.hasNext()) {
            i++;
            ListItem sibling = siblings.next();
            if (sibling.getName().equals(item.getName())) {
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
    
    public LinkedHashMap<String, String> getMapping() {
        return dataProvider.getMapping();
    }

}
