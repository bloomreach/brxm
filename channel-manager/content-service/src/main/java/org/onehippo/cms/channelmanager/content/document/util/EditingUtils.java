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
import java.util.Map;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.onehippo.repository.security.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EditingUtils provides utility methods for dealing with the workflow of a document.
 */
public class EditingUtils {

    private static final Logger log = LoggerFactory.getLogger(EditingUtils.class);

    static final String HINT_COMMIT_EDITABLE_INSTANCE = "commitEditableInstance";
    static final String HINT_DISPOSE_EDITABLE_INSTANCE = "disposeEditableInstance";
    static final String HINT_OBTAIN_EDITABLE_INSTANCE = "obtainEditableInstance";

    private static final String HINT_PREVIEW_AVAILABLE = "previewAvailable";
    private static final String HINT_DELETE = "delete";
    private static final String HINT_RENAME = "rename";

    private EditingUtils() {
    }

    /**
     * Check if a workflow indicates that editing of a document can be started.
     *
     * @param workflow workflow for the current user on a specific document
     * @return true/false.
     */
    public static boolean canCreateDraft(final EditableWorkflow workflow) {
        return isActionAvailable(workflow, HINT_OBTAIN_EDITABLE_INSTANCE);
    }

    /**
     * Check if a document can be updated, given its workflow.
     *
     * @param workflow editable workflow of a document
     * @return true if document can be updated, false otherwise
     */
    public static boolean canUpdateDraft(final EditableWorkflow workflow) {
        return isActionAvailable(workflow, HINT_COMMIT_EDITABLE_INSTANCE);
    }

    /**
     * Check if a document can be updated, given its workflow.
     *
     * @param workflow editable workflow of a document
     * @return true if document can be updated, false otherwise
     */
    public static boolean canDeleteDraft(final EditableWorkflow workflow) {
        return isActionAvailable(workflow, HINT_DISPOSE_EDITABLE_INSTANCE);
    }

    /**
     * Check if document can be archived (i.e. moved to the attic and stripped of all its data), given its workflow.
     *
     * @param workflow workflow of the document
     * @return true if the document can be archived, false otherwise
     */
    public static boolean canArchiveDocument(final DocumentWorkflow workflow) {
        return isActionAvailable(workflow, HINT_DELETE);
    }

    /**
     * Check if a document can be erased from a folder, i.e. permanently deleted without any archiving in the attic.
     *
     * @param workflow workflow of the folder
     * @return true if the document can be erased, false otherwise
     */
    public static boolean canEraseDocument(final FolderWorkflow workflow) {
        return isActionAvailable(workflow, HINT_DELETE);
    }

    /**
     * Check if document can be renamed (i.e. change its URL name), given its workflow.
     *
     * @param workflow workflow of the document
     * @return true if the document can be renamed, false otherwise
     */
    public static boolean canRenameDocument(final DocumentWorkflow workflow) {
        return isActionAvailable(workflow, HINT_RENAME);
    }

    /**
     * Check if a document has a 'preview' variant.
     *
     * @param workflow the workflow of the document
     * @return true if the document has a 'preview' variant, false otherwise.
     */
    public static boolean hasPreview(final DocumentWorkflow workflow) {
        return isActionAvailable(workflow, HINT_PREVIEW_AVAILABLE);
    }

    public static boolean isActionAvailable(final Workflow workflow, final String action) {
        try {
            final Map<String, Serializable> hints = workflow.hints();
            return hints.containsKey(action) && ((Boolean) hints.get(action));

        } catch (ClassCastException | RemoteException | RepositoryException | WorkflowException e) {
            log.warn("Failed reading hints from workflow", e);
        }
        return false;
    }

    public static boolean isRequestActionAvailable(final Workflow workflow, final String action, final String requestIdentifier) {
        try {
            final Map<String, Serializable> hints = workflow.hints();
            if (hints.containsKey("requests")) {
                final Map requestsMap = (Map) hints.get("requests");
                if (requestsMap.containsKey(requestIdentifier)) {
                    final Map actions = (Map) requestsMap.get(requestIdentifier);
                    return actions.containsKey(action) && ((Boolean) actions.get(action));
                }
            }
        } catch (ClassCastException | RemoteException | RepositoryException | WorkflowException e) {
            log.warn("Failed reading hints from workflow", e);
        }
        return false;
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
     * Create a draft variant node for a document represented by handle node.
     *
     * @param workflow Editable workflow for the desired document
     * @param session  JCR session for obtaining the draft node
     * @return JCR draft node or nothing, wrapped in an Optional
     */
    public static Optional<Node> createDraft(final EditableWorkflow workflow, final Session session) {
        try {
            final Document document = workflow.obtainEditableInstance();
            return Optional.of(document.getNode(session));
        } catch (WorkflowException | RepositoryException | RemoteException e) {
            log.warn("Failed to obtain draft for user '{}'.", session.getUserID(), e);
        }
        return Optional.empty();
    }

    /**
     * Copy the (validated) draft to the preview, and re-obtain the editable instance.
     *
     * @param workflow Editable workflow for the desired document
     * @param session  JCR session for re-obtaining the draft node
     * @return JCR draft node or nothing, wrapped in an Optional
     */
    public static Optional<Node> copyToPreviewAndKeepEditing(final EditableWorkflow workflow, final Session session) {
        try {
            workflow.commitEditableInstance();
        } catch (WorkflowException | RepositoryException | RemoteException e) {
            log.warn("Failed to commit changes for user '{}'.", session.getUserID(), e);
            return Optional.empty();
        }

        return createDraft(workflow, session);
    }
}
