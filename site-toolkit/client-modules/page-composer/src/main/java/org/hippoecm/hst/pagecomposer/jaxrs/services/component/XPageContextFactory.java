/*
 * Copyright 2020 Bloomreach
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
 *
 */
package org.hippoecm.hst.pagecomposer.jaxrs.services.component;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.component.state.util.DocumentState;
import org.hippoecm.hst.pagecomposer.jaxrs.services.component.state.util.DocumentStateUtils;
import org.hippoecm.hst.pagecomposer.jaxrs.services.component.state.util.ScheduledRequest;
import org.hippoecm.hst.pagecomposer.jaxrs.services.component.state.util.WorkflowRequest;
import org.hippoecm.hst.pagecomposer.jaxrs.services.experiencepage.XPageUtils;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.util.DocumentUtils;
import org.onehippo.repository.branch.BranchConstants;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;

import static java.lang.Boolean.TRUE;

final class XPageContextFactory {

    XPageContext make(final PageComposerContextService contextService) throws RepositoryException, WorkflowException, RemoteException {

        if (!contextService.isExperiencePageRequest()) {
            return null;
        }

        final String experiencePageHandleUUID = contextService.getExperiencePageHandleUUID();
        final HippoSession userSession = (HippoSession) contextService.getRequestContext().getSession();
        final Node handle = userSession.getNodeByIdentifier(experiencePageHandleUUID);
        final DocumentState documentState = DocumentStateUtils.getPublicationStateFromHandle(handle);
        final String name = DocumentUtils.getDisplayName(handle).orElse(handle.getName());
        final ScheduledRequest scheduledRequest = DocumentStateUtils.getScheduledRequest(handle);
        final WorkflowRequest workflowRequest = DocumentStateUtils.getWorkflowRequest(handle);
        final DocumentWorkflow workflow = XPageUtils.getDocumentWorkflow(userSession, contextService);
        final String selectedBranchId = contextService.getSelectedBranchId();
        final Map<String, Serializable> hints = workflow.hints(selectedBranchId);

        final XPageContext xPageContext = new XPageContext()
                .setBranchId(selectedBranchId)
                .setXPageId(experiencePageHandleUUID)
                .setXPageName(name)
                .setXPageState(documentState.name().toLowerCase())
                .setScheduledRequest(scheduledRequest)
                .setWorkflowRequest(workflowRequest)
                .setCopyAllowed(TRUE.equals(hints.get("copy")))
                .setMoveAllowed(TRUE.equals(hints.get("move")))
                .setDeleteAllowed(TRUE.equals(hints.get("delete")));

        if (!BranchConstants.MASTER_BRANCH_ID.equals(selectedBranchId)) {
            return xPageContext;
        }

        if (hints.containsKey("publish")) {
            xPageContext.setPublishable(TRUE.equals(hints.get("publish")));
        } else if (hints.containsKey("requestPublication")) {
            xPageContext.setRequestPublication(TRUE.equals(hints.get("requestPublication")));
        }

        if (hints.containsKey("depublish")) {
            xPageContext.setUnpublishable(TRUE.equals(hints.get("depublish")));
        } else if (hints.containsKey("requestDepublication")) {
            xPageContext.setRequestDepublication(TRUE.equals(hints.get("requestDepublication")));
        }

        if (hints.containsKey("inUseBy")) {
            xPageContext.setLockedBy((String) hints.get("inUseBy"));
        }

        return xPageContext;
    }
}
