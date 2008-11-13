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

package org.hippoecm.frontend.plugins.yui.dragdrop;

import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.IModelService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderService;
import org.hippoecm.repository.api.ISO9075Helper;

public class PluginDragBehavior extends DragBehavior {
    private static final long serialVersionUID = 1L;
    
    protected final IPluginContext context;
    protected final IPluginConfig config;
    
    public PluginDragBehavior(IPluginContext context, IPluginConfig config) {
        super(context, config, new DragSettings(config));

        this.context = context;
        this.config = config;
    }
    
    @Override
    protected void updateSettings() {
        super.updateSettings();
        dragSettings.setLabel(getLabel());
    }
    
    @Override
    protected IModel getDragModel() {
        String pluginModelId = config.getString(RenderService.MODEL_ID);
        if (pluginModelId != null) {
            //TODO: generic gedrag uitzoeken
            IModelService pluginModelService = context.getService(pluginModelId, IModelService.class);
            if (pluginModelService != null) {
                return pluginModelService.getModel();
            }
        }
        return null;
    }
    
    private String getLabel() {
      String pluginModelId = config.getString(RenderService.MODEL_ID);
      if (pluginModelId != null) {
          IModelService pluginModelService = context.getService(pluginModelId, IModelService.class);
          if (pluginModelService != null) {
              IModel draggedModel = pluginModelService.getModel();
              if (draggedModel instanceof JcrNodeModel) {
                  JcrNodeModel nodeModel = (JcrNodeModel) draggedModel;
                  try {
                      return ISO9075Helper.decodeLocalName(nodeModel.getNode().getDisplayName());
                  } catch (RepositoryException e) {
                      return nodeModel.getItemModel().getPath();
                  }
              }
          }
      }
      return getComponent().getMarkupId();
  }

}
