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
import java.util.Collection;
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
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.documenttype.field.validation.CompoundContext;
import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Base class for all field types of a {@link DocumentType} that store their value in a JCR property.
 * Can be serialized into JSON to expose it through a REST API.
 */
@JsonInclude(Include.NON_EMPTY)
public abstract class PropertyFieldType extends LeafFieldType {

    @JsonIgnore
    private static final Logger log = LoggerFactory.getLogger(PropertyFieldType.class);

    @Override
    public Optional<List<FieldValue>> readFrom(final Node node) {
        final List<FieldValue> values = readValues(node);

        FieldTypeUtils.trimToMaxValues(values, getMaxValues());
        fillToMinValues(values);
        fillMissingRequiredValues(values);

        return values.isEmpty() ? Optional.empty() : Optional.of(values);
    }

    @Override
    public int validate(final List<FieldValue> valueList, final CompoundContext context) {
        return valueList.stream()
                .mapToInt(value -> validateValue(value, context))
                .sum();
    }

    @Override
    public void writeValues(final Node node, final Optional<List<FieldValue>> optionalValues, final boolean checkCardinality) throws ErrorWithPayloadException {
        final List<FieldValue> processedValues = processValues(optionalValues);

        if (checkCardinality) {
            FieldTypeUtils.checkCardinality(this, processedValues);
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

                    strings[i] = checkCardinality ? value.orElseThrow(FieldTypeUtils.INVALID_DATA) : value.orElse(null);

                    if (checkCardinality) {
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
                    final int jcrPropertyType = PropertyType.valueFromName(getJcrType());
                    if (isMultiple()) {
                        node.setProperty(propertyName, convertToSpecificTypeArray(strings), jcrPropertyType);
                    } else {
                        node.setProperty(propertyName, convertToSpecificType(strings[0]), jcrPropertyType);
                    }
                } catch (final IllegalArgumentException | ValueFormatException ignore) {
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
        if(input != null) {
            return fieldSpecificConversion(input);
        }
        throw new ValueFormatException("Trying to convert null value");
    }

    private String[] convertToSpecificTypeArray(final String[] strings) throws ValueFormatException {
        final List<String> convertedStrings = new ArrayList<>();
        for (final String element : strings) {
            convertedStrings.add(convertToSpecificType(element));
        }
        return convertedStrings.toArray(new String[0]);
    }

    protected void fieldSpecificValidations(final String validatedField) throws ErrorWithPayloadException {
        // empty on purpose
    }

    protected abstract String getDefault();

    @Override
    public List<FieldValue> readValues(final Node node) {
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

    protected FieldValue getFieldValue(final String value) {
        return new FieldValue(value);
    }

    private void storeProperty(final Collection<FieldValue> values, final Property property) throws RepositoryException {
        if (property.isMultiple()) {
            for (final Value v : property.getValues()) {
                values.add(getFieldValue(v.getString()));
            }
        } else {
            values.add(getFieldValue(property.getString()));
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
