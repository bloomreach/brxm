/*
 *  Copyright 2013-2017 Hippo B.V. (http://www.onehippo.com)
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoQuery;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WorkflowUtils {
    private static final Logger log = LoggerFactory.getLogger(WorkflowUtils.class);

    public static final int MAX_REFERENCE_COUNT = 100;
    private static final int QUERY_LIMIT = 1000;

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

            if (workflow == null) {
                log.info("Failed to find a workflow for category '{}' and node '{}'", category, node.getPath());
            } else if (clazz.isAssignableFrom(workflow.getClass())) {
                return Optional.of((T)workflow);
            } else {
                log.info("Failed to obtain workflow of desired class '{}' for category '{}' and node '{}'.",
                        clazz.getName(), category, node.getPath());
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

    /**
     * For a document, get a list of nodes and id's of documents that reference it.
     *
     * @param handle handle node of the document
     * @param retrieveUnpublished if true unpublished variants are retrieved otherwise live variants
     * @return a map of nodes that refer to the document. The entry key contains the id of the node.
     * @throws RepositoryException a generic error while accessing the repository
     */
    public static Map<String, Node> getReferringDocuments(final Node handle, final boolean retrieveUnpublished)
            throws RepositoryException {

        // Please note: The unit test for this method is located in the 'test' module.

        final Map<String, Node> referrers = new HashMap<>();
        final String requiredAvailability;
        if (retrieveUnpublished) {
            requiredAvailability = "preview";
        } else {
            requiredAvailability = "live";
        }
        final String handleId = handle.getIdentifier();

        StringBuilder query = new StringBuilder("//element(*,hippo:facetselect)[@hippo:docbase='")
                .append(handleId).append("']");
        addReferrers(handle, requiredAvailability, MAX_REFERENCE_COUNT, query.toString(), referrers);

        query = new StringBuilder("//element(*,hippo:mirror)[@hippo:docbase='").append(handleId).append("']");
        addReferrers(handle, requiredAvailability, MAX_REFERENCE_COUNT, query.toString(), referrers);

        return referrers;
    }

    private static void addReferrers(final Node handle, final String requiredAvailability, final int resultMaxCount,
                             final String queryStatement, final Map<String, Node> referrers) throws RepositoryException {
        final QueryManager queryManager = handle.getSession().getWorkspace().getQueryManager();
        @SuppressWarnings("deprecation")
        final HippoQuery query = (HippoQuery) queryManager.createQuery(queryStatement, Query.XPATH);
        query.setLimit(QUERY_LIMIT);
        final QueryResult result = query.execute();
        final Node root = handle.getSession().getRootNode();

        for (Node hit : new NodeIterable(result.getNodes())) {
            if (referrers.size() >= resultMaxCount) {
                break;
            }
            Node current = hit;
            while (!current.isSame(root)) {
                Node parent = current.getParent();
                if (parent.isNodeType(HippoNodeType.NT_HANDLE) && current.isNodeType(HippoNodeType.NT_DOCUMENT) &&
                        hasAvailability(current, requiredAvailability)) {
                    referrers.put(parent.getIdentifier(), parent);
                    break;
                }
                current = current.getParent();
            }
        }
    }

    /**
     * Check if a variant node has a given availability (typically: live, preview or draft)
     *
     * @param variant document node containing workflow properties
     * @param availability availability value
     * @return true if the availablitly property exists and contains the availability value
     * @throws RepositoryException a generic error while accessing the repository
     */
    public static boolean hasAvailability(final Node variant, final String availability) throws RepositoryException {
        final String[] availabilityValues = JcrUtils.getMultipleStringProperty(variant, HippoNodeType.HIPPO_AVAILABILITY, new String[0]);
        return Arrays.stream(availabilityValues).anyMatch(a -> a.equals(availability));
    }

    /**
     * For an unpublished document, get a list of nodeId's of unpublished documents that it references.
     *
     * @param handle the handle node of the document
     * @param session a JCR session
     * @return a map of nodes of unpublished document nodes that the document refers to. The entry key contains the id of the node.
     * @throws RepositoryException a generic error while accessing the repository
     */
    public static Map<String, Node> getReferencesToUnpublishedDocuments(final Node handle, final Session session) throws RepositoryException {

        // Please note: The unit test for this method is located in the 'test' module.

        final Map<String, Node> entries = new HashMap<>();
        final Optional<Node> unpublishedVariant = getDocumentVariantNode(handle, Variant.UNPUBLISHED);
        if (unpublishedVariant.isPresent()) {
            unpublishedVariant.get().accept(new ItemVisitor() {

                public void visit(final Node node) throws RepositoryException {
                    if (!JcrUtils.isVirtual(node)) {
                        if (node.hasProperty(HippoNodeType.HIPPO_DOCBASE)) {
                            visit(node.getProperty(HippoNodeType.HIPPO_DOCBASE));
                        }
                        for (NodeIterator children = node.getNodes(); children.hasNext();) {
                            visit(children.nextNode());
                        }
                    }
                }

                public void visit(final Property docBaseProperty) throws RepositoryException {
                    if (docBaseProperty.getType() == PropertyType.STRING) {
                        final String uuid = docBaseProperty.getString();
                        try {
                            final Node referencedNode = session.getNodeByIdentifier(uuid);
                            if (referencedNode.isNodeType(HippoNodeType.NT_HANDLE)) {
                                for (Node document : new NodeIterable(referencedNode.getNodes(referencedNode.getName()))) {
                                    if (hasAvailability(document, "live")) {
                                        return;
                                    }
                                }
                                entries.put(uuid, referencedNode);
                            }
                        } catch (ItemNotFoundException e) {
                            log.debug("Reference to UUID " + uuid + " could not be dereferenced.");
                        }
                    }
                }

            });
        }

        return entries;
    }

    public enum Variant {
        PUBLISHED(HippoStdNodeType.PUBLISHED),
        UNPUBLISHED(HippoStdNodeType.UNPUBLISHED),
        DRAFT(HippoStdNodeType.DRAFT);

        private final String state;

        Variant(final String state) {
            this.state = state;
        }

        public String getState() {
            return state;
        }
    }
}
