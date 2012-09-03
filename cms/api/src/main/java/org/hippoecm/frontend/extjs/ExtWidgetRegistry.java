package org.hippoecm.frontend.extjs;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.JavascriptPackageResource;
import org.apache.wicket.markup.html.internal.HtmlHeaderContainer;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.wicketstuff.js.ext.ExtContainer;

/**
 * Central registry for the configuration of Ext widgets. The configuration of these widgets
 * can be retrieved in Javascript via the xtype of the widget:
 * <pre>
 * var config = Hippo.ExtWidgets.getConfig('somextype')
 * </pre>
 * @see ExtWidget
 */
public class ExtWidgetRegistry extends ExtContainer {

    public static final String EXT_WIDGET_SERVICE_ID = ExtWidgetRegistry.class.getName();

    private final IPluginContext context;
    private boolean renderedHead;

    public ExtWidgetRegistry(String id, IPluginContext context) {
        super(id);

        this.context = context;

        add(JavascriptPackageResource.getHeaderContribution(ExtWidgetRegistry.class, "ExtWidgetRegistry.js"));
    }

    @Override
    protected void onBeforeRender() {
        List<ExtWidget> widgets = context.getServices(EXT_WIDGET_SERVICE_ID, ExtWidget.class);
        for (ExtWidget widget : widgets) {
            add(widget);
        }
        super.onBeforeRender();
    }

    /**
     * Prevent that we render the head more than once, because it is explicitly rendered before other head contributions.
     * This way we force the initialization of the component registry before the initialization of child
     * components, so Ext widget configurations can always be accessed in constructors of normal Ext components.
     *
     * @param container the HTML container to the head to
     */
    @Override
    public void renderHead(final HtmlHeaderContainer container) {
        if (!renderedHead) {
            super.renderHead(container);
            renderedHead = true;
        }
    }

    /**
     * Do no include Ext widgets in the normal Ext component hierarchy. That way the head contributions
     * and properties of Ext widgets are still initialized like other Ext components, but they do not become
     * child items of the parent Ext component.
     *
     * @return the list of child components without any Ext widgets.
     */
    @Override
    public List<Component> getItems() {
        List<Component> items = super.getItems();
        List<Component> filteredItems = new ArrayList<Component>();
        for (Component component : items) {
            if (!(component instanceof ExtWidget)) {
                filteredItems.add(component);
            }
        }
        return filteredItems;
    }

}
