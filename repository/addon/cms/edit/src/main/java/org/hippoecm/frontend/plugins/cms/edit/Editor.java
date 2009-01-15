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

import java.util.List;

import org.apache.wicket.IClusterable;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.ModelService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.IPluginControl;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
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

    private IPluginControl plugin;
    private IRenderService editor;
    private ModelService<IModel> modelService;

    Editor(IPluginContext context, String cluster, IModel model) throws EditorException {
        IPluginConfigService pluginConfigService = context.getService(IPluginConfigService.class.getName(),
                IPluginConfigService.class);
        IClusterConfig clusterConfig = pluginConfigService.getCluster(cluster);

        String modelId = clusterConfig.getString("wicket.model");
        modelService = new ModelService<IModel>(modelId, model);
        modelService.init(context);

        plugin = context.start(clusterConfig);

        List<IRenderService> targetServices = context.getServices(clusterConfig.getString(RenderService.WICKET_ID),
                IRenderService.class);
        List<IRenderService> clusterServices = context.getServices(context.getReference(plugin).getServiceId(),
                IRenderService.class);
        for (IRenderService target : targetServices) {
            if (clusterServices.contains(target)) {
                // found it!
                editor = target;
                break;
            }
        }
        if (editor == null) {
            plugin.stopPlugin();
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
        plugin.stopPlugin();
    }
}
