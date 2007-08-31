package org.hippoecm.repository.frontend.plugin;

import org.apache.wicket.Component;
import org.apache.wicket.IClusterable;
import org.apache.wicket.Component.IVisitor;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.hippoecm.repository.frontend.model.JcrNodeModel;
import org.hippoecm.repository.frontend.plugin.config.PluginDescriptor;

public class PluginManager implements IClusterable {
    private static final long serialVersionUID = 1L;

    private Plugin rootPlugin;

    public PluginManager(Plugin rootPlugin) {
        this.rootPlugin = rootPlugin;
    }

    public void notifyPlugins(final AjaxRequestTarget target, final JcrNodeModel model) {
        rootPlugin.visitChildren(Plugin.class, new IVisitor() {
            public Object component(Component component) {
                Plugin plugin = (Plugin) component;
                if (plugin instanceof ContextPlugin) {
                    String newPluginClassname = ((ContextPlugin) plugin).newPluginClass(model);
                    if (newPluginClassname != null) {
                        PluginDescriptor pluginDescriptor = new PluginDescriptor(plugin.getPath(), newPluginClassname);
                        Plugin newPlugin = new PluginFactory(pluginDescriptor).getPlugin(model);
                        newPlugin.setRenderBodyOnly(true);
                        rootPlugin.replace(newPlugin);
                        target.addComponent(rootPlugin);
                    }
                }
                plugin.update(target, model);

                return IVisitor.CONTINUE_TRAVERSAL;
            }
        });
    }

}
