/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.image.CachingImage;
import org.hippoecm.frontend.plugins.standards.list.datatable.IPagingDefinition;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListPagingDefinition;
import org.hippoecm.frontend.plugins.yui.datatable.DataTableBehavior;
import org.hippoecm.frontend.plugins.yui.datatable.DataTableSettings;
import org.hippoecm.frontend.plugins.yui.layout.ExpandCollapseLink;
import org.hippoecm.frontend.plugins.yui.layout.IExpandableCollapsable;

public abstract class ExpandCollapseListingPlugin<T> extends AbstractListingPlugin<T> implements IExpandableCollapsable {
    private static final long serialVersionUID = 1L;

    private static final String TOGGLE_FULLSCREEN_IMG = "but-small.png";

    private WebMarkupContainer buttons;
    private DataTableBehavior behavior;
    private DataTableSettings settings;

    private boolean isExpanded = false;
    private String className = null;
    private transient boolean updateDatatable = false;

    public ExpandCollapseListingPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        add(buttons = new WebMarkupContainer("buttons") {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return true;
//                return link.isVisible();
            }

            @Override
            public boolean isEnabled() {
                return true;
//                return link.isEnabled();
            }
        });

        ExpandCollapseLink link = new ExpandCollapseLink("toggleFullscreen");
        ResourceReference toggleFullscreenImage = new PackageResourceReference(ExpandCollapseListingPlugin.class,
                TOGGLE_FULLSCREEN_IMG);
        link.add(new CachingImage("toggleFullscreenImage", toggleFullscreenImage));

        addButton(link);

        updateDatatable = true;
        settings = new DataTableSettings();
    }

    protected void addButton(Component c) {
        buttons.add(c);
    }

    public void setClassName(String className) {
        this.className = className;
    }

    @Override
    protected TableDefinition<Node> newTableDefinition() {
        return new TableDefinition<Node>(isExpanded ? getExpandedColumns() : getColumns());
    }

    public void collapse() {
        if (isExpanded) {
            isExpanded = false;
            onModelChanged();
        }
    }

    public boolean isSupported() {
        return getPluginConfig().getAsBoolean("expand.collapse.supported", false);
    }

    public void expand() {
        if (!isExpanded) {
            isExpanded = true;
            onModelChanged();
        }
    }

    @Override
    protected void onSelectionChanged(IModel<Node> model) {
        super.onSelectionChanged(model);
        updateDatatable = true;
    }

    @Override
    protected final ListDataTable<Node> getListDataTable(String id,
                                                         TableDefinition<Node> tableDefinition,
                                                         ISortableDataProvider<Node, String> dataProvider,
                                                         ListDataTable.TableSelectionListener<Node> selectionListener,
                                                         boolean triState,
                                                         ListPagingDefinition pagingDefinition) {
        ListDataTable<Node> datatable = super.getListDataTable(id, tableDefinition, dataProvider, selectionListener,
                triState, pagingDefinition);

        if (className != null) {
            datatable.add(new AttributeAppender("class", new Model<String>(className), " "));
        }
        datatable.add(behavior = getBehavior());
        return datatable;
    }


    protected ListDataTable<Node> newListDataTable(String id, TableDefinition<Node> tableDefinition,
            ISortableDataProvider<Node, String> dataProvider, ListDataTable.TableSelectionListener<Node> selectionListener, boolean triState,
            ListPagingDefinition pagingDefinition) {
        return new Grid(id, tableDefinition, dataProvider, selectionListener, triState, pagingDefinition);
    }

    protected DataTableBehavior getBehavior() {
        return new DataTableBehavior(settings);
    }

    public void setSettings(DataTableSettings settings) {
        this.settings = settings;
    }

    public DataTableSettings getSettings() {
        return settings;
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        for (IListColumnProvider provider : getListColumnProviders()) {
            IHeaderContributor providerHeader = provider.getHeaderContributor();
            if (providerHeader != null) {
                providerHeader.renderHead(response);
            }
        }
    }

    @Override
    public void render(final PluginRequestTarget target) {
        super.render(target);
        if (target != null && updateDatatable && isVisible()) {
            target.appendJavaScript(behavior.getUpdateScript());
        }
        updateDatatable = false;
    }

    protected List<IListColumnProvider> getListColumnProviders() {
        IPluginConfig config = getPluginConfig();

        List<IListColumnProvider> providers = null;
        if (config.containsKey(IListColumnProvider.SERVICE_ID)) {
            IPluginContext context = getPluginContext();
            providers = context
                    .getServices(config.getString(IListColumnProvider.SERVICE_ID), IListColumnProvider.class);
        }
        if (providers == null || providers.size() == 0) {
            providers = getDefaultColumnProviders();
        }
        return providers;
    }

    protected List<ListColumn<Node>> getColumns() {
        List<ListColumn<Node>> columns = new ArrayList<ListColumn<Node>>();

        List<IListColumnProvider> providers = getListColumnProviders();
        for (IListColumnProvider provider : providers) {
            columns.addAll(provider.getColumns());
        }

        return columns;
    }

    protected List<ListColumn<Node>> getExpandedColumns() {
        List<ListColumn<Node>> columns = new ArrayList<ListColumn<Node>>();

        List<IListColumnProvider> providers = getListColumnProviders();
        for (IListColumnProvider provider : providers) {
            columns.addAll(provider.getExpandedColumns());
        }

        return columns;
    }

    protected List<IListColumnProvider> getDefaultColumnProviders() {
        List<IListColumnProvider> providers = new LinkedList<IListColumnProvider>();
        providers.add(getDefaultColumnProvider());
        return providers;
    }

    protected IListColumnProvider getDefaultColumnProvider() {
        return new IListColumnProvider() {

            public IHeaderContributor getHeaderContributor() {
                return null;
            }

            public List<ListColumn<Node>> getColumns() {
                List<ListColumn<Node>> cols = new LinkedList<ListColumn<Node>>();
                ListColumn<Node> column = new ListColumn<Node>(new ResourceModel("default-column-nodename"), "name");
                cols.add(column);
                return cols;
            }

            public List<ListColumn<Node>> getExpandedColumns() {
                return getColumns();
            }
        };
    }

    class Grid extends ListDataTable<Node> {

        public Grid(String id, TableDefinition tableDefinition, ISortableDataProvider iSortableDataProvider, TableSelectionListener tableSelectionListener, final boolean triState, IPagingDefinition pagingDefinition) {
            super(id, tableDefinition, iSortableDataProvider, tableSelectionListener, triState, pagingDefinition);
        }
    }
}
