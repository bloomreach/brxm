/*
 * Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.onehippo.repository.security.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.cms.channelmanager.content.document.ContextPayloadUtils.getBranchId;

/**
 * EditingUtils provides utility methods for dealing with the workflow of a document.
 */
public class EditingUtils {

    private static final Logger log = LoggerFactory.getLogger(EditingUtils.class);

    public static final String HINT_PUBLISH = "publish";
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
     * Gets the hints from the workflow, taking into account the branchId present in the context payload.
     * The hints that are returned can then be passed to the other methods of this class. Please note that the hints
     * are valid until a workflow action has been executed. After execution of a workflow action the hints should be
     * recomputed because the state of the involved nodes usually changes.
     *
     * @param workflow       a workflow
     * @param contextPayload the context payload
     * @return hints
     */
    public static Map<String, Serializable> getHints(EditableWorkflow workflow, Map<String, Serializable> contextPayload) {
        final Map<String, Serializable> hints = new HashMap<>();
        if (contextPayload != null) {
            hints.putAll(contextPayload);
        }
        final String branchId = getBranchId(contextPayload);
        try {
            hints.putAll(workflow.hints(branchId));
        } catch (WorkflowException | RepositoryException | RemoteException e) {
            log.warn("Failed reading hints from workflow", e);
        }
        return hints;
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
     * @param hints
     * @return true if the document can be erased, false otherwise
     */
    public static boolean canEraseDocument(final Map<String, Serializable> hints) {
        return isActionAvailable(HINT_DELETE, hints);
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
        if (!(value instanceof Boolean)) {
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
     * Look up the real user name pertaining to a user ID
     *
     * @param userId  ID of some user
     * @param session current user's JCR session
     * @return name of the user or nothing, wrapped in an Optional
     */
    public static Optional<String> getUserName(final String userId, final Session session) {
        try {
            final HippoWorkspace workspace = (HippoWorkspace) session.getWorkspace();
            final User user = workspace.getSecurityService().getUser(userId);
            final String firstName = user.getFirstName();
            final String lastName = user.getLastName();

            final StringBuilder sb = new StringBuilder();
            if (firstName != null) {
                sb.append(firstName.trim());
                sb.append(" ");
            }
            if (lastName != null) {
                sb.append(lastName.trim());
            }
            return Optional.of(sb.toString().trim());
        } catch (final RepositoryException e) {
            log.debug("Unable to determine displayName of user '{}'.", userId, e);
        }
        return Optional.empty();
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
}
