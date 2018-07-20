/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.plugin.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.wicket.Application;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.core.util.resource.locator.IResourceStreamLocator;
import org.apache.wicket.markup.IMarkupCacheKeyProvider;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.settings.ResourceSettings;
import org.apache.wicket.util.io.IClusterable;
import org.apache.wicket.util.resource.IResourceStream;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugin.error.ErrorPlugin;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.service.render.RenderService;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginFactory implements IClusterable {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(PluginFactory.class);

    public PluginFactory() {
    }

    public IPlugin createPlugin(PluginContext context, IPluginConfig config) {
        String className = config.getString(IPlugin.CLASSNAME);
        IPlugin plugin = null;
        String message = null;
        if (className == null) {
            message = "No plugin classname configured, please set plugin configuration parameter " + IPlugin.CLASSNAME;
        } else {
            className = className.trim();
            ClassLoader loader = UserSession.get().getClassLoader();
            if (loader == null) {
                log.info("Unable to retrieve repository classloader, falling back to default classloader.");
                loader = getClass().getClassLoader();
            }
            try {
                @SuppressWarnings("unchecked")
                Class<? extends IPlugin> clazz = (Class<? extends IPlugin>) Class.forName(className, true, loader);
                Class<?>[] formalArgs = new Class[] { IPluginContext.class, IPluginConfig.class };
                Constructor<? extends IPlugin> constructor = clazz.getConstructor(formalArgs);
                Object[] actualArgs = new Object[] { context, config };
                plugin = constructor.newInstance(actualArgs);

            } catch (ClassNotFoundException e) {
                ResourceSettings resourceSettings = Application.get().getResourceSettings();
                IResourceStreamLocator locator = resourceSettings.getResourceStreamLocator();
                IResourceStream stream = locator.locate(null, className.replace('.', '/') + ".html");
                if (stream != null) {
                    plugin = new LayoutPlugin(context, config, stream);
                } else {
                    message = e.getClass().getName() + ": " + e.getMessage();
                    log.error(message, e);
                }

            } catch (InvocationTargetException e) {
                if(e.getTargetException() instanceof RestartResponseException) {
                    throw (RestartResponseException) e.getTargetException();
                }
                message = e.getTargetException().getClass().getName() + ": " + e.getTargetException().getMessage();
                log.error(message, e);
            } catch (Exception e) {
                message = e.getClass().getName() + ": " + e.getMessage();
                log.error(message, e);
            }
        }
        if (plugin == null && message != null) {
            message += "\nFailed to instantiate plugin class '" + className + "' for wicket id '"
                    + config.getString(RenderService.WICKET_ID) + "' in plugin '" + config.getName() + "' (" + config + ")";

            // reset context, i.e. unregister everything that was registered so far
            context.reset();

            if (config.getString(RenderService.WICKET_ID) != null) {
                IPluginConfig errorConfig = new JavaPluginConfig();
                errorConfig.put(ErrorPlugin.ERROR_MESSAGE, message);
                errorConfig.put(RenderService.WICKET_ID, config.getString(RenderService.WICKET_ID));
                plugin = new ErrorPlugin(context, errorConfig);
            } else {
                log.error(message);
            }
        }
        return plugin;
    }

    private static class LayoutPlugin extends RenderPlugin implements IMarkupCacheKeyProvider, IMarkupResourceStreamProvider {
        private static final long serialVersionUID = 1L;

        private final IResourceStream stream;

        public LayoutPlugin(IPluginContext context, IPluginConfig config, IResourceStream stream) {
            super(context, config);

            this.stream = stream;
        }

        public String getCacheKey(MarkupContainer container, Class<?> containerClass) {
            return getPluginConfig().getString(IPlugin.CLASSNAME);
        }

        // implement IMarkupResourceStreamProvider.
        public IResourceStream getMarkupResourceStream(MarkupContainer container, Class<?> containerClass) {
            return stream;
        }
    }

}
