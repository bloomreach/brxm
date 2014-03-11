/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.pagecomposer.jaxrs.services.helpers;

public enum Position {

    FIRST("first"),
    AFTER("after"),
    LAST("last"),
    ANY("any");

    public static final String FIRST_AS_STRING = "first";
    public static final String AFTER_AS_STRING = "after";
    public static final String LAST_AS_STRING = "last";
    public static final String ANY_AS_STRING = "any";

    public static Position fromString(String name) {
        if (name == null) {
            return ANY;
        }
        name = name.toLowerCase();
        for (Position position : Position.values()) {
            if (position.name.equals(name)) {
                return position;
            }
        }
        return ANY;
    }

    private String name;

    private Position(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
