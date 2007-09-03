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
import org.hippoecm.repository.frontend.plugin.error.ErrorPlugin;

public class PluginFactory {

    private PluginDescriptor descriptor;

    public PluginFactory(PluginDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    public Plugin getPlugin(JcrNodeModel model) {
        Plugin plugin;
        if (descriptor.getClassName() == null) {
            String message = "Implementation class name for plugin '" + descriptor.getId()
                    + "' could not be retrieved from configuration.";
            plugin = new ErrorPlugin(descriptor.getId(), null, message);
        } else {
            try {
                Class clazz = Class.forName(descriptor.getClassName());
                Class[] formalArgs = new Class[] { String.class, JcrNodeModel.class };
                Constructor constructor = clazz.getConstructor(formalArgs);
                Object[] actualArgs = new Object[] { descriptor.getId(), model };
                plugin = (Plugin) constructor.newInstance(actualArgs);
            } catch (Exception e) {
                String message = "Failed to instantiate plugin implementation '" + descriptor.getClassName()
                        + "' for id '" + descriptor.getId() + "'.";
                plugin = new ErrorPlugin(descriptor.getId(), e, message);
            }
        }
        return plugin;
    }

}
