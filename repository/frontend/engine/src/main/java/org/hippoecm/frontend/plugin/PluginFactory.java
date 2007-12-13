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
package org.hippoecm.frontend.plugin;

import java.lang.reflect.Constructor;

import org.apache.wicket.Application;
import org.hippoecm.frontend.Main;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.error.ErrorPlugin;

public class PluginFactory {

    private PluginManager pluginManager;

    public PluginFactory(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    public Plugin createPlugin(PluginDescriptor descriptor, JcrNodeModel model, Plugin parentPlugin) {
        Plugin plugin;
        if (descriptor.getClassName() == null) {
            String message = "Implementation class name for plugin '" + descriptor
                    + "' could not be retrieved from configuration.";
            plugin = new ErrorPlugin(descriptor, null, message);
        } else {
            try {
                ClassLoader loader = ((Main) Application.get()).getRepository().getClassLoader();
                Class clazz = Class.forName(descriptor.getClassName(), true, loader);
                Class[] formalArgs = new Class[] { PluginDescriptor.class, JcrNodeModel.class, Plugin.class };
                Constructor constructor = clazz.getConstructor(formalArgs);
                Object[] actualArgs = new Object[] { descriptor, model, parentPlugin };
                plugin = (Plugin) constructor.newInstance(actualArgs);
            } catch (Exception e) {
                String message = "Failed to instantiate plugin '" + descriptor.getClassName() + "' for id '"
                        + descriptor + "'.";
                plugin = new ErrorPlugin(descriptor, e, message);
            }
        }
        pluginManager.registerPlugin(plugin);
        return plugin;
    }

}
