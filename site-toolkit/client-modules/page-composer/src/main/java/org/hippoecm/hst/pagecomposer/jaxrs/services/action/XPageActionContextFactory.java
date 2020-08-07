/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.services.action;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.experiencepage.XPageUtils;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.WorkflowException;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;

import static java.lang.Boolean.TRUE;

public class XPageActionContextFactory {

    public XPageActionContext make(final PageComposerContextService contextService) throws WorkflowException, RemoteException, RepositoryException {
        final XPageActionContext xPageActionContext = new XPageActionContext()
                .setXPageId(contextService.getExperiencePageHandleUUID());

        if (!contextService.isExperiencePageRequest()) {
            return xPageActionContext;
        }

        final HippoSession userSession = (HippoSession) contextService.getRequestContext().getSession();
        final DocumentWorkflow workflow = XPageUtils.getDocumentWorkflow(userSession, contextService);
        final Map<String, Serializable> hints = workflow.hints();

        if (hints.containsKey("publish")) {
            xPageActionContext.setPublishable(TRUE.equals(hints.get("publish")));
        }
        if (hints.containsKey("depublish")) {
            xPageActionContext.setUnpublishable(TRUE.equals(hints.get("depublish")));
        }
        if (hints.containsKey("requestPublication")) {
            xPageActionContext.setRequestPublication(TRUE.equals(hints.get("requestPublication")));
        }
        if (hints.containsKey("requestDepublication")) {
            xPageActionContext.setRequestDepublication(TRUE.equals(hints.get("requestDepublication")));
        }

        xPageActionContext
                .setCopyAllowed(TRUE.equals(hints.get("copy")))
                .setMoveAllowed(TRUE.equals(hints.get("move")))
                .setDeleteAllowed(TRUE.equals(hints.get("delete")));


        return xPageActionContext;
    }
}
