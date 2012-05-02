/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.frontend.plugin;

import org.hippoecm.frontend.plugin.config.IPluginConfig;

/**
 * Base class for plugins.  Implements the IPlugin lifecycle events with no-ops, storing references
 * to the {@link IPluginContext} and {@link IPluginConfig}.
 */
public abstract class Plugin implements IPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private IPluginContext context;
    private IPluginConfig config;

    /**
     * Construct a new Plugin.
     *
     * @param context the plugin context
     * @param config the plugin config
     */
    public Plugin(IPluginContext context, IPluginConfig config) {
        this.context = context;
        this.config = config;
    }

    /**
     * The {@link IPluginContext} for the plugin.
     */
    protected IPluginContext getPluginContext() {
        return context;
    }

    /**
     * The {@link IPluginConfig} for the plugin.
     */
    protected IPluginConfig getPluginConfig() {
        return config;
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

}
