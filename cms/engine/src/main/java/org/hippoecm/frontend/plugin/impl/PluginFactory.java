/*
 *  Copyright 2008-2019 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.frontend.plugin.PluginInstantiationException;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugin.error.ErrorPlugin;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.service.render.RenderService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.hst.diagnosis.HDC;
import org.hippoecm.hst.diagnosis.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginFactory implements IClusterable {

    private static final Logger log = LoggerFactory.getLogger(PluginFactory.class);

    public PluginFactory() {
    }

    public IPlugin createPlugin(final PluginContext context, final IPluginConfig config) {
        Task createPluginTask = null;

        try {
            String className = config.getString(IPlugin.CLASSNAME);

            if (HDC.isStarted()) {
                createPluginTask = HDC.getCurrentTask().startSubtask(this.getClass().getSimpleName() + ".createPlugin");
                createPluginTask.setAttribute("pluginClass", className != null ? className : "unknown");
            }

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
                    final Class<? extends IPlugin> clazz = (Class<? extends IPlugin>) Class.forName(className, true, loader);
                    final Class<?>[] formalArgs = new Class[]{IPluginContext.class, IPluginConfig.class};
                    final Constructor<? extends IPlugin> constructor = clazz.getConstructor(formalArgs);
                    final Object[] actualArgs = new Object[]{context, config};
                    plugin = constructor.newInstance(actualArgs);

                } catch (ClassNotFoundException e) {
                    final ResourceSettings resourceSettings = Application.get().getResourceSettings();
                    final IResourceStreamLocator locator = resourceSettings.getResourceStreamLocator();
                    final IResourceStream stream = locator.locate(null, className.replace('.', '/') + ".html");
                    if (stream != null) {
                        plugin = new LayoutPlugin(context, config, stream);
                    } else {
                        message = e.getClass().getName() + ": " + e.getMessage();
                        log.error(message, e);
                    }

                } catch (InvocationTargetException e) {
                    final Throwable targetException = e.getTargetException();

                    if (targetException instanceof RestartResponseException) {
                        throw (RestartResponseException) targetException;
                    }
                    if (e.getTargetException() instanceof PluginInstantiationException) {
                        if (log.isDebugEnabled()) {
                            log.info("Ignore plugin '{}' :", config.getName(), e);
                        } else {
                            log.info("Ignore plugin '{}' :", config.getName(), e.getMessage());
                        }
                        // org.hippoecm.frontend.plugin.impl.PluginManager.start() will invoke context.stop() when
                        // returning null cleaning up the context
                        context.reset();
                        return null;
                    }

                    if (targetException instanceof PluginInstantiationException) {
                        if (log.isDebugEnabled()) {
                            log.info("Ignore plugin '{}'", config.getName(), e);
                        } else {
                            log.info("Ignore plugin '{}' : {}", config.getName(), e.getMessage());
                        }
                        // org.hippoecm.frontend.plugin.impl.PluginManager.start() will invoke context.stop() when
                        // returning null cleaning up the context
                        context.reset();
                        return null;
                    }

                    message = targetException.getClass().getName() + ": " + targetException.getMessage();
                    log.error(message, e);
                } catch (Exception e) {
                    message = e.getClass().getName() + ": " + e.getMessage();
                    log.error(message, e);
                }
            }

            if (plugin == null && message != null) {
                final String wicketId = config.getString(RenderService.WICKET_ID);
                message += String.format("\nFailed to instantiate plugin class '%s' for wicket id '%s' in plugin '%s' (%s)",
                        className, wicketId, config.getName(), config);

                // reset context, i.e. unregister everything that was registered so far
                context.reset();

                if (wicketId != null) {
                    final IPluginConfig errorConfig = new JavaPluginConfig();
                    errorConfig.put(ErrorPlugin.ERROR_MESSAGE, message);
                    errorConfig.put(RenderService.WICKET_ID, wicketId);
                    plugin = new ErrorPlugin(context, errorConfig);
                } else {
                    log.error(message);
                }
            }
            return plugin;
        } finally {
            if (createPluginTask != null) {
                createPluginTask.stop();
            }
        }
    }

    private static class LayoutPlugin extends RenderPlugin implements IMarkupCacheKeyProvider, IMarkupResourceStreamProvider {

        private final IResourceStream stream;

        public LayoutPlugin(final IPluginContext context, final IPluginConfig config, final IResourceStream stream) {
            super(context, config);

            this.stream = stream;
        }

        public String getCacheKey(final MarkupContainer container, final Class<?> containerClass) {
            return getPluginConfig().getString(IPlugin.CLASSNAME);
        }

        // implement IMarkupResourceStreamProvider.
        public IResourceStream getMarkupResourceStream(final MarkupContainer container, final Class<?> containerClass) {
            return stream;
        }
    }

}
