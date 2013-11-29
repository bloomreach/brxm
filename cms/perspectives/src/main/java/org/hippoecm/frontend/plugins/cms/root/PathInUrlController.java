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

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.service.EditorException;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PathInUrlController extends UrlControllerBehavior implements IObserver<IModelReference<Node>> {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(PathInUrlController.class);

    private static final String URL_PARAMETER_MODE = "mode";
    private static final String URL_PARAMETER_MODE_VALUE_EDIT = "edit";
    public static final String PATH = "path";

    private final IModelReference<Node> modelReference;
    private final IBrowseService browseService;
    private final IEditorManager editorMgr;

    public PathInUrlController(final IModelReference<Node> modelReference, IBrowseService browseService, IEditorManager editorMgr) {
        this.modelReference = modelReference;
        this.browseService = browseService;
        this.editorMgr = editorMgr;
    }

    @Override
    public IModelReference<Node> getObservable() {
        return modelReference;
    }

    @Override
    public void onEvent(final Iterator<? extends IEvent<IModelReference<Node>>> events) {
        showPathInUrl(modelReference.getModel());
    }

    private void showPathInUrl(final IModel<Node> nodeModel) {
        final String path = getPathToShow(nodeModel.getObject());
        showPathInUrl(path);
    }

    private String getPathToShow(final Node node) {
        if (node != null) {
            try {
                return node.getPath();
            } catch (RepositoryException e) {
                log.warn("Could not retrieve path of node model, path to the node will not be shown in the URL", e);
            }
        }
        return StringUtils.EMPTY;
    }

    private void showPathInUrl(final String path) {
        setParameter(PATH, path);
    }

    @Override
    protected void onRequest(Map<String, String> parameters) {
        String jcrPath = parameters.get(PATH);
        if (jcrPath != null) {
            JcrNodeModel nodeModel = new JcrNodeModel(jcrPath);

            if (browseService != null) {
                browseService.browse(nodeModel);
            } else {
                log.info("Could not find browse service - document " + jcrPath + " will not be selected");
            }

            if (parameters.containsKey(URL_PARAMETER_MODE)) {
                String modeStr = parameters.get(URL_PARAMETER_MODE);
                if (modeStr != null) {
                    IEditor.Mode mode;
                    if (URL_PARAMETER_MODE_VALUE_EDIT.equals(modeStr)) {
                        mode = IEditor.Mode.EDIT;
                    } else {
                        mode = IEditor.Mode.VIEW;
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
                }
            }
        }
    }
}
