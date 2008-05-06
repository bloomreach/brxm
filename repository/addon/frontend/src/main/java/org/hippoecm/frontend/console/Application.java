package org.hippoecm.frontend.console;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.hippoecm.frontend.core.Plugin;
import org.hippoecm.frontend.core.PluginContext;
import org.hippoecm.frontend.core.impl.PluginConfig;

public class Application implements Plugin, Serializable {
    private static final long serialVersionUID = 1L;

    private JavaConfigService configuration;
    private List<Plugin> plugins;

    public Application() {
        configuration = new JavaConfigService();
        plugins = new LinkedList<Plugin>();
    }

    public void start(PluginContext context) {
        Iterator<PluginConfig> iter = configuration.getPlugins().iterator();
        while (iter.hasNext()) {
            plugins.add(context.start(iter.next()));
        }
    }

    public void stop() {
        Iterator<Plugin> iter = plugins.iterator();
        while (iter.hasNext()) {
            iter.next().stop();
        }
    }

}
