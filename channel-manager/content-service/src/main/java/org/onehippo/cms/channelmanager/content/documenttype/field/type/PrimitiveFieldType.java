/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.document.util.FieldPath;
import org.onehippo.cms.channelmanager.content.documenttype.field.validation.ValidationErrorInfo;
import org.onehippo.cms.channelmanager.content.documenttype.field.validation.ValidationErrorInfo.Code;
import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * This bean represents a primitive field type, used for the fields of a {@link DocumentType}. It can be serialized into
 * JSON to expose it through a REST API.
 */
@JsonInclude(Include.NON_EMPTY)
public abstract class PrimitiveFieldType extends AbstractFieldType {

    @JsonIgnore
    private static final Logger log = LoggerFactory.getLogger(PrimitiveFieldType.class);

    @Override
    public Optional<List<FieldValue>> readFrom(final Node node) {
        final List<FieldValue> values = readValues(node);

        trimToMaxValues(values);
        fillToMinValues(values);
        fillMissingRequiredValues(values);

        return values.isEmpty() ? Optional.empty() : Optional.of(values);
    }

    @Override
    public boolean validate(final List<FieldValue> valueList) {
        boolean isValid = true;

        if (isRequired()) {
            if (!validateValues(valueList, PrimitiveFieldType::validateSingleRequired)) {
                isValid = false;
            }
        }

        return isValid;
    }

    @Override
    protected void writeValues(final Node node, final Optional<List<FieldValue>> optionalValues, final boolean validateValues) throws ErrorWithPayloadException {
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

                    strings[i] = validateValues ? value.orElseThrow(INVALID_DATA) : value.orElse(null);

                    if (validateValues) {
                        fieldSpecificValidations(strings[i]);
                    }
                }

                // make sure we can set the new property value
                if (node.hasProperty(propertyName)) {
                    final Property property = node.getProperty(propertyName);
                    if (isMultiple() != property.isMultiple()) {
                        property.remove();
                    }
                }

                try {
                    if (isMultiple()) {
                        node.setProperty(propertyName, convertToSpecificTypeArray(strings), getPropertyType());
                    } else {
                        node.setProperty(propertyName, convertToSpecificType(strings[0]), getPropertyType());
                    }
                } catch (final NumberFormatException | ValueFormatException ignore) {
                }
            }
        } catch (final RepositoryException e) {
            log.warn("Failed to write value(s) to property {}", propertyName, e);
            throw new InternalServerErrorException();
        }
    }

    protected String fieldSpecificConversion(final String input) {
        return input;
    }

    private String convertToSpecificType(final String input) throws ValueFormatException {
        if (input != null) {
            return fieldSpecificConversion(input);
        }
        throw new ValueFormatException("Trying to convert null value");
    }

    private String[] convertToSpecificTypeArray(final String[] strings) {
        final List<String> convertedStrings = new ArrayList<>();
        for (final String element : strings) {
            String specificType;
            try {
                specificType = convertToSpecificType(element);
            } catch (final ValueFormatException ignore) {
                specificType = null;
            }
            convertedStrings.add(specificType);
        }
        return convertedStrings.toArray(new String[0]);
    }

    protected void fieldSpecificValidations(final String validatedField) throws ErrorWithPayloadException {
        // empty on purpose
    }

    protected abstract int getPropertyType();

    @Override
    public boolean writeField(final Node node, final FieldPath fieldPath, final List<FieldValue> values) throws ErrorWithPayloadException {
        if (!fieldPath.is(getId())) {
            return false;
        }
        writeValues(node, Optional.of(values), false);
        return true;
    }

    protected abstract String getDefault();

    protected List<FieldValue> readValues(final Node node) {
        final String propertyName = getId();
        final List<FieldValue> values = new ArrayList<>();

        try {
            if (node.hasProperty(propertyName)) {
                final Property property = node.getProperty(propertyName);
                storeProperty(values, property);
            }
        } catch (final RepositoryException e) {
            log.warn("Failed to read field '{}' from '{}'", propertyName, JcrUtils.getNodePathQuietly(node), e);
        }

        return values;
    }

    protected static boolean validateSingleRequired(final FieldValue value) {
        if (value.findValue().orElse(StringUtils.EMPTY).isEmpty()) {
            value.setErrorInfo(new ValidationErrorInfo(Code.REQUIRED_FIELD_EMPTY));
            return false;
        }
        return true;
    }

    private static void storeProperty(final Collection<FieldValue> values, final Property property) throws RepositoryException {
        if (property.isMultiple()) {
            for (final Value v : property.getValues()) {
                values.add(new FieldValue(v.getString()));
            }
        } else {
            values.add(new FieldValue(property.getString()));
        }
    }

    private void fillToMinValues(final List<FieldValue> values) {
        while (values.size() < getMinValues()) {
            values.add(new FieldValue(getDefault()));
        }
    }

    private void fillMissingRequiredValues(final List<FieldValue> values) {
        if (isRequired() && values.isEmpty()) {
            values.add(new FieldValue(getDefault()));
        }
    }
}
