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
import java.util.Map;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowTransition;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.api.DocumentWorkflowAction.COMMIT_EDITABLE_INSTANCE;
import static org.hippoecm.repository.api.DocumentWorkflowAction.OBTAIN_EDITABLE_INSTANCE;

public class EditingUtils {

    private static final Logger log = LoggerFactory.getLogger(EditingUtils.class);

    private final HintsInspector hintsInspector;

    public EditingUtils(HintsInspector hintsInspector) {
        this.hintsInspector = hintsInspector;
    }

    public boolean canCreateDraft(final WorkflowContext workflowContext) {
        return getHints(workflowContext).map(hintsInspector::canCreateDraft).orElse(false);
    }

    public boolean canUpdateDraft(final WorkflowContext workflowContext) {
        return getHints(workflowContext).map(hintsInspector::canUpdateDraft).orElse(false);
    }

    public boolean canDeleteDraft(final WorkflowContext workflowContext) {
        return getHints(workflowContext).map(hintsInspector::canDeleteDraft).orElse(false);
    }

    public Optional<ErrorInfo> determineEditingFailure(final WorkflowContext workflowContext) {
        return getHints(workflowContext).flatMap(hints -> hintsInspector.determineEditingFailure(hints, workflowContext.getSession()));
    }

    public Optional<Node> createDraft(final WorkflowContext workflowContext) {
        try {
            final Document document = (Document)workflowContext.getWorkflow().transition(workflowContext.createWorkflowTransition(OBTAIN_EDITABLE_INSTANCE));
            return Optional.of(document.getNode(workflowContext.getSession()));
        } catch (WorkflowException | RepositoryException e) {
            log.warn("Failed to obtain draft for user '{}'.", workflowContext.getSession().getUserID(), e);
        }
        return Optional.empty();
    }

    public Optional<Node> copyToPreviewAndKeepEditing(final WorkflowContext workflowContext) {
        try {
            workflowContext.getWorkflow().transition(workflowContext.createWorkflowTransition(COMMIT_EDITABLE_INSTANCE));
        } catch (WorkflowException e) {
            log.warn("Failed to commit changes for user '{}'.", workflowContext.getSession().getUserID(), e);
            return Optional.empty();
        }

        return createDraft(workflowContext);
    }

    private Optional<Map<String, Serializable>> getHints(final WorkflowContext workflowContext) {
        try {
            return Optional.of(workflowContext.getWorkflow().hints(workflowContext.getContextPayload()));
        } catch (WorkflowException | RemoteException | RepositoryException e) {
            log.warn("Failed reading hints from workflow", e);
        }
        return Optional.empty();
    }

}
