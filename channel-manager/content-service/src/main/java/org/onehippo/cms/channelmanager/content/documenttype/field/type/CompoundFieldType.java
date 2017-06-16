/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.document.util.FieldPath;
import org.onehippo.cms.channelmanager.content.documenttype.ContentTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompoundFieldType extends AbstractFieldType implements NodeFieldType {
    private static final Logger log = LoggerFactory.getLogger(CompoundFieldType.class);

    private final List<FieldType> fields = new ArrayList<>();

    public CompoundFieldType() {
        setType(Type.COMPOUND);
    }

    public List<FieldType> getFields() {
        return fields;
    }

    @Override
    public boolean isValid() {
        return super.isValid() && !fields.isEmpty();
    }

    @Override
    public void init(final FieldTypeContext fieldContext) {
        super.init(fieldContext);

        fieldContext.createContextForCompound()
                .ifPresent(context -> FieldTypeUtils.populateFields(fields, context));
    }

    void initProviderBasedChoice(final FieldTypeContext fieldContext, final String choiceId) {
        init(fieldContext);
        setId(choiceId);
    }

    void initListBasedChoice(final ContentTypeContext parentContext, final String choiceId) {
        FieldTypeUtils.populateFields(fields, parentContext);
        setId(choiceId);
    }

    @Override
    public Optional<List<FieldValue>> readFrom(final Node node) {
        List<FieldValue> values = readValues(node);

        trimToMaxValues(values);

        return values.isEmpty() ? Optional.empty() : Optional.of(values);
    }

    private List<FieldValue> readValues(final Node node) {
        final String nodeName = getId();
        final List<FieldValue> values = new ArrayList<>();
        try {
            for (Node child : new NodeIterable(node.getNodes(nodeName))) {
                // Note: we add the valueMap to the values even if it is empty, because we need to
                // maintain the 1-to-1 mapping between exposed values and internal nodes.
                values.add(readValue(child));
            }
        } catch (RepositoryException e) {
            log.warn("Failed to read nodes for compound type '{}'", getId(), e);
        }
        return values;
    }

    @Override
    public FieldValue readValue(final Node node) {
        Map<String, List<FieldValue>> valueMap = new HashMap<>();
        FieldTypeUtils.readFieldValues(node, getFields(), valueMap);
        return new FieldValue(valueMap);
    }

    @Override
    protected void writeValues(final Node node,
                               final Optional<List<FieldValue>> optionalValues,
                               final boolean validateValues) throws ErrorWithPayloadException {
        final List<FieldValue> values = optionalValues.orElse(Collections.emptyList());

        if (validateValues) {
            checkCardinality(values);
        }

        final String nodeName = getId();
        try {
            NodeIterator children = node.getNodes(nodeName);
            FieldTypeUtils.writeNodeValues(children, values, getMaxValues(), this);
        } catch (RepositoryException e) {
            log.warn("Failed to write compound value to node {}", nodeName, e);
            throw new InternalServerErrorException();
        }
    }

    @Override
    public boolean writeField(final Node node, final FieldPath fieldPath, final List<FieldValue> values) throws ErrorWithPayloadException {
        if (!fieldPath.startsWith(getId())) {
            return false;
        }
        try {
            final String childName = fieldPath.getFirstSegment();
            if (!node.hasNode(childName)) {
                throw INVALID_DATA.get();
            }
            final Node child = node.getNode(childName);
            return writeFieldValue(child, fieldPath.getRemainingSegments(), values);
        } catch (RepositoryException e) {
            log.warn("Failed to write value of compound field '{}' to node '{}'", fieldPath, JcrUtils.getNodePathQuietly(node), e);
            throw new InternalServerErrorException();
        }
    }

    @Override
    public boolean writeFieldValue(final Node node, final FieldPath fieldPath, final List<FieldValue> values) throws ErrorWithPayloadException, RepositoryException {
        return FieldTypeUtils.writeFieldValue(fieldPath, values, fields, node);
    }

    @Override
    public void writeValue(final Node node, final FieldValue fieldValue) throws ErrorWithPayloadException {
        final Map<String, List<FieldValue>> valueMap = fieldValue.findFields().orElseThrow(INVALID_DATA);

        FieldTypeUtils.writeFieldValues(valueMap, getFields(), node);
    }

    @Override
    public boolean validate(final List<FieldValue> valueList) {
        return validateValues(valueList, this::validateValue);
    }

    @Override
    public boolean validateValue(final FieldValue value) {
        // The "required: validator only applies to the cardinality of a compound field, and has
        // therefore already been checked during the writeTo-validation (#checkCardinality).

        // #readValue guarantees that value.getFields is not empty
        return FieldTypeUtils.validateFieldValues(value.findFields().get(), getFields());
    }
}
