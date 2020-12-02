/*
 * Copyright 2016-2020 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms.channelmanager.content.document.util;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EditingUtils provides utility methods for dealing with the workflow of a document.
 */
public class EditingUtils {

    private static final Logger log = LoggerFactory.getLogger(EditingUtils.class);

    public static final String HINT_PUBLISH = "publishBranch";
    public static final String HINT_REQUEST_PUBLICATION = "requestPublication";

    static final String HINT_COMMIT_EDITABLE_INSTANCE = "commitEditableInstance";
    static final String HINT_DISPOSE_EDITABLE_INSTANCE = "disposeEditableInstance";
    static final String HINT_OBTAIN_EDITABLE_INSTANCE = "obtainEditableInstance";

    private static final String HINT_DELETE = "delete";
    private static final String HINT_PREVIEW_AVAILABLE = "previewAvailable";
    private static final String HINT_RENAME = "rename";

    private EditingUtils() {
    }

    /**
     * Check if a workflow indicates that editing of a document can be started.
     *
     * @param hints
     * @return true/false.
     */
    public static boolean canObtainEditableDocument(final Map<String, Serializable> hints) {
        return isActionAvailable(HINT_OBTAIN_EDITABLE_INSTANCE, hints);
    }

    /**
     * Check if a document can be updated, given its workflow.
     *
     * @param hints
     * @return true if document can be updated, false otherwise
     */
    public static boolean canUpdateDocument(final Map<String, Serializable> hints) {
        return isActionAvailable(HINT_COMMIT_EDITABLE_INSTANCE, hints);
    }

    /**
     * Check if a document can be disposed of, given its workflow.
     *
     * @param hints
     * @return true if document can be disposed of, false otherwise
     */
    public static boolean canDisposeEditableDocument(final Map<String, Serializable> hints) {
        return isActionAvailable(HINT_DISPOSE_EDITABLE_INSTANCE, hints);
    }

    /**
     * Check if document can be archived (i.e. moved to the attic and stripped of all its data), given its workflow.
     *
     * @param hints
     * @return true if the document can be archived, false otherwise
     */
    public static boolean canArchiveDocument(final Map<String, Serializable> hints) {
        return isActionAvailable(HINT_DELETE, hints);
    }

    /**
     * Check if a document can be erased from a folder, i.e. permanently deleted without any archiving in the attic.
     *
     * @param workflow the folder workflow
     * @return true if the document can be erased, false otherwise
     */
    public static boolean canEraseDocument(final FolderWorkflow workflow) {
        return isActionAvailable(workflow, HINT_DELETE);
    }

    /**
     * Check if document can be renamed (i.e. change its URL name), given its workflow.
     *
     * @param hints
     * @return true if the document can be renamed, false otherwise
     */
    public static boolean canRenameDocument(final Map<String, Serializable> hints) {
        return isActionAvailable(HINT_RENAME, hints);
    }

    /**
     * Check if a document has a 'preview' variant.
     *
     * @param hints
     * @return true if the document has a 'preview' variant, false otherwise.
     */
    public static boolean hasPreview(final Map<String, Serializable> hints) {
        return isActionAvailable(HINT_PREVIEW_AVAILABLE, hints);
    }

    /**
     * Check a workflow to see if an action is available.
     *
     * @param action name of the action to check for
     * @param hints
     * @return true if the action is present as a workflow hint and its value is true
     */
    public static boolean isActionAvailable(final String action, final Map<String, Serializable> hints) {
        return isHintActionTrue(hints, action);
    }

    /**
     * Check a workflow to see if an action is available.
     *
     * @param workflow the workflow to check
     * @param action   name of the action to check for
     * @return true if the action is present as a workflow hint and its value is true
     */
    public static boolean isActionAvailable(final Workflow workflow, final String action) {
        try {
            final Map<String, Serializable> hints = workflow.hints();
            return isHintActionTrue(hints, action);
        } catch (RemoteException | RepositoryException | WorkflowException e) {
            log.warn("Failed reading hints from workflow", e);
        }
        return false;
    }

    public static boolean isRequestActionAvailable(final String action, final String requestIdentifier, final Map<String, Serializable> hints) {
        if (hints.containsKey("requests")) {
            final Map requestsMap = (Map) hints.get("requests");
            if (requestsMap.containsKey(requestIdentifier)) {
                final Map requestHints = (Map) requestsMap.get(requestIdentifier);
                return isHintActionTrue(requestHints, action);
            }
        }
        return false;
    }

    /**
     * Check if an action is available as hint with value true.
     *
     * @param hints  map of workflow hints
     * @param action name of the action to check for
     * @return true if the hints map contains the action and its value is true
     */
    public static boolean isHintActionTrue(final Map<String, Serializable> hints, final String action) {
        final Serializable value = hints.get(action);
        if (value != null && !(value instanceof Boolean)) {
            log.warn("Value of hint action '{}' is expected to be boolean but it was '{}'", action, value);
        }
        return Boolean.TRUE.equals(value);
    }

    /**
     * Check if an action is available as hint and has value false.
     *
     * @param hints  map of workflow hints
     * @param action name of the action to check for
     * @return true if the hints map contains the action and its value is false
     */
    public static boolean isHintActionFalse(final Map<String, Serializable> hints, final String action) {
        final Serializable value = hints.get(action);
        if (!(value instanceof Boolean)) {
            log.warn("Value of hint action '{}' is expected to be boolean but it was '{}'", action, value);
        }
        return Boolean.FALSE.equals(value);
    }

    /**
     * Get a backing JCR node of an editable document for the branch given by branchId. If workflow or repository
     * exceptions occur while getting the node an empty optional is returned and a warning is logged.
     *
     * @param workflow Editable workflow for the desired document
     * @param branchId id of the branch
     * @param session  JCR session for obtaining the backing node
     * @return JCR node or nothing, wrapped in an Optional
     */
    public static Optional<Node> getEditableDocumentNode(final EditableWorkflow workflow, final String branchId, final Session session) {
        try {
            final Document document = workflow.obtainEditableInstance(branchId);
            return Optional.of(document.getNode(session));
        } catch (WorkflowException | RepositoryException | RemoteException e) {
            log.warn("Failed to obtain draft for user '{}'.", session.getUserID(), e);
        }
        return Optional.empty();
    }

    /**
     * <p>Get a backing JCR node of the draft variant of an editable document. If workflow or repository
     * exceptions occur while getting the node an empty optional is returned and a warning is logged.</p>
     *
     * @param workflow {@link EditableWorkflow} for the desired document
     * @param session  {@link Session} for obtaining the backing node
     * @return {@link Node} or nothing, wrapped in an {@link Optional}
     */
    public static Optional<Node> getDraftNode(final EditableWorkflow workflow, final Session session) {
        try {
            final Document document = workflow.editDraft();
            return Optional.of(document.getNode(session));
        } catch (WorkflowException | RepositoryException | RemoteException e) {
            log.warn("Failed to obtain draft for user '{}'.", session.getUserID(), e);
        }
        return Optional.empty();
    }
}
