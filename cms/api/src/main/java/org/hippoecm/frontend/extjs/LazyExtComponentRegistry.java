package org.hippoecm.frontend.extjs;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.JavascriptPackageResource;
import org.apache.wicket.markup.html.internal.HtmlHeaderContainer;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.wicketstuff.js.ext.ExtContainer;

/**
 * Central registry for the configuration of lazy Ext components. The configuration of these lazy components
 * can be retrieved in Javascript via the xtype of the lazy component:
 * <pre>
 * var config = Hippo.LazyExtComponents.getConfig('somextype')
 * </pre>
 * @see LazyExtComponent
 */
public class LazyExtComponentRegistry extends ExtContainer {

    public static final String LAZY_EXT_COMPONENT_SERVICE_ID = LazyExtComponentRegistry.class.getName();

    private final IPluginContext context;
    private boolean renderedHead;

    public LazyExtComponentRegistry(IPluginContext context) {
        super("lazyExtComponentRegistry");

        this.context = context;

        add(JavascriptPackageResource.getHeaderContribution(LazyExtComponentRegistry.class, "LazyExtComponentRegistry.js"));
    }

    @Override
    protected void onBeforeRender() {
        List<LazyExtComponent> lazyExtComponents = context.getServices(LAZY_EXT_COMPONENT_SERVICE_ID, LazyExtComponent.class);
        for (LazyExtComponent lazyComponent : lazyExtComponents) {
            add(lazyComponent);
        }
        super.onBeforeRender();
    }

    /**
     * Prevent that we render the head more than once, because it is explicitly rendered before other head contributions.
     * This way we force the initialization of the component registry before the initialization of child
     * components, so lazy component configurations can always be accessed in constructors of non-lazy Ext objects.
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
     * Do no include lazy Ext components in the normal Ext component hierarchy. That way the head contributions
     * and properties of lazy Ext component are still initialized like other Ext components, but they do not become
     * child items of the parent Ext component.
     *
     * @return the list of child components without any lazy Ext components.
     */
    @Override
    public List<Component> getItems() {
        List<Component> items = super.getItems();
        List<Component> filteredItems = new ArrayList<Component>();
        for (Component component : items) {
            if (!(component instanceof LazyExtComponent)) {
                filteredItems.add(component);
            }
        }
        return filteredItems;
    }

}
