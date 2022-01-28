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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms.channelmanager.content.document.util.FieldPath;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.ChoiceFieldType;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.FieldType;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.cms.channelmanager.content.document.util.FieldPath.SEPARATOR;
import static org.onehippo.cms.channelmanager.content.documenttype.field.type.FieldType.Type.CHOICE;
import static org.onehippo.cms.channelmanager.content.documenttype.field.type.FieldType.Type.COMPOUND;
import static org.onehippo.cms.channelmanager.content.error.ErrorInfo.Reason.SERVER_ERROR;

public class NodeFieldAddition {

    private static final Logger log = LoggerFactory.getLogger(NodeFieldAddition.class);
    private final String documentPath;
    private final FieldPath fieldPath;
    private final List<FieldType> fields;
    private final String type;
    private final Session session;

    public NodeFieldAddition(final String documentPath, final FieldPath fieldPath, final List<FieldType> fields,
                             final String type, final Session session) {
        this.documentPath = documentPath;
        this.fieldPath = fieldPath;
        this.fields = fields;
        this.type = type;
        this.session = session;
    }

    private static String stripSuffix(final String path) {
        if (path.endsWith("]")) {
            return StringUtils.substringBeforeLast(path, "[");
        }
        return path;
    }

    @Override
    public String toString() {
        return "NodeFieldAddition{" + "documentPath='" + documentPath + '\'' + ", fieldPath=" + fieldPath
                + ", fields=" + fields + ", type='" + type + '\''
                + ", userId=" + session.getUserID() + '}';
    }

    public void add() {
        final FieldType fieldType = NodeFieldServiceImpl.getFieldType(fieldPath, fields);
        if (fieldType.getType() == COMPOUND && !fieldType.getJcrType().equals(type)) {
            log.error("The compound field '{}' does not support subfields of type '{}'", fieldPath, type);
            throw new InternalServerErrorException(new ErrorInfo(SERVER_ERROR, "type", "not-supported"));
        }

        if (fieldType.getType() == CHOICE && !((ChoiceFieldType) fieldType).getChoices().containsKey(type)) {
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

        final String prototypePath = "/"  + String.join("/", HippoNodeType.NAMESPACES_PATH, getTypePrefix(type), getTypeName(type));
        final String targetPath = documentPath + SEPARATOR + fieldPath;
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

    public static String getTypePrefix(final String type) {
        return org.apache.commons.lang3.StringUtils.contains(type, ":") ? org.apache.commons.lang3.StringUtils.substringBefore(type, ":") : "system";
    }

    public static String getTypeName(String type) {
        return org.apache.commons.lang3.StringUtils.defaultIfEmpty(
                org.apache.commons.lang3.StringUtils.substringAfter(type, ":"), type);
    }
}
