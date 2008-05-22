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
import java.lang.reflect.InvocationTargetException;

import org.apache.wicket.Session;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.PluginModel;
import org.hippoecm.frontend.plugin.error.ErrorPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @deprecated use org.hippoecm.frontend.sa.plugin.PluginFactory instead
 */
@Deprecated
public class PluginFactory {

    private static final Logger log = LoggerFactory.getLogger(PluginFactory.class);

    public PluginFactory(PluginManager pluginManager) {
    }

    private IPluginModel getErrorModel(String message) {
        PluginModel error = new PluginModel();
        error.put("error", message);
        return error;
    }

    public Plugin createPlugin(PluginDescriptor descriptor, IPluginModel model, Plugin parentPlugin) {
        Plugin plugin;
        if (descriptor.getClassName() == null) {
            String message = "Implementation class name for plugin '" + descriptor
                    + "' could not be retrieved from configuration.";
            plugin = new ErrorPlugin(descriptor, getErrorModel(message), parentPlugin);
        } else {
            try {
                ClassLoader loader = ((UserSession) Session.get()).getClassLoader();
                Class clazz = Class.forName(descriptor.getClassName(), true, loader);
                Class[] formalArgs = new Class[] { PluginDescriptor.class, IPluginModel.class, Plugin.class };
                Constructor constructor = clazz.getConstructor(formalArgs);
                Object[] actualArgs = new Object[] { descriptor, model, parentPlugin };
                plugin = (Plugin) constructor.newInstance(actualArgs);
            } catch (InvocationTargetException e) {
                String message = e.getTargetException().getClass().getName() + ": "
                        + e.getTargetException().getMessage() + "\n" + "Failed to instantiate plugin '"
                        + descriptor.getClassName() + "' for id '" + descriptor + "'.";
                plugin = new ErrorPlugin(descriptor, getErrorModel(message), parentPlugin);
                log.error(e.getMessage());
                e.printStackTrace();
            } catch (Exception e) {
                String message = e.getClass().getName() + ": " + e.getMessage() + "\n"
                        + "Failed to instantiate plugin '" + descriptor.getClassName() + "' for id '" + descriptor
                        + "'.";
                plugin = new ErrorPlugin(descriptor, getErrorModel(message), parentPlugin);
                log.error(e.getMessage());
                e.printStackTrace();
            }
        }
        return plugin;
    }
}
