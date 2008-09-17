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
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.list.AbstractListingPlugin;
import org.hippoecm.frontend.plugins.standards.list.DocumentsProvider;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.TableDefinition;
import org.hippoecm.frontend.plugins.standards.list.comparators.NameComparator;
import org.hippoecm.frontend.plugins.standards.list.resolvers.EmptyRenderer;

public class AssetGalleryPlugin extends AbstractListingPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: $";
    private static final long serialVersionUID = 1L;

    public AssetGalleryPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    public TableDefinition getTableDefinition() {
        List<ListColumn> columns = new ArrayList<ListColumn>();

        ListColumn column = new ListColumn(new Model("Type"), "type");
        column.setRenderer(new EmptyRenderer());
        column.setAttributeModifier(new MimeTypeAttributeModifier());
        column.setComparator(new MimeTypeComparator());
        columns.add(column);

        column = new ListColumn(new Model("Size"), "size");
        column.setRenderer(new SizeRenderer());
        column.setComparator(new SizeComparator());
        columns.add(column);        

        column = new ListColumn(new Model("Name"), "name");
        column.setComparator(new NameComparator());
        columns.add(column);
        
        return new TableDefinition(columns);
    }

    @Override
    protected ISortableDataProvider getDataProvider() {
        return new DocumentsProvider((JcrNodeModel) getModel(), getTableDefinition().getComparators());
    }
}
