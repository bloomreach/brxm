package org.hippoecm.frontend.plugins.yui;

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.javascript.Settings;
import org.hippoecm.frontend.plugins.yui.webapp.IYuiManager;

public class YuiPluginHelper {

    private static final String SERVICE_ID = "service.behavior.yui";
    private static final String CONFIG_ID = "yui.config";

    public static IYuiManager getManager(IPluginContext context) {
        return context.getService(SERVICE_ID, IYuiManager.class);
    }
    
    public static IPluginConfig getConfig(IPluginConfig config) {
        return config.getPluginConfig(CONFIG_ID);
    }
}
