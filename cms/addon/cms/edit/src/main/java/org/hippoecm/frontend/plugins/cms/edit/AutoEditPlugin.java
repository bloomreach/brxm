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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.IModelListener;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IEditService;
import org.hippoecm.frontend.service.render.RenderService;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoEditPlugin implements IPlugin, IModelListener, IDetachable {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(AutoEditPlugin.class);

    private IPluginContext context;
    private IPluginConfig config;

    public AutoEditPlugin(IPluginContext context, IPluginConfig config) {
        this.context = context;
        this.config = config;

        if (config.getString("editor.id") == null) {
            log.error("No editor defined (editor.id)");
            return;
        }

        if (config.getString(RenderService.MODEL_ID) != null) {
            context.registerService(this, config.getString(RenderService.MODEL_ID));
        } else {
            log.warn("No model defined ({})", RenderService.MODEL_ID);
        }
    }

    public void updateModel(IModel imodel) {
        JcrNodeModel model = (JcrNodeModel) imodel;
        if (model != null && model.getNode() != null) {
            Node modelNode = model.getNode();
            if (modelNode != null) {
                IEditService editor = context.getService(config.getString("editor.id"), IEditService.class);
                if (editor != null) {
                    editor.edit(new JcrNodeModel(modelNode));
                }
            }
        }
    }

    public void detach() {
        config.detach();
    }

}