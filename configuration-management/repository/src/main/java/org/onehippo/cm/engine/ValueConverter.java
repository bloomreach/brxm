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
package org.onehippo.cm.engine;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.List;

import javax.jcr.Binary;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;

import org.apache.commons.io.IOUtils;
import org.onehippo.cm.model.DefinitionProperty;
import org.onehippo.cm.model.Source;
import org.onehippo.cm.model.Value;
import org.onehippo.cm.model.ValueType;
import org.onehippo.cm.model.impl.ModelUtils;

/**
 * Config {@link Value} -> JCR {@link javax.jcr.Value} converter
 */
public class ValueConverter {

    /**
     * Creates array of {@link javax.jcr.Value} based on {@link Value} list
     * @param modelValues - list of model values
     * @return
     * @throws Exception
     */
    public javax.jcr.Value[] valuesFrom(final List<Value> modelValues, final Session session) throws RepositoryException, IOException {
        final javax.jcr.Value[] jcrValues = new javax.jcr.Value[modelValues.size()];
        for (int i = 0; i < jcrValues.length; i++) {
            jcrValues[i] = valueFrom(modelValues.get(i), session);
        }
        return jcrValues;
    }

    /**
     * Creates {@link javax.jcr.Value} based on {@link Value}
     * @param modelValue - model value
     * @return {@link javax.jcr.Value}
     * @throws Exception
     */
    public javax.jcr.Value valueFrom(final Value modelValue, final Session session) throws RepositoryException, IOException {
        final ValueFactory factory = session.getValueFactory();
        final ValueType type = modelValue.getType();

        switch (type) {
            case STRING:
                return factory.createValue(getStringValue(modelValue));
            case BINARY:
                final Binary binary = factory.createBinary(getBinaryInputStream(modelValue));
                try {
                    return factory.createValue(binary);
                } finally {
                    binary.dispose();
                }
            case LONG:
                return factory.createValue((Long) modelValue.getObject());
            case DOUBLE:
                return factory.createValue((Double) modelValue.getObject());
            case DATE:
                return factory.createValue((Calendar) modelValue.getObject());
            case BOOLEAN:
                return factory.createValue((Boolean) modelValue.getObject());
            case URI:
            case NAME:
            case PATH:
            case REFERENCE:
            case WEAKREFERENCE:
                // REFERENCE and WEAKREFERENCE type values already are resolved to hold a validated uuid
                return factory.createValue(modelValue.getString(), type.ordinal());
            case DECIMAL:
                return factory.createValue((BigDecimal) modelValue.getObject());
            default:
                DefinitionProperty parentProperty = modelValue.getParent();
                final String msg = String.format(
                        "Failed to process property '%s' defined in %s: unsupported value type '%s'.",
                        parentProperty.getPath(), ModelUtils.formatDefinition(parentProperty.getDefinition()), type);
                throw new RuntimeException(msg);
        }
    }

    private String getStringValue(final Value modelValue) throws IOException {
        if (modelValue.isResource()) {
            try (final InputStream inputStream = getResourceInputStream(modelValue)) {
                return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            }
        } else {
            return modelValue.getString();
        }
    }

    private InputStream getBinaryInputStream(final Value modelValue) throws IOException {
        return modelValue.isResource() ? getResourceInputStream(modelValue) : new ByteArrayInputStream((byte[]) modelValue.getObject());
    }

    private InputStream getResourceInputStream(final Source source, final String resourceName) throws IOException {
        return source.getModule().getContentResourceInputProvider().getResourceInputStream(source, resourceName);
    }

    private InputStream getResourceInputStream(final Value modelValue) throws IOException {
        return getResourceInputStream(modelValue.getParent().getDefinition().getSource(), modelValue.getString());
    }
}