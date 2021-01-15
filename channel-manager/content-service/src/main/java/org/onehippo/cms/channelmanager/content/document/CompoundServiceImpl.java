/*
 * Copyright 2021 Bloomreach
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

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms.channelmanager.content.document.util.FieldPath;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.CompoundFieldType;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.FieldType;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.onehippo.cms.channelmanager.content.error.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROTOTYPES;
import static org.hippoecm.repository.api.HippoNodeType.NAMESPACES_PATH;
import static org.onehippo.cms.channelmanager.content.document.util.FieldPath.SEPARATOR;
import static org.onehippo.cms.channelmanager.content.error.ErrorInfo.Reason.DOES_NOT_EXIST;
import static org.onehippo.cms.channelmanager.content.error.ErrorInfo.Reason.SERVER_ERROR;

public class CompoundServiceImpl implements CompoundService {
    private static final Logger log = LoggerFactory.getLogger(CompoundServiceImpl.class);
    public static final String SYSTEM_PREFIX = "system";

    private final Session session;

    public CompoundServiceImpl(final Session session) {
        this.session = session;
    }

    @Override
    public void addCompoundField(final String documentPath, final FieldPath fieldPath, final List<FieldType> fields) {
        final String compoundPath = documentPath + SEPARATOR + fieldPath;
        final String prototypePath = getPrototypePath(fieldPath, fields);
        try {
            addCompound(prototypePath, compoundPath);
            session.save();
        } catch (final RepositoryException e) {
            log.warn("Failed to copy prototype '{}' to document '{}'", prototypePath, compoundPath, e);
            throw new InternalServerErrorException(new ErrorInfo(SERVER_ERROR));
        }
    }

    private String getPrototypePath(final FieldPath fieldPath, final List<FieldType> fields) {
        final FieldType fieldType = findFieldType(fieldPath, fields);
        if (fieldType == null) {
            log.warn("Failed to find field with path '{}'", fieldPath);
            throw new NotFoundException(new ErrorInfo(DOES_NOT_EXIST, "fieldType", fieldPath.toString()));
        }

        final Node prototypeNode = findPrototype(fieldType.getJcrType());
        if (prototypeNode == null) {
            log.warn("Failed to find prototype node for field of type '{}'", fieldType.getJcrType());
            throw new NotFoundException(new ErrorInfo(DOES_NOT_EXIST, "prototype", fieldType.getJcrType()));
        }

        try {
            return prototypeNode.getPath();
        } catch (final RepositoryException e) {
            log.error("An error occurred while retrieving the prototype path for type '{}'", fieldType, e);
            throw new InternalServerErrorException(new ErrorInfo(SERVER_ERROR));
        }
    }

    private void addCompound(final String prototypePath, final String compoundPath) throws RepositoryException {
        final Node compound = JcrUtils.copy(session, prototypePath, stripSuffix(compoundPath));

        final String srcPath = compound.getName() + (compound.getIndex() > 1 ? "[" + compound.getIndex() + "]" : "");
        final String destPath = StringUtils.substringAfterLast(compoundPath, SEPARATOR);
        compound.getParent().orderBefore(srcPath, destPath);
    }

    private Node findPrototype(final String type) {
        try {
            final String path = getPrototypeLocation(type);
            if (!session.itemExists(path) || !session.getItem(path).isNode()) {
                return null;
            }
            final NodeIterator iter = ((Node) session.getItem(path)).getNodes(HippoNodeType.HIPPO_PROTOTYPE);

            while (iter.hasNext()) {
                final Node node = iter.nextNode();
                if (!node.isNodeType(JcrConstants.NT_UNSTRUCTURED)) {
                    return node;
                }
            }
        } catch (final RepositoryException e) {
            log.error("An error occurred while looking up the prototype node for type '{}'", type, e);
        }

        return null;
    }

    private String getPrototypeLocation(final String type) throws RepositoryException {
        String prefix = SYSTEM_PREFIX;
        String subType = type;

        final int separatorIndex = type.indexOf(':');
        if (separatorIndex > 0) {
            prefix = type.substring(0, separatorIndex);
            subType = type.substring(separatorIndex + 1);
        }

        final String uri = getNamespaceURI(prefix);
        final String nsVersion = "_" + uri.substring(uri.lastIndexOf('/') + 1);

        final int prefixLength = prefix.length();
        final int nsVersionLength = nsVersion.length();

        if (prefixLength > nsVersionLength && nsVersion.equals(prefix.substring(prefixLength - nsVersionLength))) {
            prefix = prefix.substring(0, prefixLength - nsVersionLength);
        }

        return String.format("/%s/%s/%s/%s", NAMESPACES_PATH, prefix, subType, HIPPO_PROTOTYPES);
    }

    private String getNamespaceURI(final String prefix) throws RepositoryException {
        if (SYSTEM_PREFIX.equals(prefix)) {
            return "internal";
        }

        final NamespaceRegistry nsReg = session.getWorkspace().getNamespaceRegistry();
        return nsReg.getURI(prefix);
}

    private FieldType findFieldType(final FieldPath fieldPath, final List<FieldType> fields) {
        final FieldType fieldType = fields.stream()
                .filter(field -> fieldPath.startsWith(field.getId()))
                .findFirst()
                .orElse(null);

        final FieldPath remaining = fieldPath.getRemainingSegments();
        if (remaining.isEmpty()) {
            return fieldType;
        }

        if (!(fieldType instanceof CompoundFieldType)) {
            return null;
        }

        return findFieldType(remaining, ((CompoundFieldType)fieldType).getFields());
    }

    private static String stripSuffix(final String path) {
        if (path.endsWith("]")) {
            return StringUtils.substringBeforeLast(path, "[");
        }
        return path;
    }

}
