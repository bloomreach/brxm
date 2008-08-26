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
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.IRequestTarget;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
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

    private ListDataTable dataTable;

    public ReorderPanel(String id, JcrNodeModel model) {
        super(id);

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

        column = new ListColumn(new Model("Name"), "name");
        column.setRenderer(new IListCellRenderer() {
            private static final long serialVersionUID = 1L;

            public Component getRenderer(String id, IModel model) {
                if (model instanceof ListItem) {
                    ListItem nvp = (ListItem) model.getObject();
                    return new Label(id, ISO9075Helper.decodeLocalName(nvp.getName()));
                }
                return new Label(id);
            }
        });
        columns.add(column);

        TableDefinition tableDefinition = new TableDefinition(columns);
        DocumentsProvider documents = new DocumentsProvider(model, new HashMap<String, Comparator<IModel>>());
        ISortableDataProvider dataProvider = new ReorderDataProvider(documents);

        add(dataTable = new ListDataTable("table", tableDefinition, dataProvider, this, dataProvider.size(), false));
    }

    public void selectionChanged(IModel model) {
 
        dataTable.setModel(model);
        IRequestTarget target = RequestCycle.get().getRequestTarget();
        if (AjaxRequestTarget.class.isAssignableFrom(target.getClass())) {
            ((AjaxRequestTarget) target).addComponent(dataTable);
        }
    }

}
