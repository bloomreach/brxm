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

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.error.ErrorPlugin;

public class PluginFactory {

    private PluginDescriptor pluginDescriptor;

    public PluginFactory(PluginDescriptor descriptor) {
        this.pluginDescriptor = descriptor;
    }

    public Plugin getPlugin(JcrNodeModel model) {
        Plugin plugin;
        if (pluginDescriptor.getClassName() == null) {
            String message = "Implementation class name for plugin '" + pluginDescriptor.getId()
                    + "' could not be retrieved from configuration.";
            plugin = new ErrorPlugin(pluginDescriptor.getId(), null, message);
        } else {
            try {
                Class clazz = Class.forName(pluginDescriptor.getClassName());
                Class[] formalArgs = new Class[] { String.class, JcrNodeModel.class };
                Constructor constructor = clazz.getConstructor(formalArgs);
                Object[] actualArgs = new Object[] { pluginDescriptor.getId(), model };
                plugin = (Plugin) constructor.newInstance(actualArgs);
            } catch (Exception e) {
                String message = "Failed to instantiate plugin '" + pluginDescriptor.getClassName()
                        + "' for id '" + pluginDescriptor.getId() + "'.";
                plugin = new ErrorPlugin(pluginDescriptor.getId(), e, message);
            }
        }
        return plugin;
    }

}
