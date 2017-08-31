/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.cms.channelmanager.content.documenttype.field.type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.document.util.FieldPath;
import org.onehippo.cms.channelmanager.content.documenttype.field.validation.ValidationErrorInfo;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LongFieldType extends AbstractFieldType {

    public static final Logger log = LoggerFactory.getLogger(LongFieldType.class);

    private static final String DEFAULT_VALUE = "0";

    public LongFieldType() {
        setType(Type.LONG);
    }

    @Override
    public Optional<List<FieldValue>> readFrom(final Node node) {
        final List<FieldValue> values = readValues(node);

        trimToMaxValues(values);
        fillToMinValues(values);
        fillMissingRequiredValues(values);

        return values.isEmpty() ? Optional.empty() : Optional.of(values);
    }

    protected List<FieldValue> readValues(final Node node) {
        final String propertyName = getId();
        final List<FieldValue> values = new ArrayList<>();

        try {
            if (node.hasProperty(propertyName)) {
                final Property property = node.getProperty(propertyName);
                if (property.isMultiple()) {
                    for (final Value v : property.getValues()) {
                        values.add(new FieldValue(v.getString()));
                    }
                } else {
                    values.add(new FieldValue(property.getString()));
                }
            }
        } catch (final RepositoryException e) {
            log.warn("Failed to read long field '{}' from '{}'", propertyName, JcrUtils.getNodePathQuietly(node), e);
        }

        return values;
    }

    @Override
    protected void writeValues(final Node node, final Optional<List<FieldValue>> optionalValues,
                               final boolean validateValues) throws ErrorWithPayloadException {
        final List<FieldValue> processedValues = processValues(optionalValues);

        if (validateValues) {
            checkCardinality(processedValues);
        }

        final String propertyName = getId();
        try {
            if (processedValues.isEmpty()) {
                if (hasProperty(node, propertyName)) {
                    node.getProperty(propertyName).remove();
                }
            } else {
                final String[] strings = new String[processedValues.size()];
                for (int i = 0; i < strings.length; i++) {
                    final Optional<String> value = processedValues.get(i).findValue();

                    strings[i] = validateValues ? value.orElseThrow(INVALID_DATA) : value.orElse(DEFAULT_VALUE);
                }

                if (node.hasProperty(propertyName)) {
                    final Property property = node.getProperty(propertyName);
                    if (isMultiple() != property.isMultiple()) {
                        property.remove();
                    }
                }

                try {
                    if (isMultiple()) {
                        node.setProperty(propertyName, strings, PropertyType.LONG);
                    } else {
                        node.setProperty(propertyName, strings[0], PropertyType.LONG);
                    }
                } catch (final ValueFormatException ignore) {
                }
            }
        } catch (final RepositoryException e) {
            log.warn("Failed to write long value(s) to property {}", propertyName, e);
            throw new InternalServerErrorException();
        }
    }

    @Override
    public boolean writeField(final Node node, final FieldPath fieldPath, final List<FieldValue> values) throws ErrorWithPayloadException {
        if (!fieldPath.is(getId())) {
            return false;
        }
        writeValues(node, Optional.of(values), false);
        return true;
    }

    @Override
    public boolean validate(final List<FieldValue> valueList) {
        boolean isValid = true;

        if (isRequired()) {
            if (!validateValues(valueList, this::validateSingleRequired)) {
                isValid = false;
            }
        }

        return isValid;
    }

    protected boolean validateSingleRequired(final FieldValue value) {
        if (value.findValue().orElse("").isEmpty()) {
            value.setErrorInfo(new ValidationErrorInfo(ValidationErrorInfo.Code.REQUIRED_FIELD_EMPTY));
            return false;
        }
        return true;
    }

    protected List<FieldValue> processValues(final Optional<List<FieldValue>> optionalValues) {
        return optionalValues.orElse(Collections.emptyList());
    }

    private void fillMissingRequiredValues(final List<FieldValue> values) {
        if (isRequired() && values.isEmpty()) {
            values.add(new FieldValue(DEFAULT_VALUE));
        }
    }

    private void fillToMinValues(final List<FieldValue> values) {
        while (values.size() < getMinValues()) {
            values.add(new FieldValue(DEFAULT_VALUE));
        }
    }

    private static boolean hasProperty(final Node node, final String propertyName) throws RepositoryException {
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
}
