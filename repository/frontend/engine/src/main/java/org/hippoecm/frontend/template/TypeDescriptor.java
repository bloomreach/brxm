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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.apache.jackrabbit.value.BooleanValue;
import org.apache.jackrabbit.value.DateValue;
import org.apache.jackrabbit.value.DoubleValue;
import org.apache.jackrabbit.value.LongValue;
import org.apache.jackrabbit.value.NameValue;
import org.apache.jackrabbit.value.PathValue;
import org.apache.jackrabbit.value.ReferenceValue;
import org.apache.jackrabbit.value.StringValue;
import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.plugin.PluginDescriptor;

public class TypeDescriptor implements IClusterable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String type;
    private String superType;
    private List<String> mixinTypes;
    private List<FieldDescriptor> fields;
    private boolean node;
    private boolean mixin;

    public TypeDescriptor(String name, String type, PluginDescriptor plugin) {
        this.name = name;
        this.type = type;
        this.superType = "";
        this.mixinTypes = new LinkedList<String>();
        this.fields = new LinkedList<FieldDescriptor>();
        this.node = true;
        this.mixin = false;
    }

    public TypeDescriptor(Map<String, Object> map) {
        this.name = (String) map.get("name");
        this.type = (String) map.get("type");
        this.superType = (String) map.get("superType");
        this.mixinTypes = (List<String>) map.get("mixinType");

        this.fields = new LinkedList<FieldDescriptor>();
        if(map.get("fields") != null) {
            List<Map<String, Object>> fieldList = (List<Map<String, Object>>) map.get("fields");
            for(Map<String, Object> subMap : fieldList) {
                fields.add(new FieldDescriptor(subMap));
            }
        }

        this.node = ((Boolean) map.get("isNode")).booleanValue();
        this.mixin = ((Boolean) map.get("isMixin")).booleanValue();
    }

    public Map<String, Object> getMapRepresentation() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("name", name);
        map.put("type", type);
        map.put("superType", superType);
        map.put("isNode", new Boolean(node));
        map.put("isMixin", new Boolean(mixin));
        map.put("mixinType", getMixinTypes());
        List<Map<String, Object>> fieldList = new LinkedList<Map<String, Object>>();
        for(FieldDescriptor field : getFields()) {
            fieldList.add(field.getMapRepresentation());
        }
        return map;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getSuperType() {
        return superType;
    }

    public void setSuperType(String superType) {
        this.superType = superType;
    }

    public List<String> getMixinTypes() {
        return mixinTypes;
    }

    public void setMixinTypes(List<String> mixins) {
        this.mixinTypes = mixins;
    }

    public List<FieldDescriptor> getFields() {
        return fields;
    }

    public boolean isNode() {
        return node;
    }

    public void setIsNode(boolean isNode) {
        this.node = isNode;
    }

    public boolean isMixin() {
        return mixin;
    }

    public void setIsMixin(boolean isMixin) {
        this.mixin = isMixin;
    }

    public Value createValue(Object object) {
        try {
            int propertyType = PropertyType.valueFromName(type);
            String string = object.toString();
            switch (propertyType) {
            case PropertyType.BOOLEAN:
                return BooleanValue.valueOf(string);
            case PropertyType.DATE:
                return DateValue.valueOf(string);
            case PropertyType.DOUBLE:
                return DoubleValue.valueOf(string);
            case PropertyType.LONG:
                return LongValue.valueOf(string);
            case PropertyType.NAME:
                return NameValue.valueOf(string);
            case PropertyType.PATH:
                return PathValue.valueOf(string);
            case PropertyType.REFERENCE:
                return ReferenceValue.valueOf(string);
            case PropertyType.STRING:
            case PropertyType.UNDEFINED:
                return new StringValue(string);
            default:
                return null;
            }
        } catch (ValueFormatException ex) {
            return null;
        }
    }
}
