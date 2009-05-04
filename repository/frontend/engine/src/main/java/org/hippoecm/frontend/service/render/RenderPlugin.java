/*
 *  Copyright 2008 Hippo.
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

import java.util.AbstractList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.wicket.Component;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.AbstractPluginDecorator;
import org.hippoecm.frontend.plugin.config.impl.JavaClusterConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.service.IRenderService;

public class RenderPlugin extends RenderService implements IPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    class PluginEntry {
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

    @Override
    public void render(PluginRequestTarget target) {
        super.render(target);

        for (PluginEntry entry : childPlugins.values()) {
            entry.render(target);
        }
    }

    protected Component newPlugin(String id, IPluginConfig config) {
        IPluginContext pluginContext = getPluginContext();
        JavaClusterConfig childClusterConfig = new JavaClusterConfig();
        IPluginConfig childPluginConfig = new InheritingPluginConfig(new JavaPluginConfig(config));

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

    private class InheritingPluginConfig extends AbstractPluginDecorator {
        private static final long serialVersionUID = 1L;

        InheritingPluginConfig(IPluginConfig upstream) {
            super(upstream);
        }

        @Override
        protected Object decorate(Object object) {
            if (object instanceof String) {
                String value = (String) object;
                if (value.startsWith("${") && value.endsWith("}")) {
                    return RenderPlugin.this.getPluginConfig().get(value.substring(2, value.length() - 1));
                } else {
                    return value;
                }
            } else if (object instanceof IPluginConfig) {
                return new InheritingPluginConfig((IPluginConfig) object);
            } else if (object instanceof List) {
                final List list = (List) object;
                return new AbstractList() {
                    @Override
                    public Object get(int index) {
                        return decorate(list.get(index));
                    }

                    @Override
                    public int size() {
                        return list.size();
                    }
                };
            }
            return object;
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
