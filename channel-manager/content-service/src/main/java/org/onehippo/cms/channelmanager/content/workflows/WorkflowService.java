/*
 * Copyright 2018-2021 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms.channelmanager.content.workflows;

import java.util.Optional;

import javax.jcr.Session;

import org.onehippo.cms.channelmanager.content.document.model.Version;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;

/**
 * Exposes an API for retrieving workflow information and executing workflow actions
 */
public interface WorkflowService {

    /**
     * @see #executeDocumentWorkflowAction(String, String, Session, String, Optional<Version>)
     */
    void executeDocumentWorkflowAction(String uuid, String action, Session session,
                                       String branchId) throws ErrorWithPayloadException;

    /**
     * Executes a {@link DocumentWorkflow} action.
     *
     * @param uuid     UUID of the document to be updated
     * @param action   the name of the workflow action to be executed (e.g. "publish")
     * @param session  user-authenticated, invocation-scoped JCR session. In case of a bad request, changes may be
     *                 pending.
     * @param branchId id of branch for which to execute the action
     * @param version optional parameter containing the Version
     * @throws ErrorWithPayloadException If executing the action failed.
     */
    void executeDocumentWorkflowAction(String uuid, String action, Session session,
                                       String branchId, Optional<Version> version) throws ErrorWithPayloadException;


    void restoreDocumentWorkflowAction(String documentId, String frozenNodeId, Session session, String branchId)
            throws ErrorWithPayloadException;

}
