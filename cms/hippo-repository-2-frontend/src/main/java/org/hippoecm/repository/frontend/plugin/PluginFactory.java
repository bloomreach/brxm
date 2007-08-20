/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.frontend.plugin;

import java.lang.reflect.Constructor;

import org.hippoecm.repository.frontend.model.JcrNodeModel;
import org.hippoecm.repository.frontend.plugin.config.PluginJavaConfig;
import org.hippoecm.repository.frontend.plugin.error.ErrorPlugin;

public class PluginFactory {

    private String pluginClass;

    public PluginFactory(String classname) {
        this.pluginClass = classname;
    }

    public Plugin getPlugin(String id, JcrNodeModel model) {
        Plugin plugin;
        if (pluginClass == null) {
            String message = "Implementation class name for plugin '" + id
                    + "' could not be retrieved from configuration, falling back to default plugin.";
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
        String defaultPluginClassname = new PluginJavaConfig().getPluginMap().get(id).toString();
        try {
            plugin = loadPlugin(id, model, defaultPluginClassname);
        } catch (Exception e) {
            String message = "Failed to instantiate default plugin, something is seriously wrong.";
            plugin = new ErrorPlugin(id, e, message);
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
