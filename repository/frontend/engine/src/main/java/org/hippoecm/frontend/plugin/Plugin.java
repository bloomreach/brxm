package org.hippoecm.frontend.plugin;

import org.hippoecm.frontend.plugin.config.IPluginConfig;

public abstract class Plugin implements IPlugin {
    private static final long serialVersionUID = 1L;

    private IPluginContext context;
    private IPluginConfig config;
    
    public Plugin(IPluginContext context, IPluginConfig config) {
        this.context = context;
        this.config = config;
    }

    /**
     * The {@link IPluginContext} for the plugin.
     */
    protected IPluginContext getPluginContext() {
        return context;
    }

    /**
     * The {@link IPluginConfig} for the plugin.
     */
    protected IPluginConfig getPluginConfig() {
        return config;
    }
    
    public void start() {
    }

    public void stop() {
    }

}
