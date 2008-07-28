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

import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.list.AbstractListingPlugin;
import org.hippoecm.frontend.plugins.standards.list.DocumentsProvider;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.TableDefinition;
import org.hippoecm.frontend.plugins.standards.list.comparators.NameComparator;
import org.hippoecm.frontend.plugins.standards.list.comparators.TypeComparator;
import org.hippoecm.frontend.plugins.standards.list.resolvers.EmptyRenderer;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IListCellRenderer;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IconAttributeModifier;
import org.hippoecm.frontend.service.ITitleDecorator;

public class TypesListingPlugin extends AbstractListingPlugin implements ITitleDecorator {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: DocumentListingPlugin.java 12651 2008-07-18 11:59:05Z fvlankvelt $";

    private static final long serialVersionUID = 1L;

    public TypesListingPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }
    
    public String getTitle() {
        return "Document listing";
    }

    @Override
    protected TableDefinition getTableDefinition() {
        List<ListColumn> columns = new ArrayList<ListColumn>();
        
        ListColumn column = new ListColumn(new Model(""), "icon");
        column.setComparator(new TypeComparator());
        column.setRenderer(new EmptyRenderer());
        column.setAttributeModifier(new IconAttributeModifier());
        columns.add(column);
        
        column = new ListColumn(new Model("Name"), "name");
        column.setComparator(new NameComparator());
        columns.add(column);

        column = new ListColumn(new Model("Type"), null);
        column.setRenderer(new IListCellRenderer() {
            private static final long serialVersionUID = 1L;
            public Component getRenderer(String id, IModel model) {
                return new Label(id, "Document type");
            }
        });
        columns.add(column);
        
        return new TableDefinition(columns);
    }

    @Override
    protected ISortableDataProvider getDataProvider() {
        return new DocumentsProvider((JcrNodeModel) getModel(), getTableDefinition().getComparators());
    }

}
