/*
 * Copyright 2021-2022 Bloomreach
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

import java.util.List;
import java.util.Objects;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms.channelmanager.content.document.util.FieldPath;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.ChoiceFieldType;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.CompoundFieldType;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.FieldType;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.onehippo.cms.channelmanager.content.error.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.cms.channelmanager.content.document.util.FieldPath.SEPARATOR;
import static org.onehippo.cms.channelmanager.content.document.util.PrototypeUtils.findFirstPrototypeNode;
import static org.onehippo.cms.channelmanager.content.documenttype.field.type.FieldType.Type.CHOICE;
import static org.onehippo.cms.channelmanager.content.documenttype.field.type.FieldType.Type.COMPOUND;
import static org.onehippo.cms.channelmanager.content.error.ErrorInfo.Reason.DOES_NOT_EXIST;
import static org.onehippo.cms.channelmanager.content.error.ErrorInfo.Reason.SERVER_ERROR;

public class NodeFieldServiceImpl implements NodeFieldService {
    private static final Logger log = LoggerFactory.getLogger(NodeFieldServiceImpl.class);
    public static final String SYSTEM_PREFIX = "system";

    private final Session session;

    public NodeFieldServiceImpl(final Session session) {
        this.session = session;
    }

    @Override
    public void addNodeField(final String documentPath, final FieldPath fieldPath, final List<FieldType> fields,
                             final String type) {

        final FieldType fieldType = getFieldType(fieldPath, fields);
        if (fieldType.getType() == COMPOUND && !fieldType.getJcrType().equals(type)) {
            log.error("The compound field '{}' does not support subfields of type '{}'", fieldPath, type);
            throw new InternalServerErrorException(new ErrorInfo(SERVER_ERROR, "type", "not-supported"));
        }

        if (fieldType.getType() == CHOICE && !((ChoiceFieldType)fieldType).getChoices().containsKey(type)) {
            log.error("The choice field '{}' does not support subfields of type '{}'", fieldPath, type);
            throw new InternalServerErrorException(new ErrorInfo(SERVER_ERROR, "type", "not-supported"));
        }

        final String nodeFieldPath = documentPath + SEPARATOR + fieldPath;
        try {
            final String parentPath = StringUtils.substringBeforeLast(nodeFieldPath, SEPARATOR);
            final Node parent = session.getNode(parentPath);
            final long numberOfFields = parent.getNodes(fieldPath.getLastSegmentName()).getSize();

            if (numberOfFields == fieldType.getMaxValues()) {
                log.error("Cannot add field '{}', the maximum amount of fields allowed is {}", fieldPath,
                        fieldType.getMaxValues());
                throw new InternalServerErrorException(new ErrorInfo(SERVER_ERROR, "cardinality", "max-values"));
            }
        } catch (final RepositoryException e) {
            log.error("An error occurred while checking the cardinality of field '{}'", fieldPath, e);
            throw new InternalServerErrorException(new ErrorInfo(SERVER_ERROR));
        }

        final String prototypePath = getPrototypePath(type);
        final String targetPath = documentPath + SEPARATOR + fieldPath;
        copyPrototype(prototypePath, targetPath);
    }

    @Override
    public void reorderNodeField(final String documentPath, final FieldPath fieldPath, final int position) {
        final String nodeFieldPath = documentPath + SEPARATOR + fieldPath;
        try {
            final Node fieldNode = session.getNode(nodeFieldPath);
            final Node parentNode = fieldNode.getParent();
            final String fieldName = fieldPath.getLastSegmentName();
            final long numberOfFields = parentNode.getNodes(fieldName).getSize();

            if (position > numberOfFields) {
                log.error("Failed to re-order field '{}', new position '{}' is out of bounds", fieldPath, position);
                throw new InternalServerErrorException(new ErrorInfo(SERVER_ERROR, "order", "out-of-bounds"));
            }

            final String relativeFieldPath = fieldPath.getLastSegment();
            if (numberOfFields == position) {
                // move to last position
                parentNode.orderBefore(relativeFieldPath, null);
            } else if (fieldNode.getIndex() > position) {
                // move field up
                parentNode.orderBefore(relativeFieldPath, fieldName + "[" + position + "]");
            } else {
                // move field down
                parentNode.orderBefore(relativeFieldPath, fieldName + "[" + (position + 1) + "]");
            }

        } catch (final RepositoryException e) {
            log.error("Failed to re-order field '{}' to position '{}'", nodeFieldPath, position, e);
            throw new InternalServerErrorException(new ErrorInfo(SERVER_ERROR));
        }
    }

    @Override
    public void removeNodeField(final String documentPath, final FieldPath fieldPath,
                                final List<FieldType> fields) {

        getFieldType(fieldPath, fields);

        final String nodeFieldPath = documentPath + SEPARATOR + fieldPath;
        try {
            session.removeItem(nodeFieldPath);
        } catch (final PathNotFoundException e) {
            throw new NotFoundException(new ErrorInfo(DOES_NOT_EXIST, "field", fieldPath.toString()));
        } catch (final RepositoryException e) {
            log.error("Failed to remove field '{}' from document '{}'", fieldPath, nodeFieldPath, e);
            throw new InternalServerErrorException(new ErrorInfo(SERVER_ERROR));
        }
    }

    private String getPrototypePath(final String jcrType) {
        final Node prototypeNode = findFirstPrototypeNode(session, jcrType);
        if (prototypeNode == null) {
            log.warn("Failed to find prototype node for field of type '{}'", jcrType);
            throw new NotFoundException(new ErrorInfo(DOES_NOT_EXIST, "prototype", jcrType));
        }

        try {
            return prototypeNode.getPath();
        } catch (final RepositoryException e) {
            log.error("An error occurred while retrieving the prototype path for type '{}'", jcrType, e);
            throw new InternalServerErrorException(new ErrorInfo(SERVER_ERROR));
        }
    }

    private void copyPrototype(final String prototypePath, final String targetPath) {
        try {
            final Node fieldNode = JcrUtils.copy(session, prototypePath, stripSuffix(targetPath));
            final String srcPath = fieldNode.getName() + "[" + fieldNode.getIndex() + "]";
            final String destPath = StringUtils.substringAfterLast(targetPath, SEPARATOR);
            fieldNode.getParent().orderBefore(srcPath, destPath);
        } catch (final RepositoryException e) {
            log.error("Failed to copy prototype '{}' to document '{}'", prototypePath, targetPath, e);
            throw new InternalServerErrorException(new ErrorInfo(SERVER_ERROR));
        }
    }

    static FieldType getFieldType(final FieldPath fieldPath, final List<FieldType> fields) {
        final FieldType fieldType = findFieldType(fieldPath, fields);
        if (fieldType == null) {
            log.warn("Failed to find field with path '{}'", fieldPath);
            throw new NotFoundException(new ErrorInfo(DOES_NOT_EXIST, "fieldType", fieldPath.toString()));
        }

        if (fieldType.getMaxValues() > 1 && !fieldType.isMultiple()) {
            log.error("The field '{}' does not support multiple values but expects a maximum of {} values", fieldPath,
                    fieldType.getMaxValues());
            throw new InternalServerErrorException(new ErrorInfo(SERVER_ERROR, "fieldType", "not-multiple"));
        }

        return fieldType;
    }

    private static FieldType findFieldType(final FieldPath fieldPath, final List<FieldType> fields) {
        final FieldType fieldType = fields.stream()
                .filter(field -> fieldPath.startsWith(field.getId()))
                .findFirst()
                .orElse(null);

        final FieldPath remaining = fieldPath.getRemainingSegments();
        if (remaining.isEmpty()) {
            return fieldType;
        }

        if (fieldType instanceof CompoundFieldType) {
            return findFieldType(remaining, ((CompoundFieldType) fieldType).getFields());
        }

        if (fieldType instanceof ChoiceFieldType) {
            // We arrive here if we are traversing into a nested choice-field. This means that *if* we want to add
            // a child node, the chosen field has to be a compound.
            // Since we don't know which choice-field to traverse into, we try them all and return the first that
            // returns a hit.
            final ChoiceFieldType choiceFieldType = (ChoiceFieldType) fieldType;
            return choiceFieldType.getChoices().values().stream()
                    .filter(type -> type.getType().equals(COMPOUND))
                    .map(type -> findFieldType(remaining, ((CompoundFieldType)type).getFields()))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
        }

        return null;
    }

    private static String stripSuffix(final String path) {
        if (path.endsWith("]")) {
            return StringUtils.substringBeforeLast(path, "[");
        }
        return path;
    }

}
