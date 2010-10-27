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

import javax.jcr.Node;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.internal.HtmlHeaderContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.browse.BrowserStyle;
import org.hippoecm.frontend.plugins.standards.list.AbstractListingPlugin;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.TableDefinition;
import org.hippoecm.frontend.plugins.standards.list.comparators.NameComparator;
import org.hippoecm.frontend.plugins.standards.list.comparators.TypeComparator;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListPagingDefinition;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable.TableSelectionListener;
import org.hippoecm.frontend.plugins.standards.list.resolvers.DocumentAttributeModifier;
import org.hippoecm.frontend.plugins.standards.list.resolvers.EmptyRenderer;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IconAttributeModifier;
import org.hippoecm.frontend.plugins.standards.list.resolvers.TypeRenderer;
import org.hippoecm.frontend.plugins.yui.datatable.DataTableBehavior;
import org.hippoecm.frontend.plugins.yui.datatable.DataTableSettings;
import org.hippoecm.frontend.plugins.yui.layout.ExpandCollapseLink;
import org.hippoecm.frontend.plugins.yui.layout.IExpandableCollapsable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentListingPlugin extends AbstractListingPlugin implements IExpandableCollapsable {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(DocumentListingPlugin.class);

    private static final String DOCUMENT_LISTING_CSS = "DocumentListingPlugin.css";
    private static final String TOGGLE_FULLSCREEN_IMG = "but-small.png";

    boolean isExpanded = false;
    private DataTableBehavior datatableBehavior;

    public DocumentListingPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        add(BrowserStyle.getStyleSheet());

        final ExpandCollapseLink<String> link = new ExpandCollapseLink<String>("toggleFullscreen");
        link.add(new Image("toggleFullscreenImage", TOGGLE_FULLSCREEN_IMG));

        WebMarkupContainer c = new WebMarkupContainer("toggleContainer") {

            @Override
            public boolean isVisible() {
                return link.isVisible();
            }

            @Override
            public boolean isEnabled() {
                return link.isEnabled();
            }
        };

        c.add(link);
        add(c);
    }

    public void collapse() {
        isExpanded = false;
        onModelChanged();
    }

    public boolean isSupported() {
        return getPluginConfig().getAsBoolean("expand.collapse.supported", false);
    }

    public void expand() {
        isExpanded = true;
        onModelChanged();
    }

    @Override
    public void renderHead(HtmlHeaderContainer container) {
        container.getHeaderResponse().renderCSSReference(new ResourceReference(DocumentListingPlugin.class, DOCUMENT_LISTING_CSS));
    }

    @Override
    protected TableDefinition<Node> getTableDefinition() {
        return new TableDefinition<Node>(isExpanded ? getExpandedColumns() : getCollapsedColumns());
    }

    protected List<ListColumn<Node>> getCollapsedColumns() {
        List<ListColumn<Node>> columns = new ArrayList<ListColumn<Node>>();

        //Type Icon
        ListColumn<Node> column = new ListColumn<Node>(new Model(""), "icon");
        column.setComparator(new TypeComparator());
        column.setRenderer(new EmptyRenderer());
        column.setAttributeModifier(new IconAttributeModifier());
        column.setCssClass("doclisting-icon");
        columns.add(column);

        //Name
        column = new ListColumn<Node>(new StringResourceModel("doclisting-name", this, null), "name");
        column.setComparator(new NameComparator());
        column.setAttributeModifier(new DocumentAttributeModifier());
        column.setCssClass("doclisting-name");
        columns.add(column);

        return columns;
    }

    protected List<ListColumn<Node>> getExpandedColumns() {
        List<ListColumn<Node>> columns = getCollapsedColumns();

        //Type
        ListColumn<Node> column = new ListColumn<Node>(new StringResourceModel("doclisting-type", this, null), "type");
        column.setComparator(new TypeComparator());
        column.setRenderer(new TypeRenderer());
        column.setCssClass("doclisting-type");
        columns.add(column);

        return columns;
    }

    @Override
    protected ListDataTable<Node> getListDataTable(String id, TableDefinition<Node> tableDefinition,
            ISortableDataProvider<Node> dataProvider, TableSelectionListener<Node> selectionListener, boolean triState,
            ListPagingDefinition pagingDefinition) {

        ListDataTable<Node> datatable = super.getListDataTable(id, tableDefinition, dataProvider, selectionListener,
                triState, pagingDefinition);
        DataTableSettings settings = new DataTableSettings();
        settings.setAutoWidthClassName("doclisting-name");
        datatable.add(datatableBehavior = new DataTableBehavior(settings));
        return datatable;
    }

    @Override
    protected void onSelectionChanged(IModel<Node> model) {
        super.onSelectionChanged(model);
        AjaxRequestTarget target = AjaxRequestTarget.get();
        if(target != null) {
            target.appendJavascript(datatableBehavior.getUpdateScript());
        }
    }
}
