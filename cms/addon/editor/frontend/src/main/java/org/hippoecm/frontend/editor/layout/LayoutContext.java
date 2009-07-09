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
package org.hippoecm.frontend.editor.layout;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hippoecm.frontend.editor.builder.BuilderContext;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Context for layout editing plugins.  It implements the ILayoutContext interface
 * using the plugin.id config variable.
 */
public class LayoutContext extends BuilderContext implements ILayoutContext {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(LayoutContext.class);

    public static final String WICKET_ID = "wicket.id";

    public LayoutContext(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    public ILayoutPad getLocation() {
        return context.getService(getWicketId(), ILayoutPad.class);
    }

    public void apply(ILayoutTransition transition) {
        if ("up".equals(transition.getName())) {
            IClusterConfig clusterConfig = getTemplate();
            List<IPluginConfig> plugins = clusterConfig.getPlugins();
            Map<String, IPluginConfig> last = new TreeMap<String, IPluginConfig>();
            for (IPluginConfig config : plugins) {
                if (config.getName().equals(getPluginId())) {
                    IPluginConfig previous = last.get(getWicketId());
                    if (previous != null) {
                        IPluginConfig backup = new JavaPluginConfig(config);
                        config.clear();
                        config.putAll(previous);

                        previous.clear();
                        previous.putAll(backup);
                    } else {
                        log.warn("Unable to move the first plugin further up");
                    }
                    break;
                }
                if (config.getString("wicket.id") != null) {
                    last.put(config.getString(WICKET_ID), config);
                }
            }
        } else if ("down".equals(transition.getName())) {
            IClusterConfig clusterConfig = getTemplate();
            List<IPluginConfig> plugins = clusterConfig.getPlugins();
            IPluginConfig previous = null;
            for (IPluginConfig config : plugins) {
                if (config.getName().equals(getPluginId())) {
                    previous = config;
                    if (previous.getString(WICKET_ID) == null) {
                        log.warn("No wicket.id present; cannot move plugin");
                        break;
                    }
                } else if (previous != null && previous.getString(WICKET_ID).equals(getWicketId())) {
                    IPluginConfig backup = new JavaPluginConfig(config);
                    config.clear();
                    config.putAll(previous);

                    previous.clear();
                    previous.putAll(backup);
                    break;
                }
            }
        }
    }

    protected String getWicketId() {
        return config.getString(WICKET_ID);
    }
}
