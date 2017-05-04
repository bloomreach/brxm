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
package org.onehippo.cm.engine.parser;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.onehippo.cm.api.ResourceInputProvider;
import org.onehippo.cm.api.model.PropertyOperation;
import org.onehippo.cm.api.model.PropertyType;
import org.onehippo.cm.api.model.Value;
import org.onehippo.cm.api.model.ValueType;
import org.onehippo.cm.impl.model.ContentDefinitionImpl;
import org.onehippo.cm.impl.model.DefinitionNodeImpl;
import org.onehippo.cm.impl.model.DefinitionPropertyImpl;
import org.onehippo.cm.impl.model.ModuleImpl;
import org.onehippo.cm.impl.model.ValueImpl;
import org.yaml.snakeyaml.constructor.Construct;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;

import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;
import static org.onehippo.cm.engine.Constants.DEFAULT_EXPLICIT_SEQUENCING;
import static org.onehippo.cm.engine.Constants.OPERATION_KEY;
import static org.onehippo.cm.engine.Constants.PATH_KEY;
import static org.onehippo.cm.engine.Constants.RESOURCE_KEY;
import static org.onehippo.cm.engine.Constants.TYPE_KEY;
import static org.onehippo.cm.engine.Constants.VALUE_KEY;

public abstract class SourceParser extends AbstractBaseParser {

    // After the compose step, SnakeYaml does not yet provide parsed scalar values. An extension of the Constructor
    // class is needed to access the protected Constructor#construct method which uses the built-in parsers for the
    // known basic scalar types. The additional check for the ConstructYamlTimestamp is done as the constructor for
    // timestamp returns a Date by internally constructing a Calendar.
    static final class ScalarConstructor extends Constructor {
        Object constructScalarNode(final ScalarNode node) {
            final Construct constructor = getConstructor(node);
            final Object object = constructor.construct(node);
            if (constructor instanceof ConstructYamlTimestamp) {
                final Calendar result = (Calendar)((ConstructYamlTimestamp)constructor).getCalendar().clone();
                result.setLenient(false);
                return result;
            }
            return object;
        }
    }

    static final ScalarConstructor scalarConstructor = new ScalarConstructor();

    final ResourceInputProvider resourceInputProvider;
    final boolean verifyOnly;

    public SourceParser(final ResourceInputProvider resourceInputProvider) {
        this(resourceInputProvider, false);
    }

    public SourceParser(final ResourceInputProvider resourceInputProvider, boolean verifyOnly) {
        this(resourceInputProvider, verifyOnly, DEFAULT_EXPLICIT_SEQUENCING);
    }

    public SourceParser(final ResourceInputProvider resourceInputProvider, boolean verifyOnly, boolean explicitSequencing) {
        super(explicitSequencing);
        this.resourceInputProvider = resourceInputProvider;
        this.verifyOnly = verifyOnly;
    }

    /**
     * Parses a YAML source file from the given {@link InputStream} and adds a {@link org.onehippo.cm.api.model.Source}
     * to the given {@link ModuleImpl}.
     * @param inputStream  the {@link InputStream} to read the YAML source file from
     * @param relativePath the relative path from the module, used in the model
     * @param location     the location of the file, used for error reporting
     * @param parent       the parent for this source
     * @throws ParserException in case the file could not be parsed correctly
     */
    public void parse(final InputStream inputStream, final String relativePath, final String location, final ModuleImpl parent) throws ParserException {
        final Node node = composeYamlNode(inputStream, location);
        constructSource(relativePath, node, parent);
    }

    protected abstract void constructSource(final String path, final Node src, final ModuleImpl parent) throws ParserException;

    void constructDefinitionNode(final String path, final Node value, final ContentDefinitionImpl definition) throws ParserException {
        final String name = StringUtils.substringAfterLast(path, "/");
        final DefinitionNodeImpl definitionNode = new DefinitionNodeImpl(path, name, definition);
        definition.setNode(definitionNode);
        populateDefinitionNode(definitionNode, value);
    }

    protected abstract void populateDefinitionNode(final DefinitionNodeImpl definitionNode, final Node node) throws ParserException;

    protected void constructDefinitionNode(final String name, final Node value, final DefinitionNodeImpl parent) throws ParserException {
        final DefinitionNodeImpl node = parent.addNode(name);
        populateDefinitionNode(node, value);
    }

    protected void constructDefinitionProperty(final String name, final Node value, final DefinitionNodeImpl parent) throws ParserException {
        if (name.equals(JCR_PRIMARYTYPE)) {
            constructJcrPrimaryTypeProperty(name, value, parent);
        } else if (name.equals(JCR_MIXINTYPES)) {
            constructJcrMixinTypesProperty(name, value, parent);
        } else {
            switch (value.getNodeId()) {
                case scalar:
                    constructDefinitionPropertyFromScalar(name, value, parent);
                    break;
                case sequence:
                    constructDefinitionPropertyFromSequence(name, value, parent);
                    break;
                case mapping:
                    constructDefinitionPropertyFromMap(name, value, ValueType.STRING, parent);
                    break;
                default:
                    throw new ParserException("Property value must be scalar, sequence or map", value);
            }
        }
    }

    private void constructJcrPrimaryTypeProperty(final String name, final Node value, final DefinitionNodeImpl parent) throws ParserException {
        switch (value.getNodeId()) {
            case scalar:
                parent.addProperty(name, constructValueFromScalar(value, ValueType.NAME));
                break;
            case mapping:
                final DefinitionPropertyImpl property = constructDefinitionPropertyFromMap(name, value, ValueType.NAME, parent);
                validateJcrTypePropertyOperations(property,
                        new PropertyOperation[]{PropertyOperation.REPLACE, PropertyOperation.OVERRIDE}, value);
                validateJcrTypePropertyValueType(property, value);
                validateJcrTypePropertyType(property, PropertyType.SINGLE, value);
                break;
            default:
                throw new ParserException("Property value for 'jcr:primaryType' must be scalar or mapping", value);
        }
    }

    private void constructJcrMixinTypesProperty(final String name, final Node value, final DefinitionNodeImpl parent) throws ParserException {
        switch (value.getNodeId()) {
            case sequence:
                parent.addProperty(name, ValueType.NAME, constructValuesFromSequence(value, ValueType.NAME));
                break;
            case mapping:
                final DefinitionPropertyImpl property = constructDefinitionPropertyFromMap(name, value, ValueType.NAME, parent);
                validateJcrTypePropertyOperations(property,
                        new PropertyOperation[]{PropertyOperation.ADD, PropertyOperation.REPLACE, PropertyOperation.OVERRIDE},
                        value);
                validateJcrTypePropertyValueType(property, value);
                validateJcrTypePropertyType(property, PropertyType.LIST, value);
                break;
            default:
                throw new ParserException("Property value for 'jcr:mixinTypes' must be sequence or mapping", value);
        }
    }

    private void validateJcrTypePropertyValueType(final DefinitionPropertyImpl property, final Node node) throws ParserException {
        if (property.getValueType() != ValueType.NAME) {
            throw new ParserException("Property '" + property.getName() + "' must be of type 'name'", node);
        }
    }

    private void validateJcrTypePropertyType(final DefinitionPropertyImpl property, final PropertyType expectedPropertyType, final Node node) throws ParserException {
        if (property.getType() != expectedPropertyType) {
            throw new ParserException("Property '" + property.getName() + "' must be property type '"
                    + expectedPropertyType.toString() + "'", node);
        }
    }

    private void validateJcrTypePropertyOperations(final DefinitionPropertyImpl property, final PropertyOperation[] propertyOperations, final Node node) throws ParserException {
        if (!ArrayUtils.contains(propertyOperations, property.getOperation())) {
            final String supportedOperations = StringUtils.join(propertyOperations, ", ").toLowerCase();
            throw new ParserException("Property '" + property.getName() + "' supports only the following operations: " + supportedOperations, node);
        }
    }

    private void constructDefinitionPropertyFromScalar(final String name, final Node value, final DefinitionNodeImpl parent) throws ParserException {
        parent.addProperty(name, constructValueFromScalar(value));
    }

    private void constructDefinitionPropertyFromSequence(final String name, final Node value, final DefinitionNodeImpl parent) throws ParserException {
        final ValueImpl[] values = constructValuesFromSequence(value);

        if (values.length == 0) {
            parent.addProperty(name, ValueType.STRING, values);
        } else {
            parent.addProperty(name, values[0].getType(), values);
        }
    }

    DefinitionPropertyImpl constructDefinitionPropertyFromMap(final String name, final Node value, final ValueType defaultValueType, final DefinitionNodeImpl parent) throws ParserException {
        final DefinitionPropertyImpl property;
        final Map<String, Node> map = asMapping(value, new String[0],
                new String[]{OPERATION_KEY,TYPE_KEY,VALUE_KEY,RESOURCE_KEY,PATH_KEY});

        int expectedMapSize = 1; // the 'value', 'resource', or 'path' key
        final PropertyOperation operation;
        if (map.keySet().contains(OPERATION_KEY)) {
            operation = constructPropertyOperation(map.get(OPERATION_KEY));
            if (operation == PropertyOperation.DELETE) {
                if (map.size() > 1) {
                    throw new ParserException("Property map cannot contain '"+OPERATION_KEY+": "+PropertyOperation.DELETE.toString()+"' and other keys", value);
                }
                property = parent.addProperty(name, ValueType.STRING, new ValueImpl[0]);
                property.setOperation(operation);
                return property;
            }
            expectedMapSize++;
        } else {
            operation = PropertyOperation.REPLACE;
        }

        final ValueType valueType;
        if (map.keySet().contains(TYPE_KEY)) {
            valueType = constructValueType(map.get(TYPE_KEY));
            expectedMapSize++;
        } else {
            valueType = defaultValueType;
        }

        if (map.size() != expectedMapSize) {
            throw new ParserException(
                    "Property map must have either a '"+VALUE_KEY+"', '"+RESOURCE_KEY+"' or '"+PATH_KEY+"' key",
                    value);
        }

        if (map.containsKey(VALUE_KEY)) {
            property = constructDefinitionPropertyFromValueMap(name, map.get(VALUE_KEY), parent, valueType);
        } else if (map.containsKey(RESOURCE_KEY)) {
            property = constructDefinitionPropertyFromResourceMap(name, map.get(RESOURCE_KEY), parent, valueType);
        } else if (map.containsKey(PATH_KEY)) {
            property = constructDefinitionPropertyFromPathMap(name, map.get(PATH_KEY), parent, valueType);
        } else {
            throw new ParserException(
                    "Property map must have either a '"+VALUE_KEY+"', '"+RESOURCE_KEY+"' or '"+PATH_KEY+"' key",
                    value);
        }

        if (operation == PropertyOperation.ADD && property.getType() == PropertyType.SINGLE) {
            throw new ParserException(
                    "Property map with operation 'add' must have a sequence for '"+VALUE_KEY+"', '"+RESOURCE_KEY+"' or '"+PATH_KEY+"'",
                    value);
        }

        property.setOperation(operation);
        return property;
    }

    private DefinitionPropertyImpl constructDefinitionPropertyFromValueMap(final String name, final Node node, final DefinitionNodeImpl parent, final ValueType valueType) throws ParserException {
        switch (node.getNodeId()) {
            case scalar:
                final ValueImpl propertyValue = constructValueFromScalar(node, valueType);
                return parent.addProperty(name, propertyValue);
            case sequence:
                final ValueImpl[] propertyValues = constructValuesFromSequence(node, valueType);
                return parent.addProperty(name, valueType, propertyValues);
            default:
                throw new ParserException(
                        "Property value in map must be scalar or sequence, found '" + node.getNodeId() + "'", node);
        }
    }

    /**
     * Auto detects {@link ValueType} and parses {@link Value} from scalar <code>node</code>
     * @param node scalar node
     * @return parsed {@link Value}
     * @throws ParserException in case the value cannot be parsed
     */
    private ValueImpl constructValueFromScalar(final Node node) throws ParserException {
        final ScalarNode scalar = asScalar(node);
        try {
            final Object object = scalarConstructor.constructScalarNode(scalar);

            if (Tag.BINARY.equals(scalar.getTag())) {
                return new ValueImpl((byte[]) object);
            }
            if (Tag.BOOL.equals(scalar.getTag())) {
                return new ValueImpl((Boolean) object);
            }
            if (Tag.FLOAT.equals(scalar.getTag())) {
                return new ValueImpl((Double) object);
            }
            if (Tag.INT.equals(scalar.getTag())) {
                if (object instanceof Integer) {
                    return new ValueImpl(((Integer) object).longValue());
                } else if (object instanceof Long) {
                    return new ValueImpl((Long) object);
                } else {
                    throw new ParserException("Value is too big to fit into a long, use a property of type decimal", node);
                }
            }
            if (Tag.STR.equals(scalar.getTag())) {
                return new ValueImpl((String) object);
            }
            if (Tag.TIMESTAMP.equals(scalar.getTag())) {
                return new ValueImpl((Calendar) object);
            }
        } catch (YAMLException e) {
            throw new ParserException("YAML parse exception: " + e.getMessage(), node, e);
        }

        throw new ParserException("Tag not recognized: " + scalar.getTag(), node);
    }

    /**
     * Parses {@link Value} from scalar <code>node</code> and validates it is of the expected type
     * @param node scalar node
     * @param expectedValueType expected {@link ValueType}
     * @return parsed {@link Value}
     * @throws ParserException in case the value cannot be parsed or it is not of the expected type
     */
    ValueImpl constructValueFromScalar(final Node node, final ValueType expectedValueType) throws ParserException {
        switch (expectedValueType) {
            case DECIMAL:
                return constructDecimalValue(node);
            case NAME:
                return constructNameValue(node);
            case PATH:
                return constructPathValue(node);
            case REFERENCE:
                return constructReferenceValue(node);
            case URI:
                return constructUriValue(node);
            case WEAKREFERENCE:
                return constructWeakReferenceValue(node);
            default:
                final ValueImpl value = constructValueFromScalar(node);
                if (value.getType() != expectedValueType) {
                    if (checkForBinaryString(expectedValueType, value)) {
                        return new ValueImpl(new String((byte[])value.getObject()));
                    }
                    throw new ParserException(
                            MessageFormat.format(
                                    "Property value is not of the correct type, expected ''{0}'', found ''{1}''",
                                    expectedValueType,
                                    value.getType()),
                            node);
                }
                return value;
        }
    }

    private boolean checkForBinaryString(ValueType expectedValueType, ValueImpl value) {
        return ValueType.BINARY.equals(value.getType()) && ValueType.STRING.equals(expectedValueType);
    }

    private ValueImpl constructDecimalValue(final Node node) throws ParserException {
        final String string = asStringScalar(node);
        try {
            return new ValueImpl(new BigDecimal(string));
        } catch (NumberFormatException e) {
            throw new ParserException("Could not parse scalar value as BigDecimal: " + string, node);
        }
    }

    private ValueImpl constructNameValue(final Node node) throws ParserException {
        final String name = asStringScalar(node);
        return new ValueImpl(name, ValueType.NAME, false, false);
    }

    private ValueImpl constructPathValue(final Node node) throws ParserException {
        final String path = asStringScalar(node);
        return new ValueImpl(path, ValueType.PATH, false, false);
    }

    private ValueImpl constructReferenceValue(final Node node) throws ParserException {
        final String string = asStringScalar(node);
        try {
            return new ValueImpl(UUID.fromString(string), ValueType.REFERENCE, false, false);
        } catch (IllegalArgumentException e) {
            throw new ParserException("Could not parse scalar value as Reference (UUID): " + string, node);
        }
    }

    private ValueImpl constructUriValue(final Node node) throws ParserException {
        final URI uri = asURIScalar(node);
        return new ValueImpl(uri);
    }

    private ValueImpl constructWeakReferenceValue(final Node node) throws ParserException {
        final String string = asStringScalar(node);
        try {
            return new ValueImpl(UUID.fromString(string), ValueType.WEAKREFERENCE, false, false);
        } catch (IllegalArgumentException e) {
            throw new ParserException("Could not parse scalar value as WeakReference (UUID): " + string, node);
        }
    }

    /**
     * Auto detects {@link ValueType}, parses {@link Value} from each element in sequence <code>node</code>
     * and validates all values are of the same {@link ValueType}
     * @param node sequence node
     * @return parsed {@link Value} array
     * @throws ParserException in case a value cannot be parsed or not all values are of the same {@link ValueType}
     */
    private ValueImpl[] constructValuesFromSequence(final Node node) throws ParserException {
        final List<Node> valueNodes = asSequence(node);
        final ValueImpl[] values = new ValueImpl[valueNodes.size()];
        for (int i = 0; i < valueNodes.size(); i++) {
            values[i] = constructValueFromScalar(valueNodes.get(i));
        }

        if (values.length > 1) {
            final ValueType initialType = values[0].getType();
            for (int i = 1; i < values.length; i++) {
                if (values[i].getType() != initialType) {
                    throw new ParserException(
                            MessageFormat.format(
                                    "Property values must all be of the same type, found value type ''{0}'' as well as ''{1}''",
                                    initialType,
                                    values[i].getType()),
                            node);
                }
            }
        }

        return values;
    }

    /**
     * Parses {@link Value} from each element in sequence <code>node</code> and validates they are of the expected type
     * @param node sequence node
     * @param expectedValueType expected {@link ValueType}
     * @return parsed {@link Value} array
     * @throws ParserException in case a value cannot be parsed or it is not of the expected type
     */
    private ValueImpl[] constructValuesFromSequence(final Node node, final ValueType expectedValueType) throws ParserException {
        final List<Node> valueNodes = asSequence(node);
        final ValueImpl[] values = new ValueImpl[valueNodes.size()];
        for (int i = 0; i < valueNodes.size(); i++) {
            values[i] = constructValueFromScalar(valueNodes.get(i), expectedValueType);
        }
        return values;
    }

    private DefinitionPropertyImpl constructDefinitionPropertyFromPathMap(final String name, final Node node, final DefinitionNodeImpl parent, final ValueType valueType) throws ParserException {
        if (!(valueType == ValueType.REFERENCE || valueType == ValueType.WEAKREFERENCE)) {
            throw new ParserException("Path values can only be used for value type '"+ValueType.REFERENCE.toString()+"' or '"
            		+ValueType.WEAKREFERENCE.toString()+"'", node);
        }
        switch (node.getNodeId()) {
            case scalar:
                final ValueImpl propertyValue = constructPathValueFromScalar(node, valueType);
                return parent.addProperty(name, propertyValue);
            case sequence:
                final ValueImpl[] propertyValues = constructPathValuesFromSequence(node, valueType);
                return parent.addProperty(name, valueType, propertyValues);
            default:
                throw new ParserException(
                        "Path value must be scalar or sequence, found '" + node.getNodeId() + "'", node);
        }
    }

    private ValueImpl constructPathValueFromScalar(final Node node, final ValueType valueType) throws ParserException {
        final String path = asPathScalar(node, false);
        return new ValueImpl(path, valueType, false, true);
    }

    private ValueImpl[] constructPathValuesFromSequence(final Node node, final ValueType valueType) throws ParserException {
        final List<Node> pathNodes = asSequence(node);
        if (pathNodes.size() == 0) {
            throw new ParserException("Path value must define at least one value", node);
        }
        final ValueImpl[] values = new ValueImpl[pathNodes.size()];
        for (int i = 0; i < pathNodes.size(); i++) {
            values[i] = constructPathValueFromScalar(pathNodes.get(i), valueType);
        }
        return values;
    }

    private DefinitionPropertyImpl constructDefinitionPropertyFromResourceMap(final String name, final Node node, final DefinitionNodeImpl parent, final ValueType valueType) throws ParserException {
        if (!(valueType == ValueType.STRING || valueType == ValueType.BINARY)) {
            throw new ParserException("Resource values can only be used for value type '"+ValueType.BINARY.toString()
            		+"' or '"+ValueType.STRING.toString()+"'", node);
        }
        switch (node.getNodeId()) {
            case scalar:
                final ValueImpl propertyValue = constructResourceValueFromScalar(node, valueType, parent);
                return parent.addProperty(name, propertyValue);
            case sequence:
                final ValueImpl[] propertyValues = constructResourceValuesFromSequence(node, valueType, parent);
                return parent.addProperty(name, valueType, propertyValues);
            default:
                throw new ParserException(
                        "Resource value must be scalar or sequence, found '" + node.getNodeId() + "'", node);
        }
    }

    private ValueImpl constructResourceValueFromScalar(final Node node, final ValueType valueType, final DefinitionNodeImpl parent) throws ParserException {
        final String resourcePath = asResourcePathScalar(node, parent.getDefinition().getSource(), resourceInputProvider);
        return new ValueImpl(resourcePath, valueType, true, false);
    }

    private ValueImpl[] constructResourceValuesFromSequence(final Node node, final ValueType valueType, final DefinitionNodeImpl parent) throws ParserException {
        final List<Node> valueNodes = asSequence(node);
        if (valueNodes.size() == 0) {
            throw new ParserException("Resource value must define at least one value", node);
        }
        final ValueImpl[] values = new ValueImpl[valueNodes.size()];
        for (int i = 0; i < valueNodes.size(); i++) {
            values[i] = constructResourceValueFromScalar(valueNodes.get(i), valueType, parent);
        }
        return values;
    }

    private PropertyOperation constructPropertyOperation(final Node node) throws ParserException {
        final String operation = asStringScalar(node);
        try {
            return PropertyOperation.valueOf(operation.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ParserException("Unrecognized property operation: '" + operation + "'", node);
        }
    }

    private ValueType constructValueType(final Node node) throws ParserException {
        final String type = asStringScalar(node);
        try {
            return ValueType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ParserException("Unrecognized value type: '" + type + "'", node);
        }
    }


}
