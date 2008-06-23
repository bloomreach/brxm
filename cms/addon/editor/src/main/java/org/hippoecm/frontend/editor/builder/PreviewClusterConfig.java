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

import java.util.LinkedList;
import java.util.List;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JcrClusterConfig;
import org.hippoecm.frontend.plugin.config.impl.JcrPluginConfig;

public class PreviewClusterConfig extends JcrClusterConfig {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static int instanceCount = 0;
    private String pluginConfigModel;

    public PreviewClusterConfig(IPluginContext context, JcrNodeModel node, ModelService model) {
        super(node);

        this.pluginConfigModel = context.getReference(model).getServiceId();
    }

    @Override
    public List<IPluginConfig> getPlugins() {
        List<IPluginConfig> plugins = super.getPlugins();
        List<IPluginConfig> result = new LinkedList<IPluginConfig>();
        for (final IPluginConfig config : plugins) {
            if (config.get("wicket.id") != null && !"cluster:wicket.id".equals(config.get("wicket.id"))) {
                final String wrappedId = PreviewClusterConfig.class.getName() + "." + newId();
                IPluginConfig previewWrapper = new JavaPluginConfig();
                previewWrapper.put("plugin.class", PreviewPluginPlugin.class.getName());
                previewWrapper.put("wicket.id", config.get("wicket.id"));
                previewWrapper.put("wicket.model", pluginConfigModel);
                previewWrapper.put("preview", wrappedId);

                JcrNodeModel pluginNodeModel = ((JcrPluginConfig) config).getNodeModel();
                previewWrapper.put("plugin.node.path", pluginNodeModel.getItemModel().getPath());

                result.add(previewWrapper);

                result.add(new JavaPluginConfig() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public Object get(Object key) {
                        if ("wicket.id".equals(key)) {
                            return wrappedId;
                        }
                        return config.get(key);
                    }
                });
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
