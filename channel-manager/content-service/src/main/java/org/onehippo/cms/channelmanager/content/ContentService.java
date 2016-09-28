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

package org.onehippo.cms.channelmanager.content;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.onehippo.cms.channelmanager.content.exception.DocumentNotFoundException;
import org.onehippo.cms.channelmanager.content.model.Document;
import org.onehippo.cms.channelmanager.content.model.DocumentInfo;
import org.onehippo.cms.channelmanager.content.model.EditingInfo;
import org.onehippo.cms.channelmanager.content.model.UserInfo;
import org.onehippo.cms.channelmanager.content.util.MockResponse;
import org.onehippo.repository.security.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ContentService provides functionality to manipulate CMS content.
 */
public class ContentService {
    private static final Logger log = LoggerFactory.getLogger(ContentService.class);

    public Document getDocument(final Session session, final String id) throws DocumentNotFoundException {
        if ("test".equals(id)) {
            return MockResponse.createTestDocument(id);
        }

        final Document document = new Document();
        document.setId(id);

        try {
            final Node documentHandle = session.getNodeByIdentifier(id);
            document.setDisplayName(((HippoNode)documentHandle).getDisplayName());

            if (!HippoNodeType.NT_HANDLE.equals(documentHandle.getPrimaryNodeType().getName())
                    || !documentHandle.hasNode(documentHandle.getName())) {
                log.debug("Id {} doesn't refer to valid document", id);
                throw new DocumentNotFoundException();
            }

            final DocumentInfo info = new DocumentInfo();
            final Node variant = documentHandle.getNode(documentHandle.getName());
            info.setTypeId(variant.getPrimaryNodeType().getName());
            info.setEditingInfo(determineEditingInfo(session, documentHandle));
            document.setInfo(info);
        } catch (RepositoryException e) {
            log.debug("Problem reading vital document parameters", e);
            throw new DocumentNotFoundException();
        }

        // look-up document type and fill in document content

        return document;
    }

    // protected for better testability
    protected EditingInfo determineEditingInfo(final Session session, final Node documentHandle) {
        final EditingInfo info = new EditingInfo();
        final HippoWorkspace workspace = (HippoWorkspace)session.getWorkspace();

        try {
            final WorkflowManager workflowManager = workspace.getWorkflowManager();
            final Map<String, Serializable> hints = workflowManager.getWorkflow("editing", documentHandle).hints();

            if ((Boolean) hints.get("obtainEditableInstance")) {
                info.setState(EditingInfo.State.AVAILABLE);
            } else if (hints.containsKey("inUseBy")) {
                final String inUseBy = (String)hints.get("inUseBy");
                if (inUseBy.equals(session.getUserID())) {
                    info.setState(EditingInfo.State.AVAILABLE);
                } else {
                    info.setState(EditingInfo.State.UNAVAILABLE_HELD_BY_OTHER_USER);
                    info.setHolder(determineHolder(inUseBy, workspace));
                }
            } else if (hints.containsKey("requests")) {
                info.setState(EditingInfo.State.UNAVAILABLE_REQUEST_PENDING);
            }
        } catch (RepositoryException|WorkflowException|RemoteException e) {
            log.debug("Failed to determine editing info", e);
        }
        return info;
    }

    // protected for better testability
    protected UserInfo determineHolder(final String holderId, final HippoWorkspace workspace) {
        final UserInfo holder = new UserInfo();
        holder.setId(holderId);
        try {
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
}
