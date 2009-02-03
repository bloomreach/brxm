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
package org.hippoecm.frontend.plugins.cms.edit;

import org.apache.wicket.IClusterable;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfigService;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.render.RenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Editor implements IClusterable {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final Logger log = LoggerFactory.getLogger(Editor.class);

    private IClusterControl plugin;
    private IRenderService editor;
    private ModelReference<IModel> modelService;

    Editor(IPluginContext context, String cluster, IPluginConfig config, IModel model) throws EditorException {
        IPluginConfigService pluginConfigService = context.getService(IPluginConfigService.class.getName(),
                IPluginConfigService.class);
        IClusterConfig clusterConfig = pluginConfigService.getCluster(cluster);
        plugin = context.newCluster(clusterConfig, config);
        IClusterConfig decorated = plugin.getClusterConfig();

        String modelId = decorated.getString(RenderService.MODEL_ID);
        modelService = new ModelReference<IModel>(modelId, model);
        modelService.init(context);

        plugin.start();

        editor = context.getService(decorated.getString(RenderService.WICKET_ID), IRenderService.class);
        if (editor == null) {
            plugin.stop();
            modelService.destroy();
            throw new EditorException("No IRenderService found");
        }
    }

    IModel getModel() {
        return modelService.getModel();
    }

    void focus() {
        if (editor.getParentService() != null) {
            editor.getParentService().focus(editor);
        } else {
            log.warn("Editor is not bound to parent");
        }
    }

    void close() {
        plugin.stop();
    }
}
