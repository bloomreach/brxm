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

import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.onehippo.cm.api.model.PropertyOperation;
import org.onehippo.cm.api.model.PropertyType;
import org.onehippo.cm.api.model.Value;
import org.onehippo.cm.api.model.ValueType;
import org.onehippo.cm.impl.model.ConfigDefinitionImpl;
import org.onehippo.cm.impl.model.ContentDefinitionImpl;
import org.onehippo.cm.impl.model.DefinitionNodeImpl;
import org.onehippo.cm.impl.model.DefinitionPropertyImpl;
import org.onehippo.cm.impl.model.ModuleImpl;
import org.onehippo.cm.impl.model.SourceImpl;
import org.onehippo.cm.impl.model.ValueImpl;
import org.yaml.snakeyaml.constructor.Construct;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;

import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;

public class SourceParser extends AbstractBaseParser {

    // After the compose step, SnakeYaml does not yet provide parsed scalar values. An extension of the Constructor
    // class is needed to access the protected Constructor#construct method which uses the built-in parsers for the
    // known basic scalar types. The additional check for the ConstructYamlTimestamp is done as the constructor for
    // timestamp returns a Date by internally constructing a Calendar.
    private static final class ScalarConstructor extends Constructor {
        Object constructScalarNode(final ScalarNode node) {
            final Construct constructor = getConstructor(node);
            final Object object = constructor.construct(node);
            if (constructor instanceof ConstructYamlTimestamp) {
                return ((ConstructYamlTimestamp)constructor).getCalendar().clone();
            }
            return object;
        }
    }

    private static final ScalarConstructor scalarConstructor = new ScalarConstructor();

    private final ResourceInputProvider resourceInputProvider;

    public SourceParser(final ResourceInputProvider resourceInputProvider) {
        this.resourceInputProvider = resourceInputProvider;
    }

    public void parse(final String path, final InputStream inputStream, final ModuleImpl parent) throws ParserException {
        final Node node = composeYamlNode(inputStream);
        constructSource(path, node, parent);
    }

    protected void constructSource(final String path, final Node src, final ModuleImpl parent) throws ParserException {
        final Map<String, Node> sourceMap = asMapping(src, new String[]{"instructions"}, null);
        final SourceImpl source = parent.addSource(path);

        final Map<Node, Node> definitions = asOrderedMap(sourceMap.get("instructions"));
        for (Node definitionKeyNode : definitions.keySet()) {
            final String definitionName = asStringScalar(definitionKeyNode);
            final Node definitionNode = definitions.get(definitionKeyNode);
            switch (definitionName) {
                case "namespace":
                    constructNamespaceDefinitions(definitionNode, source);
                    break;
                case "cnd":
                    constructNodeTypeDefinitions(definitionNode, source);
                    break;
                case "config":
                    constructConfigDefinitions(definitionNode, source);
                    break;
                case "content":
                    constructContentDefinitions(definitionNode, source);
                    break;
                default:
                    throw new ParserException("Unknown instruction type '" + definitionName + "'", definitionKeyNode);
            }
        }
    }

    private void constructNamespaceDefinitions(final Node src, final SourceImpl parent) throws ParserException {
        for (Node node : asSequence(src)) {
            final Map<String, Node> namespaceMap = asMapping(node, new String[]{"prefix", "uri"}, null);
            final String prefix = asStringScalar(namespaceMap.get("prefix"));
            final URI uri = asURIScalar(namespaceMap.get("uri"));
            parent.addNamespaceDefinition(prefix, uri);
        }
    }

    private void constructNodeTypeDefinitions(final Node src, final SourceImpl parent) throws ParserException {
        for (Node node : asSequence(src)) {
            switch (node.getNodeId()) {
                case scalar:
                    final String cndString = asStringScalar(node);
                    parent.addNodeTypeDefinition(cndString, false);
                    break;
                case mapping:
                    final Map<String, Node> map = asMapping(node, new String[]{"resource"}, new String[0]);
                    final String resource = asResourcePathScalar(map.get("resource"), parent, resourceInputProvider);
                    parent.addNodeTypeDefinition(resource, true);
                    break;
                default:
                    throw new ParserException("CND instruction item must be a string or a map with key 'resource'", node);
            }
        }
    }

    private void constructConfigDefinitions(final Node src, final SourceImpl parent) throws ParserException {
        final Map<Node, Node> definitions = asOrderedMap(src);
        for (Node keyNode : definitions.keySet()) {
            final ConfigDefinitionImpl definition = parent.addConfigDefinition();
            final String key = asPathScalar(keyNode, true);
            constructDefinitionNode(key, definitions.get(keyNode), definition);
        }
    }

    private void constructContentDefinitions(final Node src, final SourceImpl parent) throws ParserException {
        final Map<Node, Node> definitions = asOrderedMap(src);
        for (Node keyNode : definitions.keySet()) {
            final ContentDefinitionImpl definition = parent.addContentDefinition();
            final String key = asPathScalar(keyNode, true);
            constructDefinitionNode(key, definitions.get(keyNode), definition);
        }
    }

    private void constructDefinitionNode(final String path, final Node value, final ContentDefinitionImpl definition) throws ParserException {
        final String name = StringUtils.substringAfterLast(path, "/");
        final DefinitionNodeImpl definitionNode = new DefinitionNodeImpl(path, name, definition);
        definition.setNode(definitionNode);
        populateDefinitionNode(definitionNode, value);
    }

    private void populateDefinitionNode(final DefinitionNodeImpl definitionNode, final Node value) throws ParserException {
        final Map<Node, Node> children = asOrderedMap(value);
        for (Node keyNode : children.keySet()) {
            final String key = asStringScalar(keyNode);
            if (key.equals(".meta:delete")) {
                if (children.size() > 1) {
                    throw new ParserException("Node cannot contain '.meta:delete' and other keys", value);
                }
                final boolean delete = asNodeDeleteValue(children.get(keyNode));
                definitionNode.setDelete(delete);
            } else if (key.equals(".meta:order-before")) {
                final String name = asNodeOrderBeforeValue(children.get(keyNode));
                definitionNode.setOrderBefore(name);
            } else if (key.startsWith("/")) {
                final String name = key.substring(1);
                constructDefinitionNode(name, children.get(keyNode), definitionNode);
            } else {
                constructDefinitionProperty(key, children.get(keyNode), definitionNode);
            }
        }
    }

    private boolean asNodeDeleteValue(final Node node) throws ParserException {
        final ScalarNode scalar = asScalar(node);
        final Object object = scalarConstructor.constructScalarNode(scalar);
        if (!object.equals(true)) {
            throw new ParserException("Value for .meta:delete must be boolean value 'true'", node);
        }
        return true;
    }

    private String asNodeOrderBeforeValue(final Node node) throws ParserException {
        final String string = asStringScalar(node);
        if (StringUtils.isEmpty(string)) {
            throw new ParserException("Value for .meta:order-before must be non-empty string", node);
        }
        return string;
    }

    private void constructDefinitionNode(final String name, final Node value, final DefinitionNodeImpl parent) throws ParserException {
        final DefinitionNodeImpl node = parent.addNode(name);
        populateDefinitionNode(node, value);
    }

    private void constructDefinitionProperty(final String name, final Node value, final DefinitionNodeImpl parent) throws ParserException {
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
        final Value[] values = constructValuesFromSequence(value);

        if (values.length == 0) {
            parent.addProperty(name, ValueType.STRING, values);
        } else {
            parent.addProperty(name, values[0].getType(), values);
        }
    }

    private DefinitionPropertyImpl constructDefinitionPropertyFromMap(final String name, final Node value, final ValueType defaultValueType, final DefinitionNodeImpl parent) throws ParserException {
        final DefinitionPropertyImpl property;
        final Map<String, Node> map = asMapping(value, new String[0], new String[]{"operation","type","value","resource","path"});

        int expectedMapSize = 1; // the 'value', 'resource', or 'path' key
        final PropertyOperation operation;
        if (map.keySet().contains("operation")) {
            operation = constructPropertyOperation(map.get("operation"));
            if (operation == PropertyOperation.DELETE) {
                if (map.size() > 1) {
                    throw new ParserException("Property map cannot contain 'operation: delete' and other keys", value);
                }
                property = parent.addProperty(name, ValueType.STRING, new Value[0]);
                property.setOperation(operation);
                return property;
            }
            expectedMapSize++;
        } else {
            operation = PropertyOperation.REPLACE;
        }

        final ValueType valueType;
        if (map.keySet().contains("type")) {
            valueType = constructValueType(map.get("type"));
            expectedMapSize++;
        } else {
            valueType = defaultValueType;
        }

        if (map.size() != expectedMapSize) {
            throw new ParserException(
                    "Property map must have either a 'value', 'resource' or 'path' key",
                    value);
        }

        if (map.keySet().contains("value")) {
            property = constructDefinitionPropertyFromValueMap(name, map.get("value"), parent, valueType);
        } else if (map.keySet().contains("resource")) {
            property = constructDefinitionPropertyFromResourceMap(name, map.get("resource"), parent, valueType);
        } else if (map.keySet().contains("path")) {
            property = constructDefinitionPropertyFromPathMap(name, map.get("path"), parent, valueType);
        } else {
            throw new ParserException(
                    "Property map must have a 'value', 'resource' or 'path' key", value);
        }

        if (operation == PropertyOperation.ADD && property.getType() == PropertyType.SINGLE) {
            throw new ParserException(
                    "Property map with operation 'add' must have a sequence for 'value', 'resource' or 'path'", value);
        }

        property.setOperation(operation);
        return property;
    }

    private DefinitionPropertyImpl constructDefinitionPropertyFromValueMap(final String name, final Node node, final DefinitionNodeImpl parent, final ValueType valueType) throws ParserException {
        switch (node.getNodeId()) {
            case scalar:
                final Value propertyValue = constructValueFromScalar(node, valueType);
                return parent.addProperty(name, propertyValue);
            case sequence:
                final Value[] propertyValues = constructValuesFromSequence(node, valueType);
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
    private Value constructValueFromScalar(final Node node) throws ParserException {
        final ScalarNode scalar = asScalar(node);
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
                return new ValueImpl(((Integer)object).longValue());
            } else if (object instanceof Long) {
                return new ValueImpl((Long)object);
            } else {
                throw new ParserException("Value is too big to fit into a long, use a property of type decimal", node);
            }
        }
        if (Tag.STR.equals(scalar.getTag())) {
            return new ValueImpl((String) object);
        }
        if (Tag.TIMESTAMP.equals(scalar.getTag())) {
            final Calendar calendar = (Calendar) object;
            calendar.setLenient(false);
            return new ValueImpl(calendar);
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
    private Value constructValueFromScalar(final Node node, final ValueType expectedValueType) throws ParserException {
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
                final Value value = constructValueFromScalar(node);
                if (value.getType() != expectedValueType) {
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

    private Value constructDecimalValue(final Node node) throws ParserException {
        final String string = asStringScalar(node);
        try {
            return new ValueImpl(new BigDecimal(string));
        } catch (NumberFormatException e) {
            throw new ParserException("Could not parse scalar value as BigDecimal: " + string, node);
        }
    }

    private Value constructNameValue(final Node node) throws ParserException {
        final String name = asStringScalar(node);
        return new ValueImpl(name, ValueType.NAME, false, false);
    }

    private Value constructPathValue(final Node node) throws ParserException {
        final String path = asStringScalar(node);
        return new ValueImpl(path, ValueType.PATH, false, false);
    }

    private Value constructReferenceValue(final Node node) throws ParserException {
        final String string = asStringScalar(node);
        try {
            return new ValueImpl(UUID.fromString(string), ValueType.REFERENCE, false, false);
        } catch (IllegalArgumentException e) {
            throw new ParserException("Could not parse scalar value as Reference (UUID): " + string, node);
        }
    }

    private Value constructUriValue(final Node node) throws ParserException {
        final URI uri = asURIScalar(node);
        return new ValueImpl(uri);
    }

    private Value constructWeakReferenceValue(final Node node) throws ParserException {
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
    private Value[] constructValuesFromSequence(final Node node) throws ParserException {
        final List<Node> valueNodes = asSequence(node);
        final Value[] values = new Value[valueNodes.size()];
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
    private Value[] constructValuesFromSequence(final Node node, final ValueType expectedValueType) throws ParserException {
        final List<Node> valueNodes = asSequence(node);
        final Value[] values = new Value[valueNodes.size()];
        for (int i = 0; i < valueNodes.size(); i++) {
            values[i] = constructValueFromScalar(valueNodes.get(i), expectedValueType);
        }
        return values;
    }

    private DefinitionPropertyImpl constructDefinitionPropertyFromPathMap(final String name, final Node node, final DefinitionNodeImpl parent, final ValueType valueType) throws ParserException {
        if (!(valueType == ValueType.REFERENCE || valueType == ValueType.WEAKREFERENCE)) {
            throw new ParserException("Path can only be used for value type 'reference' or 'weakreference'", node);
        }
        switch (node.getNodeId()) {
            case scalar:
                final Value propertyValue = constructPathValueFromScalar(node, valueType);
                return parent.addProperty(name, propertyValue);
            case sequence:
                final Value[] propertyValues = constructPathValuesFromSequence(node, valueType);
                return parent.addProperty(name, valueType, propertyValues);
            default:
                throw new ParserException(
                        "Path value must be scalar or sequence, found '" + node.getNodeId() + "'", node);
        }
    }

    private Value constructPathValueFromScalar(final Node node, final ValueType valueType) throws ParserException {
        final String path = asPathScalar(node, false);
        return new ValueImpl(path, valueType, false, true);
    }

    private Value[] constructPathValuesFromSequence(final Node node, final ValueType valueType) throws ParserException {
        final List<Node> pathNodes = asSequence(node);
        if (pathNodes.size() == 0) {
            throw new ParserException("Path value must define at least one value", node);
        }
        final Value[] values = new Value[pathNodes.size()];
        for (int i = 0; i < pathNodes.size(); i++) {
            values[i] = constructPathValueFromScalar(pathNodes.get(i), valueType);
        }
        return values;
    }

    private DefinitionPropertyImpl constructDefinitionPropertyFromResourceMap(final String name, final Node node, final DefinitionNodeImpl parent, final ValueType valueType) throws ParserException {
        if (!(valueType == ValueType.STRING || valueType == ValueType.BINARY)) {
            throw new ParserException("Resource values can only be used for value type 'binary' or 'string'", node);
        }
        switch (node.getNodeId()) {
            case scalar:
                final Value propertyValue = constructResourceValueFromScalar(node, valueType, parent);
                return parent.addProperty(name, propertyValue);
            case sequence:
                final Value[] propertyValues = constructResourceValuesFromSequence(node, valueType, parent);
                return parent.addProperty(name, valueType, propertyValues);
            default:
                throw new ParserException(
                        "Resource value must be scalar or sequence, found '" + node.getNodeId() + "'", node);
        }
    }

    private Value constructResourceValueFromScalar(final Node node, final ValueType valueType, final DefinitionNodeImpl parent) throws ParserException {
        final String resourcePath = asResourcePathScalar(node, parent.getDefinition().getSource(), resourceInputProvider);
        return new ValueImpl(resourcePath, valueType, true, false);
    }

    private Value[] constructResourceValuesFromSequence(final Node node, final ValueType valueType, final DefinitionNodeImpl parent) throws ParserException {
        final List<Node> valueNodes = asSequence(node);
        if (valueNodes.size() == 0) {
            throw new ParserException("Resource value must define at least one value", node);
        }
        final Value[] values = new Value[valueNodes.size()];
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
