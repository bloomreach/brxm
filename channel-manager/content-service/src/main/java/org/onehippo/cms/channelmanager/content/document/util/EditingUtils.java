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
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EditingUtils provides utility methods for dealing with the workflow of a document.
 */
public class EditingUtils {

    private static final Logger log = LoggerFactory.getLogger(EditingUtils.class);
    private static final String HINT_IN_USE_BY = "inUseBy";
    private static final String HINT_COMMIT_EDITABLE_INSTANCE = "commitEditableInstance";
    private static final String HINT_DISPOSE_EDITABLE_INSTANCE = "disposeEditableInstance";
    private static final String HINT_OBTAIN_EDITABLE_INSTANCE = "obtainEditableInstance";
    private static final String HINT_REQUESTS = "requests";

    /**
     * Check if a workflow indicates that editing of a document can be started.
     *
     * @param workflow workflow for the current user on a specific document
     * @return true/false.
     */
    public boolean canCreateDraft(final Workflow workflow) {
        return isActionAvailable(workflow, HINT_OBTAIN_EDITABLE_INSTANCE);
    }

    /**
     * Check if a document can be updated, given its workflow.
     *
     * @param workflow editable workflow of a document
     * @return true if document can be updated, false otherwise
     */
    public boolean canUpdateDraft(final Workflow workflow) {
        return isActionAvailable(workflow, HINT_COMMIT_EDITABLE_INSTANCE);
    }

    /**
     * Check if a document can be updated, given its workflow.
     *
     * @param workflow editable workflow of a document
     * @return true if document can be updated, false otherwise
     */
    public boolean canDeleteDraft(final Workflow workflow) {
        return isActionAvailable(workflow, HINT_DISPOSE_EDITABLE_INSTANCE);
    }

    private boolean isActionAvailable(final Workflow workflow, final String action) {
        try {
            Map<String, Serializable> hints = workflow.hints();
            return hints.containsKey(action) && ((Boolean) hints.get(action));

        } catch (WorkflowException | RemoteException | RepositoryException e) {
            log.warn("Failed reading hints from workflow", e);
        }
        return false;
    }

    /**
     * Determine the reason why editing failed for the present workflow.
     *
     * @param workflow workflow for the current user on a specific document
     * @param session  current user's JCR session
     * @return Specific reason or nothing (unknown), wrapped in an Optional
     */
    public Optional<ErrorInfo> determineEditingFailure(final Workflow workflow, final Session session) {
        try {
            final Map<String, Serializable> hints = workflow.hints();
            if (hints.containsKey(HINT_IN_USE_BY)) {
                final Map<String, Serializable> params = new HashMap<>();
                final String userId = (String) hints.get(HINT_IN_USE_BY);
                params.put("userId", userId);
                UserUtils.getUserName(userId, session).ifPresent(userName -> params.put("userName", userName));

                return Optional.of(new ErrorInfo(ErrorInfo.Reason.OTHER_HOLDER, params));
            }

            if (hints.containsKey(HINT_REQUESTS)) {
                return Optional.of(new ErrorInfo(ErrorInfo.Reason.REQUEST_PENDING));
            }
        } catch (RepositoryException | WorkflowException | RemoteException e) {
            log.warn("Failed to retrieve hints for workflow '{}'", workflow, e);
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
    public Optional<Node> createDraft(final EditableWorkflow workflow, final Session session) {
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
    public Optional<Node> copyToPreviewAndKeepEditing(final EditableWorkflow workflow, final Session session) {
        try {
            workflow.commitEditableInstance();
        } catch (WorkflowException | RepositoryException | RemoteException e) {
            log.warn("Failed to commit changes for user '{}'.", session.getUserID(), e);
            return Optional.empty();
        }

        return createDraft(workflow, session);
    }
}
