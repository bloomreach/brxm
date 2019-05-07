/*
 * Copyright 2017-2019 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms.channelmanager.content.documenttype.field.type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.document.util.FieldPath;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.documenttype.field.validation.CompoundContext;
import org.onehippo.cms.channelmanager.content.error.BadRequestException;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A field that stores its value(s) in a node.
 */
public interface NodeFieldType extends BaseFieldType {

    Logger log = LoggerFactory.getLogger(NodeFieldType.class);

    @Override
    default Optional<List<FieldValue>> readFrom(Node node) {
        List<FieldValue> values = readValues(node);

        FieldTypeUtils.trimToMaxValues(values, getMaxValues());

        if (values.size() < getMinValues()) {
            log.error("No values available for node of type '{}' of document at {}. This document type cannot be " +
                    "used to create new documents in the Channel Manager.", getId(), JcrUtils.getNodePathQuietly(node));
        }

        return values.isEmpty() ? Optional.empty() : Optional.of(values);
    }

    default List<FieldValue> readValues(final Node node) {
        final String nodeName = getId();

        try {
            final NodeIterator children = node.getNodes(nodeName);
            final List<FieldValue> values = new ArrayList<>((int) children.getSize());
            for (final Node child : new NodeIterable(children)) {
                final FieldValue value = readValue(child);
                // Note: we add the valueMap to the values even if it is empty, because we need to
                // maintain the 1-to-1 mapping between exposed values and internal nodes.
                values.add(value);
            }
            return values;
        } catch (final RepositoryException e) {
            log.warn("Failed to read nodes for {} type '{}'", getType(), getId(), e);
        }
        return Collections.emptyList();
    }

    /**
     * Reads a single field value from a node.
     * @param node the node to read from
     * @return the value read
     */
    FieldValue readValue(final Node node);

    default void writeValues(final Node node, final Optional<List<FieldValue>> optionalValues, final boolean checkCardinality) {
        final String valueName = getId();
        final List<FieldValue> values = optionalValues.orElse(Collections.emptyList());

        if (checkCardinality) {
            FieldTypeUtils.checkCardinality(this, values);
        }

        try {
            final NodeIterator children = node.getNodes(valueName);
            final long count = children.getSize();

            // additional cardinality check to prevent creating new values or remove a subset of the old values
            if (!values.isEmpty() && values.size() != count && count <= getMaxValues()) {
                throw new BadRequestException(new ErrorInfo(ErrorInfo.Reason.CARDINALITY_CHANGE));
            }

            for (final FieldValue value : values) {
                final Node child = children.nextNode();
                writeValue(child, value);
            }

            // delete excess nodes to match field type
            while (children.hasNext()) {
                final Node child = children.nextNode();
                child.remove();
            }
        } catch (final RepositoryException e) {
            log.warn("Failed to write {} field '{}'", getType(), valueName, e);
            throw new InternalServerErrorException();
        }
    }

    /**
     * Writes a single field value to a node.
     * @param node the node to write to
     * @param fieldValue the value to write
     * @throws ErrorWithPayloadException when the field value is wrong
     * @throws RepositoryException when the write failed
     */
    void writeValue(final Node node, final FieldValue fieldValue) throws ErrorWithPayloadException, RepositoryException;

    /**
     * Writes a field value to the field specified by the field path. Can be this field or a child field in case of
     * compound or compound-like fields.
     *
     * @param fieldPath the path to the field
     * @param values the values to write
     * @param context context of the field
     * @return true if the value has been written, false otherwise.
     * @throws ErrorWithPayloadException when the field path or field value is wrong
     * @throws RepositoryException when the write failed
     */
    default boolean writeFieldValue(final FieldPath fieldPath,
                                    final List<FieldValue> values,
                                    final CompoundContext context) throws ErrorWithPayloadException, RepositoryException {
        return FieldTypeUtils.writeChoiceFieldValue(fieldPath, values, this, context);
    }

    default int validate(final List<FieldValue> values, final CompoundContext context) {
        final String valueName = getId();

        try {
            final NodeIterator children = context.getNode().getNodes(valueName);

            final long count = children.getSize();
            if (values.size() != count) {
                throw new BadRequestException(new ErrorInfo(ErrorInfo.Reason.INVALID_DATA));
            }

            int violationCount = 0;

            for (final FieldValue value : values) {
                final Node child = children.nextNode();
                final CompoundContext childContext = context.getChildContext(child);
                violationCount += validateValue(value, childContext);
            }

            return violationCount;
        } catch (final RepositoryException e) {
            log.warn("Failed to validate {} field '{}'", getType(), valueName, e);
            throw new InternalServerErrorException();
        }
    }

    /**
     * Validators for node fields always get the node as the value to validate.
     */
    default Object getValidatedValue(final FieldValue value, final CompoundContext context) {
        return context.getNode();
    }
}
