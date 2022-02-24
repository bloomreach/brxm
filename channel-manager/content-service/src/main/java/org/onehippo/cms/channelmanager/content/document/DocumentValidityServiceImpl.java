/*
 * Copyright 2022 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms.channelmanager.content.document;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.FieldType;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.NodeFieldType;
import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;
import org.onehippo.cms.channelmanager.content.documenttype.util.NodeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.util.JcrUtils.getNodePathQuietly;
import static org.onehippo.cms.channelmanager.content.document.util.FieldPath.SEPARATOR;
import static org.onehippo.cms.channelmanager.content.document.util.PrototypeUtils.findFirstPrototypeNode;

public class DocumentValidityServiceImpl implements DocumentValidityService {

    private static final Logger log = LoggerFactory.getLogger(DocumentValidityServiceImpl.class);

    @Override
    public void handleDocumentTypeChanges(final Session workflowSession, final DocumentType documentType,
                                          final List<Node> variants) throws RepositoryException {
        if (variants.isEmpty()) {
            return;
        }

        ContentTypeValidator.processChanges(workflowSession, documentType, variants);

        if (workflowSession.hasPendingChanges()) {
            workflowSession.save();
        }
    }

    private static class ContentTypeValidator {

        static void processChanges(final Session workflowSession, final DocumentType documentType,
                                   final List<Node> variants) {
            new ContentTypeValidator(workflowSession).processChanges(documentType, variants);
        }

        private final Session workflowSession;

        private ContentTypeValidator(final Session workflowSession) {
            this.workflowSession = workflowSession;
        }

        private void processChanges(final DocumentType documentType, final List<Node> variants) {
            // The document prototype contains the prototype nodes of the fields that where created with the doc-type editor
            final Node prototype = findFirstPrototypeNode(workflowSession, documentType.getId());
            if (prototype == null) {
                log.warn("Unable to find prototype node of type '{}', skipping handling of type changes", documentType.getId());
                return;
            }

            for (final FieldType field : documentType.getFields()) {
                if (field instanceof NodeFieldType) {
                    checkFieldChanges(variants, prototype, field);
                }
            }
        }

        private void checkFieldChanges(final List<Node> variants, final Node documentPrototype, final FieldType field) {

            final Node variant = variants.get(0);
            try {
                long numberOfMissingNodes = getNumberOfMissingNodes(variant, field);
                if (numberOfMissingNodes == 0) {
                    return;
                }

                if (log.isDebugEnabled()) {
                    log.debug("Found {} missing node(s) for field '{}' of type '{}' in document node {}",
                            numberOfMissingNodes, field.getId(), field.getType(), getNodePathQuietly(variant));
                }

                final List<Node> missingPrototypeNodes = findMissingPrototypeNodes(documentPrototype, field);
                if (missingPrototypeNodes.isEmpty()) {
                    log.error("Failed to find prototype nodes for field '{}' in document '{}' which is missing {} nodes",
                            field.getId(), getNodePathQuietly(variant), numberOfMissingNodes);
                    return;
                }

                copyMissingPrototypeNodes(field, numberOfMissingNodes, missingPrototypeNodes, variants);
            } catch (RepositoryException e) {
                log.warn("An error occurred while checking the cardinality of field '{}': {}", field.getId(), e.getMessage());
            }
        }

        private List<Node> findMissingPrototypeNodes(final Node documentPrototype, final FieldType field) throws RepositoryException {
            List<Node> prototypeNodes = Collections.emptyList();

            // check if document prototype has nodes
            if (documentPrototype != null) {
                prototypeNodes = NodeUtils.getNodes(documentPrototype, field.getId()).collect(Collectors.toList());
            }

            // otherwise, do a ContentTypeHandle lookup by JCR type
            if (prototypeNodes.isEmpty()) {
                final Node fieldPrototype = findFirstPrototypeNode(workflowSession, field.getJcrType());
                if (fieldPrototype != null) {
                    log.debug("Will use the prototype at {}", getNodePathQuietly(fieldPrototype));
                    prototypeNodes = Collections.singletonList(fieldPrototype);
                }
            }

            return prototypeNodes;
        }

        private void copyMissingPrototypeNodes(final FieldType field, long numberOfMissingNodes,
                                               final List<Node> prototypeNodes, final List<Node> variantNodes) throws RepositoryException {

            Iterator<Node> prototypes = prototypeNodes.listIterator();
            while (numberOfMissingNodes > 0 && prototypes.hasNext()) {

                final Node prototypeNode = prototypes.next();
                final String prototypePath = prototypeNode.getPath();

                for (final Node variant : variantNodes) {
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

        private long getNumberOfMissingNodes(final Node node, final FieldType field) throws RepositoryException {
            final long numberOfNodes = NodeUtils.getNodes(node, field.getId()).count();
            return Math.max(0, field.getMinValues() - numberOfNodes);
        }
    }
}
