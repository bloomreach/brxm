/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.upgrade;


public final class Value implements Comparable<Value> {

    enum ValueType {
        BOOLEAN("Boolean"), DOUBLE("Double"), LONG("Long"), UNKNOWN(null);
        
        String name;

        ValueType(String name) {
            this.name = name;
        }

        static ValueType fromString(String name) {
            for (ValueType type : ValueType.values()) {
                if (name.equals(type.name)) {
                    return type;
                }
            }
            return UNKNOWN;
        }
    }

    private final Object value;
    private final ValueType type;
    
    Value(String value, ValueType type) {
        this.type = type;
        switch (type) {
        case BOOLEAN:
            this.value = Boolean.parseBoolean(value);
            break;
        case DOUBLE:
            this.value = Double.parseDouble(value);
            break;
        case LONG:
            this.value = Long.parseLong(value);
            break;
        default:
            this.value = value;
        }
    }
    
    public ValueType getType() {
        return type;
    }

    @Override
    public String toString() {
        return getValue().toString();
    }

    @Override
    public int hashCode() {
        return getValue().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Value) {
            Value that = (Value) obj;
            if (that.type == type) {
                return getValue().equals(that.getValue());
            }
        }
        return false;
    }
    
    public int compareTo(Value that) {
        int typeCmp = type.compareTo(that.type);
        if (typeCmp != 0) {
            return typeCmp;
        }
        switch (type) {
        case BOOLEAN:
            return ((Boolean) getValue()).compareTo((Boolean) that.getValue());
        case LONG:
            return ((Long) getValue()).compareTo((Long) that.getValue());
        case DOUBLE:
            return ((Double) getValue()).compareTo((Double) that.getValue());
        default:
            return getValue().toString().compareTo(that.getValue().toString());
        }
    }

    public Object getValue() {
        return value;
    }
}
