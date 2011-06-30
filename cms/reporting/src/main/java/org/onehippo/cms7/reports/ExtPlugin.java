/**
 * Copyright (c) 2011 Hippo B.V.
 */

package org.onehippo.cms7.reports;

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.wicketstuff.js.ext.ExtComponent;

/**
 * The root class for the plugins which want to be treated as ExtJS based plugins.
 * The ExtJs based plugins provide the {@link ExtComponent} rather than a plain wicket component,
 * which in turn be added to any ExtPlugin aware root component, e.g. {@link com.onehippo.cms7.reports.layout.ReportsRenderPlugin}
 */
public abstract class ExtPlugin<T> extends RenderPlugin<T> {

    private static final long serialVersionUID = 1L;

    public ExtPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    /**
     * Should return the ExtPanel that needs to be added to the parent plugin, that manages this plugin.
     *
     * @return ExtComponent - The root ExtComponent that is provided by this plugin.
     */
    public abstract ExtComponent getExtComponent();

}
