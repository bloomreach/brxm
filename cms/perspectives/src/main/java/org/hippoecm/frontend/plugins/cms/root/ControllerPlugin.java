/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.cms.root;

import javax.jcr.Node;

import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.request.IRequestParameters;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.IController;
import org.hippoecm.frontend.service.IEditorManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControllerPlugin extends Plugin implements IController {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(ControllerPlugin.class);
    private PathInUrlController controller;

    public ControllerPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        context.registerService(this, IController.class.getName());

        registerPathInUrlController(context, config);
    }

    private void registerPathInUrlController(final IPluginContext context, IPluginConfig config) {
        @SuppressWarnings("unchecked")
        final IModelReference<Node> modelReference = context.getService("model.browse.document", IModelReference.class);

        if (modelReference != null) {
            IBrowseService browseService = context.getService(config.getString("browser.id", "service.browse"), IBrowseService.class);
            IEditorManager editorMgr = context.getService(config.getString("editor.id", "service.edit"), IEditorManager.class);

            controller = new PathInUrlController(modelReference, browseService, editorMgr);
            context.registerService(controller, IObserver.class.getName());
            context.registerService(controller, Behavior.class.getName());
        }
    }

    public void process(IRequestParameters parameters) {
        if (controller != null) {
            controller.process(parameters);
        }
    }

}
