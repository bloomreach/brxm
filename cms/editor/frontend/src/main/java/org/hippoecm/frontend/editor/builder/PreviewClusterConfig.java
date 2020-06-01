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
package org.hippoecm.frontend.editor.builder;

import java.util.Map;

import org.apache.wicket.Application;
import org.apache.wicket.core.util.resource.locator.IResourceStreamLocator;
import org.apache.wicket.settings.ResourceSettings;
import org.apache.wicket.util.resource.IResourceStream;
import org.hippoecm.frontend.editor.plugins.field.AbstractFieldPlugin;
import org.hippoecm.frontend.editor.plugins.field.FieldPluginEditorPlugin;
import org.hippoecm.frontend.editor.plugins.mixin.MixinLoaderPlugin;
import org.hippoecm.frontend.editor.plugins.mixin.MixinLoaderPluginEditorPlugin;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.AbstractClusterDecorator;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.render.ListViewPlugin;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A cluster configuration to preview another cluster configuration.  It adds builder
 * plugins for the plugins in the decorated configuration.  The only builder plugins
 * that are currently supported are the render plugin editor plugin and the field plugin
 * editor plugin.
 */
public class PreviewClusterConfig extends AbstractClusterDecorator {


    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(PreviewClusterConfig.class);

    private final class PreviewPluginConfig extends JavaPluginConfig {
        private static final long serialVersionUID = 1L;

        private final IPluginConfig config;

        private PreviewPluginConfig(String name, IPluginConfig config) {
            super(name);
            this.config = config;
        }

        @Override
        public Object get(Object key) {
            if ("model.effective".equals(key)) {
                return config;
            }
            return super.get(key);
        }
    }

    private Map<String, String> parameters;
    private Boolean editable;

    public PreviewClusterConfig(IClusterConfig template, Map<String, String> parameters, boolean editable) {
        super(template);

        this.parameters = parameters;
        this.editable = editable;
    }

    protected String getPluginConfigEditorClass(String pluginClass) {
        if (pluginClass == null) {
            return null;
        }
        try {
            Class<?> clazz = Class.forName(pluginClass);
            if (AbstractFieldPlugin.class.isAssignableFrom(clazz)) {
                return FieldPluginEditorPlugin.class.getName();
            } else if (MixinLoaderPlugin.class.isAssignableFrom(clazz)) {
                return MixinLoaderPluginEditorPlugin.class.getName();
            } else if (ListViewPlugin.class.isAssignableFrom(clazz)) {
                return ListViewPluginEditorPlugin.class.getName();
            } else if (RenderPlugin.class.isAssignableFrom(clazz)) {
                return RenderPluginEditorPlugin.class.getName();
            }
        } catch (ClassNotFoundException ex) {
            ResourceSettings resourceSettings = Application.get().getResourceSettings();
            IResourceStreamLocator locator = resourceSettings.getResourceStreamLocator();
            IResourceStream stream = locator.locate(null, pluginClass.replace('.', '/') + ".html");
            if (stream == null) {
                String message = ex.getClass().getName() + ": " + ex.getMessage();
                log.error(message, ex);
            } else {
                return LayoutPluginEditorPlugin.class.getName();
            }
        }
        return null;
    }

    private IPluginConfig getEditorConfig(String clazz, final IPluginConfig config) {
        IPluginConfig previewWrapper = new PreviewPluginConfig(config.getName() + "-preview", config);
        previewWrapper.put("plugin.class", clazz);
        previewWrapper.put("model.effective", config);

        previewWrapper.put("plugin.id", config.getName());
        previewWrapper.put("builder.mode", editable ? IEditor.Mode.EDIT.toString() : IEditor.Mode.VIEW.toString());
        previewWrapper.putAll(parameters);

        if (config.get("wicket.id") != null) {
            previewWrapper.put("wicket.id", config.get("wicket.id"));
        }

        return previewWrapper;
    }

    @Override
    protected Object decorate(Object object) {
        if (object instanceof IPluginConfig) {
            IPluginConfig config = (IPluginConfig) object;
            String editorClass = getPluginConfigEditorClass(config.getString(IPlugin.CLASSNAME));
            if (editorClass != null) {
                return getEditorConfig(editorClass.trim(), config);
            } else {
                return config;
            }
        }
        return object;
    }
}
