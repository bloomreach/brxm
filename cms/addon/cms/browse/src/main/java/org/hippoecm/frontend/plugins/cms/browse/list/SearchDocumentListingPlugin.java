/*
 *  Copyright 2010 Hippo.
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
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;

import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.browse.list.comparators.StateComparator;
import org.hippoecm.frontend.plugins.standards.browse.BrowserSearchResult;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.SearchDocumentsProvider;
import org.hippoecm.frontend.plugins.standards.list.TableDefinition;
import org.hippoecm.frontend.plugins.standards.list.comparators.NameComparator;
import org.hippoecm.frontend.plugins.standards.list.comparators.TypeComparator;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListPagingDefinition;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable.TableSelectionListener;
import org.hippoecm.frontend.plugins.standards.list.resolvers.DocumentAttributeModifier;
import org.hippoecm.frontend.plugins.standards.list.resolvers.EmptyRenderer;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IconAttributeModifier;
import org.hippoecm.frontend.plugins.standards.list.resolvers.StateIconAttributeModifier;
import org.hippoecm.frontend.plugins.yui.YuiPluginHelper;
import org.hippoecm.frontend.plugins.yui.tables.TableHelperBehavior;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchDocumentListingPlugin extends RenderPlugin<BrowserSearchResult> implements
        TableSelectionListener<Node> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(SearchDocumentListingPlugin.class);
    
    private final IModelReference<Node> documentReference;

    private ListPagingDefinition pagingDefinition;
    private ListDataTable<Node> dataTable;

    public SearchDocumentListingPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        pagingDefinition = new ListPagingDefinition(config);

        dataTable = getListDataTable("table", getTableDefinition(), newDataProvider(), this, pagingDefinition);
        add(dataTable);
        dataTable.init(context);

        if (config.getString("model.document") != null) {
            documentReference = context.getService(config.getString("model.document"), IModelReference.class);
            if (documentReference != null) {
                context.registerService(new IObserver<IModelReference<Node>>() {
                    private static final long serialVersionUID = 1L;

                    public IModelReference<Node> getObservable() {
                        return documentReference;
                    }

                    public void onEvent(Iterator<? extends IEvent<IModelReference<Node>>> event) {
                        updateSelection(documentReference.getModel());
                    }

                }, IObserver.class.getName());
                updateSelection(documentReference.getModel());
            }
        } else {
            documentReference = null;
            log.warn("No document model service configured (model.document)");
        }
    }

    @SuppressWarnings("unchecked")
    public void selectionChanged(IModel model) {
        IPluginConfig config = getPluginConfig();
        if (config.getString("model.document") != null) {
            IModelReference<IModel> documentService = getPluginContext().getService(config.getString("model.document"),
                    IModelReference.class);
            if (documentService != null) {
                documentService.setModel(model);
                if (model != dataTable.getDefaultModel() && (model == null || !model.equals(dataTable.getDefaultModel()))) {
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

    public void updateSelection(IModel<Node> model) {
        dataTable.setDefaultModel(model);
        onSelectionChanged(model);
    }

    protected void onSelectionChanged(IModel<Node> model) {
    }

    @Override
    public void onModelChanged() {
        dataTable.destroy();

        dataTable = getListDataTable("table", getTableDefinition(), newDataProvider(), this, pagingDefinition);
        replace(dataTable);
        dataTable.init(getPluginContext());

        IPluginConfig config = getPluginConfig();
        if (config.getString("model.document") != null) {
            IModelReference<IModel> documentService = getPluginContext().getService(config.getString("model.document"),
                    IModelReference.class);
            if (documentService != null) {
                dataTable.setDefaultModel(documentService.getModel());
            }
        }
        redraw();
    }

    @Override
    public void render(PluginRequestTarget target) {
        super.render(target);
        dataTable.render(target);
    }

    protected TableDefinition<Node> getTableDefinition() {
        List<ListColumn<Node>> columns = new ArrayList<ListColumn<Node>>();

        ListColumn<Node> column = new ListColumn<Node>(new Model(""), "icon");
        column.setComparator(new TypeComparator());
        column.setRenderer(new EmptyRenderer());
        column.setAttributeModifier(new IconAttributeModifier());
        columns.add(column);

        column = new ListColumn<Node>(new StringResourceModel("doclisting-name", this, null), "name");
        column.setComparator(new NameComparator());
        column.setAttributeModifier(new DocumentAttributeModifier());
        columns.add(column);

        column = new ListColumn<Node>(new StringResourceModel("doclisting-state", this, null), "state");
        column.setComparator(new StateComparator());
        column.setRenderer(new EmptyRenderer());
        column.setAttributeModifier(new StateIconAttributeModifier());
        columns.add(column);

        return new TableDefinition<Node>(columns);
    }

    protected ISortableDataProvider<Node> newDataProvider() {
        return new SearchDocumentsProvider(getModel(), getTableDefinition().getComparators());
    }

    protected ListDataTable<Node> getListDataTable(String id, TableDefinition<Node> tableDefinition,
            ISortableDataProvider<Node> dataProvider, TableSelectionListener<Node> selectionListener,
            ListPagingDefinition pagingDefinition) {
        ListDataTable<Node> table = new ListDataTable<Node>(id, tableDefinition, dataProvider, selectionListener,
                true, pagingDefinition);
        table.add(new TableHelperBehavior(YuiPluginHelper.getManager(getPluginContext())));
        return table;
    }

}
