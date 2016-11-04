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
import org.onehippo.cms.channelmanager.content.documenttype.ContentTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.DocumentTypesService;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.validation.ValidationErrorInfo;
import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;
import org.onehippo.cms.channelmanager.content.error.BadRequestException;
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
    public Optional<Object> readFrom(Node node) {
        final String nodeName = getId();
        final List<Map<String, Object>> values = new ArrayList<>();
        try {
            for (Node child : new NodeIterable(node.getNodes(nodeName))) {
                Map<String, Object> valueMap = new HashMap<>();
                for (FieldType fieldType : getFields()) {
                    fieldType.readFrom(child).ifPresent(value -> valueMap.put(fieldType.getId(), value));
                }
                if (!valueMap.isEmpty()) {
                    values.add(valueMap);
                }
            }
            if (!values.isEmpty()) {
                if (isMultiple()) {
                    if (isOptional() && values.size() > 1) {
                        return Optional.of(Collections.singletonList(values.get(0))); // optional has max cardinality 1
                    }
                    return Optional.of(values);
                } else {
                    return Optional.of(values.get(0));
                }
            }
        } catch (RepositoryException e) {
            log.warn("Failed to read nodes for compound type '{}'", getId(), e);
        }
        return Optional.empty();
    }

    @Override
    public void writeTo(final Node node, Optional<Object> optionalValue) throws ErrorWithPayloadException {
        final String nodeName = getId();
        try {
            final NodeIterator iterator = node.getNodes(nodeName);
            long numberOfNodes = iterator.getSize();

            if (isMultiple()) {
                final List values = checkMultipleType(optionalValue, numberOfNodes);
                for (Object value : values) {
                    writeToCompoundNode(iterator.nextNode(), value);
                }
            } else {
                final Object value = checkSingularType(optionalValue, numberOfNodes);
                writeToCompoundNode(iterator.nextNode(), value);
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

    private Object checkSingularType(final Optional<Object> optionalValue, final long currentCount) throws BadRequestException {
        final Object value = optionalValue.orElse("invalid");
        if (currentCount == 0) {
            throw BAD_REQUEST_CARDINALITY_CHANGE; // creation of new nodes not yet supported
        }
        return value;
    }

    private List checkMultipleType(final Optional<Object> optionalValue, final long currentCount) throws BadRequestException {
        final Object value = optionalValue.orElse(Collections.emptyList());
        if (!(value instanceof List)) {
            throw BAD_REQUEST_INVALID_DATA;
        }
        final List values = (List)value;
        if (isOptional()) {
            if (values.size() > 1) {
                throw BAD_REQUEST_INVALID_DATA;
            }
            if (values.size() == 1 && currentCount == 0) {
                throw BAD_REQUEST_CARDINALITY_CHANGE; // creation of new nodes not yet supported
            }
        } else {
            if (!values.isEmpty() && values.size() != currentCount) {
                throw BAD_REQUEST_CARDINALITY_CHANGE;
            }
        }
        return values;
    }

    private void writeToCompoundNode(final Node compound, final Object value) throws ErrorWithPayloadException {
        if (!(value instanceof Map)) {
            throw BAD_REQUEST_INVALID_DATA;
        }
        final Map valueMap = (Map)value;
        for (FieldType field : getFields()) {
            field.writeTo(compound, Optional.ofNullable(valueMap.get(field.getId())));
        }
    }

    @Override
    public Optional<Object> validate(final Optional<Object> optionalValue) {
        final boolean isRequired = getValidators().contains(Validator.REQUIRED);
        if (isRequired) {
            if (!optionalValue.isPresent()) {
                return Optional.of(new ValidationErrorInfo(ValidationErrorInfo.Code.REQUIRED_FIELD_ABSENT));
            }
        }

        return validateCompounds(optionalValue);
    }

    /**
     * Here, we perform the validation of the nested fields inside the compound.
     *
     * If the field is "multiple", its value - if present - is represented by an ordered list of compound
     * value maps. In order for a client to be able to associate a single value map with its corresponding
     * "error map", we construct a list of error maps of equal length. In case of a valid compound value map,
     * the error map will be there, but it will be empty.
     *
     * If the field is "single", its value - again, if present - is represented by a single compound value
     * map. In case of a valid compound value map, the error map will be empty, and we drop it, i.e. we return
     * an empty Optional.
     *
     * @param optionalValue optional Value for a single or multiple compound field.
     * @return
     */
    private Optional<Object> validateCompounds(final Optional<Object> optionalValue) {
        if (optionalValue.isPresent()) {
            final Object value = optionalValue.get();
            if (value instanceof List) {
                final List<Map<String, Object>> valueMapList = (List) value;
                final List<Map<String, Object>> errorMapList = new ArrayList<>();
                boolean errorFound = false;
                for (Map<String, Object> valueMap : valueMapList) {
                    final Map<String, Object> errorMap = validateCompound(valueMap);
                    if (!errorMap.isEmpty()) {
                        errorFound = true;
                    }
                    errorMapList.add(errorMap);
                }

                if (errorFound) {
                    return Optional.of(errorMapList);
                }
            } else {
                final Map<String, Object> errorMap = validateCompound((Map<String, Object>)value);
                if (!errorMap.isEmpty()) {
                    return Optional.of(errorMap);
                }
            }
        }
        return Optional.empty();
    }

    private Map<String, Object> validateCompound(final Map<String, Object> valueMap) {
        final Map<String, Object> errorMap = new HashMap<>();
        for (FieldType fieldType : getFields()) {
            final Optional<Object> optionalValue = Optional.ofNullable(valueMap.get(fieldType.getId()));
            fieldType.validate(optionalValue).ifPresent(error -> errorMap.put(fieldType.getId(), error));
        }
        return errorMap;
    }
}
