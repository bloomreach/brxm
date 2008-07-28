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
package org.hippoecm.frontend.plugins.standards.list;

import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.IJcrNodeModelListener;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable;
import org.hippoecm.frontend.service.IJcrService;
import org.hippoecm.frontend.service.render.RenderPlugin;

public abstract class AbstractListingPlugin extends RenderPlugin implements IJcrNodeModelListener {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private ListDataTable dataTable;
    private int pageSize;

    public AbstractListingPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        // register for flush notifications
        context.registerService(this, IJcrService.class.getName());

        pageSize = config.getInt("list.page.size", 15);
        dataTable = new ListDataTable("table", getTableDefinition(), getDataProvider(), pageSize);
        dataTable.setModel(getModel());
        add(dataTable);
    }

    protected abstract ISortableDataProvider getDataProvider();

    protected abstract TableDefinition getTableDefinition();

    private boolean newList = true;
    
    public void selectionChanged(IModel model) {
        newList = false;
        dataTable.setModel(model);
        setModel(model);
    }

    @Override
    public void onModelChanged() {
        if (newList) {
            dataTable = new ListDataTable("table", getTableDefinition(), getDataProvider(), pageSize);
            dataTable.setModel(getModel());
            replace(dataTable);
        } else {
            newList = true;
        }
        redraw();
    }

    public void onFlush(JcrNodeModel nodeModel) {
        modelChanged();
    }

}
