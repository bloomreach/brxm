package org.hippoecm.frontend.plugins.cms.browse.list;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.list.AbstractListingPlugin;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.TableDefinition;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListPagingDefinition;
import org.hippoecm.frontend.plugins.yui.layout.ExpandCollapseLink;
import org.hippoecm.frontend.plugins.yui.layout.IExpandableCollapsable;
import org.hippoecm.frontend.plugins.yui.widget.WidgetBehavior;
import org.hippoecm.frontend.plugins.yui.widget.WidgetSettings;

import javax.jcr.Node;
import java.util.List;

public abstract class ExpandCollapseListingPlugin extends AbstractListingPlugin implements IExpandableCollapsable {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    private static final String TOGGLE_FULLSCREEN_IMG = "but-small.png";

    private WebMarkupContainer buttons;
    private WidgetBehavior behavior;

    private boolean isExpanded = false;
    private String className = null;

    public ExpandCollapseListingPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        add(buttons = new WebMarkupContainer("buttons") {

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

        ExpandCollapseLink<String> link = new ExpandCollapseLink<String>("toggleFullscreen");
        link.add(new Image("toggleFullscreenImage", TOGGLE_FULLSCREEN_IMG));

        addButton(link);

    }

    protected void addButton(Component c) {
        buttons.add(c);
    }

    public void setClassName(String className) {
        this.className = className;
    }

    @Override
    protected TableDefinition<Node> getTableDefinition() {
        return new TableDefinition<Node>(isExpanded ? getExpandedColumns() : getCollapsedColumns());
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
    protected void onSelectionChanged(IModel<Node> model) {
        super.onSelectionChanged(model);
        AjaxRequestTarget target = AjaxRequestTarget.get();
        if (target != null) {
            target.appendJavascript(behavior.getUpdateScript());
        }
    }

    @Override
    protected final ListDataTable<Node> getListDataTable(String id, TableDefinition<Node> tableDefinition,
                                                   ISortableDataProvider<Node> dataProvider, ListDataTable.TableSelectionListener<Node> selectionListener, boolean triState,
                                                   ListPagingDefinition pagingDefinition) {
        ListDataTable<Node> datatable = newListDataTable(id, tableDefinition, dataProvider, selectionListener,
                triState, pagingDefinition);

        if(className != null) {
            datatable.add(new AttributeAppender("class", new Model<String>(className), " "));
        }
        datatable.add(behavior = getBehavior());
        return datatable;
    }

    protected final ListDataTable<Node> newListDataTable(String id, TableDefinition<Node> tableDefinition,
                                                         ISortableDataProvider<Node> dataProvider, ListDataTable.TableSelectionListener<Node> selectionListener, boolean triState,
                                                         ListPagingDefinition pagingDefinition) {
        return super.getListDataTable(id, tableDefinition, dataProvider, selectionListener,
                triState, pagingDefinition);
    }

    protected WidgetBehavior getBehavior() {
        return new WidgetBehavior(new WidgetSettings());
    }

    protected abstract List<ListColumn<Node>> getCollapsedColumns();

    protected abstract List<ListColumn<Node>> getExpandedColumns();

}
