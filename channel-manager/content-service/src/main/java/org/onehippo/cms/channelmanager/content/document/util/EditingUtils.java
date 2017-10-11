/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.Session;

import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;

/**
 * EditingUtils provides utility methods for dealing with the workflow of a document.
 */
public interface EditingUtils {

    /**
     * Check if a workflow indicates that editing of a document can be started.
     *
     * @param workflow workflow for the current user on a specific document
     * @return true/false.
     */
    boolean canCreateDraft(Workflow workflow);

    /**
     * Check if a document can be updated, given its workflow.
     *
     * @param workflow editable workflow of a document
     * @return true if document can be updated, false otherwise
     */
    boolean canUpdateDraft(Workflow workflow);

    /**
     * Check if a document can be updated, given its workflow.
     *
     * @param workflow editable workflow of a document
     * @return true if document can be updated, false otherwise
     */
    boolean canDeleteDraft(Workflow workflow);

    /**
     * Determine the reason why editing failed for the present workflow.
     *
     * @param workflow workflow for the current user on a specific document
     * @param session  current user's JCR session
     * @return Specific reason or nothing (unknown), wrapped in an Optional
     */
    Optional<ErrorInfo> determineEditingFailure(Workflow workflow, Session session);

    /**
     * Create a draft variant node for a document represented by handle node.
     *
     * @param workflow Editable workflow for the desired document
     * @param session  JCR session for obtaining the draft node
     * @return JCR draft node or nothing, wrapped in an Optional
     */
    Optional<Node> createDraft(EditableWorkflow workflow, Session session);

    /**
     * Copy the (validated) draft to the preview, and re-obtain the editable instance.
     *
     * @param workflow Editable workflow for the desired document
     * @param session  JCR session for re-obtaining the draft node
     * @return JCR draft node or nothing, wrapped in an Optional
     */
    Optional<Node> copyToPreviewAndKeepEditing(EditableWorkflow workflow, Session session);

}
