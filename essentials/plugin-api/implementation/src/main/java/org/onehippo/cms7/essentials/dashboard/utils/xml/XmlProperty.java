/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.utils.xml;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;


@XmlRootElement(name = "property", namespace = EssentialConst.URI_JCR_NAMESPACE)
public class XmlProperty implements NodeOrProperty {

    private static final long serialVersionUID = 1L;

    private Collection<String> values;

    private Boolean multiple;
    private String name;
    private String type;
    private String merge;
    private String location;


    public XmlProperty() {
    }

    public XmlProperty(final String name, final String type) {
        this.name = name;
        this.type = type;
    }

    public XmlProperty(final String name, final String type, final Boolean multiple) {
        this.name = name;
        this.type = type;
        this.multiple = multiple;
    }

    public XmlProperty(final String type) {
        this.type = type;
    }

    public Collection<String> getValues() {
        if (values == null) {
            values = new LinkedList<>();
        }
        return values;
    }

    @XmlElement(name = "value")
    public void setValues(final Collection<String> values) {
        this.values = values;
    }

    public void addValue(final String value) {
        getValues().add(value);
    }


    public String getSingleValue() {
        final Iterator<String> iterator = getValues().iterator();
        if (iterator.hasNext()) {
            return iterator.next();
        }
        return null;
    }

    @Override
    @XmlAttribute(name = "multiple", namespace = EssentialConst.URI_JCR_NAMESPACE)
    public Boolean getMultiple() {
        if (multiple == null) {
            return Boolean.FALSE;
        }
        return multiple;
    }


    @Override
    public XmlProperty getPropertyForName(final String propertyName) {
        return null;
    }


    public void setMultiple(Boolean value) {
        this.multiple = value;
    }

    @XmlAttribute(name = "name", namespace = EssentialConst.URI_JCR_NAMESPACE, required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "NMTOKEN")
    public String getName() {
        return name;
    }

    public void setName(String value) {
        this.name = value;
    }

    @Override
    @XmlAttribute(name = "type", namespace = EssentialConst.URI_JCR_NAMESPACE, required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "NCName")
    public String getType() {
        return type;
    }

    public void setType(String value) {
        this.type = value;
    }


    @XmlAttribute(name = "merge", namespace = EssentialConst.URI_AUTOEXPORT_NAMESPACE, required = false)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    public String getMerge() {
        return merge;
    }

    public void setMerge(final String merge) {
        this.merge = merge;
    }

    @XmlAttribute(name = "location", namespace = EssentialConst.URI_AUTOEXPORT_NAMESPACE, required = false)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    public String getLocation() {
        return location;
    }

    public void setLocation(final String location) {
        this.location = location;
    }

    @XmlTransient
    @Override
    public Collection<NodeOrProperty> getXmlNodeOrXmlProperty() {
        return Collections.emptyList();
    }

    @Override
    public boolean isNode() {
        return false;
    }

    @Override
    public boolean isProperty() {
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("XmlProperty{");

        sb.append("type='").append(type).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", merge='").append(merge).append('\'');
        sb.append("values=").append(values);
        sb.append(", multiple=").append(multiple);
        sb.append(", location='").append(location).append('\'');
        sb.append('}');
        return sb.toString();
    }


}