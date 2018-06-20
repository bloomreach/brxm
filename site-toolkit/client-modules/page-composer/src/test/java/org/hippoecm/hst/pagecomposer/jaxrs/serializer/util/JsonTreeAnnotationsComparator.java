/*
 *  Copyright 2012-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.pagecomposer.jaxrs.serializer.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;

import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.configuration.channel.HstPropertyDefinition;
import org.hippoecm.hst.core.parameters.HstValueType;
import org.hippoecm.hst.pagecomposer.jaxrs.serializer.AnnotationJsonSerializer;
import org.hippoecm.hst.pagecomposer.jaxrs.serializer.AnnotationType;
import org.hippoecm.hst.pagecomposer.jaxrs.serializer.CouldNotFindHstPropertyDefinition;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

/**
 * A utility class to compare between java Annotations and its JSON serialization in the form of {@link JsonNode}
 * The {@link Annotation} are defined on an interface which extends {@link ChannelInfo}
 */
public final class JsonTreeAnnotationsComparator {

    public static void assertEquivalentHstPropertyDefinitions(JsonNode expected, List<HstPropertyDefinition> actual)
            throws CouldNotFindHstPropertyDefinition, SecurityException, IllegalArgumentException,
            IllegalAccessException, InvocationTargetException {

        assertTrue("Expected array of objects! But I got: '" + getNodeType(expected) + "'", expected.isArray());

        final Iterator<JsonNode> objects = expected.elements();
        while (objects.hasNext()) {
            final JsonNode object = objects.next();
            // Assert that that the current JSON node is an object node
            assertTrue("Expected an object JSON node but got: '" + getNodeType(expected) + "'", object.isObject());
            // Assert that the current JSON node has a textual 'name' field
            final JsonNode nameFieldNode = object.get("name");
            assertTrue("Expectd an object with a textual 'name' field",
                    (nameFieldNode != null && nameFieldNode.isTextual()));

            // Assert that there is an HST property definition with that has the name and structure of the current
            // JSON node
            assertEquivalent(object, getHstPropertyDefinition(nameFieldNode.textValue(), actual));
        }
    }

    public static void assertEquivalent(JsonNode expected, HstPropertyDefinition actual) throws SecurityException,
            IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        // Assert that both the JSON node and the HST property definition are not null
        assertNotNull("Expected a not null JSON node!", expected);
        assertNotNull("Expected a not null HST propert definition!", actual);

        // Again assert that the JSON node is an object node and that it has a textual 'name' field with the same
        // value of the 'name' attribute of the HST property definition, this assertion is useful in case the call
        // is made to this method directly
        assertTrue("Expected an object JSON node, but got: '" + getNodeType(expected) + "'", expected.isObject());
        final JsonNode nameField = expected.get("name");
        assertTrue("Expected an object JSON node with a textual 'name' field!",
                (nameField != null && nameField.isTextual()));

        assertTrue("Expected an object JSON node with a non-empty textual 'name' field!",
                (nameField.textValue() != null && !nameField.textValue().isEmpty()));

        // Asset that the object has the type meta data added by the custom JSON serializer
        final JsonNode classTypeField = expected.get(AnnotationJsonSerializer.TYPE_ATTRIBUTE);
        // Assert that we have an object with a textual AnnotationJsonSerializer.TYPE_ATTRIBUTE field
        assertTrue("Expected an object with a textual '" + AnnotationJsonSerializer.TYPE_ATTRIBUTE + "' field",
                (classTypeField != null && classTypeField.isTextual()));

        // Assert that the AnnotationJsonSerializer.TYPE_ATTRIBUTE field's value is not null or empty
        assertTrue("Expected an object with a non-empty textual '" + AnnotationJsonSerializer.TYPE_ATTRIBUTE + "' field",
                (classTypeField.textValue() != null && !classTypeField.textValue().isEmpty()));

        // Assert that the class type of the HST property definition is the same as the value of the
        // AnnotationJsonSerializer.TYPE_ATTRIBUTE field
        assertTrue("Hst property definition class type mis-match: expected '" + classTypeField.textValue()
                + "', atcual: '" + actual.getClass().getName() + "'",
                actual.getClass().getName().equals(classTypeField.textValue()));

        // Assert that the expected JSON node has a field of name 'required' and it has the same value as of the
        // 'isRequired' attribute of the actual HST property definition
        final JsonNode requiredField = expected.get("required");
        assertTrue("Expected an object with a boolean 'required' field",
                (requiredField != null && requiredField.isBoolean()));

        assertTrue("Expected an hst property with 'required' equals '" + requiredField.booleanValue()
                + "', actual is '" + actual.isRequired() + "'",
                Boolean.valueOf(actual.isRequired()).equals(requiredField.booleanValue()));

        // Assert that the expected JSON node has a textual 'defaultValue' field which might have an empty value
        final JsonNode defaultValueField = expected.get("defaultValue");
        assertTrue("Expected an object with a textual 'defaultValue' field",
                (defaultValueField != null && defaultValueField.isTextual()));

        // Assert that the expected JSON node has a textual 'valueType' field which must have one of the values
        // defined by HstValueType enumeration
        final JsonNode valueTypeField = expected.get("valueType");
        assertTrue("Expected an object with a textual 'valueType' field",
                (valueTypeField != null && valueTypeField.isTextual()));

        assertTrue("Expected an object with a textual non-empty 'valueType' field with serialized value of '"
                + valueTypeField.textValue() + "', but I got : '" + actual.getValueType() + "'", actual
                .getValueType().name().equals(valueTypeField.textValue()));

        // Assert that that serialized value of 'valueType' is a valid HstValue enumeration value
        boolean isHstValueType = true;

        try {
            HstValueType.valueOf(valueTypeField.textValue());
        } catch (Throwable thrble) {
            isHstValueType = false;
        }

        assertTrue(
                "Expected an object with a textual non-empty 'valueType' field with a valid HstValueType value but got: '"
                        + valueTypeField.textValue() + "'", isHstValueType);

        // Assert that the JSON node has a valid 'annotations' field and that it is equivalent in value and structure
        // with the annotations of the HST property definition
        final JsonNode annotationsField = expected.get("annotations");
        assertEquivalentAnnoations(annotationsField, actual.getAnnotations());
    }

    public static void assertEquivalentAnnoations(JsonNode expected, List<Annotation> actual) throws SecurityException,
            IllegalArgumentException, IllegalAccessException, InvocationTargetException {

        // expected should be an array JSON node and it should consist of 2 elements
        // the 1st element should be a textual JSON node having the value of the concrete class name which implements
        // java.util.List
        // the 2nd node should be an array JSON node which consists of elements same in number as in actual
        // also for each element in that array JSON node there should be an equivalent annotation in actual
        // Just to make the code safe I will assume that these two elements of expected shouldn't always be in the same
        // order

        // Assert that both the JSON node and the list of annotations are not null
        assertNotNull("Expected not null JSON node", expected);
        assertNotNull("Expected not null list of annotations", actual);

        // Assert that the JSON node is an Array node and that it consists of two elements
        assertTrue("Expected an array JSON node with 2 elements but got a node of type: '" + getNodeType(expected)
                + "' with " + expected.size() + " elements", (expected.isArray() && expected.size() == 2));

        final JsonNode listClassNameNode;
        final JsonNode annotationsArryNode;
        if (expected.get(0).isTextual()) {
            listClassNameNode = expected.get(0);
            annotationsArryNode = expected.get(1);
        } else {
            listClassNameNode = expected.get(1);
            annotationsArryNode = expected.get(0);
        }

        // Assert that listClassNamdeNode is a textual JSON node with the value of a concrete
        // class name of a type that implements java.util.List
        assertTrue("Expected a not null JSON node!", listClassNameNode != null);
        assertTrue("Expected to get a JSON textual node but got a '" + getNodeType(listClassNameNode) + "' node!",
                listClassNameNode.isTextual());

        // Assert that the class name text value is not an empty string value
        assertTrue("Expected a non-empty string value!", !listClassNameNode.textValue().isEmpty());
        // Assert that the concrete class which implementing actual is the same as the class name serialized in expected
        assertTrue(
                "Expected a class of type '" + actual.getClass().getName() + "', but got '"
                        + listClassNameNode.textValue() + "'",
                actual.getClass().getName().equals(listClassNameNode.textValue()));

        // Assert that annotationsArrayNode is an array JSON node and that it has the same number of
        // elements as in actual for each annotation in actual assert that it has the correct JSON serialization
        assertTrue("Expected an array JSON node but got '" + getNodeType(annotationsArryNode) + "'",
                annotationsArryNode.isArray());

        assertTrue("Expected an array JSON node of size " + actual.size() + " elemements but got "
                + annotationsArryNode.size() + " elements", actual.size() == annotationsArryNode.size());

        // Assert that for each annotation in actual there is an equivalent JSON node in annotationsArrayNode
        final List<Annotation> equivalentFoundAnnotations = new ArrayList<Annotation>(actual.size());
        final Iterator<JsonNode> annotationNodes = annotationsArryNode.elements();

        while (annotationNodes.hasNext()) {
            boolean foundAnEquivalentAnnotation = false;
            final JsonNode annotationNode = annotationNodes.next();
            // Assert that for each annotation JSON node there is an equivalent annotation action
            // notice that we pass the whole annotations list to cover the case that annotation might not always be
            // serialized in the same order
            for (Annotation annotation : actual) {
                if (!equivalentFoundAnnotations.contains(annotation)) {
                    // Assert that JSON node is an object JSON node
                    assertTrue("Expected an object JSON node but got '" + getNodeType(annotationNode) + "' JSON node!",
                            annotationNode.isObject());

                    // Assert that expected JSON node has a AnnotationJsonSerializer#TYPE_ATTRIBUTE} textual field which
                    // is not null, not empty and has a value equals to the class name of actual
                    final JsonNode annotationTypeNode = annotationNode.get(AnnotationJsonSerializer.TYPE_ATTRIBUTE);
                    assertNotNull("Expected a '" + AnnotationJsonSerializer.TYPE_ATTRIBUTE + "' field!", annotationTypeNode);
                    assertTrue("Expected a textual '" + AnnotationJsonSerializer.TYPE_ATTRIBUTE + "' field but got '" + getNodeType(annotationTypeNode)
                            + "' field", annotationTypeNode.isTextual());

                    assertTrue("Expected a textual '" + AnnotationJsonSerializer.TYPE_ATTRIBUTE + "' field with a non-empty value", !annotationTypeNode
                            .textValue().isEmpty());

                    if (AnnotationType.fromClass(annotation.annotationType()).toString().equals(annotationTypeNode.textValue())) {
                        assertEquivalent(annotationNode, annotation);
                        foundAnEquivalentAnnotation = true;
                        equivalentFoundAnnotations.add(annotation);
                        break;
                    }

                }
            }

            // Check whether we found an equivalent annotation or not, if no fail the test with a proper message
            if (!foundAnEquivalentAnnotation) {
                fail("Could not find an equivalent annotation for '" + annotationNode + "'");
            }
        }
    }

    public static void assertEquivalent(JsonNode expected, Annotation actual) throws SecurityException,
            IllegalArgumentException, IllegalAccessException, InvocationTargetException {

        // NOTE: The few initial assertions here are the same as some in assertEquivalentAnnoations(JsonNode, List<Annotation>)
        // thats because this method can be called directly without going through the former one and hence these
        // assertions needs to be done

        // Assert that neither expected nor actual are null
        assertNotNull("Expected a non null JSON node!", expected);
        assertNotNull("Expected a non null annotations list!", actual);
        // Assert that JSON node is an object JSON node
        assertTrue("Expected an object JSON node but got '" + getNodeType(expected) + "' JSON node!",
                expected.isObject());

        // Assert that expected JSON node has a AnnotationJsonSerializer.TYPE_ATTRIBUTE textual field which is not null,
        // not empty and has a value equals to the class name of actual
        final JsonNode annotationClassNameNode = expected.get(AnnotationJsonSerializer.TYPE_ATTRIBUTE);
        assertNotNull("Expected a '" + AnnotationJsonSerializer.TYPE_ATTRIBUTE + "' field!", annotationClassNameNode);
        assertTrue("Expected a textual '" + AnnotationJsonSerializer.TYPE_ATTRIBUTE + "' field but got '" + getNodeType(annotationClassNameNode) + "' field",
                annotationClassNameNode.isTextual());

        assertTrue("Expected a textual '" + AnnotationJsonSerializer.TYPE_ATTRIBUTE + "' field with a non-empty value", !annotationClassNameNode.textValue()
                .isEmpty());

        assertTrue("Expected a text value of '" + actual.getClass().getName() + "', but got '"
                + annotationClassNameNode.textValue() + "'",
                AnnotationType.fromClass(actual.annotationType()).toString().equals(annotationClassNameNode.textValue()));

        // Assert that for each field defined for expected object JSON node has an equivalent attribute/property
        // defined on actual annotation
        final Iterator<Entry<String, JsonNode>> jsonFieldNodes = expected.fields();
        while (jsonFieldNodes.hasNext()) {
            Method annotationAttribute;
            final Entry<String, JsonNode> jsonFieldNode = jsonFieldNodes.next();

            // Skip the class type field as it has been tested before
            if (jsonFieldNode.getKey().equals(AnnotationJsonSerializer.TYPE_ATTRIBUTE)) {
                continue;
            }

            try {
                annotationAttribute = actual.getClass().getDeclaredMethod(jsonFieldNode.getKey(), new Class<?>[] {});
                // Assert that the values of both the JSON field and the annotation attribute are equivalent
                assertEquivalent(jsonFieldNode.getValue(), annotationAttribute.getReturnType(),
                        annotationAttribute.invoke(actual, new Object[] {}));

            } catch (NoSuchMethodException nsme) {
                fail("Could not find an equivalent field of name '" + jsonFieldNode.getKey() + "' for '"
                        + jsonFieldNode.getValue() + "' when compared to annotation '" + actual.getClass().getName()
                        + "'");
            }
        }
    }

    public static void assertEquivalent(JsonNode expected, Class<?> actualType, Object actualValue) {
        // Assert that neither expected JSON node nor actual type nor actual value are null
        assertNotNull("Expected non null JSON node", expected);
        assertNotNull("Expected non null method return type", actualType);
        assertNotNull("Expected non null method return value", actualType);

        // Assert that values are equivalent in structure, type and value
        if (actualType == byte.class || actualType == Byte.class) {
            assertTrue("Expected an integral valued JSON node but got " + getNodeType(expected), expected.isInt());
            assertEquals("Expected " + expected.numberValue().byteValue() + " but got " + (Byte) actualValue,
                    Byte.valueOf(expected.numberValue().byteValue()), (Byte) actualValue);

        } else if (actualType == short.class || actualType == Short.class) {
            assertTrue("Expected an integral valued JSON node but got " + getNodeType(expected), expected.isInt());
            assertEquals("Exepcted " + expected.numberValue().shortValue() + " but got " + (Short) actualValue,
                    Short.valueOf(expected.numberValue().shortValue()), (Short) actualValue);

        } else if (actualType == int.class || actualType == Integer.class) {
            assertTrue("Expected an integral valued JSON node but got " + getNodeType(expected), expected.isInt());
            assertEquals("Expected " + expected.numberValue().intValue() + " but got " + (Integer) actualValue,
                    Integer.valueOf(expected.numberValue().intValue()), (Integer) actualValue);

        } else if (actualType == long.class || actualType == Long.class) {
            // Notice the oring with the expected.isInt, reason is Jackson when writing numerical integer values
            // according to the values it writes some long values as int(s)
            assertTrue("Expected a long valued JSON node but got " + getNodeType(expected), expected.isLong() || expected.isInt());
            assertEquals("Expected " + expected.numberValue().longValue() + " but got " + (Long) actualValue,
                    Long.valueOf(expected.numberValue().longValue()), (Long) actualValue);

        } else if (actualType == float.class || actualType == Float.class) {
            assertTrue("Expected a float valued JSON node but got " + getNodeType(expected),
                    expected.isFloatingPointNumber());
            assertEquals("Expected " + expected.numberValue().floatValue() + " but got " + (Float) actualValue,
                    Float.valueOf(expected.numberValue().floatValue()), (Float) actualValue);

        } else if (actualType == double.class || actualType == Double.class) {
            assertTrue("Expected a float valued JSON node but got " + getNodeType(expected), expected.isDouble());
            assertEquals("Expected " + expected.numberValue().doubleValue() + " but got " + (Double) actualValue,
                    Double.valueOf(expected.numberValue().doubleValue()), (Double) actualValue);

        } else if (actualType == boolean.class || actualType == Boolean.class) {
            assertTrue("Expected a boolean valued JSON node but got " + getNodeType(expected), expected.isBoolean());
            assertEquals("Expected " + expected.booleanValue() + " but got " + (Boolean) actualValue,
                    Boolean.valueOf(expected.booleanValue()), (Boolean) actualValue);

        } else if (actualType == char.class || actualType == Character.class) {
            assertTrue("Expected a textua valued JSON node but got " + getNodeType(expected), expected.isTextual());
            assertEquals("Expected '" + expected.textValue() + "' but got '" + (Character) actualValue + "'",
                    Character.valueOf(expected.textValue().charAt(0)), (Character) actualValue);

        } else if (actualType == String.class) {
            assertTrue("Expected a textua valued JSON node but got " + getNodeType(expected), expected.isTextual());
            assertEquals("Expected '" + expected.textValue() + " but got " + (String) actualValue,
                    expected.textValue(), (String) actualValue);

        } else if (actualType == byte[].class) {
            assertEquivalent(expected, (byte[]) actualValue);
        } else if (actualType == Byte[].class) {
            assertEquivalent(expected, (Byte[]) actualValue);
        } else if (actualType == short[].class) {
            assertEquivalent(expected, (short[]) actualValue);
        } else if (actualType == Short[].class) {
            assertEquivalent(expected, (Short[]) actualValue);
        } else if (actualType == int[].class) {
            assertEquivalent(expected, (int[]) actualValue);
        } else if (actualType == Integer[].class) {
            assertEquivalent(expected, (Integer[]) actualValue);
        } else if (actualType == long[].class) {
            assertEquivalent(expected, (long[]) actualValue);
        } else if (actualType == Long[].class) {
            assertEquivalent(expected, (Long[]) actualValue);
        } else if (actualType == float[].class) {
            assertEquivalent(expected, (float[]) actualValue);
        } else if (actualType == Float[].class) {
            assertEquivalent(expected, (Float[]) actualValue);
        } else if (actualType == double[].class) {
            assertEquivalent(expected, (double[]) actualValue);
        } else if (actualType == Double[].class) {
            assertEquivalent(expected, (Double[]) actualValue);
        } else if (actualType == boolean[].class) {
            assertEquivalent(toBooleanPrimitiveArray(expected), (boolean[]) actualValue);
        } else if (actualType == Boolean[].class) {
            assertEquivalent(toBooleanArray(expected), (Boolean[]) actualValue);
        } else if (actualType == char[].class) {
            assertEquivalent(expected, (char[]) actualValue);
        } else if (actualType == Character[].class) {
            assertEquivalent(expected, (Character[]) actualValue);
        } else if (actualType == String[].class) {
            assertEquivalent(expected, (String[]) actualValue);
        } else {
            throw new IllegalArgumentException("Unrecognized annotation attribute value type '" + actualType.getName()
                    + "'");
        }
    }

    public static void assertEquivalent(JsonNode expected, byte[] actual) {
        // I am passing length cause in Java primitive types arrays are not type compatible with their
        // classes counterparts arrays
        applyCommonArrayAssertions(expected, actual, actual.length);

        // Assert that for each array JSON node element there is an equivalent value in actual
        final Iterator<JsonNode> elementJsonNodes = expected.elements();
        while (elementJsonNodes.hasNext()) {
            final JsonNode elementJsonNode = elementJsonNodes.next();
            assertTrue("Expected a textual JSON node but got " + getNodeType(elementJsonNode), elementJsonNode.isInt());

            assertTrue("Could not find an equivalent value of '" + expected.textValue() + "'",
                    Arrays.binarySearch(actual, elementJsonNode.numberValue().byteValue()) >= 0);
        }
    }

    public static void assertEquivalent(JsonNode expected, Byte[] actual) {
        // I am passing length cause in Java primitive types arrays are not type compatible with their
        // classes counterparts arrays
        applyCommonArrayAssertions(expected, actual, actual.length);

        // Assert that for each array JSON node element there is an equivalent value in actual
        final Iterator<JsonNode> elementJsonNodes = expected.elements();
        while (elementJsonNodes.hasNext()) {
            final JsonNode elementJsonNode = elementJsonNodes.next();
            assertTrue("Expected a textual JSON node but got " + getNodeType(elementJsonNode), elementJsonNode.isInt());

            assertTrue("Could not find an equivalent value of '" + expected.textValue() + "'",
                    Arrays.binarySearch(actual, Byte.valueOf(elementJsonNode.numberValue().byteValue())) >= 0);
        }
    }

    public static void assertEquivalent(JsonNode expected, short[] actual) {
        // I am passing length cause in Java primitive types arrays are not type compatible with their
        // classes counterparts arrays
        applyCommonArrayAssertions(expected, actual, actual.length);

        // Assert that for each array JSON node element there is an equivalent value in actual
        final Iterator<JsonNode> elementJsonNodes = expected.elements();
        while (elementJsonNodes.hasNext()) {
            final JsonNode elementJsonNode = elementJsonNodes.next();
            assertTrue("Expected a textual JSON node but got " + getNodeType(elementJsonNode), elementJsonNode.isInt());

            assertTrue("Could not find an equivalent value of '" + expected.textValue() + "'",
                    Arrays.binarySearch(actual, elementJsonNode.numberValue().shortValue()) >= 0);
        }
    }

    public static void assertEquivalent(JsonNode expected, Short[] actual) {
        // I am passing length cause in Java primitive types arrays are not type compatible with their
        // classes counterparts arrays
        applyCommonArrayAssertions(expected, actual, actual.length);

        // Assert that for each array JSON node element there is an equivalent value in actual
        final Iterator<JsonNode> elementJsonNodes = expected.elements();
        while (elementJsonNodes.hasNext()) {
            final JsonNode elementJsonNode = elementJsonNodes.next();
            assertTrue("Expected a textual JSON node but got " + getNodeType(elementJsonNode), elementJsonNode.isInt());

            assertTrue("Could not find an equivalent value of '" + expected.textValue() + "'",
                    Arrays.binarySearch(actual, Short.valueOf(elementJsonNode.numberValue().shortValue())) >= 0);
        }
    }

    public static void assertEquivalent(JsonNode expected, int[] actual) {
        // I am passing length cause in Java primitive types arrays are not type compatible with their
        // classes counterparts arrays
        applyCommonArrayAssertions(expected, actual, actual.length);

        // Assert that for each array JSON node element there is an equivalent value in actual
        final Iterator<JsonNode> elementJsonNodes = expected.elements();
        while (elementJsonNodes.hasNext()) {
            final JsonNode elementJsonNode = elementJsonNodes.next();
            assertTrue("Expected a textual JSON node but got " + getNodeType(elementJsonNode), elementJsonNode.isInt());

            assertTrue("Could not find an equivalent value of '" + expected.textValue() + "'",
                    Arrays.binarySearch(actual, elementJsonNode.numberValue().intValue()) >= 0);
        }
    }

    public static void assertEquivalent(JsonNode expected, Integer[] actual) {
        // I am passing length cause in Java primitive types arrays are not type compatible with their
        // classes counterparts arrays
        applyCommonArrayAssertions(expected, actual, actual.length);

        // Assert that for each array JSON node element there is an equivalent value in actual
        final Iterator<JsonNode> elementJsonNodes = expected.elements();
        while (elementJsonNodes.hasNext()) {
            final JsonNode elementJsonNode = elementJsonNodes.next();
            assertTrue("Expected a textual JSON node but got " + getNodeType(elementJsonNode), elementJsonNode.isInt());

            assertTrue("Could not find an equivalent value of '" + expected.textValue() + "'",
                    Arrays.binarySearch(actual, Integer.valueOf(elementJsonNode.numberValue().intValue())) >= 0);
        }
    }

    public static void assertEquivalent(JsonNode expected, long[] actual) {
        // I am passing length cause in Java primitive types arrays are not type compatible with their
        // classes counterparts arrays
        applyCommonArrayAssertions(expected, actual, actual.length);

        // Assert that for each array JSON node element there is an equivalent value in actual
        final Iterator<JsonNode> elementJsonNodes = expected.elements();
        while (elementJsonNodes.hasNext()) {
            final JsonNode elementJsonNode = elementJsonNodes.next();
            // Notice the oring with the expected.isInt, reason is Jackson when writing numerical integer values
            // according to the values it writes some long values as int(s)
            assertTrue("Expected a textual JSON node but got " + getNodeType(elementJsonNode), elementJsonNode.isLong() || elementJsonNode.isInt());

            assertTrue("Could not find an equivalent value of '" + expected.textValue() + "'",
                    Arrays.binarySearch(actual, elementJsonNode.numberValue().longValue()) >= 0);
        }
    }

    public static void assertEquivalent(JsonNode expected, Long[] actual) {
        // I am passing length cause in Java primitive types arrays are not type compatible with their
        // classes counterparts arrays
        applyCommonArrayAssertions(expected, actual, actual.length);

        // Assert that for each array JSON node element there is an equivalent value in actual
        final Iterator<JsonNode> elementJsonNodes = expected.elements();
        while (elementJsonNodes.hasNext()) {
            final JsonNode elementJsonNode = elementJsonNodes.next();
            assertTrue("Expected a textual JSON node but got " + getNodeType(elementJsonNode), elementJsonNode.isLong());

            assertTrue("Could not find an equivalent value of '" + expected.textValue() + "'",
                    Arrays.binarySearch(actual, Long.valueOf(elementJsonNode.numberValue().longValue())) >= 0);
        }
    }

    public static void assertEquivalent(JsonNode expected, float[] actual) {
        // I am passing length cause in Java primitive types arrays are not type compatible with their
        // classes counterparts arrays
        applyCommonArrayAssertions(expected, actual, actual.length);

        // Assert that for each array JSON node element there is an equivalent value in actual
        final Iterator<JsonNode> elementJsonNodes = expected.elements();
        while (elementJsonNodes.hasNext()) {
            final JsonNode elementJsonNode = elementJsonNodes.next();
            assertTrue("Expected a textual JSON node but got " + getNodeType(elementJsonNode),
                    elementJsonNode.isFloatingPointNumber());

            assertTrue("Could not find an equivalent value of '" + expected.textValue() + "'",
                    Arrays.binarySearch(actual, elementJsonNode.numberValue().floatValue()) >= 0);
        }
    }

    public static void assertEquivalent(JsonNode expected, Float[] actual) {
        // I am passing length cause in Java primitive types arrays are not type compatible with their
        // classes counterparts arrays
        applyCommonArrayAssertions(expected, actual, actual.length);

        // Assert that for each array JSON node element there is an equivalent value in actual
        final Iterator<JsonNode> elementJsonNodes = expected.elements();
        while (elementJsonNodes.hasNext()) {
            final JsonNode elementJsonNode = elementJsonNodes.next();
            assertTrue("Expected a textual JSON node but got " + getNodeType(elementJsonNode),
                    elementJsonNode.isFloatingPointNumber());

            assertTrue("Could not find an equivalent value of '" + expected.textValue() + "'",
                    Arrays.binarySearch(actual, Float.valueOf(elementJsonNode.numberValue().floatValue())) >= 0);
        }
    }

    public static void assertEquivalent(JsonNode expected, double[] actual) {
        // I am passing length cause in Java primitive types arrays are not type compatible with their
        // classes counterparts arrays
        applyCommonArrayAssertions(expected, actual, actual.length);

        // Assert that for each array JSON node element there is an equivalent value in actual
        final Iterator<JsonNode> elementJsonNodes = expected.elements();
        while (elementJsonNodes.hasNext()) {
            final JsonNode elementJsonNode = elementJsonNodes.next();
            assertTrue("Expected a textual JSON node but got " + getNodeType(elementJsonNode),
                    elementJsonNode.isDouble());

            assertTrue("Could not find an equivalent value of '" + expected.textValue() + "'",
                    Arrays.binarySearch(actual, elementJsonNode.numberValue().doubleValue()) >= 0);
        }
    }

    public static void assertEquivalent(JsonNode expected, Double[] actual) {
        // I am passing length cause in Java primitive types arrays are not type compatible with their
        // classes counterparts arrays
        applyCommonArrayAssertions(expected, actual, actual.length);

        // Assert that for each array JSON node element there is an equivalent value in actual
        final Iterator<JsonNode> elementJsonNodes = expected.elements();
        while (elementJsonNodes.hasNext()) {
            final JsonNode elementJsonNode = elementJsonNodes.next();
            assertTrue("Expected a textual JSON node but got " + getNodeType(elementJsonNode),
                    elementJsonNode.isDouble());

            assertTrue("Could not find an equivalent value of '" + expected.textValue() + "'",
                    Arrays.binarySearch(actual, Double.valueOf(elementJsonNode.numberValue().doubleValue())) >= 0);
        }
    }

    public static void assertEquivalent(boolean[] expected, boolean[] actual) {
        applyCommonArrayAssertions(expected, actual);

        assertTrue("Expected two equivalent boolean arrays!", Arrays.equals(actual, expected));
    }

    public static void assertEquivalent(Boolean[] expected, Boolean[] actual) {
        applyCommonArrayAssertions(expected, actual);

        assertTrue("Expected two equivalent boolean arrays!", actual.equals(expected));
    }

    public static void assertEquivalent(JsonNode expected, char[] actual) {
        // I am passing length cause in Java primitive types arrays are not type compatible with their
        // classes counterparts arrays
        applyCommonArrayAssertions(expected, actual, actual.length);

        // Assert that for each array JSON node element there is an equivalent value in actual
        final Iterator<JsonNode> elementJsonNodes = expected.elements();
        while (elementJsonNodes.hasNext()) {
            final JsonNode elementJsonNode = elementJsonNodes.next();
            assertTrue("Expected a textual JSON node but got " + getNodeType(elementJsonNode),
                    elementJsonNode.isTextual());

            assertTrue("Could not find an equivalent value of '" + expected.textValue() + "'",
                    Arrays.binarySearch(actual, elementJsonNode.textValue().toCharArray()[0]) >= 0);
        }
    }

    public static void assertEquivalent(JsonNode expected, Character[] actual) {
        // I am passing length cause in Java primitive types arrays are not type compatible with their
        // classes counterparts arrays
        applyCommonArrayAssertions(expected, actual, actual.length);

        // Assert that for each array JSON node element there is an equivalent value in actual
        final Iterator<JsonNode> elementJsonNodes = expected.elements();
        while (elementJsonNodes.hasNext()) {
            final JsonNode elementJsonNode = elementJsonNodes.next();
            assertTrue("Expected a textual JSON node but got " + getNodeType(elementJsonNode),
                    elementJsonNode.isTextual());

            assertTrue(
                    "Could not find an equivalent value of '" + expected.textValue() + "'",
                    Arrays.binarySearch(actual, Character.valueOf(elementJsonNode.textValue().toCharArray()[0])) >= 0);
        }
    }

    public static void assertEquivalent(JsonNode expected, String[] actual) {
        // I am passing length cause in Java primitive types arrays are not type compatible with their
        // classes counterparts arrays
        applyCommonArrayAssertions(expected, actual, actual.length);

        // Make a sorted copy of actual preparing for comparison
        String[] sortedActual = Arrays.copyOf(actual, actual.length);
        Arrays.sort(sortedActual);
        // Assert that for each array JSON node element there is an equivalent value in actual
        final Iterator<JsonNode> elementJsonNodes = expected.elements();
        while (elementJsonNodes.hasNext()) {
            final JsonNode elementJsonNode = elementJsonNodes.next();
            assertTrue("Expected a textual JSON node but got " + getNodeType(elementJsonNode),
                    elementJsonNode.isTextual());

            assertTrue("Could not find an equivalent value of '" + expected.textValue() + "'",
                    Arrays.binarySearch(sortedActual, elementJsonNode.textValue()) >= 0);
        }
    }

    private static void applyCommonArrayAssertions(JsonNode expected, Object actual, int actualLength) {
        // Assert that neither expected nor actual are null
        assertNotNull("Expected a non null JSON node", expected);
        assertNotNull("Expected a non null value to compare with", actual);
        // Assert that expected JSON node is an array node
        assertTrue("Expected an array JSON node but got " + getNodeType(expected), expected.isArray());
        // Assert that both expected and actual have the same length/size
        assertEquals(
                "Expected that both expected and actual have the same lenght/size but expected: " + expected.size()
                        + ", actual: " + actualLength, expected.size(), actualLength);
    }

    private static void applyCommonArrayAssertions(boolean[] expected, boolean[] actual) {
        // Assert that neither expected nor actual are null
        assertNotNull("Expected a non null expected value", expected);
        assertNotNull("Expected a non null value to compare with", actual);
    }

    private static void applyCommonArrayAssertions(Boolean[] expected, Boolean[] actual) {
        // Assert that neither expected nor actual are null
        assertNotNull("Expected a non null expected value", expected);
        assertNotNull("Expected a non null value to compare with", actual);
    }

    private static String getNodeType(final JsonNode jsonNode) {
        if (jsonNode == null) {
            throw new IllegalArgumentException("Null JSON nodes are not allowed!");
        }

        String nodeType = "NODE_TYPE_NOT_DETECTED";

        if (jsonNode.isArray()) {
            nodeType = "ARRAY_JSON_NODE";
        } else if (jsonNode.isBoolean()) {
            nodeType = "BOOLEAN_JSON_NODE";
        } else if (jsonNode.isDouble()) {
            nodeType = "DOUBLE_JSON_NODE";
        } else if (jsonNode.isFloatingPointNumber()) {
            nodeType = "FLOAT_JSON_NODE";
        } else if (jsonNode.isInt() || jsonNode.isIntegralNumber()) {
            nodeType = "INTEGER_JSON_NODE";
        } else if (jsonNode.isLong()) {
            nodeType = "LONG_JSON_NODE";
        } else if (jsonNode.isNumber()) {
            nodeType = "NUMBER_JSON_NODE";
        } else if (jsonNode.isObject()) {
            nodeType = "OBJECT_JSON_NODE";
        } else if (jsonNode.isTextual()) {
            nodeType = "TEXTUAL_JSON_NODE";
        } else if (jsonNode.isValueNode()) {
            nodeType = "VALUE_JSON_NODE";
        } else if (jsonNode.isBigDecimal()) {
            nodeType = "BIG_DECIMAL_JSON_NODE";
        } else if (jsonNode.isBigInteger()) {
            nodeType = "BIG_INTEGER_JSON_NODE";
        } else if (jsonNode.isBinary()) {
            nodeType = "BINARY_JSON_NODE";
        } else if (jsonNode.isContainerNode()) {
            nodeType = "CONTAINER_JSON_NODE";
        } else if (jsonNode.isMissingNode()) {
            nodeType = "MISSING_JSON_NODE";
        } else if (jsonNode.isNull()) {
            nodeType = "NULL_JSON_NODE";
        } else if (jsonNode.isPojo()) {
            nodeType = "POJO_JSON_NODE";
        }

        return nodeType;
    }

    private static HstPropertyDefinition getHstPropertyDefinition(String name, List<HstPropertyDefinition> hstPropDefs)
            throws CouldNotFindHstPropertyDefinition {

        HstPropertyDefinition foundHstPropDef = null;

        for (HstPropertyDefinition hstPropDef : hstPropDefs) {
            if (hstPropDef.getName().equals(name)) {
                foundHstPropDef = hstPropDef;
                break;
            }
        }

        // I am throwing this exception because it is not a good practice of returning null values
        // Also throwing such an exception will be reflected as an error in JUnit indicating that there is something
        // wrong with the data set under test
        if (foundHstPropDef == null) {
            throw new CouldNotFindHstPropertyDefinition("Could not find an hst property definition with name: '" + name
                    + "'");
        }

        return foundHstPropDef;
    }

    private static boolean[] toBooleanPrimitiveArray(JsonNode jsonArrayNode) {
        // Assert that jsonArrayNode is not null
        assertNotNull("Expected a non null JSON node!", jsonArrayNode);
        // Assert that jsonArrayNode is a JSON array node
        assertTrue("Expected a JSON array node but got a '" + getNodeType(jsonArrayNode) + "' node!",
                jsonArrayNode.isArray());

        boolean[] returnValue = new boolean[jsonArrayNode.size()];

        for (int index = 0; index < returnValue.length; index++) {
            final JsonNode booleanJsonNode = jsonArrayNode.get(index);
            // Assert that booleanJsonNode is a boolean node 
            assertTrue("Expected a JSON boolean node but got a '" + getNodeType(booleanJsonNode) + "' node!",
                    booleanJsonNode.isBoolean());

            returnValue[index] = booleanJsonNode.booleanValue();
        }

        return returnValue;
    }

    private static Boolean[] toBooleanArray(JsonNode jsonArrayNode) {
        // Assert that jsonArrayNode is not null
        assertNotNull("Expected a non null JSON node!", jsonArrayNode);
        // Assert that jsonArrayNode is a JSON array node
        assertTrue("Expected a JSON array node but got a '" + getNodeType(jsonArrayNode) + "' node!",
                jsonArrayNode.isArray());

        Boolean[] returnValue = new Boolean[jsonArrayNode.size()];

        for (int index = 0; index < returnValue.length; index++) {
            final JsonNode booleanJsonNode = jsonArrayNode.get(index);
            // Assert that booleanJsonNode is a boolean node 
            assertTrue("Expected a JSON boolean node but got a '" + getNodeType(booleanJsonNode) + "' node!",
                    booleanJsonNode.isBoolean());

            returnValue[index] = booleanJsonNode.booleanValue();
        }

        return returnValue;
    }

}
