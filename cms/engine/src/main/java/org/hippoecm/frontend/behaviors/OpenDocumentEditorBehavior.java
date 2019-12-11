/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hippoecm.frontend.behaviors;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.template.PackageTextTemplate;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.service.EditorException;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.ServiceException;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Behavior to open a document.
 * <p>
 * This class adds header item containing javascript ( {@link #OPEN_DOCUMENT_EDITOR_JS} ).
 * </p>
 * <p>
 * That script adds two functions:
 * <ul>
 *     <li><code>Hippo.openDocumentById(id,mode)</code></li>
 *     <li><code>Hippo.openDocumentByPath(path,mode)</code></li>
 * </ul>
 *
 * </p>
 * <p>
 * The script accepts the following values for its parameters:
 * <ul>
 * <li>Valid values for mode are {@link IEditor.Mode#VIEW}, {@link IEditor.Mode#EDIT}. If the mode is not valid it
 * will default to {@link IEditor.Mode#VIEW} without error</li>
 * <li>The id is the identified of the handle of the document to open. An invalid id will result in an error.</li>
 * <li>The path the is absolute path of the handle of the document to open. An invalid path will result in an error.</li>
 * </ul>
 * </p>
 */
public class OpenDocumentEditorBehavior extends AbstractDefaultAjaxBehavior {

    private static final String OPEN_DOCUMENT_EDITOR_JS = "open-document-editor.js";
    private static final Logger log = LoggerFactory.getLogger(OpenDocumentEditorBehavior.class);

    private final IPluginContext context;

    public OpenDocumentEditorBehavior(IPluginContext context) {
        this.context = context;
    }

    @Override
    public void renderHead(final Component component, final IHeaderResponse response) {
        super.renderHead(component, response);
        response.render(OnLoadHeaderItem.forScript(createScript()));
    }

    private String createScript() {
        final Map<String, String> scriptParams = new TreeMap<>();
        scriptParams.put("callbackUrl", this.getCallbackUrl().toString());
        String resource = null;
        try (final PackageTextTemplate openDocumentEditorJs = new PackageTextTemplate(OpenDocumentEditorBehavior.class, OPEN_DOCUMENT_EDITOR_JS)) {
            resource = openDocumentEditorJs.asString(scriptParams);
        } catch (IOException e) {
            log.warn("Resource {} could not be closed.", OPEN_DOCUMENT_EDITOR_JS, e);
        }
        return resource;
    }

    @Override
    protected void respond(final AjaxRequestTarget target) {
        final Request request = RequestCycle.get().getRequest();
        final IRequestParameters requestParameters = request.getRequestParameters();
        final String documentId = requestParameters.getParameterValue("documentId").toString();
        final String documentPath = requestParameters.getParameterValue("documentPath").toString();
        final String modeString = requestParameters.getParameterValue("mode").toString();
        final IEditor.Mode mode = IEditor.Mode.fromString(modeString, IEditor.Mode.VIEW);
        String handleIdentifier = documentId;
        try {
            if (documentId == null && documentPath != null) {
                handleIdentifier = getDocumentId(documentPath);
            }
            final IEditor<?> editor = getEditor(handleIdentifier);
            if (mode == IEditor.Mode.EDIT && editor.getMode() != IEditor.Mode.EDIT) {
                editor.setMode(mode);
            }
            editor.focus();
        } catch (ItemNotFoundException e) {
            log.warn("Could not find document with uuid '{}'", handleIdentifier, e);
        } catch (EditorException | RepositoryException | ServiceException e) {
            log.warn("Failed to open editor in '{}' mode for document with uuid '{}'", mode, handleIdentifier, e);
        }
    }

    private IEditor<?> getEditor(String documentId) throws ServiceException, RepositoryException {
        final JcrNodeModel documentHandleModel = getJcrNodeModel(documentId);
        final IEditorManager editorManager = context.getService("service.edit", IEditorManager.class);
        final IEditor<?> editor = editorManager.getEditor(documentHandleModel);
        if (editor != null) {
            return editor;
        }
        return editorManager.openEditor(documentHandleModel);
    }

    private JcrNodeModel getJcrNodeModel(final String documentId) throws RepositoryException {
        final Node documentHandle = UserSession.get().getJcrSession().getNodeByIdentifier(documentId);
        return new JcrNodeModel(documentHandle);
    }

    private String getDocumentId(String path) throws RepositoryException {
        return UserSession.get().getJcrSession().getNode(path).getIdentifier();
    }
}
