/*
 * Copyright 2017-2020 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.document.util.FieldPath;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.documenttype.util.NodeUtils;
import org.onehippo.cms.channelmanager.content.documenttype.validation.CompoundContext;
import org.onehippo.cms.channelmanager.content.error.BadRequestException;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A field that stores its value(s) in a node.
 */
public abstract class NodeFieldType extends AbstractFieldType implements BaseFieldType {

    private static final Logger log = LoggerFactory.getLogger(NodeFieldType.class);

    @Override
    public final Optional<List<FieldValue>> readFrom(final Node node) {
        final List<FieldValue> values = readValues(node);

        FieldTypeUtils.trimToMaxValues(values, getMaxValues());

        if (values.size() < getMinValues()) {
            log.error("No values available for node of type '{}' of document at {}. This document type cannot be " +
                    "used to create new documents in the Channel Manager.", getId(), JcrUtils.getNodePathQuietly(node));
        }

        return values.isEmpty() ? Optional.empty() : Optional.of(values);
    }

    @Override
    public List<FieldValue> readValues(final Node node) {
        final String nodeName = getId();

        try {
            // Note: we add the valueMap to the values even if it is empty, because we need to
            // maintain the 1-to-1 mapping between exposed values and internal nodes.
            return NodeUtils.getNodes(node, nodeName)
                    .map(this::readValue)
                    .collect(Collectors.toList());
        } catch (final RepositoryException e) {
            log.warn("Failed to read nodes for {} type '{}'", getType(), getId(), e);
        }
        return Collections.emptyList();
    }

    /**
     * Reads a single field value from a node.
     *
     * @param node the node to read from
     * @return the value read
     */
    protected abstract FieldValue readValue(final Node node);

    @Override
    public void writeValues(final Node node, final Optional<List<FieldValue>> optionalValues) {
        final String valueName = getId();
        final List<FieldValue> values = optionalValues.orElse(Collections.emptyList());

        try {
            final NodeIterator children = node.getNodes(valueName);
            final Iterator<FieldValue> fieldValues = values.iterator();

            while (children.hasNext() && fieldValues.hasNext()) {
                writeValue(children.nextNode(), fieldValues.next());
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
     *
     * @param node       the node to write to
     * @param fieldValue the value to write
     * @throws ErrorWithPayloadException when the field value is wrong
     * @throws RepositoryException       when the write failed
     */
    public abstract void writeValue(final Node node, final FieldValue fieldValue) throws RepositoryException;

    /**
     * Writes a field value to the field specified by the field path. Can be this field or a child field in case of
     * compound or compound-like fields.
     * <p>
     * The default implementation writes this field as a choice field.
     *
     * @param fieldPath the path to the field
     * @param values    the values to write
     * @param context   context of the field
     * @return true if the value has been written, false otherwise.
     * @throws ErrorWithPayloadException when the field path or field value is wrong
     * @throws RepositoryException       when the write failed
     */
    public boolean writeFieldValue(final FieldPath fieldPath,
                                   final List<FieldValue> values,
                                   final CompoundContext context) throws RepositoryException {
        return FieldTypeUtils.writeChoiceFieldValue(fieldPath, values, this, context);
    }

    /**
     * <p>Validates the multiplicity and the individual values of a property.</p>
     * <p>Throws a {@link BadRequestException} with the following {@link ErrorInfo.Reason}'s:
     * <ul>
     *     <li>{@link ErrorInfo.Reason#CARDINALITY_CHANGE} if the multiplicity of the values does not
     *     match that of the property and the number of properties are smaller that {@link #getMaxValues()}</li>
     *     <li>{@link ErrorInfo.Reason#INVALID_DATA} if:
     *     <ul>
     *         <li>the multiplicity of the values does not match that of the property</li>
     *         <li>the field is required, but there are not values</li>
     *         <li>the multiplicity is outside the range {@link #getMaxValues()} - {@link #getMaxValues()}</li>
     *     </ul>
     * </ul>
     * <p>The {@link ErrorInfo.Reason#CARDINALITY_CHANGE} is dominant.</p>
     * <p></p>
     *
     * <p>The {@link CompoundContext} has a {@link CompoundContext#getNode()}
     * and a {@link CompoundContext#getDocument()} method.
     *
     * <p>In case the property of a compound is validated, {@link #getId()} ( the path of the property or node )
     * matches the name of the compound node ({@link CompoundContext#getNode()}). In that case
     * {@link CompoundContext#getNode()} is used to determine the multiplicity of the backing property
     * ( the node is a compound node ). </p>
     * <p>If the {@link CompoundContext#getDocument()} is {@code null}, the node is used.</p>
     * <p>In most cases {@link CompoundContext#getDocument()} and {@link CompoundContext#getNode()} reference
     * the same node, the document node.</p>
     *
     * @param values  {@Link List} of {@link FieldValue}'s
     * @param context context of this field
     * @return The number of values that have an invalid value
     */
    @Override
    public int validate(final List<FieldValue> values, final CompoundContext context) {
        final String valueName = getId();
        try {
            final Node compoundOrDocument = getCompoundOrDocument(context.getNode(), context.getDocument());
            final NodeIterator children = compoundOrDocument.getNodes(valueName);
            final long count = children.getSize();

            // additional cardinality check to prevent creating new values or remove a subset of the old values
            if (!values.isEmpty() && values.size() != count && count <= getMaxValues()) {
                throw new BadRequestException(new ErrorInfo(ErrorInfo.Reason.CARDINALITY_CHANGE));
            }

            if (values.size() != count) {
                throw new BadRequestException(new ErrorInfo(ErrorInfo.Reason.INVALID_DATA));
            }

            FieldTypeUtils.checkCardinality(this, values);

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

    private Node getCompoundOrDocument(final Node node, final Node document) throws RepositoryException {
        if (node == null) {
            throw new BadRequestException(new ErrorInfo(ErrorInfo.Reason.INVALID_DATA));
        }
        if (node.isNodeType(HippoNodeType.NT_COMPOUND) || document == null) {
            return node;
        }
        return document;
    }

    /**
     * Validators for node fields always get the node as the value to validate.
     */
    @Override
    public final Object getValidatedValue(final FieldValue value, final CompoundContext context) {
        return context.getNode();
    }
}
