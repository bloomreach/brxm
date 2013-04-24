/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.service.restproxy.custom.json.deserializers;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.NotImplementedException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.TypeDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A custom Jackson JSON deserializer to be able to deserialize Java {@link Annotation}(s)
 */
public class AnnotationJsonDeserializer extends JsonDeserializer<Annotation> {

    private static final Logger log = LoggerFactory.getLogger(AnnotationJsonDeserializer.class);
    
    @SuppressWarnings("unchecked")
    @Override
    public Annotation deserialize(JsonParser jsonParser, DeserializationContext deserContext) throws IOException,
            JsonProcessingException {

        Annotation annotation = null;
        String annotationTypeName = null;
        Class<? extends Annotation> annotationClass = null;
        Map<String, Object> annotationAttributes = null;

        while(jsonParser.nextToken() != JsonToken.END_OBJECT) {
            // Read the '@class' field name
            jsonParser.nextToken();
            // Now read the '@class' field value
            annotationTypeName = jsonParser.getText();

            try {
                annotationClass = (Class<? extends Annotation>) Class.forName(annotationTypeName);
                annotationAttributes = new HashMap<String, Object>(annotationClass.getDeclaredMethods().length);
                while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                    final String fieldName = jsonParser.getCurrentName();
                    final Method annotationAttribute = annotationClass.getDeclaredMethod(fieldName, new Class<?>[] {});
                    annotationAttributes.put(fieldName, deserializeAnnotationAttribute(annotationClass, annotationAttribute, jsonParser));
                }
                // Annotation deserialization is done here
                break;
            } catch (ClassNotFoundException cnfe) {
                throw new AnnotationProcessingException("Error while processing annotation: " + annotationTypeName, cnfe);
            } catch (SecurityException se) {
                throw new AnnotationProcessingException("Error while processing annotation: " + annotationTypeName, se);
            } catch (NoSuchMethodException nsme) {
                if (log.isDebugEnabled()) {
                    log.info("Error while processing annotation: " + annotationTypeName + ". " + nsme);
                } else {
                    log.info("Error while processing annotation: {}. {}", annotationTypeName, nsme);
                }

            }
        }

        annotation = (Annotation) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class<?>[] { annotationClass }, new AnnotationProxyInvocationHandler(annotationClass, annotationAttributes));

        return annotation;
    }

    @Override
    public Object deserializeWithType(JsonParser jp, DeserializationContext ctxt, TypeDeserializer typeDeserializer)
            throws IOException, JsonProcessingException {

        return deserialize(jp, ctxt);
    }

    protected Object deserializeAnnotationAttribute(Class<? extends Annotation> annotationClass, Method annotationAttribute,
            JsonParser jsonParser) throws JsonParseException, IOException {

        jsonParser.nextToken();
        if (annotationAttribute.getReturnType() == byte.class || annotationAttribute.getReturnType() == Byte.class) {
            return jsonParser.getNumberValue().byteValue();
        } else if (annotationAttribute.getReturnType() == short.class || annotationAttribute.getReturnType() == Short.class) {
            return jsonParser.getNumberValue().shortValue();
        } else if (annotationAttribute.getReturnType() == int.class || annotationAttribute.getReturnType() == Integer.class) {
            return jsonParser.getNumberValue().intValue();
        } else if (annotationAttribute.getReturnType() == long.class || annotationAttribute.getReturnType() == Long.class) {
            return jsonParser.getNumberValue().longValue();
        } else if (annotationAttribute.getReturnType() == float.class || annotationAttribute.getReturnType() == Float.class) {
            return jsonParser.getNumberValue().floatValue();
        } else if (annotationAttribute.getReturnType() == double.class || annotationAttribute.getReturnType() == Double.class) {
            return jsonParser.getNumberValue().doubleValue();
        } else if (annotationAttribute.getReturnType() == double.class || annotationAttribute.getReturnType() == Double.class) {
            return jsonParser.getNumberValue().doubleValue();
        } else if (annotationAttribute.getReturnType() == boolean.class || annotationAttribute.getReturnType() == Boolean.class ) {
            return jsonParser.getBooleanValue();
        } else if (annotationAttribute.getReturnType() == char.class || annotationAttribute.getReturnType() == Character.class ) {
            return jsonParser.getText().charAt(0);
        } else if (annotationAttribute.getReturnType() == String.class) {
            return jsonParser.getText();
        } else if (annotationAttribute.getReturnType() == byte[].class) {
            return deserializeBytePrimitiveArrayAnnotationAttribute(jsonParser);
        } else if (annotationAttribute.getReturnType() == Byte[].class) {
            return deserializeByteArrayAnnotationAttribute(jsonParser);
        } else if (annotationAttribute.getReturnType() == short[].class) {
            return deserializeShortPrimitiveArrayAnnotationAttribute(jsonParser);
        } else if (annotationAttribute.getReturnType() == Short[].class) {
            return deserializeShortArrayAnnotationAttribute(jsonParser);
        } else if (annotationAttribute.getReturnType() == int[].class) {
            return deserializeIntegerPrimitiveArrayAnnotationAttribute(jsonParser);
        } else if (annotationAttribute.getReturnType() == Short[].class) {
            return deserializeIntegerArrayAnnotationAttribute(jsonParser);
        } else if (annotationAttribute.getReturnType() == long[].class) {
            return deserializeLongPrimitiveArrayAnnotationAttribute(jsonParser);
        } else if (annotationAttribute.getReturnType() == Long[].class) {
            return deserializeLongArrayAnnotationAttribute(jsonParser);
        } else if (annotationAttribute.getReturnType() == float[].class) {
            return deserializeFloatPrimitiveArrayAnnotationAttribute(jsonParser);
        } else if (annotationAttribute.getReturnType() == Float[].class) {
            return deserializeFloatArrayAnnotationAttribute(jsonParser);
        } else if (annotationAttribute.getReturnType() == double[].class) {
            return deserializeDoublePrimitiveArrayAnnotationAttribute(jsonParser);
        } else if (annotationAttribute.getReturnType() == Double[].class) {
            return deserializeDoubleArrayAnnotationAttribute(jsonParser);
        } else if (annotationAttribute.getReturnType() == boolean[].class) {
            return deserializeBooleanPrimitiveArrayAnnotationAttribute(jsonParser);
        } else if (annotationAttribute.getReturnType() == Boolean[].class) {
            return deserializeBooleanArrayAnnotationAttribute(jsonParser);
        } else if (annotationAttribute.getReturnType() == char[].class) {
            return deserializeCharacterPrimitiveArrayAnnotationAttribute(jsonParser);
        } else if (annotationAttribute.getReturnType() == Character[].class) {
            return deserializeCharacterArrayAnnotationAttribute(jsonParser);
        } else if (annotationAttribute.getReturnType() == String[].class) {
            return deserializeStringArrayAnnotationAttribute(jsonParser);
        } else {
            throw new IllegalArgumentException("Unrecognized attribute value type "
                    + annotationAttribute.getReturnType().getName() + " for annotation " + annotationClass.getName());
            
        }
    }
    
    protected byte[] deserializeBytePrimitiveArrayAnnotationAttribute(JsonParser jsonParser) throws JsonParseException, IOException {
        Byte[] byteArray = deserializeByteArrayAnnotationAttribute(jsonParser);
        byte[] returnValue = new byte[byteArray.length];

        for (int index = 0; index < byteArray.length; index++) {
            returnValue[index] = byteArray[index];
        }

        return returnValue;
    }

    protected Byte[] deserializeByteArrayAnnotationAttribute(JsonParser jsonParser) throws JsonParseException, IOException {
        List<Byte> byteArray = new ArrayList<Byte>();

        while(jsonParser.nextToken() != JsonToken.END_ARRAY) {
            byteArray.add(jsonParser.getByteValue());
        }

        return byteArray.toArray(new Byte[byteArray.size()]);
    }

    protected short[] deserializeShortPrimitiveArrayAnnotationAttribute(JsonParser jsonParser) throws JsonParseException, IOException {
        Short[] shortArray = deserializeShortArrayAnnotationAttribute(jsonParser);
        short[] returnValue = new short[shortArray.length];

        for (int index = 0; index < shortArray.length; index++) {
            returnValue[index] = shortArray[index];
        }

        return returnValue;
    }

    protected Short[] deserializeShortArrayAnnotationAttribute(JsonParser jsonParser) throws JsonParseException, IOException {
        List<Short> integerArray = new ArrayList<Short>();

        while(jsonParser.nextToken() != JsonToken.END_ARRAY) {
            integerArray.add(jsonParser.getShortValue());
        }

        return integerArray.toArray(new Short[integerArray.size()]);
    }

    protected int[] deserializeIntegerPrimitiveArrayAnnotationAttribute(JsonParser jsonParser) throws JsonParseException, IOException {
        Integer[] integerArray = deserializeIntegerArrayAnnotationAttribute(jsonParser);
        int[] returnValue = new int[integerArray.length];

        for (int index = 0; index < integerArray.length; index++) {
            returnValue[index] = integerArray[index];
        }

        return returnValue;
    }

    protected Integer[] deserializeIntegerArrayAnnotationAttribute(JsonParser jsonParser) throws JsonParseException, IOException {
        List<Integer> integerArray = new ArrayList<Integer>();

        while(jsonParser.nextToken() != JsonToken.END_ARRAY) {
            integerArray.add(jsonParser.getIntValue());
        }

        return integerArray.toArray(new Integer[integerArray.size()]);
    }

    protected long[] deserializeLongPrimitiveArrayAnnotationAttribute(JsonParser jsonParser) throws JsonParseException, IOException {
        Long[] longArray = deserializeLongArrayAnnotationAttribute(jsonParser);
        long[] returnValue = new long[longArray.length];

        for (int index = 0; index < longArray.length; index++) {
            returnValue[index] = longArray[index];
        }

        return returnValue;
    }

    protected Long[] deserializeLongArrayAnnotationAttribute(JsonParser jsonParser) throws JsonParseException, IOException {
        List<Long> longArray = new ArrayList<Long>();

        while(jsonParser.nextToken() != JsonToken.END_ARRAY) {
            longArray.add(jsonParser.getLongValue());
        }

        return longArray.toArray(new Long[longArray.size()]);
    }

    protected float[] deserializeFloatPrimitiveArrayAnnotationAttribute(JsonParser jsonParser) throws JsonParseException, IOException {
        Float[] floatArray = deserializeFloatArrayAnnotationAttribute(jsonParser);
        float[] returnValue = new float[floatArray.length];

        for (int index = 0; index < floatArray.length; index++) {
            returnValue[index] = floatArray[index];
        }

        return returnValue;
    }

    protected Float[] deserializeFloatArrayAnnotationAttribute(JsonParser jsonParser) throws JsonParseException, IOException {
        List<Float> floatArray = new ArrayList<Float>();

        while(jsonParser.nextToken() != JsonToken.END_ARRAY) {
            floatArray.add(jsonParser.getFloatValue());
        }

        return floatArray.toArray(new Float[floatArray.size()]);
    }

    protected double[] deserializeDoublePrimitiveArrayAnnotationAttribute(JsonParser jsonParser) throws JsonParseException, IOException {
        Double[] doubleArray = deserializeDoubleArrayAnnotationAttribute(jsonParser);
        double[] returnValue = new double[doubleArray.length];

        for (int index = 0; index < doubleArray.length; index++) {
            returnValue[index] = doubleArray[index];
        }

        return returnValue;
    }

    protected Double[] deserializeDoubleArrayAnnotationAttribute(JsonParser jsonParser) throws JsonParseException, IOException {
        List<Double> doubleArray = new ArrayList<Double>();

        while(jsonParser.nextToken() != JsonToken.END_ARRAY) {
            doubleArray.add(jsonParser.getDoubleValue());
        }

        return doubleArray.toArray(new Double[doubleArray.size()]);
    }

    protected boolean[] deserializeBooleanPrimitiveArrayAnnotationAttribute(JsonParser jsonParser) throws JsonParseException, IOException {
        Boolean[] booleanArray = deserializeBooleanArrayAnnotationAttribute(jsonParser);
        boolean[] returnArray = new boolean[booleanArray.length];

        for (int index = 0; index < booleanArray.length; index++) {
            returnArray[index] = booleanArray[index];
        }

        return returnArray;
    }

    protected Boolean[] deserializeBooleanArrayAnnotationAttribute(JsonParser jsonParser) throws JsonParseException, IOException {
        List<Boolean> booleanArray = new ArrayList<Boolean>();

        while(jsonParser.nextToken() != JsonToken.END_ARRAY) {
            booleanArray.add(jsonParser.getBooleanValue());
        }

        return booleanArray.toArray(new Boolean[booleanArray.size()]);
    }

    protected char[] deserializeCharacterPrimitiveArrayAnnotationAttribute(JsonParser jsonParser) throws JsonParseException, IOException {
        Character[] characterArray = deserializeCharacterArrayAnnotationAttribute(jsonParser);
        char[] returnValue = new char[characterArray.length];

        for (int index = 0; index < characterArray.length; index++) {
            returnValue[index] = characterArray[index];
        }
        return returnValue;
    }

    protected Character[] deserializeCharacterArrayAnnotationAttribute(JsonParser jsonParser) throws JsonParseException, IOException {
        List<Character> characterArray = new ArrayList<Character>();

        while(jsonParser.nextToken() != JsonToken.END_ARRAY) {
            characterArray.add(jsonParser.getText().charAt(0));
        }

        return characterArray.toArray(new Character[characterArray.size()]);
    }

    protected String[] deserializeStringArrayAnnotationAttribute(JsonParser jsonParser) throws JsonParseException, IOException {
        List<String> stringArray = new ArrayList<String>();

        while(jsonParser.nextToken() != JsonToken.END_ARRAY) {
            stringArray.add(jsonParser.getText());
        }

        return stringArray.toArray(new String[stringArray.size()]);
    }

    /**
     * Annotation proxy support.  The implementation of the hashCode and equals methods has
     * been copied from the commons-lang project.
     */
    public static class AnnotationProxyInvocationHandler implements InvocationHandler {

        private final Class<? extends Annotation> annotationClass;
        private final Map<String, ?> annotationAttributes;

        public AnnotationProxyInvocationHandler(Class<? extends Annotation> annotationTypeName, Map<String, ?> annotationAttributes) {
            this.annotationClass = annotationTypeName;
            this.annotationAttributes = annotationAttributes;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Object returnValue = this.annotationAttributes.get(method.getName());

            if (returnValue != null) {
                return returnValue;
            } else if (method.getName().equals("toString")) {
                return handleToString();
            } else if (method.getName().equals("annotationType")) {
                return this.annotationClass;
            } else if (method.getName().equals("equals") && args.length == 1) {
                Annotation that = (Annotation) args[0];
                if (that.annotationType() == annotationClass) {
                    for (Method annotationMethod : annotationClass.getDeclaredMethods()) {
                        if (annotationMethod.getParameterTypes().length > 0) {
                            continue;
                        }
                        final Object thisValue = this.annotationAttributes.get(annotationMethod.getName());
                        final Object thatValue = annotationMethod.invoke(that);
                        if (thisValue == null && thatValue != null) {
                            return false;
                        } else if (thisValue == null || thatValue == null) {
                            return false;
                        }
                        if (!thisValue.equals(thatValue)) {
                            return false;
                        }
                    }
                    return true;
                }
                return false;
            } else if (method.getName().equals("hashCode") && method.getParameterTypes().length == 0) {
                return hashCode((Annotation) proxy);
            } else {
                throw new NotImplementedException("This method can not be handled for : " + this.handleToString());
            }
        }

        private static int hashCode(final Annotation a) {
            int result = 0;
            final Class<? extends Annotation> type = a.annotationType();
            for (final Method m : type.getDeclaredMethods()) {
                try {
                    final Object value = m.invoke(a);
                    if (value == null) {
                        throw new IllegalStateException(
                                String.format("Annotation method %s returned null", m));
                    }
                    result += hashMember(m.getName(), value);
                } catch (final RuntimeException ex) {
                    throw ex;
                } catch (final Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
            return result;
        }

        private static int hashMember(final String name, final Object value) {
            final int part1 = name.hashCode() * 127;
            if (value.getClass().isArray()) {
                return part1 ^ arrayMemberHash(value.getClass().getComponentType(), value);
            }
            if (value instanceof Annotation) {
                return part1 ^ hashCode((Annotation) value);
            }
            return part1 ^ value.hashCode();
        }

        private static int arrayMemberHash(final Class<?> componentType, final Object o) {
            if (componentType.equals(Byte.TYPE)) {
                return Arrays.hashCode((byte[]) o);
            }
            if (componentType.equals(Short.TYPE)) {
                return Arrays.hashCode((short[]) o);
            }
            if (componentType.equals(Integer.TYPE)) {
                return Arrays.hashCode((int[]) o);
            }
            if (componentType.equals(Character.TYPE)) {
                return Arrays.hashCode((char[]) o);
            }
            if (componentType.equals(Long.TYPE)) {
                return Arrays.hashCode((long[]) o);
            }
            if (componentType.equals(Float.TYPE)) {
                return Arrays.hashCode((float[]) o);
            }
            if (componentType.equals(Double.TYPE)) {
                return Arrays.hashCode((double[]) o);
            }
            if (componentType.equals(Boolean.TYPE)) {
                return Arrays.hashCode((boolean[]) o);
            }
            return Arrays.hashCode((Object[]) o);
        }

        protected String handleToString() {
            String superString = super.toString();

            return superString + " : " + this.annotationClass.getName();
        }

    }

}
