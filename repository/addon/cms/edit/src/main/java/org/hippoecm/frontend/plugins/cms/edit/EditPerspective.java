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

import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.perspective.Perspective;
import org.hippoecm.frontend.service.IEditService;
import org.hippoecm.frontend.service.render.RenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditPerspective extends Perspective implements IEditService {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(EditPerspective.class);

    private ModelService modelService;

    public EditPerspective(IPluginContext context, IPluginConfig config) {
        super(context, config);

        if (config.getString(RenderService.MODEL_ID) != null) {
            modelService = new ModelService(config.getString(RenderService.MODEL_ID), new JcrNodeModel("/"));
            modelService.init(context);
        } else {
            log.error("no model service specified");
        }

        if (config.getString(IEditService.EDITOR_ID) != null) {
            context.registerService(this, config.getString(IEditService.EDITOR_ID));
        }

        onModelChanged();
    }

    @Override
    public void onModelChanged() {
        JcrNodeModel nodeModel = (JcrNodeModel) getModel();
        try {
            if (nodeModel != null && nodeModel.getNode() != null) {
                setTitle(nodeModel.getNode().getName());
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }

    // IEditService

    public void edit(IModel model) {
        if (modelService != null) {
            modelService.setModel(model);
        }
    }

}
