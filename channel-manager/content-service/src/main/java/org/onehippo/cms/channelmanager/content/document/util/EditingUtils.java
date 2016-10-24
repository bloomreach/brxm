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
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms.channelmanager.content.document.model.EditingInfo;
import org.onehippo.cms.channelmanager.content.document.model.UserInfo;
import org.onehippo.repository.security.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EditingUtils provides utility methods for dealing with the "editing state" of a document.
 * @see EditingInfo
 */
public class EditingUtils {

    private static final Logger log = LoggerFactory.getLogger(EditingUtils.class);
    private static final String HINT_IN_USE_BY = "inUseBy";
    private static final String HINT_COMMIT_EDITABLE_INSTANCE = "commitEditableInstance";
    private static final String HINT_DISPOSE_EDITABLE_INSTANCE = "disposeEditableInstance";
    private static final String HINT_OBTAIN_EDITABLE_INSTANCE = "obtainEditableInstance";
    private static final String HINT_REQUESTS = "requests";

    private EditingUtils() { }

    /**
     * Create and populate a {@link EditingInfo}, given a document's handle node and workflow.
     *
     * @param workflow Workflow of a document, providing access to its 'hints'
     * @param handle   JCR node representing the handle of a document
     * @return         New and populated instance of EditingInfo
     */
    public static EditingInfo determineEditingInfo(final Workflow workflow, final Node handle) {
        final EditingInfo info = new EditingInfo();

        try {
            final Session session = handle.getSession();
            final Map<String, Serializable> hints = workflow.hints();

            if (isDocumentEditable(hints, session)) {
                info.setState(EditingInfo.State.AVAILABLE);
            } else if (hints.containsKey(HINT_IN_USE_BY)) {
                info.setState(EditingInfo.State.UNAVAILABLE_HELD_BY_OTHER_USER);
                info.setHolder(determineHolder((String)hints.get(HINT_IN_USE_BY), session));
            } else if (hints.containsKey(HINT_REQUESTS)) {
                info.setState(EditingInfo.State.UNAVAILABLE_REQUEST_PENDING);
            }
        } catch (RepositoryException | WorkflowException | RemoteException e) {
            log.warn("Failed to determine editing info for node '{}'", JcrUtils.getNodePathQuietly(handle), e);
        }
        return info;
    }

    private static boolean isDocumentEditable(final Map<String, Serializable> hints, final Session session) {
        if ((Boolean) hints.get(HINT_OBTAIN_EDITABLE_INSTANCE)) {
            return true;
        }

        // TODO: initial tests suggested that once the user has obtained the editable instance of a document,
        //       the hints would have set the obtainEditableInstance flag to false and the inUseBy flag to the
        //       current holder (self). Subsequent tests no longer observed this behaviour. Should we keep below
        //       extra check or not?
        if (hints.containsKey(HINT_IN_USE_BY)) {
            final String inUseBy = (String) hints.get(HINT_IN_USE_BY);
            if (inUseBy.equals(session.getUserID())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if a document can be updated, given its workflow.
     *
     * @param workflow editable workflow of a document
     * @return         true if document can be updated, false otherwise
     */
    public static boolean canUpdateDocument(final EditableWorkflow workflow) {
        return isHintAvailable(workflow, HINT_COMMIT_EDITABLE_INSTANCE);
    }

    /**
     * Check if a document can be updated, given its workflow.
     *
     * @param workflow editable workflow of a document
     * @return         true if document can be updated, false otherwise
     */
    public static boolean canDeleteDraft(final EditableWorkflow workflow) {
        return isHintAvailable(workflow, HINT_DISPOSE_EDITABLE_INSTANCE);
    }

    private static boolean isHintAvailable(final EditableWorkflow workflow, final String hint) {
        try {
            Map<String, Serializable> hints = workflow.hints();
            return hints.containsKey(hint) && ((Boolean)hints.get(hint));

        } catch (WorkflowException | RemoteException | RepositoryException e) {
            log.warn("Failed reading hints from workflow", e);
        }
        return false;
    }

    /**
     * Create and populate a {@link UserInfo}, given a user's ID
     *
     * @param holderId ID of the desired user
     * @param session  JCR session to access information about users
     * @return         New and populated instance of UserInfo
     */
    public static UserInfo determineHolder(final String holderId, final Session session) {
        final UserInfo holder = new UserInfo();
        holder.setId(holderId);
        try {
            final HippoWorkspace workspace = (HippoWorkspace) session.getWorkspace();
            final User user =  workspace.getSecurityService().getUser(holderId);
            final String firstName = user.getFirstName();
            final String lastName = user.getLastName();

            // TODO: Below logic was copied from the org.hippoecm.frontend.plugins.cms.admin.users.User.java
            // (hippo-cms-perspectives). Move this logic into the repository's User class to be able to share it?
            StringBuilder sb = new StringBuilder();
            if (firstName != null) {
                sb.append(firstName.trim());
                sb.append(" ");
            }
            if (lastName != null) {
                sb.append(lastName.trim());
            }
            holder.setDisplayName(sb.toString().trim());
        } catch (RepositoryException e) {
            log.debug("Unable to determine displayName of holder", e);
        }
        return holder;
    }

    /**
     * Create a draft variant node for a document represented by handle node.
     *
     * @param workflow Editable workflow for the desired document
     * @param handle   JCR handle node for the desired document
     * @return         JCR draft node or nothing, wrapped in an Optional
     */
    public static Optional<Node> createDraft(final EditableWorkflow workflow, final Node handle) {
        try {
            final Document document = workflow.obtainEditableInstance();
            final Session session = handle.getSession();
            return Optional.of(document.getNode(session));
        } catch (WorkflowException | RepositoryException | RemoteException e) {
            log.warn("Failed to retrieve draft node", e);
        }
        return Optional.empty();
    }
}
