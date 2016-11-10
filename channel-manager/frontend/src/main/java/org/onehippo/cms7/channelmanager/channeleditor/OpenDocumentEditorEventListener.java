/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.channelmanager.channeleditor;

import java.util.Map;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.EditorException;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.ServiceException;
import org.hippoecm.frontend.session.UserSession;
import org.json.JSONArray;
import org.json.JSONException;
import org.wicketstuff.js.ext.ExtEventAjaxBehavior;
import org.wicketstuff.js.ext.util.ExtEventListener;

/**
 * Opens an editor for a document. The UUID of the document's handle is passed as a parameter by the front-end.
 */
class OpenDocumentEditorEventListener extends ExtEventListener {

    private static final String PARAM_UUID = "uuid";

    private final String editorManagerServiceId;
    private final IPluginContext context;
    private final IEditor.Mode mode;

    OpenDocumentEditorEventListener(final IPluginConfig config, final IPluginContext context, final IEditor.Mode mode) {
        editorManagerServiceId = config.getString(IEditorManager.EDITOR_ID, "service.edit");
        this.context = context;
        this.mode = mode;
    }

    static ExtEventAjaxBehavior getExtEventBehavior() {
        return new ExtEventAjaxBehavior(PARAM_UUID);
    }

    @Override
    public void onEvent(final AjaxRequestTarget target, final Map<String, JSONArray> parameters) {
        final JSONArray values = parameters.get(PARAM_UUID);
        if (values == null || values.length() == 0) {
            return;
        }
        try {
            final String documentHandleUuid = values.get(0).toString();
            openDocumentEditor(documentHandleUuid);
        } catch (JSONException e) {
            ChannelEditor.log.warn("Invalid JSON parameter '{}'", PARAM_UUID, e);
        }
    }

    private void openDocumentEditor(final String documentHandleUuid) {
        final IEditorManager editorManager = context.getService(editorManagerServiceId, IEditorManager.class);
        try {
            final Node documentHandle = UserSession.get().getJcrSession().getNodeByIdentifier(documentHandleUuid);
            final JcrNodeModel documentHandleModel = new JcrNodeModel(documentHandle);
            IEditor<?> editor = editorManager.getEditor(documentHandleModel);
            if (editor == null) {
                editor = editorManager.openEditor(documentHandleModel);
            }
            editor.setMode(mode);
            editor.focus();
        } catch (ItemNotFoundException e) {
            ChannelEditor.log.warn("Could not find document with uuid '{}'", documentHandleUuid, e);
        } catch (EditorException|RepositoryException|ServiceException e) {
            ChannelEditor.log.warn("Failed to open editor for document with uuid '{}'", documentHandleUuid, e);
        }
    }
}
