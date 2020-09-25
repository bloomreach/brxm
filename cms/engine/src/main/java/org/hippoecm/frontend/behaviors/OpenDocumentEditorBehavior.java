/*
 * Copyright 2019-2020 Hippo B.V. (http://www.onehippo.com)
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
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.template.PackageTextTemplate;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.service.EditorException;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.ServiceException;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
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

    private static final Logger log = LoggerFactory.getLogger(OpenDocumentEditorBehavior.class);

    private static final String OPEN_DOCUMENT_EDITOR_JS = "open-document-editor.js";

    private final IPluginContext context;

    public OpenDocumentEditorBehavior(final IPluginContext context) {
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

        if (documentId == null && documentPath == null) {
            log.warn("Failed to open editor in '{}' mode; parameter 'documentId' and 'documentPath' are both empty", mode);
            return;
        }

        final Node handle = documentId != null
                ? getDocumentById(documentId)
                : getDocumentByPath(documentPath);

        if (handle == null) {
            return;
        }

        final JcrNodeModel handleModel = new JcrNodeModel(handle);
        try {
            if (!handle.isNodeType(HippoNodeType.NT_HANDLE)) {
                log.debug("Node with uuid '{}' is not a handle", handle.getIdentifier());

                final IBrowseService<IModel<Node>> service = context.getService("service.browse", IBrowseService.class);
                if (service != null) {
                    service.browse(handleModel);
                } else {
                    log.warn("No browse service found, cannot open document");
                }
                return;
            }

            final IEditor<?> editor = getEditor(handleModel);
            if (mode == IEditor.Mode.EDIT && editor.getMode() != IEditor.Mode.EDIT) {
                editor.setMode(mode);
            }
            editor.focus();
        } catch (ItemNotFoundException e) {
            log.warn("Could not find document with uuid '{}'", handleModel.getItemModel().getUuid(), e);
        } catch (EditorException | RepositoryException | ServiceException e) {
            log.warn("Failed to open editor in '{}' mode for document with uuid '{}'", mode,
                    handleModel.getItemModel().getUuid(), e);
        }
    }

    private IEditor<?> getEditor(final JcrNodeModel handleModel) throws ServiceException{
        final IEditorManager editorManager = context.getService("service.edit", IEditorManager.class);
        final IEditor<?> editor = editorManager.getEditor(handleModel);
        if (editor != null) {
            log.debug("Open existing editor for handle: { uuid: {}}", handleModel.getItemModel().getUuid());
            return editor;
        }
        log.debug("Open new editor for handle: { uuid: {}}", handleModel.getItemModel().getUuid());
        return editorManager.openEditor(handleModel);
    }

    private static Node getDocumentByPath(final String path) {
        try {
            return UserSession.get().getJcrSession().getNode(path);
        } catch (RepositoryException e) {
            log.warn("Failed to find document with path '{}'", path, e);
            return null;
        }
    }

    private static Node getDocumentById(final String identifier) {
        try {
            return UserSession.get().getJcrSession().getNodeByIdentifier(identifier);
        } catch (RepositoryException e) {
            log.warn("Failed to find document with uuid '{}'", identifier, e);
            return null;
        }
    }
}
