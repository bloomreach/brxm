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

package org.onehippo.test.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation that has no attributes
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AllInOneAnnotation {
    public byte byteValue() default 0x00;
    public short shortValue() default 0x01;
    public int intValule() default 0x02;
    public long longValue() default 0x03L;
    public float floatValue() default 0.4F;
    public float maxFloatValue() default Float.MAX_VALUE;
    public float minFloatValue() default Float.MIN_VALUE;
    public float minNormalFloatValue() default Float.MIN_NORMAL;
    public double doubleValue() default 0.5;
    public double maxDoubleValue() default Double.MAX_VALUE;
    public double minDoubleValue() default Double.MIN_VALUE;
    public double minNormalDoubleValue() default Double.MIN_NORMAL;
    public boolean trueValue() default true;
    public boolean falseValue() default false;
    public char charValue() default 'C';
    public String stringValue() default "Some String Value";
    public byte[] byteArrayValule() default {0x00, 0x01, 0x02};
    public short[] shartArrayValue() default {0x03, 0x04, 0x05};
    public int[] intArrayValue() default {0x06, 0x07, 0x08};
    public long[] longArrayValue() default {9L, 10L, 11L};
    public float[] floatArrayValue() default {0.12F, 0.13F, 0.14F};
    public double[] doubleArrayValue() default {0.15, 0.16, 0.17}; 
    public boolean[] booleanArrayValue() default {true, false, true};
    public char[] charArrayValue() default {'J', 'a', 'v', 'a'};
    public String[] strinArrayValue() default {"Java", "is", "good"};
}
