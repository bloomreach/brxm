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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.component.state.util.DocumentState;
import org.hippoecm.hst.pagecomposer.jaxrs.services.component.state.util.DocumentStateUtils;
import org.hippoecm.hst.pagecomposer.jaxrs.services.component.state.util.ScheduledRequest;
import org.hippoecm.hst.pagecomposer.jaxrs.services.component.state.util.WorkflowRequest;
import org.hippoecm.repository.util.DocumentUtils;

final class XPageContextFactory {

    XPageContext make(final PageComposerContextService contextService) throws RepositoryException {

        if (!contextService.isExperiencePageRequest()) {
            return null;
        }
        final String experiencePageHandleUUID = contextService.getExperiencePageHandleUUID();
        final Node handle = contextService.getRequestContext().getSession().getNodeByIdentifier(experiencePageHandleUUID);
        final DocumentState documentState = DocumentStateUtils.getPublicationStateFromHandle(handle);
        final String name = DocumentUtils.getDisplayName(handle).orElse(handle.getName());
        final ScheduledRequest scheduledRequest = DocumentStateUtils.getScheduledRequest(handle);
        final WorkflowRequest workflowRequest = DocumentStateUtils.getWorkflowRequest(handle);

        return new XPageContext()
                .setBranchId(contextService.getSelectedBranchId())
                .setXPageId(experiencePageHandleUUID)
                .setXPageName(name)
                .setXPageState(documentState.name().toLowerCase())
                .setScheduledRequest(scheduledRequest)
                .setWorkflowRequest(workflowRequest);

    }
}
