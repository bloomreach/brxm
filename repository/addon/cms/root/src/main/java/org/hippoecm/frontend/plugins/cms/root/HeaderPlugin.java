package org.hippoecm.frontend.plugins.cms.root;

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;

public class HeaderPlugin extends RenderPlugin {
    private static final long serialVersionUID = 1L;

    public HeaderPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        addExtensionPoint("logoutPlugin");
    }

}
