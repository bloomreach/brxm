package org.hippoecm.frontend.plugins.yui.layout;

import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IBehaviorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PageLayoutPlugin extends PageLayoutBehavior implements IPlugin, IBehaviorService, IDetachable {
    private static final long serialVersionUID = 1L;
    
    private final static Logger log = LoggerFactory.getLogger(PageLayoutPlugin.class);

    private IPluginConfig config;

    public PageLayoutPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        this.config = config;
        context.registerService(this, config.getString(ID));
    }
    
    public String getComponentPath() {
        return config.getString(IBehaviorService.PATH);
    }

    public void detach() {
        config.detach();
    }

}
