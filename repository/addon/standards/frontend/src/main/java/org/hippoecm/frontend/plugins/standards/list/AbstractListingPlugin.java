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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.IJcrNodeModelListener;
import org.hippoecm.frontend.model.IModelListener;
import org.hippoecm.frontend.model.IModelService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable.TableSelectionListener;
import org.hippoecm.frontend.service.IJcrService;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractListingPlugin extends RenderPlugin implements IJcrNodeModelListener, TableSelectionListener {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(AbstractListingPlugin.class);

    private ListDataTable dataTable;
    private int pageSize;
    private boolean triState;

    public AbstractListingPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        // register for flush notifications
        context.registerService(this, IJcrService.class.getName());

        if (config.getString("model.document") != null) {
            context.registerService(new IModelListener() {
                private static final long serialVersionUID = 1L;

                public void updateModel(IModel model) {
                    updateSelection(model);
                }

            }, config.getString("model.document"));
        } else {
            log.warn("No document model service configured (model.document)");
        }

        pageSize = config.getInt("list.page.size", 15);
        
        IModel model = getModel();
        if (model instanceof JcrNodeModel) {
            try {
                Node node = (Node) model.getObject();
                triState = node.getPrimaryNodeType().hasOrderableChildNodes();
            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }
        }
        dataTable = new ListDataTable("table", getTableDefinition(), getDataProvider(), this, pageSize, triState);
        add(dataTable);

        modelChanged();
    }

    protected abstract ISortableDataProvider getDataProvider();

    protected abstract TableDefinition getTableDefinition();

    @SuppressWarnings("unchecked")
    public void selectionChanged(IModel model) {
        IPluginConfig config = getPluginConfig();
        if (config.getString("model.document") != null) {
            IModelService<IModel> documentService = getPluginContext().getService(config.getString("model.document"),
                    IModelService.class);
            if (documentService != null) {
                documentService.setModel(model);
            } else {
                updateSelection(model);
            }
        } else {
            updateSelection(model);
        }
    }

    private void updateSelection(IModel model) {
        dataTable.setModel(model);
        redraw();

        onSelectionChanged(model);
    }

    protected void onSelectionChanged(IModel model) {
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onModelChanged() {
        replace(dataTable = new ListDataTable("table", getTableDefinition(), getDataProvider(), this, pageSize, triState));
        IPluginConfig config = getPluginConfig();
        if (config.getString("model.document") != null) {
            IModelService<IModel> documentService = getPluginContext().getService(config.getString("model.document"),
                    IModelService.class);
            if (documentService != null) {
                dataTable.setModel(documentService.getModel());
            }
        }
        redraw();
    }

    public void onFlush(JcrNodeModel nodeModel) {
        JcrNodeModel myModel = (JcrNodeModel) getModel();
        if (myModel == null || myModel.getItemModel().hasAncestor(nodeModel.getItemModel())
                || myModel.equals(nodeModel.getParentModel())) {
            modelChanged();
        }
    }

}
