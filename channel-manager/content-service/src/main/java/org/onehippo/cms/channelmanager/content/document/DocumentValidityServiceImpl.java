/*
 * Copyright 2021 Bloomreach (http://www.bloomreach.com)
 */

package org.onehippo.cms.channelmanager.content.document;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.WorkflowUtils;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.FieldType;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.NodeFieldType;
import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;
import org.onehippo.cms.channelmanager.content.documenttype.util.NodeUtils;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.project.Project;
import org.onehippo.cms7.services.project.ProjectService;
import org.onehippo.repository.branch.BranchConstants;
import org.onehippo.repository.branch.BranchHandle;
import org.onehippo.repository.contenttypeworkflow.ContentTypeHandle;
import org.onehippo.repository.documentworkflow.BranchHandleImpl;

import lombok.extern.slf4j.Slf4j;
import static org.hippoecm.repository.util.JcrUtils.getNodePathQuietly;
import static org.onehippo.cms.channelmanager.content.document.util.FieldPath.SEPARATOR;

@Slf4j
public class DocumentValidityServiceImpl implements DocumentValidityService {

    private final Session session;

    public DocumentValidityServiceImpl(final Session session) {
        this.session = session;
    }

    @Override
    public void handleDocumentTypeChanges(final String branchId, final Node documentHandle, final DocumentType documentType) {
        final BranchHandle branchHandle;
        try {
            branchHandle = new BranchHandleImpl(branchId, documentHandle);
        } catch (WorkflowException e) {
            log.error("Could not get variant info for node node : { path : {} }", getNodePathQuietly(documentHandle), e);
            throw new InternalServerErrorException(new ErrorInfo(ErrorInfo.Reason.SERVER_ERROR, "error", e.getMessage()));
        }

        final Node draft = branchHandle.getDraft();
        final Node unpublished = branchHandle.getUnpublished();

        final Node prototype = findPrototype(documentType.getId(), branchId, session);
        if (prototype == null) {
            log.warn("Unable to find prototype '{}' for branch '{}', skipping handling of document type changes",
                    documentType.getId(), branchId);
            return;
        }

        boolean saveSession = false;

        for (final FieldType field : documentType.getFields()) {
            if (!(field instanceof NodeFieldType)) {
                continue;
            }

            final String nodeName = field.getId();
            try {
                final long numberOfNodes = NodeUtils.getNodes(draft, nodeName).count();
                long numberOfMissingNodes = field.getMinValues() - numberOfNodes;
                if (numberOfMissingNodes <= 0) {
                    continue;
                }

                if (log.isDebugEnabled()) {
                    log.debug("Found {} missing node(s) for field '{}' of type '{}' in document node {}",
                            numberOfMissingNodes, nodeName, field.getType(), getNodePathQuietly(draft));
                }

                final List<Node> missingPrototypeNodes = findMissingPrototypeNodes(branchId, prototype, field, nodeName);
                if (missingPrototypeNodes.isEmpty()) {
                    final String message = String.format("Failed to find prototype nodes for field '%s' in document '%s' which is missing %d nodes",
                            nodeName, getNodePathQuietly(draft), numberOfMissingNodes);
                    log.warn(message);
                    throw new InternalServerErrorException(new ErrorInfo(ErrorInfo.Reason.INVALID_DATA,
                            "reason", message));
                }

                addMissingPrototypeNodes(numberOfMissingNodes, nodeName, missingPrototypeNodes, draft, unpublished);
                saveSession = true;
            } catch (RepositoryException e) {
                log.warn("An error occurred while checking the cardinality of field '{}': {}", field.getId(),  e.getMessage());
            }
        }

        if (saveSession) {
            try {
                session.save();
            } catch (RepositoryException e) {
                log.error("Failed to save changes to draft node of document {}", getNodePathQuietly(documentHandle), e);
                throw new InternalServerErrorException(new ErrorInfo(ErrorInfo.Reason.SERVER_ERROR));
            }
        }
    }

    private List<Node> findMissingPrototypeNodes(final String branchId, final Node documentPrototype, final FieldType field, final String nodeName) throws RepositoryException {
        List<Node> prototypeNodes = Collections.emptyList();

        // check if document prototype has nodes
        if (documentPrototype != null) {
            prototypeNodes = NodeUtils.getNodes(documentPrototype, nodeName).collect(Collectors.toList());
        }

        if (prototypeNodes.isEmpty()) {
            final Node fieldPrototype = findPrototype(field.getJcrType(), branchId, session);
            if (fieldPrototype != null) {
                log.debug("Will use the prototype at {}", JcrUtils.getNodeParentQuietly(fieldPrototype));
                prototypeNodes = Collections.singletonList(fieldPrototype);
            }
        }

        return prototypeNodes;
    }

    private void addMissingPrototypeNodes(long numberOfMissingNodes, final String nodeName, final List<Node> prototypeNodes, final Node... nodes) throws RepositoryException {
        int prototypeIndex = 0;
        int numberOfPrototypes = prototypeNodes.size();
        while (numberOfMissingNodes > 0) {
            final Node prototypeNode = prototypeNodes.get(prototypeIndex++);
            final String prototypePath = prototypeNode.getPath();


            for (final Node node: nodes) {
                if (node != null) {
                    final String targetPath = node.getPath() + SEPARATOR + nodeName;
                    final Node fieldNode = JcrUtils.copy(session, prototypePath, targetPath);
                    final String srcPath = fieldNode.getName() + "[" + fieldNode.getIndex() + "]";
                    final String destPath = StringUtils.substringAfterLast(targetPath, SEPARATOR);
                    final Node parent = fieldNode.getParent();
                    parent.orderBefore(srcPath, destPath);
                }
            }

            if (prototypeIndex >= numberOfPrototypes) {
                prototypeIndex = 0;
            }
            numberOfMissingNodes--;
        }
    }

    private Node findPrototype(final String type, final String branchId, final Session session) {
        ContentTypeHandle contentTypeHandle = null;
        try {
            contentTypeHandle = ContentTypeHandle.createContentTypeHandle(type, session).orElse(null);
        } catch (final RepositoryException | WorkflowException e) {
            log.error("An error occurred while looking up ContentTypeHandle for type '{}'", type, e);
        }

        if (contentTypeHandle == null) {
            return null;
        }

        try {
            return isDeveloperProjectWithDocumentTypes(branchId)
                    ? contentTypeHandle.getVariantPrototypeNode(WorkflowUtils.Variant.UNPUBLISHED)
                        .orElse(contentTypeHandle.getVariantPrototypeNode(WorkflowUtils.Variant.PUBLISHED)
                            .orElse(null))
                    : contentTypeHandle.getVariantPrototypeNode(WorkflowUtils.Variant.PUBLISHED)
                        .orElse(null);
        } catch (final RepositoryException e) {
            log.error("An error occurred while looking up the prototype node for type '{}'", type, e);
            return null;
        }
    }

    private boolean isDeveloperProjectWithDocumentTypes(final String branchId) {
        if (branchId.equals(BranchConstants.MASTER_BRANCH_ID)) {
            return false;
        }

        final ProjectService projectService = HippoServiceRegistry.getService(ProjectService.class);
        if (projectService == null) {
            log.warn("Unable to get 'ProjectService' from service registry while checking if project '{}' is a " +
                    "developer project with document types.", branchId);
            return false;
        }

        return projectService.getProject(branchId)
                .filter(Project::isIncludeDocumentTypes)
                .isPresent();
    }
}
