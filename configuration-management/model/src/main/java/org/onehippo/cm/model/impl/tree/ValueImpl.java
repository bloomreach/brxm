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
package org.onehippo.cm.model.impl.tree;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;

import org.onehippo.cm.model.definition.DefinitionType;
import org.onehippo.cm.model.impl.definition.AbstractDefinitionImpl;
import org.onehippo.cm.model.impl.definition.NamespaceDefinitionImpl;
import org.onehippo.cm.model.impl.source.SourceImpl;
import org.onehippo.cm.model.source.ResourceInputProvider;
import org.onehippo.cm.model.tree.Value;
import org.onehippo.cm.model.tree.ValueFormatException;
import org.onehippo.cm.model.tree.ValueType;

public class ValueImpl implements Value, Cloneable {

    protected Object value;
    protected final ValueType valueType;
    protected boolean isResource;
    protected final boolean isPath;
    protected DefinitionPropertyImpl parent = null;

    /**
     * Retained value resource Source reference after {@link #clone()} cloning) ValueImpl, which is needed when exporting
     * resource values into a different context.
     */
    private SourceImpl resourceSource;
    private String internalResource;
    private boolean isNewResource;
    private boolean isStringResource;

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

    /**
     * @return a String-ified value suitable for debugging -- for a value suitable for storage, use {@link #getString()}
     */
    public String toString() {
        if (isResource) {
            return "resource:" + value.toString();
        }
        if (valueType == ValueType.BINARY) {
            return "(binary)";
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
    public boolean isNewResource() {
        return isNewResource;
    }

    public void setNewResource(final boolean isNewResource) {
        this.isNewResource = isNewResource;
    }

    public void setResourceValue(final String resourceValue) {
        if (isResource) {
            value = resourceValue;
            isNewResource = false;
        } else {
            throw new IllegalStateException("Not allowed to set resource value: not a resource");
        }
    }

    /**
     * Converts a String value to resource value which will provide an InputStream to export the value to a (candidate)
     * resource path.
     * After the convertion the {@link #getString) will return the resourcePath instead, and {@link #getInputStream)
     * a ByteArrayInputStream wrapped around the original String value.
     * Note also that the provided resourcePath parameter is (to be) used as a candidate, e.g. {@link #isNewResource}
     * will return true.
     * @param resourcePath
     */
    public void makeStringResourceValue(final String resourcePath) {
        if (getType() != ValueType.STRING) {
            throw new ValueFormatException("Value is not of type "+ValueType.STRING.toString());
        } else if (isResource) {
            throw new ValueFormatException("Value is already a resource");
        } else {
            isResource = true;
            isNewResource = true;
            isStringResource = true;
            internalResource = (String)value;
            value = resourcePath;
        }
    }
    /**
     * Detaches a {@link #clone() cloned} resource Value from its original Source. Should be called after
     * retrieving and processing (serializing) through {@link #getResourceInputStream()}.
     */
    // todo: this is apparently never called, and it seems like it should be
    public void detach() {
        if (!isStringResource) {
            internalResource = null;
        }
        resourceSource = null;
        isNewResource = false;
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
     * Optional internal resource path for resources requiring a different resource path mapping for accessing their
     * actual data through {@link #getResourceInputStream()} like in case of a JCR backed ResourceInputProvider.
     * @param internalResource
     */
    public void setInternalResourcePath(final String internalResource) {
        if (isStringResource) {
            throw new ValueFormatException("Value does not have an internal resource provider");
        }
        this.internalResource = internalResource;
    }

    // TODO: this should be removed when writing of JCR-backed values is refactored
    public String getInternalResourcePath() {
        if (isStringResource) {
            return null;
        }
        return internalResource;
    }

    // get access to the RIP backing this value -- used to compare src and dest when writing
    public ResourceInputProvider getResourceInputProvider() {
        final AbstractDefinitionImpl definition = getDefinition();

        // If we have a "foreign" source, use that to find the RIP instead of the local Source
        SourceImpl source = (resourceSource != null)? resourceSource: definition.getSource();

        if (definition.getType() == DefinitionType.CONTENT) {
            return source.getModule().getContentResourceInputProvider();
        }
        else {
            return source.getModule().getConfigResourceInputProvider();
        }
    }

    @Override
    public InputStream getResourceInputStream() throws IOException {
        if (!isResource()) {
            throw new IllegalStateException("Cannot get an InputStream for a Value that is not a Resource!");
        }

        if (isStringResource) {
            return new ByteArrayInputStream(internalResource.getBytes(StandardCharsets.UTF_8));
        }

        // If we have a "foreign" source, use that to find the RIP instead of the local Source
        SourceImpl source = (resourceSource != null)? resourceSource: getDefinition().getSource();

        String resourcePath = internalResource != null ? internalResource : getString();

        return getResourceInputProvider().getResourceInputStream(source, resourcePath);
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
            ValueImpl clone = (ValueImpl)super.clone();
            // for resources retain their original Source reference needed to access their inputstream
            if (isResource) {
                clone.resourceSource = getDefinition().getSource();
            }
            return clone;
        }
        catch (CloneNotSupportedException e) {
            // this should be impossible
            throw new RuntimeException("Exception cloning ValueImpl", e);
        }
    }
}
