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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.behaviors.EventStoppingDecorator;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IActivator;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaClusterConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.service.render.RenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RenderPluginEditorPlugin extends RenderPlugin implements IActivator {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(RenderPluginEditorPlugin.class);

    private String pluginId;
    protected IClusterControl previewControl;
    private IObserver configObserver;

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
                IPluginConfig previous = null;
                for (IPluginConfig config : plugins) {
                    if (config.getName().equals(pluginId)) {
                        previous = config;
                        if (previous.getString("wicket.id") == null) {
                            log.warn("No wicket.id present; cannot move plugin");
                            break;
                        }
                    } else if (previous != null
                            && previous.getString("wicket.id").equals(config.getString("wicket.id"))) {
                        IPluginConfig backup = new JavaPluginConfig(config);
                        config.clear();
                        config.putAll(previous);

                        previous.clear();
                        previous.putAll(backup);
                        break;
                    }
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

        updatePreview();
    }

    public void start() {
        final IPluginConfig editedConfig = getEditablePluginConfig();
        getPluginContext().registerService(configObserver = new IObserver() {
            private static final long serialVersionUID = 1L;

            public IObservable getObservable() {
                return editedConfig;
            }

            public void onEvent(Iterator<? extends IEvent> events) {
                updatePreview();
            }
            
        }, IObserver.class.getName());
    }

    public void stop() {
        getPluginContext().unregisterService(configObserver, IObserver.class.getName());
    }

    @Override
    public void render(PluginRequestTarget target) {
        super.render(target);

        if (previewControl != null) {
            String serviceId = getPluginContext().getReference(this).getServiceId() + ".preview";
            IRenderService previewService = getPluginContext().getService(serviceId, IRenderService.class);
            if (previewService != null) {
                previewService.render(target);
            }
        }
    }

    protected void updatePreview() {
        if (previewControl != null) {
            previewControl.stop();
            previewControl = null;
        }

        IPluginContext pluginContext = getPluginContext();
        JavaClusterConfig childClusterConfig = new JavaClusterConfig();
        IPluginConfig childPluginConfig = new JavaPluginConfig(getEffectivePluginConfig());

        String serviceId = getPluginContext().getReference(this).getServiceId() + ".preview";
        childPluginConfig.put(RenderService.WICKET_ID, serviceId);
        childClusterConfig.addPlugin(childPluginConfig);

        previewControl = pluginContext.newCluster(childClusterConfig, null);
        previewControl.start();

        IRenderService renderService = pluginContext.getService(serviceId, IRenderService.class);
        if (renderService != null) {
            renderService.bind(this, "preview");
            addOrReplace(renderService.getComponent());
        } else {
            addOrReplace(new EmptyPanel("preview"));
            log.warn("No render service found in plugin preview");
        }

        redraw();
    }

    protected IPluginConfig getEffectivePluginConfig() {
        return getPluginConfig().getPluginConfig("model.effective");
    }

    protected IPluginConfig getEditablePluginConfig() {
        IClusterConfig clusterConfig = (IClusterConfig) getModelObject();
        List<IPluginConfig> plugins = clusterConfig.getPlugins();
        for (IPluginConfig plugin : plugins) {
            if (plugin.getName().equals(getPluginConfig().getString("plugin.id"))) {
                return plugin;
            }
        }
        return null;
    }

}
