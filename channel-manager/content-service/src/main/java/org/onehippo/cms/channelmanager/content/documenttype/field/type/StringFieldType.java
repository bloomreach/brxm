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
    public Optional<List> readFrom(final Node node) {
        final List<String> values = readValues(node);

        // Adjust values towards valid cardinality
        trimToMaxValues(values);
        while (values.size() < getMinValues()) {
            values.add("");
        }

        // Fix missing required
        if (isRequired() && values.isEmpty()) {
            values.add("");
        }

        return values.isEmpty() ? Optional.empty() : Optional.of(values);
    }

    private List<String> readValues(final Node node) {
        final String propertyName = getId();
        final List<String> values = new ArrayList<>();

        try {
            if (node.hasProperty(propertyName)) {
                final Property property = node.getProperty(propertyName);
                if (property.isMultiple()) {
                    for (Value v : property.getValues()) {
                        values.add(v.getString());
                    }
                } else {
                    values.add(property.getString());
                }
            }
        } catch (RepositoryException e) {
            log.warn("Failed to read string field '{}' from '{}'", propertyName, JcrUtils.getNodePathQuietly(node), e);
        }

        return values;
    }

    @Override
    public void writeTo(final Node node, final Optional<Object> optionalValue) throws ErrorWithPayloadException {
        final String propertyName = getId();
        final List<String> values = checkValue(optionalValue, String.class);

        try {
            if (values.isEmpty()) {
                if (hasProperty(node, propertyName)) {
                    node.getProperty(propertyName).remove();
                }
            } else {
                if (getMaxValues() > 1) {
                    node.setProperty(propertyName, values.toArray(new String[values.size()]));
                } else {
                    node.setProperty(propertyName, values.get(0));
                }
            }
        } catch (RepositoryException e) {
            log.warn("Failed to write singular String value to property {}", propertyName, e);
            throw new InternalServerErrorException();
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
    public Optional<List> validate(final Optional<List> optionalValue) {
        // fallback to empty list should never happen, as this is caught by the writeTo-validation (checkValue)
        final List<String> valueList = optionalValue.orElse(Collections.emptyList());
        if (isRequired()) {
            final List<ValidationErrorInfo> errorList = new ArrayList<>();
            boolean errorFound = false;
            for (String string : valueList) {
                if (string.isEmpty()) {
                    errorList.add(new ValidationErrorInfo(ValidationErrorInfo.Code.REQUIRED_FIELD_EMPTY));
                    errorFound = true;
                } else {
                    errorList.add(new ValidationErrorInfo());
                }
            }
            return errorFound ? Optional.of(errorList) : Optional.empty();
        }

        return Optional.empty();
    }
}
