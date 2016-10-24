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
        final String property = getId();
        try {
            if (hasProperty(node, property)) {
                final Property jcrProperty = node.getProperty(property);
                if (jcrProperty.isMultiple()) {
                    final Value[] values = jcrProperty.getValues();
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
                        return Optional.of(Collections.singletonList(jcrProperty.getString()));
                    } else {
                        return Optional.of(jcrProperty.getString());
                    }
                }
            }
        } catch (RepositoryException e) {
            log.warn("Failed to read string field '{}' from '{}'", property, JcrUtils.getNodePathQuietly(node), e);
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
    public int writeTo(Node node, Optional<Object> optionalValue) {
        final String property = getId();
        final boolean isRequired = getValidators().contains(Validator.REQUIRED);
        final Object value = optionalValue.orElse(Collections.emptyList());
        if (isMultiple()) {
            if (!(value instanceof List)) {
                return 1; // we require a list of Strings
            }
            final List listOfValues = (List)value;
            if (isRequired && listOfValues.isEmpty()) {
                return 1; // need at least one value for required field
            }
            if (isOptional() && listOfValues.size() > 1) {
                return 1; // "optional" field can have no more than 1 value
            }

            int errors = 0;
            for (Object v : listOfValues) {
                if (!(v instanceof String)) {
                    errors++;
                    continue;
                }
                final String s = (String)v;
                if (isRequired && s.isEmpty()) {
                    errors++;
                }
            }
            if (errors > 0) {
                return errors;
            }

            try {
                if (listOfValues.isEmpty()) {
                    removeProperty(node, property);
                } else {
                    if (isOptional()) {
                        node.setProperty(property, (String) listOfValues.get(0));
                    } else {
                        final String[] arrayOfValues = new String[listOfValues.size()];
                        node.setProperty(property, (String[]) listOfValues.toArray(arrayOfValues));
                    }
                }
                return 0;
            } catch (RepositoryException e) {
                log.warn("Failed to write multiple String value to property {}", property, e);
                return 1;
            }
        }

        // is single field
        if (!(value instanceof String)) {
            return 1;
        }
        final String string = (String)value;
        if (isRequired && string.isEmpty()) {
            return 1;
        }
        try {
            node.setProperty(property, string);
            return 0;
        } catch (RepositoryException e) {
            log.warn("Failed to write singular String value to property {}", property, e);
            return 1;
        }
    }

    private void removeProperty(final Node node, final String property) throws RepositoryException {
        if (hasProperty(node, property)) {
            node.getProperty(property).remove();
        }
    }

    private boolean hasProperty(final Node node, final String property) throws RepositoryException {
        if (!node.hasProperty(property)) {
            return false;
        }
        final Property jcrProperty = node.getProperty(property);
        if (!jcrProperty.isMultiple()) {
            return true;
        }
        // empty multiple property is equivalent to no property.
        return jcrProperty.getValues().length > 0;
    }
}
