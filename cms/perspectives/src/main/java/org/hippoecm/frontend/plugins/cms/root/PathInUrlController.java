/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Iterator;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.util.string.StringValue;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.service.EditorException;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.ServiceException;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PathInUrlController extends UrlControllerBehavior implements IObserver<IModelReference<Node>> {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(PathInUrlController.class);

    private static final String MODE_PARAM = "mode";
    private static final String MODE_VALUE_EDIT = "edit";
    private static final String PATH_PARAM = "path";

    private final IModelReference<Node> modelReference;
    private final IBrowseService browseService;
    private final IEditorManager editorMgr;

    private transient boolean browsing = false;

    public PathInUrlController(final IModelReference<Node> modelReference, IBrowseService browseService, IEditorManager editorMgr) {
        this.modelReference = modelReference;
        this.browseService = browseService;
        this.editorMgr = editorMgr;
    }

    @Override
    public IModelReference<Node> getObservable() {
        return modelReference;
    }

    public void init(IRequestParameters parameters) {
        final StringValue pathValue = parameters.getParameterValue(PATH_PARAM);
        if (!pathValue.isNull() && !pathValue.isEmpty()) {
            String jcrPath = pathValue.toString();
            IEditor.Mode mode = IEditor.Mode.VIEW;

            final StringValue modeValue = parameters.getParameterValue(MODE_PARAM);
            if (!modeValue.isEmpty() && !modeValue.isNull()) {
                if (modeValue.toString().equals(MODE_VALUE_EDIT)) {
                    mode = IEditor.Mode.EDIT;
                }
            }

            browseTo(jcrPath, mode);
        }
    }

    @Override
    public void onEvent(final Iterator<? extends IEvent<IModelReference<Node>>> events) {
        if (!browsing) {
            showPathInUrl(modelReference.getModel());
        }
    }

    private void showPathInUrl(final IModel<Node> nodeModel) {
        final Node node = nodeModel.getObject();
        if (node != null) {
            try {
                if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                    String path = node.getPath();
                    setParameter(PATH_PARAM, path);
                }
            } catch (RepositoryException e) {
                log.warn("Could not retrieve path of node model, path to the node will not be shown in the URL", e);
            }
        }
    }

    @Override
    protected void onRequest(Map<String, String> parameters) {
        String jcrPath = parameters.get(PATH_PARAM);

        IEditor.Mode mode = IEditor.Mode.VIEW;
        if (parameters.containsKey(MODE_PARAM)) {
            String modeStr = parameters.get(MODE_PARAM);
            if (modeStr != null) {
                if (MODE_VALUE_EDIT.equals(modeStr)) {
                    mode = IEditor.Mode.EDIT;
                }
            }
        }

        browseTo(jcrPath, mode);
    }

    public void browseTo(String jcrPath, IEditor.Mode mode) {
        browsing = true;
        try {
            if (jcrPath != null) {
                JcrNodeModel nodeModel = new JcrNodeModel(jcrPath);

                if (nodeModel.getNode() != null) {
                    if (browseService != null) {
                        browseService.browse(nodeModel);
                    } else {
                        log.info("Could not find browse service - document " + jcrPath + " will not be selected");
                    }

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
                } else {
                    log.debug("Cannot browse to '{}': node does not exist", jcrPath);
                }
            }
        } finally {
            browsing = false;
        }
    }

}
