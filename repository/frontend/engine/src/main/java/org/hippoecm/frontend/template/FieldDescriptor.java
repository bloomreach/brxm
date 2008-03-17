/*
 * Copyright 2007 Hippo
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
package org.hippoecm.frontend.template;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.IClusterable;

public class FieldDescriptor implements IClusterable, Cloneable {
    private static final long serialVersionUID = 1L;

    private String type;
    private String path;

    private Set<String> excluded;

    private boolean multiple;
    private boolean binary;
    private boolean protect;
    private boolean mandatory;
    private boolean ordered;

    public FieldDescriptor(String path) {
        this.type = null;
        this.path = path;
        this.excluded = null;

        multiple = protect = binary = mandatory = ordered = false;
    }

    public FieldDescriptor(Map<String, Object> map) {
        this.excluded = (Set<String>) map.get("excluded");
        this.type = (String) map.get("type");
        this.path = (String) map.get("path");

        this.multiple = ((Boolean) map.get("multiple")).booleanValue();
        this.binary = ((Boolean) map.get("binary")).booleanValue();
        this.protect = ((Boolean) map.get("protect")).booleanValue();
        this.mandatory = ((Boolean) map.get("mandatory")).booleanValue();
        this.ordered = ((Boolean) map.get("ordered")).booleanValue();
    }

    public Map<String, Object> getMapRepresentation() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("path", getPath());
        map.put("excluded", getExcluded());
        map.put("type", getType());

        map.put("multiple", new Boolean(multiple));
        map.put("binary", new Boolean(binary));
        map.put("protect", new Boolean(protect));
        map.put("mandatory", new Boolean(mandatory));
        map.put("ordered", new Boolean(ordered));
        return map;
    }

    @Override
    public FieldDescriptor clone() {
        try {
            return (FieldDescriptor) super.clone();
        } catch (CloneNotSupportedException ex) {
            // not reached
        }
        return null;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setIsMultiple(boolean multiple) {
        this.multiple = multiple;
    }

    public boolean isMultiple() {
        return multiple;
    }

    public boolean isBinary() {
        return binary;
    }

    public boolean isProtected() {
        return protect;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    public boolean isOrdered() {
        return ordered;
    }

    public void setIsOrdered(boolean isOrdered) {
        this.ordered = isOrdered;
    }

    public Set<String> getExcluded() {
        return excluded;
    }

    public void setExcluded(Set<String> set) {
        excluded = set;
    }
}
