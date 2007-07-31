package org.hippocms.repository.frontend.plugin;

import java.lang.reflect.Constructor;

import org.hippocms.repository.frontend.model.JcrNodeModel;
import org.hippocms.repository.frontend.plugin.config.PluginJavaConfig;
import org.hippocms.repository.frontend.plugin.error.ErrorPlugin;

public class PluginFactory {

    private String pluginClass;

    public PluginFactory(String classname) {
        this.pluginClass = classname;
    }

    public Plugin getPlugin(String id, JcrNodeModel model) {
        Plugin plugin;
        if (pluginClass == null) {
            String message = "Implementation class name for plugin '" + id
                    + "' could not be retreived from configuration, falling back to default plugin.";
            System.err.println(message);
            plugin = getDefaultPlugin(id, model);
        } else {
            try {
                plugin = loadPlugin(id, model, pluginClass);
            } catch (Exception e) {
                String message = "Failed to instantiate plugin implementation '" + pluginClass + "' for id '" + id
                        + "', falling back to default plugin.";
                System.err.println(message);
                plugin = getDefaultPlugin(id, model);
            }
        }
        return plugin;
    }

    private Plugin getDefaultPlugin(String id, JcrNodeModel model) {
        Plugin plugin;
        String defaultPluginClassname = new PluginJavaConfig().pluginClassname(id);
        try {
            plugin = loadPlugin(id, model, defaultPluginClassname);
        } catch (Exception e) {
            String message = "Failed to instantiate default plugin, something is seriously wrong.";
            plugin = new ErrorPlugin(id, model, e, message);
        }
        return plugin;
    }

    private Plugin loadPlugin(String id, JcrNodeModel model, String classname) throws Exception {
        Class clazz = Class.forName(classname);
        Class[] formalArgs = new Class[] { String.class, JcrNodeModel.class };
        Constructor constructor = clazz.getConstructor(formalArgs);
        Object[] actualArgs = new Object[] { id, model };
        return (Plugin) constructor.newInstance(actualArgs);
    }

}
