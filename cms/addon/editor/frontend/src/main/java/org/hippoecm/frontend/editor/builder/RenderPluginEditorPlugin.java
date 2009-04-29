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
package org.hippoecm.frontend.editor.builder;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.behaviors.EventStoppingDecorator;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RenderPluginEditorPlugin extends RenderPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(RenderPluginEditorPlugin.class);

    private String pluginId;

    public RenderPluginEditorPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        pluginId = config.getString("plugin.id");
        boolean editable = config.getBoolean("builder.mode");

        add(new AjaxLink("up") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                IClusterConfig clusterConfig = (IClusterConfig) RenderPluginEditorPlugin.this.getModelObject();
                List<IPluginConfig> plugins = clusterConfig.getPlugins();
                Map<String, IPluginConfig> last = new TreeMap<String, IPluginConfig>();
                for (IPluginConfig config : plugins) {
                    if (config.getName().equals(pluginId)) {
                        IPluginConfig previous = last.get(config.getString("wicket.id"));
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
                        last.put(config.getString("wicket.id"), config);
                    }
                }
            }
        }.setVisible(editable));
        add(new AjaxLink("down") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                IClusterConfig clusterConfig = (IClusterConfig) RenderPluginEditorPlugin.this.getModelObject();
                List<IPluginConfig> plugins = clusterConfig.getPlugins();
                int index = 0;
                for (IPluginConfig config : plugins) {
                    if (config.getName().equals(pluginId)) {
                        if (index < plugins.size() - 1) {
                            IPluginConfig previous = plugins.remove(index + 1);
                            plugins.add(index, previous);
                        } else {
                            log.warn("Unable to move the first plugin further up");
                        }
                        break;
                    }
                    index++;
                }
            }
        }.setVisible(editable));
        add(new AjaxLink("remove") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                IClusterConfig clusterConfig = (IClusterConfig) RenderPluginEditorPlugin.this.getModelObject();
                List<IPluginConfig> plugins = clusterConfig.getPlugins();
                for (IPluginConfig config : plugins) {
                    if (config.getName().equals(pluginId)) {
                        IModelReference pluginRef = context.getService(config.getString("model.plugin"),
                                IModelReference.class);
                        if (pluginRef != null && pluginRef.getModel() != null
                                && pluginId.equals(pluginRef.getModel().getObject())) {
                            pluginRef.setModel(null);
                        }
                        plugins.remove(config);
                        break;
                    }
                }
            }
        }.setVisible(editable));

        add(new AjaxEventBehavior("onclick") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                IModelReference pluginRef = context.getService(config.getString("model.plugin"), IModelReference.class);
                pluginRef.setModel(new Model(pluginId));
            }

            @Override
            protected IAjaxCallDecorator getAjaxCallDecorator() {
                return new EventStoppingDecorator(super.getAjaxCallDecorator());
            }
        });
        addExtensionPoint("preview");
    }

    protected IPluginConfig getEffectivePluginConfig() {
        return getPluginConfig().getPluginConfig("model.effective");
    }

}
