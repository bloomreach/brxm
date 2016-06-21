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

package org.hippoecm.frontend.plugins.jquery.upload.single;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.eventbus.HippoEventBus;
import org.onehippo.repository.events.HippoWorkflowEvent;
import org.onehippo.repository.security.SecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.api.HippoNodeType.NT_HANDLE;

public final class BinaryWorkflowUtils {

    private static final Logger log = LoggerFactory.getLogger(BinaryWorkflowUtils.class);
    private static final String INTERACTION_TYPE_ASSETS = "assets-gallery";
    private static final String INTERACTION_TYPE_GALLERY = "image-gallery";
    private static final String WORKFLOW_NAME = "upload";
    private static final String WORKFLOW_CATEGORY = "workflow";
    private static final String ACTION = "fileUpload";

    private BinaryWorkflowUtils() {
    }

    /**
     * Post a HippoEvent for specific asset or gallery node.
     *
     * @param node asset or gallery node.
     * @throws RepositoryException
     */
    public static void postBinaryChangedEvent(final Node node) throws RepositoryException {
        if (node == null) {
            log.warn("Cannot publish event for null");
            return;
        }
        HippoEventBus eventBus = HippoServiceRegistry.getService(HippoEventBus.class);
        if (eventBus != null) {
            final HippoWorkflowEvent event = new HippoWorkflowEvent();
            final String returnValue = getReturnValue(node);
            final Node handle = getHandle(node);
            if (handle == null) {
                log.warn("Handle was null for node: {}", node.getPath());
                return;
            }
            final String returnType = "node";
            final Boolean system = isSystemUser(node);
            final String documentType = getDocumentType(handle);
            event.user(node.getSession().getUserID()).action(ACTION).result(returnValue).system(system);

            event
                    // TODO: check if we needed e.g. back porting
                    //.handleUuid(handle.getIdentifier())
                    .returnType(returnType).returnValue(returnValue).subjectPath(node.getPath()).subjectId(node.getIdentifier())
                    .interactionId(node.getIdentifier()).interaction(getInteraction(node)).workflowCategory(WORKFLOW_CATEGORY)
                    .workflowName(WORKFLOW_NAME).documentType(documentType);
            eventBus.post(event);
        }
    }


    public static void looseSessionChanges(final Session session) {
        try {
            if (session != null) {
                session.refresh(false);
            }
        } catch (RepositoryException e) {
            log.error("Error cleaning up session", e);
        }
    }

    /**
     * Find first component with backing JcrNodeModel and fire an upload event.
     *
     * @param component top component in component chain
     */
    public static void fireUploadEvent(final MarkupContainer component) {
        MarkupContainer markupContainer = component;
        while (markupContainer != null) {
            IModel<?> model = markupContainer.getDefaultModel();
            if (model instanceof JcrNodeModel) {
                final JcrNodeModel nodeModel = (JcrNodeModel) model;
                final Node subjectNode = nodeModel.getNode();
                Session session = null;
                try {
                    session = subjectNode.getSession();
                    session.save();
                    postBinaryChangedEvent(subjectNode);
                } catch (RepositoryException e) {
                    log.error("Error saving session", e);
                    looseSessionChanges(session);
                }
                return;
            }
            markupContainer = markupContainer.getParent();
        }
    }

    private static String getInteraction(final Node node) throws RepositoryException {
        final String path = node.getPath();
        if (path.startsWith("/content/assets")) {
            return INTERACTION_TYPE_ASSETS;
        }
        return INTERACTION_TYPE_GALLERY;
    }

    private static String getDocumentType(final Node subject) {
        try {
            if (subject.isNodeType(NT_HANDLE)) {
                for (Node child : new NodeIterable(subject.getNodes())) {
                    if (child.getName().equals(subject.getName())) {
                        return child.getPrimaryNodeType().getName();
                    }
                }
            } else if (subject.getParent().isNodeType(NT_HANDLE)) {
                return subject.getPrimaryNodeType().getName();
            }
        } catch (RepositoryException ignore) {
        }
        return null;
    }

    private static Boolean isSystemUser(final Node node) {

        try {
            final String userName = node.getSession().getUserID();
            final SecurityService securityService = ((HippoWorkspace) node.getSession().getWorkspace()).getSecurityService();
            return securityService.hasUser(userName) && securityService.getUser(userName).isSystemUser();
        } catch (ItemNotFoundException ignore) {
            // If hasUser returns true, we expect getUser to return a user, so it is very unlikely that
            // securityService.getUser(userName) will throw this exception
        } catch (RepositoryException e) {
            log.error("Failed to determine system status of event", e);
        }
        return null;
    }

    private static String getReturnValue(final Node node) throws RepositoryException {
        return "node[uuid=" +
                node.getIdentifier() +
                ",path='" +
                node.getPath() +
                "']";
    }

    private static Node getHandle(final Node node) {
        try {
            Node parent = node;
            while (parent != null) {
                if (parent.isNodeType(NT_HANDLE)) {
                    return parent;
                }
                parent = parent.getParent();
            }
        } catch (RepositoryException ignore) {
            // ignore
        }
        return null;
    }


}
