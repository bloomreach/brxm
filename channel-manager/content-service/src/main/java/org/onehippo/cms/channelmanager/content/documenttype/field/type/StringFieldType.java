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
import java.util.List;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms.channelmanager.content.documenttype.field.validation.ValidationErrorInfo;
import org.onehippo.cms.channelmanager.content.error.BadRequestException;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * StringFieldType controls the reading and writing of a String type field from and to a node's property.
 *
 * The code diligently deals with the situation that the field type definition may be out of sync with the
 * actual property value, and exposes and validates a value as consistent as possible with the field type
 * definition. As such, a "no-change" read-and-write operation may have the effect that the document is
 * adjusted towards better consistency with the field type definition.
 */
public class StringFieldType extends FieldType {

    private static final Logger log = LoggerFactory.getLogger(StringFieldType.class);

    public StringFieldType() {
        this.setType(Type.STRING);
    }

    /**
     * Read an optional value (singular or multiple) from a node's property.
     *
     * If the node's property does not match the field type, we convert the property
     * to a value that's consistent with the field type. In case of a REQUIRED field,
     * we cannot determine a sensible value, so we use an empty string, which will be
     * rejected upon write.
     */
    @Override
    public Optional<Object> readFrom(final Node node) {
        final String propertyName = getId();
        try {
            if (hasProperty(node, propertyName)) {
                final Property property = node.getProperty(propertyName);
                if (property.isMultiple()) {
                    final Value[] values = property.getValues();
                    if (isMultiple()) {
                        if (isOptional()) {
                            if (values.length > 0) {
                                return Optional.of(Collections.singletonList(values[0].getString()));
                            }
                        } else {
                            final List<String> list = new ArrayList<>();
                            for (Value v : values) {
                                list.add(v.getString());
                            }
                            if (!list.isEmpty()) {
                                return Optional.of(list);
                            }
                        }
                    } else {
                        // field type is singular, but actual value multiple
                        return Optional.of(values.length > 0 ? values[0].getString() : "");
                    }
                } else {
                    if (isMultiple()) {
                        // field type is multiple, but value is singular
                        return Optional.of(Collections.singletonList(property.getString()));
                    } else {
                        return Optional.of(property.getString());
                    }
                }
            }
        } catch (RepositoryException e) {
            log.warn("Failed to read string field '{}' from '{}'", propertyName, JcrUtils.getNodePathQuietly(node), e);
        }
        return readFromEmptyProperty();
    }

    private Optional<Object> readFromEmptyProperty() {
        if (isMultiple()) {
            if (getValidators().contains(Validator.REQUIRED)) {
                return Optional.of(Collections.singletonList(""));
            }
        } else {
            return Optional.of("");
        }
        return Optional.empty();
    }

    @Override
    public void writeTo(final Node node, final Optional<Object> optionalValue) throws ErrorWithPayloadException {
        final String propertyName = getId();
        final Object value = optionalValue.orElse(Collections.emptyList());
        try {
            if (isMultiple()) {
                final List listOfValues = checkMultipleType(value);
                writeMultipleValue(node, propertyName, listOfValues);
            } else {
                final String string = checkSingularType(value);
                node.setProperty(propertyName, string);
            }
        } catch (RepositoryException e) {
            log.warn("Failed to write singular String value to property {}", propertyName, e);
            throw new InternalServerErrorException();
        }
    }

    private String checkSingularType(final Object value) throws BadRequestException {
        if (!(value instanceof String)) {
            throw BAD_REQUEST_INVALID_DATA;
        }
        return (String)value;
    }

    private List checkMultipleType(final Object value) throws BadRequestException {
        if (!(value instanceof List)) {
            throw BAD_REQUEST_INVALID_DATA;
        }
        final List listOfValues = (List)value;
        if (isOptional() && listOfValues.size() > 1) {
            throw BAD_REQUEST_INVALID_DATA;
        }

        for (Object v : listOfValues) {
            if (!(v instanceof String)) {
                throw BAD_REQUEST_INVALID_DATA;
            }
        }
        return listOfValues;
    }

    private void writeMultipleValue(final Node node, final String propertyName, final List values) throws RepositoryException {
        if (values.isEmpty()) {
            removeProperty(node, propertyName);
        } else {
            if (isOptional()) {
                node.setProperty(propertyName, (String) values.get(0));
            } else {
                final String[] arrayOfValues = new String[values.size()];
                node.setProperty(propertyName, (String[]) values.toArray(arrayOfValues));
            }
        }
    }

    private void removeProperty(final Node node, final String propertyName) throws RepositoryException {
        if (hasProperty(node, propertyName)) {
            node.getProperty(propertyName).remove();
        }
    }

    private boolean hasProperty(final Node node, final String propertyName) throws RepositoryException {
        if (!node.hasProperty(propertyName)) {
            return false;
        }
        final Property property = node.getProperty(propertyName);
        if (!property.isMultiple()) {
            return true;
        }
        // empty multiple property is equivalent to no property.
        return property.getValues().length > 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<Object> validate(final Optional<Object> optionalValue) {
        final boolean isRequired = getValidators().contains(Validator.REQUIRED);
        if (isRequired) {
            if (optionalValue.isPresent()) {
                final Object value = optionalValue.get();
                if (value instanceof List) {
                    return validateMultiple((List<String>) value);
                } else {
                    final String string = (String) value;
                    if (string.isEmpty()) {
                        return Optional.of(new ValidationErrorInfo(ValidationErrorInfo.Code.REQUIRED_FIELD_EMPTY));
                    }
                }
            } else {
                return Optional.of(new ValidationErrorInfo(ValidationErrorInfo.Code.REQUIRED_FIELD_ABSENT));
            }
        }

        return Optional.empty();
    }

    private Optional<Object> validateMultiple(final List<String> values) {
        final List<ValidationErrorInfo> errorList = new ArrayList<>();
        boolean errorFound = false;
        for (String string : values) {
            if (string.isEmpty()) {
                errorList.add(new ValidationErrorInfo(ValidationErrorInfo.Code.REQUIRED_FIELD_EMPTY));
                errorFound = true;
            } else {
                errorList.add(new ValidationErrorInfo());
            }
        }
        return errorFound ? Optional.of(errorList) : Optional.empty();
    }
}
