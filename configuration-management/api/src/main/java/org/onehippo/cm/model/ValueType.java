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
package org.onehippo.cm.model;

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

    /* other value types, not mapping on jcr PropertyType definitions, must go *after* the above required types */

    // todo: decide whether to support (or remove) these
    // ENCRYPTED
    // CLASSNAME
    // FOLDER

    public final String toString() {
        return name().toLowerCase();
    }

}
