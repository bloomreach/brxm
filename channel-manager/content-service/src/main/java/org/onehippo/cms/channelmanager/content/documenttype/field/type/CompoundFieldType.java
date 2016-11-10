/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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
import org.onehippo.cms.channelmanager.content.documenttype.ContentTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.DocumentTypesService;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;
import org.onehippo.cms.channelmanager.content.error.BadRequestException;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompoundFieldType extends FieldType {
    private static final Logger log = LoggerFactory.getLogger(CompoundFieldType.class);

    public CompoundFieldType() {
        setType(Type.COMPOUND);
        setFields(new ArrayList<>());
    }

    @Override
    public Optional<FieldType> init(final FieldTypeContext context,
                                    final ContentTypeContext contentTypeContext,
                                    final DocumentType docType) {
        return super.init(context, contentTypeContext, docType)
                .map(fieldType -> {
                    DocumentTypesService.get().populateFieldsForCompoundType(context.getContentTypeItem().getItemType(),
                                                                             fieldType.getFields(), contentTypeContext, docType);
                    return fieldType.getFields().isEmpty() ? null : fieldType;
                });
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
                Map<String, List<FieldValue>> valueMap = new HashMap<>();
                for (FieldType fieldType : getFields()) {
                    fieldType.readFrom(child).ifPresent(value -> valueMap.put(fieldType.getId(), value));
                }
                // Note: we add the valueMap to the values even if it is empty, because we need to
                // maintain the 1-to-1 mapping between exposed values and internal nodes.
                values.add(new FieldValue(valueMap));
            }
        } catch (RepositoryException e) {
            log.warn("Failed to read nodes for compound type '{}'", getId(), e);
        }
        return values;
    }

    @Override
    public void writeTo(final Node node, Optional<Object> optionalValue) throws ErrorWithPayloadException {
        final String nodeName = getId();
        final List<FieldValue> values = checkValue(optionalValue, Map.class);

        try {
            final NodeIterator iterator = node.getNodes(nodeName);
            long numberOfNodes = iterator.getSize();

            // Additional cardinality check due to not yet being able to create new
            // (or remove a subset of the old) compound nodes, unless there are more nodes than allowed
            if (!values.isEmpty() && values.size() != numberOfNodes && !(numberOfNodes > getMaxValues())) {
                throw new BadRequestException(new ErrorInfo(ErrorInfo.Reason.CARDINALITY_CHANGE));
            }

            for (FieldValue value : values) {
                Map valueMap = value.getFields();
                final Node compound = iterator.nextNode();
                for (FieldType field : getFields()) {
                    Object fieldValue = valueMap.get(field.getId());
                    field.writeTo(compound, Optional.ofNullable(fieldValue));
                }
            }

            // delete excess nodes to match field type
            while (iterator.hasNext()) {
                iterator.nextNode().remove();
            }
        } catch (RepositoryException e) {
            log.warn("Failed to write compound value to node {}", nodeName, e);
            throw new InternalServerErrorException();
        }
    }

    @Override
    public boolean validate(final Optional<List<FieldValue>> optionalValues) {
        // The "required: validator only applies to the cardinality of a compound field, and has
        // therefore already been checked during the writeTo-validation (checkValue).

        boolean isValid = true;
        final List<FieldValue> valueList = optionalValues.orElse(Collections.emptyList());

        for (FieldValue value : valueList) {
            final Map<String, List<FieldValue>> valueMap = value.getFields();
            for (FieldType fieldType : getFields()) {
                final String fieldId = fieldType.getId();
                final Optional<List<FieldValue>> optionalChildValues = Optional.ofNullable(valueMap.get(fieldId));
                if (!fieldType.validate(optionalChildValues)) {
                    isValid = false;
                }
            }
        }

        return isValid;
    }
}
