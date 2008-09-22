package org.hippoecm.frontend.plugin.loader;

import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfigService;

/**
 * Plugin loads all the plugins from a specified cluster
 * 
 * @author Jeroen Tietema
 */
public class PluginClusterLoader implements IPlugin {

    private static final long serialVersionUID = 1L;

    public static final String PLUGIN_CLUSTER = "plugin.cluster";

    public PluginClusterLoader(IPluginContext context, IPluginConfig config) {
        IPluginConfigService pluginConfigService = context.getService(IPluginConfigService.class.getName(),
                IPluginConfigService.class);

        IClusterConfig cluster = pluginConfigService.getCluster(config.getString(PLUGIN_CLUSTER));
        context.start(cluster);
    }

}
