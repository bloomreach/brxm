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
package org.hippoecm.frontend.plugins.template.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.wicket.IClusterable;

public class FieldDescriptor implements IClusterable, Cloneable {
    private static final long serialVersionUID = 1L;

    private static String[] propertyTypes = { "String", "Boolean", "Name", "Reference" };

    private String name;
    private String path;
    private String type;
    private String renderer;

    private Value[] defaults;
    private String[] constraints;
    private Set<String> excluded;

    private boolean multiple;
    private boolean binary;
    private boolean prot;
    private boolean mandatory;
    private boolean node;

    public FieldDescriptor(PropertyDefinition pd) {
        name = pd.getName();
        path = pd.getName();
        type = null;
        renderer = null;

        defaults = pd.getDefaultValues();
        constraints = pd.getValueConstraints();

        multiple = pd.isMultiple();
        prot = pd.isProtected();
        binary = pd.getRequiredType() == PropertyType.BINARY;
        mandatory = pd.isMandatory();
        node = false;
    }

    public FieldDescriptor(NodeDefinition nd) {
        name = nd.getName();
        path = nd.getName();
        if (nd.getDefaultPrimaryType() != null) {
            type = nd.getDefaultPrimaryType().getName();
        } else {
            // FIXME: throw an exception?
            type = "nt:unstructured";
        }
        renderer = null;

        defaults = null;
        constraints = new String[] {};

        multiple = nd.allowsSameNameSiblings();
        prot = nd.isProtected();
        binary = false;
        mandatory = nd.isMandatory();
        node = true;
    }

    public FieldDescriptor(String name, String path) {
        this.name = name;
        this.path = path;

        this.node = false;
        this.type = null;
        this.renderer = null;
        this.excluded = null;

        multiple = prot = binary = mandatory = false;
    }

    public FieldDescriptor(Map map) {
        this.name = (String) map.get("name");
        this.path = (String) map.get("path");
        this.type = (String) map.get("type");
        this.renderer = (String) map.get("renderer");
        this.excluded = (Set<String>) map.get("excluded");

        this.node = ((Boolean) map.get("node")).booleanValue();
        this.prot = ((Boolean) map.get("prot")).booleanValue();
        this.binary = ((Boolean) map.get("binary")).booleanValue();
        this.multiple = ((Boolean) map.get("multiple")).booleanValue();
        this.mandatory = ((Boolean) map.get("mandatory")).booleanValue();
    }

    public Map getMapRepresentation() {
        HashMap map = new HashMap();
        map.put("name", name);
        map.put("path", path);
        map.put("type", type);
        map.put("renderer", renderer);
        map.put("excluded", excluded);

        map.put("node", new Boolean(node));
        map.put("prot", new Boolean(prot));
        map.put("binary", new Boolean(binary));
        map.put("multiple", new Boolean(multiple));
        map.put("mandatory", new Boolean(mandatory));
        return map;
    }

    @Override
    public FieldDescriptor clone() {
        try {
            return (FieldDescriptor) super.clone();
        } catch (CloneNotSupportedException ex) {
            return null;
        }
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

    public void setType(String type) {
        this.type = type;
        for (String propertyType : propertyTypes) {
            if (propertyType.equals(type)) {
                return;
            }
        }
        this.node = true;
    }

    public String getType() {
        return type;
    }

    public void setRenderer(String renderer) {
        this.renderer = renderer;
    }

    public String getRenderer() {
        return renderer;
    }

    public void setMultiple(boolean multiple) {
        this.multiple = multiple;
    }

    public boolean isMultiple() {
        return multiple;
    }

    public boolean isBinary() {
        return binary;
    }

    public boolean isProtected() {
        return prot;
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
                .append("type", type).append("renderer", renderer).toString();
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
        return new EqualsBuilder().append(name, fieldDescriptor.name).append(path, fieldDescriptor.path).append(type,
                fieldDescriptor.type).append(renderer, fieldDescriptor.renderer).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(421, 23).append(name).append(path).append(type).append(renderer).toHashCode();
    }
}
