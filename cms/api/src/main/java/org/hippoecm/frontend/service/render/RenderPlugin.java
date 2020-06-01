/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.service.render;

import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.InheritingPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaClusterConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.service.IRenderService;

/**
 * Utility base class for GUI plugins.  Registers itself as an {@link IRenderService},
 * tracks extensions, etcetera.  See {@link AbstractRenderService} for a description of the
 * configuration options.
 * <p>
 * In addition, it has simple plugin-management capabilities.
 */
public class RenderPlugin<T> extends RenderService<T> implements IPlugin {

    private static final long serialVersionUID = 1L;

    private static final int FLAG_STARTING_STOPPING = FLAG_RESERVED5;

    class PluginEntry implements Serializable {
        private static final long serialVersionUID = 1L;

        IClusterControl control;
        String serviceId;

        PluginEntry(String serviceId, IClusterControl control) {
            this.serviceId = serviceId;
            this.control = control;
        }

        IRenderService getRenderService() {
            return getPluginContext().getService(serviceId, IRenderService.class);
        }

        void render(PluginRequestTarget target) {
            IRenderService renderservice = getRenderService();
            if (renderservice != null) {
                renderservice.render(target);
            }
        }
    }

    Map<String, PluginEntry> childPlugins = new TreeMap<String, PluginEntry>();
    static long childPluginCounter = 0L;

    public RenderPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    public final void start() {
        setFlag(FLAG_STARTING_STOPPING, true);
        onStart();
        if (getFlag(FLAG_STARTING_STOPPING)) {
            throw new IllegalStateException(RenderPlugin.class.getName() +
                    " has not been properly started. Something in the hierarchy of " +
                    getClass().getName() +
                    " has not called super.onStart() in the override of onStart() method");
        }
    }

    public final void stop() {
        setFlag(FLAG_STARTING_STOPPING, true);
        onStop();
        if (getFlag(FLAG_STARTING_STOPPING)) {
            throw new IllegalStateException(RenderPlugin.class.getName() +
                    " has not been properly stopped. Something in the hierarchy of " +
                    getClass().getName() +
                    " has not called super.onStop() in the override of onStop() method");
        }
    }

    /**
     * Called during the start phase of the plugin.  Services and trackers that were registered
     * during construction have been made available to other plugins.
     * <p>
     * NOTE* If you override this, you *must* call super.onStop() within your
     * implementation.
     */
    protected void onStart() {
        setFlag(FLAG_STARTING_STOPPING, false);
    }

    /**
     * Called during the stop phase of the plugin.
     * <p>
     * NOTE* If you override this, you *must* call super.onStop() within your
     * implementation.
     */
    protected void onStop() {
        setFlag(FLAG_STARTING_STOPPING, false);
    }

    @Override
    public void render(PluginRequestTarget target) {
        super.render(target);

        for (PluginEntry entry : childPlugins.values()) {
            entry.render(target);
        }
    }

    /**
     * Create a child Component with a specified id and configuration.  A cluster that
     * contains the configured plugin is started.  The component from the render service
     * of the plugin is returned, or null if no such service is available.
     * <p>
     * Only one plugin can be created with a specific component id.  When a plugin is created
     * for an id that was already used before, the old plugin is stopped.
     * <p>
     * The created plugin inherits configuration from this render plugin.
     * 
     * @param id the Wicket id of the {@link Component}
     * @param config
     * @return a Component when the configuration specifies a plugin that registers an
     *         {@link IRenderService} under its "wicket.id" key, or null when the plugin
     *         does not.
     */
    protected Component newPlugin(String id, IPluginConfig config) {
        if (config == null) {
            return new EmptyPanel(id);
        }

        IPluginContext pluginContext = getPluginContext();
        JavaClusterConfig childClusterConfig = new JavaClusterConfig();
        IPluginConfig childPluginConfig = new JavaPluginConfig(new InheritingPluginConfig(config, getPluginConfig()));

        String serviceId = getPluginContext().getReference(this).getServiceId() + "." + "id" + (++childPluginCounter);
        childPluginConfig.put(RenderService.WICKET_ID, serviceId);
        childClusterConfig.addPlugin(childPluginConfig);

        PluginEntry entry = childPlugins.get(id);
        if (entry == null) {
            entry = new PluginEntry(serviceId, null);
            childPlugins.put(id, entry);
        } else {
            entry.control.stop();
            entry.serviceId = serviceId;
        }

        entry.control = pluginContext.newCluster(childClusterConfig, null);
        entry.control.start();

        IRenderService renderservice = entry.getRenderService();
        if (renderservice != null) {
            renderservice.bind(this, id);
            return renderservice.getComponent();
        } else {
            return null;
        }
    }

    // FIXME: retrieve config from config service iso relying on the JCR implementation of the configuration
    protected Component newPlugin(String id, String name) {
        IPluginConfig pluginConfig;
        pluginConfig = getPluginConfig().getPluginConfig(name);
        if (pluginConfig == null) {
            pluginConfig = getPluginConfig().getPluginConfig("../" + name);
        }
        return newPlugin(id, pluginConfig);
    }

}
