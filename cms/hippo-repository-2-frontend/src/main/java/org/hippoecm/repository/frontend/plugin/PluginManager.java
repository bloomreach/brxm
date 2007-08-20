package org.hippoecm.repository.frontend.plugin;

import java.util.Iterator;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.IClusterable;
import org.apache.wicket.Component.IVisitor;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.hippoecm.repository.frontend.HomePlugin;
import org.hippoecm.repository.frontend.model.JcrNodeModel;
import org.hippoecm.repository.frontend.plugin.config.PluginConfig;

public class PluginManager implements IClusterable {
    private static final long serialVersionUID = 1L;

    private final HomePlugin homePlugin;

    public PluginManager(HomePlugin homePlugin, PluginConfig pluginConfig, JcrNodeModel model) {
        this.homePlugin = homePlugin;
        Map pluginMap = pluginConfig.getPluginMap();
                
        Iterator it = pluginMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            String id = entry.getKey().toString();
            String classname = entry.getValue().toString();
            Plugin plugin = new PluginFactory(classname).getPlugin(id, model);
            homePlugin.add(plugin);
        }
    }

    public void notifyPlugins(final AjaxRequestTarget target, final JcrNodeModel model) {
        homePlugin.visitChildren(Plugin.class, new IVisitor() {
            public Object component(Component component) {
                Plugin plugin = (Plugin) component;
                if (plugin instanceof ContextPlugin) {
                    String newPluginClassname = ((ContextPlugin) plugin).newPluginClass(model);
                    if (newPluginClassname != null) {
                        Plugin newPlugin = new PluginFactory(newPluginClassname).getPlugin(plugin.getId(), model);
                        newPlugin.setRenderBodyOnly(true);
                        homePlugin.replace(newPlugin);
                        target.addComponent(homePlugin);
                    }
                }
                plugin.update(target, model);

                return IVisitor.CONTINUE_TRAVERSAL;
            }
        });
    }

}
