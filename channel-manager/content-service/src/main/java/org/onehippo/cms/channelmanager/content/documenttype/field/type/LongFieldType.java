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

import java.util.List;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LongFieldType controls the reading and writing of a Long type field from and to a node's property.
 * <p>
 * The code diligently deals with the situation that the field type definition may be out of sync with the actual
 * property value, and exposes and validates a value as consistent as possible with the field type definition. As such,
 * a "no-change" read-and-write operation may have the effect that the document is adjusted towards better consistency
 * with the field type definition.
 */
public class LongFieldType extends PrimitiveFieldType {

    private static final Logger log = LoggerFactory.getLogger(LongFieldType.class);
    private static final String DEFAULT_VALUE = "0";

    public LongFieldType() {
        setType(Type.LONG);
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
    protected String getDefault() {
        return DEFAULT_VALUE;
    }

}
