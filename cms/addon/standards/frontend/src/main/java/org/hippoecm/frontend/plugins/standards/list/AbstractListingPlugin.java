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
import org.hippoecm.frontend.model.IJcrNodeModelListener;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable;
import org.hippoecm.frontend.service.IJcrService;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractListingPlugin extends RenderPlugin implements IJcrNodeModelListener {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;
    protected static final Logger log = LoggerFactory.getLogger(AbstractListingPlugin.class);

    private ISortableDataProvider dataProvider;
    private int pageSize;
    protected ListDataTable dataTable;

    public AbstractListingPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        // register for flush notifications
        context.registerService(this, IJcrService.class.getName());
        
        pageSize = config.getInt("list.page.size", 15);       
        dataTable = new ListDataTable("table", getTableDefinition(), getDataProvider(), pageSize);
        dataTable.addTopColumnHeaders();
        dataTable.addBottomPaging();
        
        add(dataTable);
    }

    protected abstract ISortableDataProvider getDataProvider();

    protected abstract TableDefinition getTableDefinition();

    @Override
    public void onModelChanged() {
        int currentPage = dataTable.getCurrentPage();
        dataTable = new ListDataTable("table", getTableDefinition(), getDataProvider(), pageSize);
        dataTable.setModel(getModel());
        dataTable.addTopColumnHeaders();
        dataTable.addBottomPaging();
                
        replace(dataTable);
        dataTable.setCurrentPage(currentPage);
        
        redraw();
    }
    
    public void onFlush(JcrNodeModel nodeModel) {
        if (nodeModel.getParentModel() != null) {
            String nodePath = nodeModel.getParentModel().getItemModel().getPath();
            String myPath = ((JcrNodeModel) getModel()).getItemModel().getPath();
            if (myPath.startsWith(nodePath)) {
                modelChanged();
            }
        } else {
            modelChanged();
        }
    }

    @Override
    protected void onDetach() {
        if (dataProvider != null) {
            dataProvider.detach();
        }
        super.onDetach();
    }

}
