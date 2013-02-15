/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.services.search.query.constraint;

public final class TextConstraint implements Constraint {

    public enum Type {
        EQUAL, CONTAINS
    }

    private final String property;
    private final String value;
    private final Type type;

    public TextConstraint(final String property, final String value, final Type type) {
        this.property = property;
        this.value = value;
        this.type = type;
    }

    public String getProperty() {
        return property;
    }

    public Type getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "(text " + (property == null ? "[any]" : property ) + (type == Type.EQUAL ? " = " : " contains ") + value + ")";
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof TextConstraint) {
            TextConstraint other = (TextConstraint) obj;
            if(other.type == type && other.value.equals(value)) {
                if (property == null) {
                    return other.property == null;
                }
                return other.property.equals(property);
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return type.hashCode() << 3 ^ value.hashCode() << 2 ^ (property != null ? property.hashCode() : 0);
    }
}
