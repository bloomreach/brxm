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
package org.hippoecm.frontend.editor.builder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.editor.builder.IEditorContext.Mode;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;

public class BuilderContext implements IBuilderContext {
    private static final long serialVersionUID = 1L;

    public static final String PLUGIN_ID = "plugin.id";
    public static final String WICKET_MODEL = "wicket.model";
    public static final String SELECTED_PLUGIN = "model.plugin";

    protected final IPluginContext context;
    protected final IPluginConfig config;

    private String pluginId;
    private List<IBuilderListener> listeners;

    public BuilderContext(IPluginContext context, IPluginConfig config) {
        this.context = context;
        this.config = config;

        this.pluginId = config.getString(PLUGIN_ID);
        this.listeners = new LinkedList<IBuilderListener>();

        final String pluginId = config.getString(PLUGIN_ID);
        final IModelReference helperModelRef = context.getService(config.getString(SELECTED_PLUGIN),
                IModelReference.class);
        if (helperModelRef != null) {
            context.registerService(new IObserver() {
                private static final long serialVersionUID = 1L;

                private boolean focussed = isFocussed();

                public IObservable getObservable() {
                    return helperModelRef;
                }

                boolean isFocussed() {
                    return helperModelRef.getModel() != null && pluginId.equals(helperModelRef.getModel().getObject());
                }

                public void onEvent(Iterator<? extends IEvent> event) {
                    if (isFocussed()) {
                        focussed = true;
                        for (IBuilderListener listener : new ArrayList<IBuilderListener>(BuilderContext.this.listeners)) {
                            listener.onFocus();
                        }
                    } else if (focussed) {
                        focussed = false;
                        for (IBuilderListener listener : new ArrayList<IBuilderListener>(BuilderContext.this.listeners)) {
                            listener.onBlur();
                        }
                    }
                }

            }, IObserver.class.getName());
        }
    }

    public IPluginConfig getEditablePluginConfig() {
        IClusterConfig clusterConfig = getTemplate();
        List<IPluginConfig> plugins = clusterConfig.getPlugins();
        for (IPluginConfig plugin : plugins) {
            if (plugin.getName().equals(config.getString(PLUGIN_ID))) {
                return plugin;
            }
        }
        return null;
    }

    public void delete() {
        IClusterConfig clusterConfig = getTemplate();
        List<IPluginConfig> plugins = clusterConfig.getPlugins();
        for (IPluginConfig config : plugins) {
            if (config.getName().equals(getPluginId())) {
                IModelReference pluginRef = context
                        .getService(config.getString(SELECTED_PLUGIN), IModelReference.class);
                if (pluginRef != null && pluginRef.getModel() != null
                        && getPluginId().equals(pluginRef.getModel().getObject())) {
                    pluginRef.setModel(null);
                }
                plugins.remove(config);
                break;
            }
        }
    }

    public void focus() {
        IModelReference pluginRef = context.getService(config.getString(SELECTED_PLUGIN), IModelReference.class);
        pluginRef.setModel(new Model(getPluginId()));
    }

    public Mode getMode() {
        if ("view".equals(config.getString("builder.mode", "view"))) {
            return IEditorContext.Mode.VIEW;
        }
        return IEditorContext.Mode.EDIT;
    }

    protected String getPluginId() {
        return pluginId;
    }

    protected IClusterConfig getTemplate() {
        IModel model = context.getService(config.getString(WICKET_MODEL), IModelReference.class).getModel();
        return (IClusterConfig) model.getObject();
    }

    public void addBuilderListener(IBuilderListener listener) {
        listeners.add(listener);
    }

    public void removeBuilderListener(IBuilderListener listener) {
        listeners.remove(listener);
    }

}
