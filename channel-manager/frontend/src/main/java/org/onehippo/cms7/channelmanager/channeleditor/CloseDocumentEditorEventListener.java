/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.frontend.session.UserSession;
import org.json.JSONArray;
import org.wicketstuff.js.ext.ExtEventAjaxBehavior;
import org.wicketstuff.js.ext.util.ExtEventListener;

/**
 * Checks if an editor is open for a certain document, and if so, closes it when the document is valid.
 * Whether the editor has been closed/was already closed or whether it still exists is reported back via JavaScript.
 */
public class CloseDocumentEditorEventListener extends ExtEventListener {

    private static final String PARAM_UUID = "uuid";

    private final String editorManagerServiceId;
    private final IPluginContext context;
    private final String channelEditorId;

    CloseDocumentEditorEventListener(final IPluginConfig config, final IPluginContext context, final String channelEditorId) {
        editorManagerServiceId = config.getString(IEditorManager.EDITOR_ID, "service.edit");
        this.context = context;
        this.channelEditorId = channelEditorId;
    }

    static ExtEventAjaxBehavior getExtEventBehavior() {
        return new ExtEventAjaxBehavior(PARAM_UUID);
    }

    @Override
    public void onEvent(final AjaxRequestTarget target, final Map<String, JSONArray> parameters) {
        getParameter(PARAM_UUID, parameters).ifPresent((uuid) -> {
            final boolean isClosed = this.closeDocumentEditor(uuid);
            returnResult(uuid, isClosed, target);
        });
    }

    private boolean closeDocumentEditor(final String documentHandleUuid) {
        final IEditorManager editorManager = context.getService(editorManagerServiceId, IEditorManager.class);
        try {
            final Node documentHandle = UserSession.get().getJcrSession().getNodeByIdentifier(documentHandleUuid);
            final JcrNodeModel documentHandleModel = new JcrNodeModel(documentHandle);
            final IEditor<?> editor = editorManager.getEditor(documentHandleModel);

            if (editor == null) {
                return true;
            }

            if (editor.isValid()) {
                editor.close();
                return true;
            }
        } catch (ItemNotFoundException e) {
            ChannelEditor.log.warn("Could not find document with uuid '{}'", documentHandleUuid, e);
            return true;
        } catch (EditorException | RepositoryException e) {
            ChannelEditor.log.warn("Failed to close editor for document with uuid '{}'", documentHandleUuid, e);
        }
        return false;
    }

    private void returnResult(final String documentHandleUuid, final boolean isClosed, final AjaxRequestTarget target) {
        final String resultScript = String.format("Ext.getCmp('%s').closeDocumentResult('%s', %s);",
                channelEditorId, documentHandleUuid, isClosed);
        target.appendJavaScript(resultScript);
    }
}
