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

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IActivator;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.DocumentListFilter;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListPagingDefinition;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable.TableSelectionListener;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractListingPlugin extends RenderPlugin implements TableSelectionListener, IActivator {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(AbstractListingPlugin.class);

    private final IModelReference documentReference;
    private ListDataTable dataTable;
    private ListPagingDefinition pagingDefinition;
    private ISortableDataProvider provider;
    private IObserver providerObserver;

    public AbstractListingPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        if (config.getString("model.document") != null) {
            documentReference = context.getService(config.getString("model.document"), IModelReference.class);
            if (documentReference != null) {
                context.registerService(new IObserver() {
                    private static final long serialVersionUID = 1L;

                    public IObservable getObservable() {
                        return documentReference;
                    }

                    public void onEvent(Iterator<? extends IEvent> event) {
                        updateSelection(documentReference.getModel());
                    }

                }, IObserver.class.getName());
            }
        } else {
            documentReference = null;
            log.warn("No document model service configured (model.document)");
        }

        pagingDefinition = new ListPagingDefinition(config);

        dataTable = getListDataTable("table", getTableDefinition(), getDataProvider(), this, isOrderable(),
                pagingDefinition);
        add(dataTable);
        dataTable.init(context);

        if (!isOrderable()) {
            updateSelection(getModel());
        }
    }

    public void start() {
        modelChanged();
    }

    public void stop() {
    }

    protected ListDataTable getListDataTable(String id, TableDefinition tableDefinition,
            ISortableDataProvider dataProvider, TableSelectionListener selectionListener, final boolean triState,
            ListPagingDefinition pagingDefinition) {
        return new ListDataTable(id, tableDefinition, dataProvider, selectionListener, triState, pagingDefinition);
    }

    private ISortableDataProvider getDataProvider() {
        if (provider == null) {
            provider = newDataProvider();
            if (provider instanceof IObservable) {
                providerObserver = new IObserver() {
                    private static final long serialVersionUID = 1L;

                    public IObservable getObservable() {
                        return (IObservable) provider;
                    }

                    public void onEvent(Iterator<? extends IEvent> event) {
                        redraw();
                    }

                };
                getPluginContext().registerService(providerObserver, IObserver.class.getName());
            }
        }
        return provider;
    }

    private void dumpDataProvider() {
        if (provider != null) {
            if (providerObserver != null) {
                getPluginContext().unregisterService(providerObserver, IObserver.class.getName());
                providerObserver = null;
            }
            provider = null;
        }
    }

    protected ISortableDataProvider newDataProvider() {
        return new DocumentsProvider((JcrNodeModel) getModel(), new DocumentListFilter(getPluginConfig()),
                getTableDefinition().getComparators());
    }

    protected abstract TableDefinition getTableDefinition();

    @SuppressWarnings("unchecked")
    public void selectionChanged(IModel model) {
        IPluginConfig config = getPluginConfig();
        if (config.getString("model.document") != null) {
            IModelReference<IModel> documentService = getPluginContext().getService(config.getString("model.document"),
                    IModelReference.class);
            if (documentService != null) {
                documentService.setModel(model);
                if (model != dataTable.getModel() && (model == null || !model.equals(dataTable.getModel()))) {
                    log.info("Did not receive model change notification for model.document ({})", config
                            .getString("model.document"));
                    updateSelection(model);
                }
            } else {
                updateSelection(model);
            }
        } else {
            updateSelection(model);
        }
    }

    private void updateSelection(IModel model) {
        dataTable.setModel(model);
        onSelectionChanged(model);
    }

    protected void onSelectionChanged(IModel model) {
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onModelChanged() {
        dataTable.destroy();
        dumpDataProvider();

        dataTable = getListDataTable("table", getTableDefinition(), getDataProvider(), this, isOrderable(),
                pagingDefinition);
        replace(dataTable);
        dataTable.init(getPluginContext());

        IPluginConfig config = getPluginConfig();
        if (config.getString("model.document") != null) {
            IModelReference<IModel> documentService = getPluginContext().getService(config.getString("model.document"),
                    IModelReference.class);
            if (documentService != null) {
                dataTable.setModel(documentService.getModel());
            }
        }
        redraw();
    }

    @Override
    public void render(PluginRequestTarget target) {
        super.render(target);
        dataTable.render(target);
    }

    private boolean isOrderable() {
        IModel model = getModel();
        if (model instanceof JcrNodeModel) {
            try {
                Node node = (Node) model.getObject();
                return node == null ? false : node.getPrimaryNodeType().hasOrderableChildNodes();
            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }
        }
        return false;
    }

}
