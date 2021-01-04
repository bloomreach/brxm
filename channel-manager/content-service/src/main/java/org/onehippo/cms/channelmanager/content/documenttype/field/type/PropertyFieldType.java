/*
 * Copyright 2017-2021 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
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
import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;
import org.onehippo.cms.channelmanager.content.documenttype.validation.CompoundContext;
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
public abstract class PropertyFieldType extends AbstractFieldType implements LeafFieldType {

    @JsonIgnore
    private static final Logger log = LoggerFactory.getLogger(PropertyFieldType.class);

    @Override
    public final Optional<List<FieldValue>> readFrom(final Node node) {
        final List<FieldValue> values = readValues(node);

        FieldTypeUtils.trimToMaxValues(values, getMaxValues());
        fillToMinValues(values);
        fillMissingRequiredValues(values);

        return values.isEmpty() ? Optional.empty() : Optional.of(values);
    }

    @Override
    public final List<FieldValue> readValues(final Node node) {
        final String propertyName = getId();
        final List<FieldValue> values = new ArrayList<>();

        try {
            if (node.hasProperty(propertyName)) {
                final Property property = node.getProperty(propertyName);
                readProperty(values, property);
                afterReadValues(values);
            }
        } catch (final RepositoryException e) {
            log.warn("Failed to read field '{}' from '{}'", propertyName, JcrUtils.getNodePathQuietly(node), e);
        }

        return values;
    }

    /**
     * Hook method for sub-classes to post-process read values.
     *
     * @param values the read values
     */
    protected void afterReadValues(final List<FieldValue> values) {
        // by default do nothing
    }

    @Override
    public final void writeValues(final Node node, final Optional<List<FieldValue>> optionalValues) {
        final List<FieldValue> values = optionalValues.orElse(Collections.emptyList());

        beforeWriteValues(values);

        try {
            if (isMultiplicityOutOfSync(node)) {
                removeProperty(node);
            }

            final String[] valuesAsStrings = values.stream()
                    .filter(Objects::nonNull)
                    .map(value -> value.findValue().orElse(null))
                    .filter(Objects::nonNull)
                    .toArray(String[]::new);

            if (valuesAsStrings.length == 0) {
                removeProperty(node);
                return;
            }

            writeProperty(node, valuesAsStrings);
        } catch (final RepositoryException e) {
            log.warn("Failed to write value(s) to property {}", getId(), e);
            throw new InternalServerErrorException();
        }
    }

    private void removeProperty(final Node node) throws RepositoryException {
        if (hasProperty(node, getId())) {
            node.getProperty(getId()).remove();
        }
    }

    private boolean isMultiplicityOutOfSync(final Node node) throws RepositoryException {
        if (node == null) {
            return false;
        }

        if (!node.hasProperty(getId())) {
            return false;
        }

        final Property property = node.getProperty(getId());
        return isMultiple() != property.isMultiple();
    }

    /**
     * Hook for sub-classes to process values before writing them. The default implementation does nothing.
     *
     * @param values the values to process
     */
    protected void beforeWriteValues(final List<FieldValue> values) {
        // by default do nothing
    }

    private void writeProperty(final Node node, final String[] strings) throws RepositoryException {
        final String propertyName = getId();
        try {
            final int jcrPropertyType = PropertyType.valueFromName(getJcrType());
            if (isMultiple()) {
                node.setProperty(propertyName, convertToSpecificTypeArray(strings), jcrPropertyType);
            } else {
                node.setProperty(propertyName, convertToSpecificType(strings[0]), jcrPropertyType);
            }
        } catch (final IllegalArgumentException | ValueFormatException e) {
            log.debug("Cannot write property '{}' to '{}'", propertyName, JcrUtils.getNodePathQuietly(node), e);
        }
    }

    @Override
    public final int validate(final List<FieldValue> valueList, final CompoundContext context) {
        final List<FieldValue> values = valueList == null ? Collections.emptyList() : valueList;

        FieldTypeUtils.checkCardinality(this, values);

        if (values.stream().anyMatch(Objects::isNull)) {
            throw FieldTypeUtils.INVALID_DATA.get();
        }

        values.stream()
                .filter(Objects::nonNull)
                .map(value -> value.findValue().orElseThrow(FieldTypeUtils.INVALID_DATA))
                .forEach(this::fieldSpecificValidations);

        return values.stream()
                .mapToInt(value -> validateValue(value, context))
                .sum();
    }

    protected String fieldSpecificConversion(final String input) {
        return input;
    }

    private String convertToSpecificType(final String input) throws ValueFormatException {
        if (input != null) {
            return fieldSpecificConversion(input);
        }
        return null;
    }

    private String[] convertToSpecificTypeArray(final String[] strings) throws ValueFormatException {
        final List<String> convertedStrings = new ArrayList<>();
        for (final String element : strings) {
            convertedStrings.add(convertToSpecificType(element));
        }
        return convertedStrings.toArray(new String[0]);
    }

    protected void fieldSpecificValidations(final String validatedField) {
        // empty on purpose
    }

    protected abstract String getDefault();

    protected FieldValue getFieldValue(final String value) {
        return new FieldValue(value);
    }

    private void readProperty(final Collection<FieldValue> values, final Property property) throws RepositoryException {
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
