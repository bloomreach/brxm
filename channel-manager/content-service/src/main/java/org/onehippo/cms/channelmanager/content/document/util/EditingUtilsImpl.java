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

public class EditingUtilsImpl implements EditingUtils {

    private static final Logger log = LoggerFactory.getLogger(EditingUtilsImpl.class);
    private static final String HINT_IN_USE_BY = "inUseBy";
    private static final String HINT_COMMIT_EDITABLE_INSTANCE = "commitEditableInstance";
    private static final String HINT_DISPOSE_EDITABLE_INSTANCE = "disposeEditableInstance";
    private static final String HINT_OBTAIN_EDITABLE_INSTANCE = "obtainEditableInstance";
    private static final String HINT_REQUESTS = "requests";

    @Override
    public boolean canCreateDraft(final Workflow workflow) {
        return isActionAvailable(workflow, HINT_OBTAIN_EDITABLE_INSTANCE);
    }

    @Override
    public boolean canUpdateDraft(final Workflow workflow) {
        return isActionAvailable(workflow, HINT_COMMIT_EDITABLE_INSTANCE);
    }

    @Override
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

    @Override
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

    @Override
    public Optional<Node> createDraft(final EditableWorkflow workflow, final Session session) {
        try {
            final Document document = workflow.obtainEditableInstance();
            return Optional.of(document.getNode(session));
        } catch (WorkflowException | RepositoryException | RemoteException e) {
            log.warn("Failed to obtain draft for user '{}'.", session.getUserID(), e);
        }
        return Optional.empty();
    }

    @Override
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
