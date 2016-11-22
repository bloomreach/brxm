/*
 *  Copyright 2013-2016 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.repository.util;

import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WorkflowUtils {
    private static final Logger log = LoggerFactory.getLogger(WorkflowUtils.class);

    private WorkflowUtils() {}

    /**
     * Get the containing folder of a document.
     *
     * @param document either a document, a handle or a folder
     * @param session the session to use to get the containing folder for
     * @return  the folder containing this document or the root document
     * @throws RepositoryException
     */
    public static Document getContainingFolder(Document document, Session session) throws RepositoryException {
        return new Document(getContainingFolder(document.getNode(session)));
    }

    /**
     * Get the containing folder node of a document node.
     *
     * @param node  either a node representing a document, a handle, or a folder
     * @return  the folder node containing this document node or the root document
     * @throws RepositoryException
     */
    public static Node getContainingFolder(Node node) throws RepositoryException {
        final Node parent = node.getParent();
        if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
            return parent;
        } else if (node.isNodeType(HippoNodeType.NT_DOCUMENT))        {
            if (parent.isNodeType(HippoNodeType.NT_HANDLE)) {
                return parent.getParent();
            } else if (parent.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                return parent;
            }
        }
        return node.getSession().getRootNode();
    }

    /**
     * Retrieve a workflow of a certain type (class).
     *
     * @param node     JCR for which the workflow is requested
     * @param category Desired workflow category
     * @param clazz    Desired (super-)class of the workflow
     * @return         Workflow of the desired category and class, or nothing, wrapped in an Optional
     */
    public static <T extends Workflow> Optional<T> getWorkflow(final Node node, final String category, final Class<T> clazz) {
        try {
            final Session session = node.getSession();
            final HippoWorkspace workspace = (HippoWorkspace) session.getWorkspace();
            final WorkflowManager workflowManager = workspace.getWorkflowManager();
            final Workflow workflow = workflowManager.getWorkflow(category, node);

            if (workflow != null && clazz.isAssignableFrom(workflow.getClass())) {
                return Optional.of((T)workflow);
            } else {
                log.info("Failed to obtain workflow of desired class {}", clazz.getName());
            }
        } catch (RepositoryException e) {
            if (log.isDebugEnabled()) {
                log.warn("Problem retrieving workflow for category '{}'", category, e);
            } else {
                log.warn("Problem retrieving workflow for category '{}': {}", category, e.getMessage());
            }
        }
        return Optional.empty();
    }

    /**
     * Retrieve the node representing a specific document variant.
     *
     * @param node    JCR node representing either a variant or the handle node
     * @param variant Indication which variant to retrieve
     * @return        The requested variant node, or null, wrapped in an Optional
     */
    public static Optional<Node> getDocumentVariantNode(Node node, final Variant variant) {
        try {
            if (!node.isNodeType(HippoNodeType.NT_HANDLE)) {
                node = node.getParent();
            }
            if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                for (Node child : new NodeIterable(node.getNodes(node.getName()))) {
                    String state = JcrUtils.getStringProperty(child, HippoStdNodeType.HIPPOSTD_STATE, null);
                    if (variant.getState().equals(state)) {
                        return Optional.of(child);
                    }
                }
            }
        } catch (RepositoryException e) {
            if (log.isDebugEnabled()) {
                log.warn("Problem retrieving a variant node", e);
            } else {
                log.warn("Problem retrieving a variant node: {}", e.getMessage());
            }
        }
        return Optional.empty();
    }

    public enum Variant {
        PUBLISHED(HippoStdNodeType.PUBLISHED),
        UNPUBLISHED(HippoStdNodeType.UNPUBLISHED),
        DRAFT(HippoStdNodeType.DRAFT);

        private final String state;

        Variant(final String state) {
            this.state = state;
        }

        private String getState() {
            return state;
        }
    }
}
