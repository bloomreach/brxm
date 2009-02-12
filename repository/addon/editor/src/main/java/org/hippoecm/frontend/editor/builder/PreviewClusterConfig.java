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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JcrClusterConfig;
import org.hippoecm.frontend.plugin.config.impl.JcrPluginConfig;

public class PreviewClusterConfig extends JcrClusterConfig implements IObservable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static int instanceCount = 0;
    private String pluginConfigModel;
    private String engineId;
    private IPluginContext pluginContext;
    private IObservationContext observationContext;
    private List<IObserver> observers;

    public PreviewClusterConfig(IPluginContext context, JcrNodeModel node, ModelReference model, String engineId) {
        super(node);

        this.pluginContext = context;
        this.engineId = engineId;
        this.pluginConfigModel = context.getReference(model).getServiceId();
    }

    public void setObservationContext(IObservationContext context) {
        observationContext = context;
    }

    public void startObservation() {
        List<IPluginConfig> plugins = super.getPlugins();
        observers = new ArrayList<IObserver>(plugins.size());
        for (final IPluginConfig config : plugins) {
            IObserver observer = new IObserver() {
                private static final long serialVersionUID = 1L;

                public IObservable getObservable() {
                    return ((JcrPluginConfig) config).getNodeModel();
                }

                public void onEvent(IEvent event) {
                    observationContext.notifyObservers(event);
                }
            };
            observers.add(observer);
            pluginContext.registerService(observer, IObserver.class.getName());
        }
    }

    public void stopObservation() {
        for (IObserver observer : observers) {
            pluginContext.unregisterService(observer, IObserver.class.getName());
        }
        observers = null;
    }

    @Override
    public List<IPluginConfig> getPlugins() {
        List<IPluginConfig> plugins = super.getPlugins();
        List<IPluginConfig> result = new LinkedList<IPluginConfig>();
        for (final IPluginConfig config : plugins) {
            if (config.get("wicket.id") != null && !"${wicket.id}".equals(config.get("wicket.id"))) {
                final String wrappedId = PreviewClusterConfig.class.getName() + "." + newId();
                IPluginConfig previewWrapper = new JavaPluginConfig(config.getName() + "-preview");
                previewWrapper.put("plugin.class", PreviewPluginPlugin.class.getName());
                previewWrapper.put("wicket.id", config.get("wicket.id"));
                previewWrapper.put("wicket.model", pluginConfigModel);
                previewWrapper.put("engine", engineId);
                previewWrapper.put("preview", wrappedId);

                JcrNodeModel pluginNodeModel = ((JcrPluginConfig) config).getNodeModel();
                previewWrapper.put("plugin.node.path", pluginNodeModel.getItemModel().getPath());

                result.add(previewWrapper);

                IPluginConfig wrappedPluginConfig = new JavaPluginConfig(config);
                wrappedPluginConfig.put("wicket.id", wrappedId);
                result.add(wrappedPluginConfig);
            } else {
                result.add(config);
            }
        }
        return result;
    }

    private int newId() {
        synchronized (PreviewClusterConfig.class) {
            return instanceCount++;
        }
    }

}
