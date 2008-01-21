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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.wicket.IClusterable;

public class TemplateDescriptor implements IClusterable {

    private static final long serialVersionUID = 1L;

    private String name;
    private LinkedHashMap<String, FieldDescriptor> fields;

    public TemplateDescriptor(String name, List<FieldDescriptor> fields) {
        this.name = name;
        this.fields = new LinkedHashMap<String, FieldDescriptor>(fields.size());
        for (FieldDescriptor desc : fields) {
            this.fields.put(desc.getName(), desc);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Iterator<FieldDescriptor> getFieldIterator() {
        return fields.values().iterator();
    }

    public boolean hasField(String fieldName) {
        return fields.containsKey(fieldName);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).append("name", name).append("fields", fields)
                .toString();
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
        return new EqualsBuilder().append(name, templateDescriptor.name).append(fields, templateDescriptor.fields)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(113, 419).append(name).append(fields).toHashCode();
    }
}
