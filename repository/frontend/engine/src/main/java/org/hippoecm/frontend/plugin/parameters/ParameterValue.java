/*
 * Copyright 2008 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugin.parameters;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.wicket.IClusterable;

public class ParameterValue implements IClusterable, Cloneable {
    private static final long serialVersionUID = 2L;

    public static final int TYPE_UNKNOWN = 0;
    public static final int TYPE_BOOLEAN = 1;
    public static final int TYPE_STRING = 2;
    public static final int TYPE_MAP = 3;

    private int type;
    private Object value;

    public ParameterValue() {
        this.type = TYPE_UNKNOWN;
        this.value = null;
    }

    public ParameterValue(boolean bool) {
        this.type = TYPE_BOOLEAN;
        this.value = new Boolean(bool);
    }

    public ParameterValue(List<String> object) {
        this.type = TYPE_STRING;
        this.value = object;
    }

    public ParameterValue(Map<String, ParameterValue> list) {
        this.type = TYPE_MAP;
        this.value = list;
    }

    public ParameterValue clone() {
        try {
            ParameterValue clone = (ParameterValue) super.clone();
            if (type == TYPE_MAP) {
                Map<String, ParameterValue> newValue = new HashMap<String, ParameterValue>();
                for (Map.Entry<String, ParameterValue> entry : ((Map<String, ParameterValue>) value).entrySet()) {
                    newValue.put(entry.getKey(), entry.getValue().clone());
                }
                clone.value = newValue;
                return clone;
            }
            return clone;
        } catch (CloneNotSupportedException ex) {
            // nothing
        }
        return null;
    }

    public int getType() {
        return type;
    }

    public boolean getBoolean() {
        if (type == TYPE_BOOLEAN) {
            return ((Boolean) value).booleanValue();
        }
        return false;
    }

    public List<String> getStrings() {
        if (type == TYPE_STRING) {
            return (List<String>) value;
        }
        return new LinkedList<String>();
    }

    public Map<String, ParameterValue> getMap() {
        if (type == TYPE_MAP) {
            return (Map<String, ParameterValue>) value;
        }
        return null;
    }
}
