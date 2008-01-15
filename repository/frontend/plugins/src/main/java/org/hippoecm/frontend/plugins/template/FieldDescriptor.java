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
package org.hippoecm.frontend.plugins.template;

import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.wicket.IClusterable;

public class FieldDescriptor implements IClusterable {
    private static final long serialVersionUID = 1L;

    private String name;
    private String path;
    private String type;
    private String renderer;

    private Value[] defaults;
    private String[] constraints;

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

    public FieldDescriptor(String name, String path, String type, String renderer) {
        this.name = name;
        this.path = path;
        this.type = type;
        this.renderer = renderer;

        multiple = prot = binary = mandatory = false;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public String getType() {
        return type;
    }

    public String getRenderer() {
        return renderer;
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

    public boolean isNode() {
        return node;
    }

    public String[] getConstraints() {
        return constraints;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).
            append("name", name).
            append("path", path).
            append("type", type).
            append("renderer", renderer).
            toString();
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
        return new EqualsBuilder().append(name, fieldDescriptor.name).
                append(path, fieldDescriptor.path).
                append(type, fieldDescriptor.type).
                append(renderer, fieldDescriptor.renderer).
                isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(421, 23).append(name).append(path).append(type).append(renderer).toHashCode();
    }
}
