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

import java.util.List;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * StringFieldType controls the reading and writing of a String type field from and to a node's property.
 * <p>
 * The code diligently deals with the situation that the field type definition may be out of sync with the actual
 * property value, and exposes and validates a value as consistent as possible with the field type definition. As such,
 * a "no-change" read-and-write operation may have the effect that the document is adjusted towards better consistency
 * with the field type definition.
 */
public class StringFieldType extends PrimitiveFieldType {

    private static final Logger log = LoggerFactory.getLogger(AbstractFieldType.class);
    private static final String DEFAULT_VALUE = "";

    private Long maxLength;

    public StringFieldType() {
        setType(Type.STRING);
    }

    @Override
    public void init(final FieldTypeContext fieldContext) {
        super.init(fieldContext);
        initializeMaxLength(fieldContext);
    }

    void initializeMaxLength(final FieldTypeContext fieldContext) {
        fieldContext.getStringConfig("maxlength").ifPresent(this::setMaxLength);
    }

    void setMaxLength(final String maxLengthString) {
        try {
            maxLength = Long.valueOf(maxLengthString);
        } catch (final NumberFormatException e) {
            log.info("Failed to parser value of String's max length '{}'", maxLengthString, e);
        }
    }

    public Long getMaxLength() {
        return maxLength;
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

                    strings[i] = validateValues ? value.orElseThrow(INVALID_DATA) : value.orElse(DEFAULT_VALUE);

                    if (validateValues && maxLength != null && strings[i].length() > maxLength) {
                        throw INVALID_DATA.get();
                    }
                }

                // make sure we can set the new property value
                if (node.hasProperty(propertyName)) {
                    final Property property = node.getProperty(propertyName);
                    if (isMultiple() != property.isMultiple()) {
                        property.remove();
                    }
                }

                if (isMultiple()) {
                    node.setProperty(propertyName, strings);
                } else {
                    node.setProperty(propertyName, strings[0]);
                }
            }
        } catch (final RepositoryException e) {
            log.warn("Failed to write String value(s) to property {}", propertyName, e);
            throw new InternalServerErrorException();
        }
    }

    @Override
    protected String getDefault() {
        return DEFAULT_VALUE;
    }

}
