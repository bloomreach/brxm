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

import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompoundFieldType extends FieldType implements CompoundWriter {
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
                values.add(readSingleFrom(child));
            }
        } catch (RepositoryException e) {
            log.warn("Failed to read nodes for compound type '{}'", getId(), e);
        }
        return values;
    }

    public FieldValue readSingleFrom(final Node node) {
        Map<String, List<FieldValue>> valueMap = new HashMap<>();
        FieldTypeUtils.readFieldValues(node, getFields(), valueMap);
        return new FieldValue(valueMap);
    }

    @Override
    public void writeTo(final Node node, Optional<List<FieldValue>> optionalValues) throws ErrorWithPayloadException {
        final String nodeName = getId();
        final List<FieldValue> values = optionalValues.orElse(Collections.emptyList());
        checkCardinality(values);

        try {
            NodeIterator children = node.getNodes(nodeName);
            FieldTypeUtils.writeCompoundValues(children, values, getMaxValues(), this);
        } catch (RepositoryException e) {
            log.warn("Failed to write compound value to node {}", nodeName, e);
            throw new InternalServerErrorException();
        }
    }

    @Override
    public void writeValue(final Node node, final FieldValue fieldValue) throws ErrorWithPayloadException {
        final Map<String, List<FieldValue>> valueMap = fieldValue.findFields().orElseThrow(INVALID_DATA);

        FieldTypeUtils.writeFieldValues(valueMap, getFields(), node);
    }

    @Override
    public boolean validate(final List<FieldValue> valueList) {
        return validateValues(valueList, this::validateSingle);
    }

    public boolean validateSingle(final FieldValue value) {
        // The "required: validator only applies to the cardinality of a compound field, and has
        // therefore already been checked during the writeTo-validation (#checkCardinality).

        // #readSingleFrom guarantees that value.getFields is not empty
        return FieldTypeUtils.validateFieldValues(value.findFields().get(), getFields());
    }
}
