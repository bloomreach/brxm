/*
 * Copyright 2021 Bloomreach (http://www.bloomreach.com)
 */

package org.onehippo.cms.channelmanager.content.document;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.WorkflowUtils;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.FieldType;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.NodeFieldType;
import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;
import org.onehippo.cms.channelmanager.content.documenttype.util.NodeUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.project.Project;
import org.onehippo.cms7.services.project.ProjectService;
import org.onehippo.repository.branch.BranchHandle;
import org.onehippo.repository.contenttypeworkflow.ContentTypeHandle;
import org.onehippo.repository.documentworkflow.BranchHandleImpl;

import com.google.common.collect.Lists;

import lombok.extern.slf4j.Slf4j;
import static org.hippoecm.repository.util.JcrUtils.getNodePathQuietly;
import static org.hippoecm.repository.util.WorkflowUtils.Variant.DRAFT;
import static org.hippoecm.repository.util.WorkflowUtils.Variant.PUBLISHED;
import static org.hippoecm.repository.util.WorkflowUtils.Variant.UNPUBLISHED;
import static org.onehippo.cms.channelmanager.content.document.util.FieldPath.SEPARATOR;

@Slf4j
public class DocumentValidityServiceImpl implements DocumentValidityService {

    @Override
    public void handleDocumentTypeChanges(final Session workflowSession, final String branchId,
                                          final Node documentHandle, final DocumentType documentType) {

        // The BranchHandle provides access to the document's variants for the specified branch
        final BranchHandle branchHandle;
        try {
            branchHandle = new BranchHandleImpl(branchId, documentHandle);
        } catch (WorkflowException e) {
            log.error("Could not load variants of document '{}'", getNodePathQuietly(documentHandle), e);
            return;
        }

        if (branchHandle.getDraft() == null) {
            log.error("Could not find '{}' variant for document {}", DRAFT, getNodePathQuietly(documentHandle));
            return;
        }

        // The document prototype contains the prototype nodes of the fields that where created with the doc-type editor
        final Node prototype = findPrototypeNode(workflowSession, branchHandle, documentType.getId());
        if (prototype == null) {
            log.warn("Unable to find prototype '{}' for branch '{}', skipping handling of document type changes",
                    documentType.getId(), branchId);
            return;
        }

        for (final FieldType field : documentType.getFields()) {
            if (field instanceof NodeFieldType) {
                checkFieldChanges(workflowSession, branchHandle, prototype, field);
            }
        }

        try {
            if (workflowSession.hasPendingChanges()) {
                workflowSession.save();
            }
        } catch (RepositoryException e) {
            log.error("Failed to save changes to draft node of document {}", getNodePathQuietly(documentHandle), e);
        }
    }

    private void checkFieldChanges(final Session workflowSession, final BranchHandle branchHandle,
                                   final Node documentPrototype, final FieldType field) {

        final Node draft = branchHandle.getDraft();
        try {
            long numberOfMissingNodes = getNumberOfMissingNodes(draft, field);
            if (numberOfMissingNodes == 0) {
                return;
            }

            if (log.isDebugEnabled()) {
                log.debug("Found {} missing node(s) for field '{}' of type '{}' in document node {}",
                        numberOfMissingNodes, field.getId(), field.getType(), getNodePathQuietly(draft));
            }

            final List<Node> missingPrototypeNodes = findMissingPrototypeNodes(workflowSession, branchHandle, documentPrototype, field);
            if (missingPrototypeNodes.isEmpty()) {
                log.error("Failed to find prototype nodes for field '{}' in document '{}' which is missing {} nodes",
                        field.getId(), getNodePathQuietly(draft), numberOfMissingNodes);
                return;
            }

            final List<Node> variants = Lists.newArrayList(branchHandle.getDraft());
            if (branchHandle.getUnpublished() != null) {
                variants.add(branchHandle.getUnpublished());
            }
            copyMissingPrototypeNodes(workflowSession, field, numberOfMissingNodes, missingPrototypeNodes, variants);
        } catch (RepositoryException e) {
            log.warn("An error occurred while checking the cardinality of field '{}': {}", field.getId(),  e.getMessage());
        }
    }

    private List<Node> findMissingPrototypeNodes(final Session workflowSession, final BranchHandle branchHandle,
                                                 final Node documentPrototype, final FieldType field) throws RepositoryException {
        List<Node> prototypeNodes = Collections.emptyList();

        // check if document prototype has nodes
        if (documentPrototype != null) {
            prototypeNodes = NodeUtils.getNodes(documentPrototype, field.getId()).collect(Collectors.toList());
        }

        // otherwise, do a ContentTypeHandle lookup by JCR type
        if (prototypeNodes.isEmpty()) {
            final Node fieldPrototype = findPrototypeNode(workflowSession, branchHandle, field.getJcrType());
            if (fieldPrototype != null) {
                log.debug("Will use the prototype at {}", JcrUtils.getNodeParentQuietly(fieldPrototype));
                prototypeNodes = Collections.singletonList(fieldPrototype);
            }
        }

        return prototypeNodes;
    }

    private Node findPrototypeNode(final Session session, final BranchHandle branchHandle, final String type) {
        ContentTypeHandle contentTypeHandle = null;
        try {
            contentTypeHandle = ContentTypeHandle.createContentTypeHandle(type, session).orElse(null);
        } catch (final RepositoryException | WorkflowException e) {
            log.error("An error occurred while looking up ContentTypeHandle for type '{}'", type, e);
        }

        if (contentTypeHandle == null) {
            return null;
        }

        final List<WorkflowUtils.Variant> candidates = isDeveloperProjectWithDocumentTypes(branchHandle)
            ? Lists.newArrayList(UNPUBLISHED, PUBLISHED)
            : Lists.newArrayList(PUBLISHED);

        try {
            for (WorkflowUtils.Variant candidate : candidates) {
                final Optional<Node> variantPrototypeNode = contentTypeHandle.getVariantPrototypeNode(candidate);
                if (variantPrototypeNode.isPresent()) {
                    return variantPrototypeNode.get();
                }
            }
        } catch (final RepositoryException e) {
            log.error("An error occurred while looking up the prototype node for type '{}'", type, e);
        }

        return null;
    }

    private void copyMissingPrototypeNodes(final Session workflowSession, final FieldType field,
                                           long numberOfMissingNodes, final List<Node> prototypeNodes,
                                           final List<Node> variantNodes) throws RepositoryException {

        Iterator<Node> prototypes = prototypeNodes.listIterator();
        while (numberOfMissingNodes > 0 && prototypes.hasNext()) {

            final Node prototypeNode = prototypes.next();
            final String prototypePath = prototypeNode.getPath();

            for (final Node variant: variantNodes) {
                // copy the prototype node to the field-path in the document
                final String targetPath = variant.getPath() + SEPARATOR + field.getId();
                JcrUtils.copy(workflowSession, prototypePath, targetPath);
            }

            if (!prototypes.hasNext()) {
                prototypes = prototypeNodes.listIterator();
            }
            numberOfMissingNodes--;
        }
    }

    private boolean isDeveloperProjectWithDocumentTypes(final BranchHandle branchHandle) {
        if (branchHandle.isMaster()) {
            return false;
        }

        final ProjectService projectService = HippoServiceRegistry.getService(ProjectService.class);
        if (projectService == null) {
            log.warn("Unable to get 'ProjectService' from service registry while checking if project '{}' is a " +
                    "developer project with document types.", branchHandle.getBranchId());
            return false;
        }

        return projectService.getProject(branchHandle.getBranchId())
                .filter(Project::isIncludeDocumentTypes)
                .isPresent();
    }

    private long getNumberOfMissingNodes(final Node node, final FieldType field) throws RepositoryException {
        final long numberOfNodes = NodeUtils.getNodes(node, field.getId()).count();
        return Math.max(0, field.getMinValues() - numberOfNodes);
    }
}
