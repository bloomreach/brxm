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
import java.util.Objects;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.hippoecm.frontend.model.BranchIdModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.EditorException;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.ServiceException;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.WorkflowException;
import org.json.JSONArray;
import org.onehippo.repository.documentworkflow.DocumentHandle;
import org.wicketstuff.js.ext.ExtEventAjaxBehavior;
import org.wicketstuff.js.ext.util.ExtEventListener;

import static org.hippoecm.repository.api.HippoNodeType.NT_HANDLE;

/**
 * Opens an editor for a document. The UUID of the document's handle and the mode to open the document in are passed
 * as parameters by the front-end.
 */
class OpenDocumentEditorEventListener extends ExtEventListener {

    private static final String PARAM_UUID = "uuid";
    private static final String PARAM_MODE = "mode";

    private final String editorManagerServiceId;
    private final IPluginContext context;

    OpenDocumentEditorEventListener(final IPluginConfig config, final IPluginContext context) {
        editorManagerServiceId = config.getString(IEditorManager.EDITOR_ID, "service.edit");
        this.context = context;
    }

    static ExtEventAjaxBehavior getExtEventBehavior() {
        return new ExtEventAjaxBehavior(PARAM_UUID, PARAM_MODE);
    }

    @Override
    public void onEvent(final AjaxRequestTarget target, final Map<String, JSONArray> parameters) {
        getParameter(PARAM_UUID, parameters).ifPresent(uuid ->
                getParameter(PARAM_MODE, parameters).ifPresent(modeName -> {
                    final IEditor.Mode mode = IEditor.Mode.fromString(modeName);
                    Objects.requireNonNull(uuid);
                    Objects.requireNonNull(mode);
                    if (IEditor.Mode.COMPARE.equals(mode)) {
                        throw new IllegalArgumentException(String.format("mode '%s' is not allowed", IEditor.Mode.COMPARE));
                    }
                    openDocumentEditor(uuid, mode);
                })
        );
    }

    private void openDocumentEditor(final String documentHandleUuid, final IEditor.Mode mode) {

        assert documentHandleUuid != null;
        assert !IEditor.Mode.COMPARE.equals(mode);

        final IEditorManager editorManager = context.getService(editorManagerServiceId, IEditorManager.class);
        try {
            final Node handle = UserSession.get().getJcrSession().getNodeByIdentifier(documentHandleUuid);
            if (!handle.isNodeType(NT_HANDLE)) {
                throw new IllegalArgumentException(String.format("Node with id '%s' is not of type %s", documentHandleUuid, NT_HANDLE));
            }
            BranchIdModel.initialize(context, handle);
            final JcrNodeModel documentHandleModel = new JcrNodeModel(handle);
            IEditor<?> editor = editorManager.getEditor(documentHandleModel);
            if (editor == null) {
                editor = editorManager.openEditor(documentHandleModel);
            }
            DocumentHandle documentHandle = new DocumentHandle(handle);
            if (documentHandle.isRetainable() && IEditor.Mode.VIEW == mode) {
                editor.saveDraft();
            }
            if (mode == IEditor.Mode.EDIT && editor.getMode() != IEditor.Mode.EDIT) {
                editor.setMode(mode);
            }
            editor.focus();
        } catch (ItemNotFoundException e) {
            ChannelEditor.log.warn("Could not find document with uuid '{}'", documentHandleUuid, e);
        } catch (WorkflowException | EditorException | RepositoryException | ServiceException e) {
            ChannelEditor.log.warn("Failed to open editor in '{}' mode for document with uuid '{}'", mode, documentHandleUuid, e);
        }
    }
}
