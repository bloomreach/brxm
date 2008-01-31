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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
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

public class TemplateDescriptor implements IClusterable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String type;
    private boolean node;
    private boolean multiple;
    private PluginDescriptor plugin;
    private LinkedList<FieldDescriptor> fields;

    public TemplateDescriptor(String name, String type, PluginDescriptor plugin) {
        this.name = name;
        this.type = type;
        this.plugin = plugin;
        this.node = true;
        this.multiple = false;
        this.fields = new LinkedList<FieldDescriptor>();
    }

    public TemplateDescriptor(Map<String, Object> map, TemplateEngine engine) {
        this.name = (String) map.get("name");
        this.type = (String) map.get("type");
        this.node = ((Boolean) map.get("isNode")).booleanValue();
        this.multiple = ((Boolean) map.get("isMultiple")).booleanValue();

        this.fields = new LinkedList<FieldDescriptor>();
        LinkedList<Map<String, Object>> fieldList = (LinkedList<Map<String, Object>>) map.get("fields");
        for (Map<String, Object> fieldMap : fieldList) {
            fields.addLast(new FieldDescriptor(fieldMap, engine));
        }

        this.plugin = new PluginDescriptor((Map<String, Object>) map.get("plugin"), engine.getChannelFactory().createChannel());
    }

    public Map<String, Object> getMapRepresentation() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("name", name);
        map.put("type", type);
        map.put("isNode", new Boolean(node));
        map.put("isMultiple", new Boolean(multiple));

        LinkedList<Map<String, Object>> fieldList = new LinkedList<Map<String, Object>>();
        for (FieldDescriptor field : getFields()) {
            fieldList.addLast(field.getMapRepresentation());
        }
        map.put("fields", fieldList);

        map.put("plugin", getPlugin().getMapRepresentation());
        return map;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public boolean isNode() {
        return node;
    }

    public void setIsNode(boolean isNode) {
        this.node = isNode;
    }

    public boolean isMultiple() {
        return multiple;
    }

    public void setIsMultiple(boolean isMultiple) {
        this.multiple = isMultiple;
    }

    public PluginDescriptor getPlugin() {
        return plugin;
    }

    public List<FieldDescriptor> getFields() {
        return fields;
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

    // override Object methods

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).append("name", name).toString();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof TemplateDescriptor == false) {
            return false;
        }
        if (this == object) {
            return true;
        }
        TemplateDescriptor templateDescriptor = (TemplateDescriptor) object;
        return new EqualsBuilder().append(name, templateDescriptor.name).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(113, 419).append(name).toHashCode();
    }
}
