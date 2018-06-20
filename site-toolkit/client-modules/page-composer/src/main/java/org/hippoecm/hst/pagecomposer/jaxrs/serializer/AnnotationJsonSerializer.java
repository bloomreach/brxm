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

package org.hippoecm.hst.pagecomposer.jaxrs.serializer;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;


/**
 * Java {@link Annotation}(s) custom JSON serializer
 */
public class AnnotationJsonSerializer extends StdSerializer<Annotation> {

    /**
     * The attribute that stores a value of {@link AnnotationType}
     */
    public static final String TYPE_ATTRIBUTE = "type";

    /**
     * {@link AnnotationJsonSerializer} constructor
     *
     * @param type - The type to which this serializer will be mapped to be used
     */
    public AnnotationJsonSerializer(Class<Annotation> type) {
        super(type);
    }

    @Override
    public void serializeWithType(final Annotation value, final JsonGenerator jgen, final SerializerProvider provider, final TypeSerializer typeSer) throws IOException, JsonProcessingException {
        serialize(value, jgen, provider);
    }

    /**
     * Serialize a Java {@link Annotation}
     */
    public void serialize(Annotation value, JsonGenerator jgen, SerializerProvider provider) throws IOException,
            JsonGenerationException {

        for (Class<?> iface : value.getClass().getInterfaces()) {
            if (iface.isAnnotation()) {
                jgen.writeStartObject();
                jgen.writeStringField(TYPE_ATTRIBUTE, AnnotationType.fromClass(iface).toString());
                for (Method method : iface.getDeclaredMethods()) {
                    if (isValidAnnotationMethod(method)) {
                        try {
                            serializeAnnotationAttribute(method, value, jgen);
                        } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException iarge) {
                            throw new JsonGenerationException(iarge.fillInStackTrace());
                        }
                    }
                }
            }

            jgen.writeEndObject();
            // Just loop for once. Annotations can not have super types or interfaces and classes implementing the
            // Annotation interface are not treated as annotations like the ones designated with the @
            break;
        }
    }

    /**
     * Check whether the given {@link Method} is of interest to be serialized. For the time being we are not interested
     * in methods which have parameters
     *
     * @param method - The method to be checked
     * @return <code>true</code> if it is a valid method, <code>false</code> otherwise
     */
    protected boolean isValidAnnotationMethod(Method method) {
        return !(method.getParameterTypes().length > 0);
    }

    /**
     * Serialize an {@link Annotation} attribute
     *
     * @param method - Method which represents
     * @param value
     * @param jgen
     * @throws JsonGenerationException
     * @throws IllegalArgumentException
     * @throws IOException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    protected void serializeAnnotationAttribute(Method method, Annotation value, JsonGenerator jgen)
            throws JsonGenerationException, IllegalArgumentException, IOException, IllegalAccessException,
            InvocationTargetException {

        if (method.getReturnType() == byte.class || method.getReturnType() == Byte.class) {
            jgen.writeNumberField(method.getName(), (Byte) method.invoke(value));
        } else if (method.getReturnType() == short.class || method.getReturnType() == Short.class) {
            jgen.writeNumberField(method.getName(), (Short) method.invoke(value));
        } else if (method.getReturnType() == int.class || method.getReturnType() == Integer.class) {
            jgen.writeNumberField(method.getName(), (Integer) method.invoke(value));
        } else if (method.getReturnType() == long.class || method.getReturnType() == Long.class) {
            jgen.writeNumberField(method.getName(), (Long) method.invoke(value));
        } else if (method.getReturnType() == float.class || method.getReturnType() == Float.class) {
            jgen.writeNumberField(method.getName(), (Float) method.invoke(value));
        } else if (method.getReturnType() == double.class || method.getReturnType() == Double.class) {
            jgen.writeNumberField(method.getName(), (Double) method.invoke(value));
        } else if (method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class) {
            jgen.writeBooleanField(method.getName(), (Boolean) method.invoke(value));
        } else if (method.getReturnType() == char.class || method.getReturnType() == Character.class) {
            jgen.writeStringField(method.getName(), String.valueOf(method.invoke(value)));
        } else if (method.getReturnType() == String.class) {
            jgen.writeStringField(method.getName(), (String) method.invoke(value));
        } else if (method.getReturnType() == byte[].class) {
            serializeBytePrimitiveArray(method, value, jgen);
        } else if (method.getReturnType() == Byte[].class) {
            serializeByteArray(method, value, jgen);
        } else if (method.getReturnType() == short[].class) {
            serializeShortPrimitiveArray(method, value, jgen);
        } else if (method.getReturnType() == Short[].class) {
            serializeShortArray(method, value, jgen);
        } else if (method.getReturnType() == int[].class) {
            serializeIntegerPrimitiveArray(method, value, jgen);
        } else if (method.getReturnType() == Integer[].class) {
            serializeIntegerArray(method, value, jgen);
        } else if (method.getReturnType() == long[].class) {
            serializeLongPrimitiveArray(method, value, jgen);
        } else if (method.getReturnType() == Long[].class) {
            serializeLongArray(method, value, jgen);
        } else if (method.getReturnType() == float[].class) {
            serializeFloatPrimitiveArray(method, value, jgen);
        } else if (method.getReturnType() == Float[].class) {
            serializeFloatArray(method, value, jgen);
        } else if (method.getReturnType() == double[].class) {
            serializeDoublePrimitiveArray(method, value, jgen);
        } else if (method.getReturnType() == Double[].class) {
            serializeDoubleArray(method, value, jgen);
        } else if (method.getReturnType() == boolean[].class) {
            serializeBooleanPrimitiveArray(method, value, jgen);
        } else if (method.getReturnType() == Boolean[].class) {
            serializeBooleanArray(method, value, jgen);
        } else if (method.getReturnType() == char[].class) {
            serializeCharPrimitiveArray(method, value, jgen);
        } else if (method.getReturnType() == Character[].class) {
            serializeCharArray(method, value, jgen);
        } else if (method.getReturnType() == String[].class) {
            serializeStringArray(method, value, jgen);
        } else if (method.getReturnType() == Class.class) {
            jgen.writeStringField(method.getName(), method.getReturnType().toString());
        } else {
            throw new IllegalArgumentException("Unrecognized attribute value type " + method.getReturnType().getName()
                    + " for annotation " + value.annotationType().getName());
        }

    }

    protected void serializeBytePrimitiveArray(Method method, Annotation value, JsonGenerator jgen)
            throws JsonGenerationException, IOException, IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {

        jgen.writeArrayFieldStart(method.getName());
        byte[] bytes = (byte[]) method.invoke(value);
        if (bytes != null) {
            for (byte aByte : bytes) {
                jgen.writeNumber(aByte);
            }
        }
        jgen.writeEndArray();
    }

    protected void serializeByteArray(Method method, Annotation value, JsonGenerator jgen)
            throws JsonGenerationException, IOException, IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {

        jgen.writeArrayFieldStart(method.getName());
        Byte[] bytes = (Byte[]) method.invoke(value);
        if (bytes != null) {
            for (byte aByte : bytes) {
                jgen.writeNumber(aByte);
            }
        }
        jgen.writeEndArray();
    }

    protected void serializeShortPrimitiveArray(Method method, Annotation value, JsonGenerator jgen)
            throws JsonGenerationException, IOException, IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {

        jgen.writeArrayFieldStart(method.getName());
        short[] shorts = (short[]) method.invoke(value);
        if (shorts != null) {
            for (short aShort : shorts) {
                jgen.writeNumber(aShort);
            }
        }
        jgen.writeEndArray();
    }

    protected void serializeShortArray(Method method, Annotation value, JsonGenerator jgen)
            throws JsonGenerationException, IOException, IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {

        jgen.writeArrayFieldStart(method.getName());
        Short[] shorts = (Short[]) method.invoke(value);
        if (shorts != null) {
            for (short aShort : shorts) {
                jgen.writeNumber(aShort);
            }
        }
        jgen.writeEndArray();
    }

    protected void serializeIntegerPrimitiveArray(Method method, Annotation value, JsonGenerator jgen)
            throws JsonGenerationException, IOException, IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {

        jgen.writeArrayFieldStart(method.getName());
        int[] integers = (int[]) method.invoke(value);
        if (integers != null) {
            for (int anInteger : integers) {
                jgen.writeNumber(anInteger);
            }
        }
        jgen.writeEndArray();
    }

    protected void serializeIntegerArray(Method method, Annotation value, JsonGenerator jgen)
            throws JsonGenerationException, IOException, IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {

        jgen.writeArrayFieldStart(method.getName());
        Integer[] integers = (Integer[]) method.invoke(value);
        if (integers != null) {
            for (int anInteger : integers) {
                jgen.writeNumber(anInteger);
            }
        }
        jgen.writeEndArray();
    }

    protected void serializeLongPrimitiveArray(Method method, Annotation value, JsonGenerator jgen)
            throws JsonGenerationException, IOException, IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {

        jgen.writeArrayFieldStart(method.getName());
        long[] longs = (long[]) method.invoke(value);
        if (longs != null) {
            for (long aLong : longs) {
                jgen.writeNumber(aLong);
            }
        }
        jgen.writeEndArray();
    }

    protected void serializeLongArray(Method method, Annotation value, JsonGenerator jgen)
            throws JsonGenerationException, IOException, IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {

        jgen.writeArrayFieldStart(method.getName());
        Long[] longs = (Long[]) method.invoke(value);
        if (longs != null) {
            for (long aLong : longs) {
                jgen.writeNumber(aLong);
            }
        }
        jgen.writeEndArray();
    }

    protected void serializeFloatPrimitiveArray(Method method, Annotation value, JsonGenerator jgen)
            throws JsonGenerationException, IOException, IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {

        jgen.writeArrayFieldStart(method.getName());
        float[] floats = (float[]) method.invoke(value);
        if (floats != null) {
            for (float aFloat : floats) {
                jgen.writeNumber(aFloat);
            }
        }
        jgen.writeEndArray();
    }

    protected void serializeFloatArray(Method method, Annotation value, JsonGenerator jgen)
            throws JsonGenerationException, IOException, IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {

        jgen.writeArrayFieldStart(method.getName());
        Float[] floats = (Float[]) method.invoke(value);
        if (floats != null) {
            for (float aFloat : floats) {
                jgen.writeNumber(aFloat);
            }
        }
        jgen.writeEndArray();
    }

    protected void serializeDoublePrimitiveArray(Method method, Annotation value, JsonGenerator jgen)
            throws JsonGenerationException, IOException, IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {

        jgen.writeArrayFieldStart(method.getName());
        double[] doubles = (double[]) method.invoke(value);
        if (doubles != null) {
            for (double aDouble : doubles) {
                jgen.writeNumber(aDouble);
            }
        }
        jgen.writeEndArray();
    }

    protected void serializeDoubleArray(Method method, Annotation value, JsonGenerator jgen)
            throws JsonGenerationException, IOException, IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {

        jgen.writeArrayFieldStart(method.getName());
        Double[] doubles = (Double[]) method.invoke(value);
        if (doubles != null) {
            for (double aDouble : doubles) {
                jgen.writeNumber(aDouble);
            }
        }
        jgen.writeEndArray();
    }

    protected void serializeBooleanPrimitiveArray(Method method, Annotation value, JsonGenerator jgen)
            throws JsonGenerationException, IOException, IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {

        jgen.writeArrayFieldStart(method.getName());
        boolean[] booleans = (boolean[]) method.invoke(value);
        if (booleans != null) {
            for (boolean booleanValue : booleans) {
                jgen.writeBoolean(booleanValue);
            }
        }
        jgen.writeEndArray();
    }

    protected void serializeBooleanArray(Method method, Annotation value, JsonGenerator jgen)
            throws JsonGenerationException, IOException, IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {

        jgen.writeArrayFieldStart(method.getName());
        Boolean[] booleans = (Boolean[]) method.invoke(value);
        if (booleans != null) {
            for (boolean booleanValue : booleans) {
                jgen.writeBoolean(booleanValue);
            }
        }
        jgen.writeEndArray();
    }

    protected void serializeCharPrimitiveArray(Method method, Annotation value, JsonGenerator jgen)
            throws JsonGenerationException, IOException, IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {

        jgen.writeArrayFieldStart(method.getName());
        char[] chars = (char[]) method.invoke(value);
        if (chars != null) {
            for (char aChar : chars) {
                jgen.writeString(String.valueOf(aChar));
            }
        }
        jgen.writeEndArray();
    }

    protected void serializeCharArray(Method method, Annotation value, JsonGenerator jgen)
            throws JsonGenerationException, IOException, IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {

        jgen.writeArrayFieldStart(method.getName());
        Character[] chars = (Character[]) method.invoke(value);
        if (chars != null) {
            for (char aChar : chars) {
                jgen.writeString(String.valueOf(aChar));
            }
        }
        jgen.writeEndArray();
    }

    protected void serializeStringArray(Method method, Annotation value, JsonGenerator jgen)
            throws JsonGenerationException, IOException, IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {

        jgen.writeArrayFieldStart(method.getName());
        String[] strings = (String[]) method.invoke(value);
        if (strings != null) {
            for (String string : strings) {
                jgen.writeString(string);
            }
        }
        jgen.writeEndArray();
    }

}
