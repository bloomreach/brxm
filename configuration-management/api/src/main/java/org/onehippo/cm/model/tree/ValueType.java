/*
 *  Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.model.tree;

import javax.jcr.Property;

/**
 * Represents the data type of a ModelProperty, which in turn represents data that may be stored in a JCR.
 */
public enum ValueType {

    /* required matching set of types, *and* their enum ordinal (order), for javax.jcr.PropertyType definitions (int) */
    UNDEFINED,
    STRING,
    BINARY,
    LONG,
    DOUBLE,
    DATE,
    BOOLEAN,
    NAME,
    PATH,
    REFERENCE,
    WEAKREFERENCE,
    URI,
    DECIMAL;

    /**
     * @param jcrType an integer value representing a JCR data type, as returned by e.g. {@link Property#getType()}
     * @return a ValueType enum value representing the jcrType
     */
    public static ValueType fromJcrType(final int jcrType) {
        return values()[jcrType];
    }

    public final String toString() {
        return name().toLowerCase();
    }

}
