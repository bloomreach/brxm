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

import org.apache.wicket.Application;
import org.apache.wicket.settings.IResourceSettings;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.locator.IResourceStreamLocator;
import org.hippoecm.frontend.editor.plugins.field.FieldPlugin;
import org.hippoecm.frontend.editor.plugins.field.FieldPluginEditorPlugin;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.AbstractClusterDecorator;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.service.render.ListViewPlugin;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreviewClusterConfig extends AbstractClusterDecorator {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(PreviewClusterConfig.class);

    private String clusterConfigModel;
    private String selectedPluginModel;
    private String selectedExtPtModel;
    private String helperId;

    private Boolean editable;

    public PreviewClusterConfig(IClusterConfig template, String clusterConfigModel, String selectedPluginModel,
            String selectedExtPt, String helperId, boolean editable) {
        super(template);

        this.clusterConfigModel = clusterConfigModel;
        this.selectedPluginModel = selectedPluginModel;
        this.selectedExtPtModel = selectedExtPt;
        this.helperId = helperId;
        this.editable = editable;
    }

    protected String getPluginConfigEditorClass(String pluginClass) {
        if (pluginClass == null) {
            return null;
        }
        try {
            Class<?> clazz = Class.forName(pluginClass);
            if (FieldPlugin.class.isAssignableFrom(clazz)) {
                return FieldPluginEditorPlugin.class.getName();
            } else if (ListViewPlugin.class.isAssignableFrom(clazz)) {
                return ListViewPluginEditorPlugin.class.getName();
            } else if (RenderPlugin.class.isAssignableFrom(clazz)) {
                return RenderPluginEditorPlugin.class.getName();
            }
        } catch (ClassNotFoundException ex) {
            IResourceSettings resourceSettings = Application.get().getResourceSettings();
            IResourceStreamLocator locator = resourceSettings.getResourceStreamLocator();
            IResourceStream stream = locator.locate(null, pluginClass.replace('.', '/') + ".html");
            if (stream == null) {
                String message = ex.getClass().getName() + ": " + ex.getMessage();
                log.error(message, ex);
            } else {
                return RenderPluginEditorPlugin.class.getName();
            }
        }
        return null;
    }

    private IPluginConfig getEditorConfig(String clazz, IPluginConfig config) {
        IPluginConfig previewWrapper = new JavaPluginConfig(config.getName() + "-preview");
        previewWrapper.put("plugin.class", clazz);
        previewWrapper.put("model.effective", config);

        previewWrapper.put("wicket.helper.id", helperId);
        previewWrapper.put("wicket.model", clusterConfigModel);
        previewWrapper.put("model.plugin", selectedPluginModel);
        previewWrapper.put("model.extensionpoint", selectedExtPtModel);
        previewWrapper.put("plugin.id", config.getName());
        previewWrapper.put("builder.mode", editable ? "edit" : "view");

        if (config.get("wicket.id") != null) {
            previewWrapper.put("wicket.id", config.get("wicket.id"));
        }

        return previewWrapper;
    }

    @Override
    protected Object decorate(Object object) {
        if (object instanceof IPluginConfig) {
            IPluginConfig config = (IPluginConfig) object;
            String editorClass = getPluginConfigEditorClass(config.getString("plugin.class"));
            if (editorClass != null) {
                return getEditorConfig(editorClass, config);
            } else {
                return config;
            }
        }
        return object;
    }
}
