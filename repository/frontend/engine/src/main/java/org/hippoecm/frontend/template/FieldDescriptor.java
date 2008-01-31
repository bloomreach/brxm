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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.plugin.PluginDescriptor;

public class FieldDescriptor implements IClusterable, Cloneable {
    private static final long serialVersionUID = 1L;

    private String name;
    private String path;

    private PluginDescriptor plugin;
    private TemplateDescriptor template;
    private FieldDescriptor field;

    private String[] constraints;
    private Set<String> excluded;

    private boolean node;
    private boolean multiple;
    private boolean binary;
    private boolean protect;
    private boolean mandatory;
    private boolean ordered;

    public FieldDescriptor(String name, String path, PluginDescriptor plugin) {
        this.name = name;
        this.path = path;
        this.plugin = plugin;

        this.excluded = null;
        this.node = true;

        multiple = protect = binary = mandatory = ordered = false;
    }

    public FieldDescriptor(Map<String, Object> map, TemplateEngine engine) {
        this.name = (String) map.get("name");
        this.path = (String) map.get("path");
        this.excluded = (Set<String>) map.get("excluded");
        if (map.get("template") != null)
            this.template = engine.getConfig().getTemplate((String) map.get("template"));
        if (map.get("field") != null)
            this.field = new FieldDescriptor((Map) map.get("field"), engine);
        this.plugin = new PluginDescriptor((Map) map.get("plugin"), engine.getChannelFactory().createChannel());

        this.node = ((Boolean) map.get("isNode")).booleanValue();
        this.multiple = ((Boolean) map.get("multiple")).booleanValue();
        this.binary = ((Boolean) map.get("binary")).booleanValue();
        this.protect = ((Boolean) map.get("protect")).booleanValue();
        this.mandatory = ((Boolean) map.get("mandatory")).booleanValue();
        this.ordered = ((Boolean) map.get("ordered")).booleanValue();
    }

    public Map<String, Object> getMapRepresentation() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("name", getName());
        map.put("path", getPath());
        map.put("excluded", getExcluded());
        if (getTemplate() != null)
            map.put("template", getTemplate().getName());
        if (getField() != null)
            map.put("field", getField().getMapRepresentation());
        map.put("plugin", getPlugin().getMapRepresentation());

        map.put("isNode", new Boolean(node));
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public TemplateDescriptor getTemplate() {
        return template;
    }

    public void setTemplate(TemplateDescriptor template) {
        this.template = template;
    }

    public FieldDescriptor getField() {
        return field;
    }

    public void setField(FieldDescriptor field) {
        this.field = field;
    }

    public PluginDescriptor getPlugin() {
        return plugin;
    }

    public void setPlugin(PluginDescriptor plugin) {
        this.plugin = plugin;
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

    public boolean isNode() {
        return node;
    }

    public void setIsNode(boolean isNode) {
        this.node = isNode;
    }

    public boolean isOrdered() {
        return node;
    }

    public void setIsOrdered(boolean isOrdered) {
        this.ordered = isOrdered;
    }

    public String[] getConstraints() {
        return constraints;
    }

    public Set<String> getExcluded() {
        return excluded;
    }

    public void setExcluded(Set<String> set) {
        excluded = set;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).append("name", name).append("path", path)
                .toString();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof FieldDescriptor == false) {
            return false;
        }
        if (this == object) {
            return true;
        }
        FieldDescriptor fieldDescriptor = (FieldDescriptor) object;
        return new EqualsBuilder().append(name, fieldDescriptor.name).append(path, fieldDescriptor.path).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(421, 23).append(name).append(path).toHashCode();
    }
}
