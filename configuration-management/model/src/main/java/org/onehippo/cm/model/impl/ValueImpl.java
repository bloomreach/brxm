/*
 *  Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.model.impl;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Calendar;

import org.onehippo.cm.model.DefinitionType;
import org.onehippo.cm.model.Value;
import org.onehippo.cm.model.ValueType;

public class ValueImpl implements Value, Cloneable {

    private final Object value;
    private final ValueType valueType;
    private final boolean isResource;
    private final boolean isPath;
    private DefinitionPropertyImpl parent = null;

    private SourceImpl foreignSource;
    private NamespaceDefinitionImpl namespaceDefinition;

    public ValueImpl(final BigDecimal value) {
        this(value, ValueType.DECIMAL, false, false);
    }

    public ValueImpl(final Boolean value) {
        this(value, ValueType.BOOLEAN, false, false);
    }

    public ValueImpl(final byte[] value) {
        this(value, ValueType.BINARY, false, false);
    }

    public ValueImpl(final Calendar value) {
        this(value, ValueType.DATE, false, false);
    }

    public ValueImpl(final Double value) {
        this(value, ValueType.DOUBLE, false, false);
    }

    public ValueImpl(final Long value) {
        this(value, ValueType.LONG, false, false);
    }

    public ValueImpl(final String value) {
        this(value, ValueType.STRING, false, false);
    }

    public ValueImpl(final URI value) {
        this(value, ValueType.URI, false, false);
    }

    public ValueImpl(final Object value, final ValueType type, final boolean isResource, final boolean isPath) {
        this.value = value;
        this.valueType = type;
        this.isResource = isResource;
        this.isPath = isPath;
    }

    @Override
    public Object getObject() {
        return value;
    }

    @Override
    public String getString() {
        if (isResource) {
            return value.toString();
        }
        if (valueType == ValueType.BINARY) {
            return new String((byte[]) value);
        }
        return value.toString();
    }

    @Override
    public ValueType getType() {
        return valueType;
    }

    @Override
    public boolean isResource() {
        return isResource;
    }

    @Override
    public boolean isPath() {
        return isPath;
    }

    @Override
    public DefinitionPropertyImpl getParent() {
        return parent;
    }

    @Override
    public AbstractDefinitionImpl getDefinition() {
        if (getParent() != null) {
            return getParent().getDefinition();
        }
        else {
            return namespaceDefinition;
        }
    }

    /**
     * To be used only in case of a CND Path value on a NamespaceDefinition, when a normal DefinitionProperty parent
     * doesn't make sense, but we still need a back-reference.
     * @param namespaceDefinition
     */
    public void setDefinition(NamespaceDefinitionImpl namespaceDefinition) {
        this.namespaceDefinition = namespaceDefinition;
    }

    public ValueImpl setParent(DefinitionPropertyImpl parent) {
        this.parent = parent;
        return this;
    }

    /**
     * Set a "foreign" Source for use when this value is a resource belonging to another Module, and
     * we want to delay actual data copying until the ultimate destination can be computed.
     * @param foreignSource a SourceImpl, typically originating from a different Module
     */
    public void setForeignSource(final SourceImpl foreignSource) {
        this.foreignSource = foreignSource;
    }

    @Override
    public InputStream getResourceInputStream() throws IOException {
        if (!isResource()) {
            throw new IllegalStateException("Cannot get an InputStream for a Value that is not a Resource!");
        }

        final AbstractDefinitionImpl definition = getDefinition();

        // If we have a "foreign" source, use that to find the RIP instead of the local Source
        SourceImpl source = definition.getSource();
        if (foreignSource != null) {
            source = foreignSource;
        }

        if (definition.getType() == DefinitionType.CONTENT) {
            return source.getModule().getContentResourceInputProvider().getResourceInputStream(source, getString());
        }
        else {
            return source
                    .getModule()
                    .getConfigResourceInputProvider()
                    .getResourceInputStream(source, getString());
        }
    }

    @Override
    public boolean equals(Object otherObj) {
        if (!(otherObj instanceof ValueImpl)) {
            return false;
        }

        final ValueImpl other = (ValueImpl) otherObj;

        if (isResource && parent != null && other.parent != null) {
            return valueType == other.valueType
                    && other.isResource
                    && isPath == other.isPath
                    && value.equals(other.value)
                    && parent.getDefinition().getSource() == other.parent.getDefinition().getSource();
        } else {
            return valueType == other.valueType
                    && isResource == other.isResource
                    && isPath == other.isPath
                    && value.equals(other.value);
        }
    }

    @Override
    public int hashCode() {
        int result = valueType.ordinal();
        result = 31 * result + (isResource ? 1 : 2);
        result = 31 * result + (isPath ? 1 : 2);
        result = 31 * result + value.hashCode();
        result = 31 * result + (parent == null ? 0 : parent.hashCode());
        return result;
    }

    @Override
    public ValueImpl clone() {
        try {
            return (ValueImpl) super.clone();
        }
        catch (CloneNotSupportedException e) {
            // this should be impossible
            throw new RuntimeException("Exception cloning ValueImpl", e);
        }
    }
}
