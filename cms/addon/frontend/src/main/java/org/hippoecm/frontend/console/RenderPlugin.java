package org.hippoecm.frontend.console;

import org.hippoecm.frontend.core.Plugin;
import org.hippoecm.frontend.core.PluginContext;
import org.hippoecm.frontend.service.RenderService;

public class RenderPlugin extends RenderService implements Plugin {
    private static final long serialVersionUID = 1L;

    public static final String WICKET_ID = "wicket";

    public void start(PluginContext context) {
        String wicketId = context.getProperty(WICKET_ID);
        init(context, wicketId);

        context.registerService(this, WICKET_ID);
    }

    public void stop() {
        PluginContext context = getPluginContext();
        context.unregisterService(this, WICKET_ID);

        destroy(context);
    }

}
