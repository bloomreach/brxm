/*
 *  Copyright 2013-2015 Hippo B.V. (http://www.onehippo.com)
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.ajax.json.JSONException;
import org.apache.wicket.ajax.json.JSONObject;
import org.apache.wicket.ajax.json.JSONTokener;
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
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ParameterInUrlController extends UrlControllerBehavior implements IObserver<IModelReference<Node>> {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(ParameterInUrlController.class);

    private static final String MODE_PARAM = "mode";
    private static final String MODE_VALUE_EDIT = "edit";
    private static final String PATH_PARAM = "path";
    private static final String UUID_PARAM = "uuid";
    private static final String URL_PARAMETERS = "parameters";
    private static final String CONTENT_PATH = "/content";
    private static final String DOCUMENTS_PATH = CONTENT_PATH + "/documents";

    private final IModelReference<Node> modelReference;
    private final IBrowseService browseService;
    private final IEditorManager editorMgr;

    private transient boolean browsing = false;

    public ParameterInUrlController(final IModelReference<Node> modelReference, IBrowseService browseService, IEditorManager editorMgr) {
        this.modelReference = modelReference;
        this.browseService = browseService;
        this.editorMgr = editorMgr;
    }

    @Override
    public IModelReference<Node> getObservable() {
        return modelReference;
    }

    public void init(IRequestParameters parameters) {
        process(parameters);
    }

    public void process(IRequestParameters requestParameters) {

        Map<String, String> parameters = getParametersMap(requestParameters);

        String jcrPath = getJcrPath(parameters);
        if(jcrPath != null){
            IEditor.Mode mode = IEditor.Mode.VIEW;
            final String modeValue = parameters.get(MODE_PARAM);
            if (modeValue != null && !modeValue.isEmpty()) {
                if (modeValue.toString().equals(MODE_VALUE_EDIT)) {
                    mode = IEditor.Mode.EDIT;
                }
            }
            browseTo(jcrPath, mode);
        }
    }

    protected String getJcrPath(final Map<String, String> parameters) {
        final String path = parameters.get(PATH_PARAM);
        if (path != null && !path.isEmpty()) {
            return path;
        }

        final String uuid = parameters.get(UUID_PARAM);
        if (uuid != null && !uuid.isEmpty()) {
            try {
                return UserSession.get().getJcrSession().getNodeByIdentifier(uuid).getPath();
            } catch (RepositoryException e) {
                log.info("Could not find document with uuid: {}", uuid);
            }
        }
        return null;
    }

    protected Map<String, String> getParametersMap(IRequestParameters requestParameters){
        final StringValue paramsValue = requestParameters.getParameterValue(URL_PARAMETERS);

        Map<String, String> parameters = new HashMap<>();
        if (!paramsValue.isNull() && !paramsValue.isEmpty()) {
            final String value = paramsValue.toString();
            try {
                final JSONObject jsonObject = new JSONObject(new JSONTokener(value));
                final Iterator keys = jsonObject.keys();
                while (keys.hasNext()) {
                    final String next = (String) keys.next();
                    parameters.put(next, jsonObject.getString(next));
                }
            } catch (JSONException e) {
                throw new RuntimeException("Unable to parse parameters from '" + value + "'", e);
            }
        }

        return parameters;
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
                setParameter(PATH_PARAM, node.getPath());
            } catch (RepositoryException e) {
                log.warn("Could not retrieve path of node model, path to the node will not be shown in the URL", e);
            }
        }
    }

    @Override
    protected void onRequest(IRequestParameters parameters) {
        process(parameters);
    }

    public void browseTo(String jcrPath, IEditor.Mode mode) {
        browsing = true;
        try {
            if (jcrPath != null) {
                JcrNodeModel nodeModel = new JcrNodeModel(jcrPath);

                if (nodeModel.getNode() != null) {
                    if (browseService != null && validateNavigationTarget(nodeModel)) {
                        browseService.browse(nodeModel);
                    } else {
                        log.info("Could not find browse service - document " + jcrPath + " will not be selected");
                    }

                    if (editorMgr != null && validateNavigationTarget(nodeModel)) {
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

    private boolean validateNavigationTarget(JcrNodeModel nodeModel) {
        try {
            final String path = nodeModel.getNode().getPath();
            if (path.startsWith(CONTENT_PATH)) {
                return path.startsWith(DOCUMENTS_PATH);
            }
            return true;
        } catch (RepositoryException e) {
            log.warn("error validating path: ", e.getMessage());
            return false;
        }
    }

}
