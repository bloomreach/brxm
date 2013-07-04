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
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.encoding.UrlDecoder;
import org.apache.wicket.util.string.StringValue;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.EditorException;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.IController;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControllerPlugin extends Plugin implements IController {

    public static final String URL_PARAMETER_PATH = "path";
    public static final String URL_PARAMETER_MODE = "mode";
    public static final String URL_PARAMETER_MODE_VALUE_EDIT = "edit";

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(ControllerPlugin.class);

    public ControllerPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        context.registerService(this, IController.class.getName());

        registerPathInUrlController(context);
    }

    private void registerPathInUrlController(final IPluginContext context) {
        @SuppressWarnings("unchecked")
        final IModelReference<Node> modelReference = context.getService("model.browse.document", IModelReference.class);

        if (modelReference != null) {
            final PathInUrlController controller = new PathInUrlController(modelReference, URL_PARAMETER_PATH);
            context.registerService(controller, IObserver.class.getName());
            context.registerService(controller, Behavior.class.getName());
        }
    }

    public void process(IRequestParameters parameters) {
        RequestCycle rc = RequestCycle.get();
        StringValue urlPaths = parameters.getParameterValue(URL_PARAMETER_PATH);
        if (urlPaths != null) {
            String jcrPath = UrlDecoder.PATH_INSTANCE.decode(urlPaths.toString(), rc.getRequest().getCharset());
            JcrNodeModel nodeModel = new JcrNodeModel(jcrPath);

            IPluginContext context = getPluginContext();
            IPluginConfig config = getPluginConfig();
            IBrowseService browseService = context.getService(config.getString("browser.id", "service.browse"),
                    IBrowseService.class);
            if (browseService != null) {
                browseService.browse(nodeModel);
            } else {
                log.info("Could not find browse service - document " + jcrPath + " will not be selected");
            }

            if (parameters.getParameterNames().contains(URL_PARAMETER_MODE)) {
                StringValue modeStr = parameters.getParameterValue(URL_PARAMETER_MODE);
                if (modeStr != null) {
                    IEditor.Mode mode;
                    if (URL_PARAMETER_MODE_VALUE_EDIT.equals(modeStr.toString())) {
                        mode = IEditor.Mode.EDIT;
                    } else {
                        mode = IEditor.Mode.VIEW;
                    }
                    IEditorManager editorMgr = context.getService(config.getString("editor.id", "service.edit"),
                            IEditorManager.class);
                    if (editorMgr != null) {
                        IEditor editor = editorMgr.getEditor(nodeModel);
                        try {
                            if (editor == null) {
                                editor = editorMgr.openPreview(nodeModel);
                            }
                            editor.setMode(mode);
                        } catch (EditorException e) {
                            log.info("Could not open editor for " + jcrPath);
                        } catch (ServiceException e) {
                            log.info("Could not open preview for " + jcrPath);
                        }
                    }
                }
            }
        }
    }

}
