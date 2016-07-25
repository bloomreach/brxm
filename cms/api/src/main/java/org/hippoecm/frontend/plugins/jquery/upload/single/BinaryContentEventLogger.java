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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.eventbus.HippoEventBus;
import org.onehippo.repository.events.HippoWorkflowEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.api.HippoNodeType.NT_HANDLE;

public final class BinaryContentEventLogger {

    private static final Logger log = LoggerFactory.getLogger(BinaryContentEventLogger.class);

    private BinaryContentEventLogger() {
    }

    /**
     * Post a HippoEvent for specific asset or gallery node.
     *
     * @param node asset or gallery node.
     * @param category workflow category name e.g. cms
     * @param type type of workflow e.g. image or resource
     * @param action name of the workflow action e.g. upload or crop
     * @throws RepositoryException
     */
    public static void fireBinaryChangedEvent(final Node node, final String category, final String type, String action) throws RepositoryException {
        if (node == null) {
            log.warn("Cannot publish event for null");
            return;
        }

        final HippoEventBus eventBus = HippoServiceRegistry.getService(HippoEventBus.class);
        if (eventBus != null) {
            final HippoWorkflowEvent event = new HippoWorkflowEvent();
            final Node handle = getHandle(node);
            if (handle == null) {
                log.warn("Handle was null for node: {}", node.getPath());
                return;
            }

            final String documentType = getDocumentType(node);
            event.user(node.getSession().getUserID()).action(action).system(false);
            event.subjectPath(node.getPath())
                    .subjectId(node.getIdentifier())
                    .interactionId(node.getIdentifier())
                    .interaction(getInteraction(category, type, action))
                    .workflowCategory(category)
                    .workflowName(action)
                    .documentType(documentType);
            eventBus.post(event);
        }
        else {
            log.warn("No event bus service found by class {}", HippoEventBus.class.getName());
        }
    }


    /**
     * Find first component with backing JcrNodeModel and fire an upload event.
     *
     * @param component top component in component chain
     * @param category workflow category e.g. cms
     * @param type type of workflow e.g. image or resource
     * @param action name of the workflow action e.g. upload or crop
     */
    public static void fireUploadEvent(final MarkupContainer component, final String category, final String type, final String action) {
        MarkupContainer markupContainer = component;
        while (markupContainer != null) {
            IModel<?> model = markupContainer.getDefaultModel();
            if (model instanceof JcrNodeModel) {
                final JcrNodeModel nodeModel = (JcrNodeModel) model;
                final Node subjectNode = nodeModel.getNode();
                try {
                    Session session = subjectNode.getSession();
                    session.save();
                    fireBinaryChangedEvent(subjectNode, category, type, action);
                } catch (RepositoryException e) {
                    log.error("Error saving session", e);
                }
                return;
            }
            markupContainer = markupContainer.getParent();
        }
    }

    private static String getInteraction(final String category, final String type, final String action) throws RepositoryException {
        return category + ':' + type + ':' + action;
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
