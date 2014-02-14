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

package org.onehippo.cms7.essentials.dashboard.rest;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.PropertyType;
import javax.xml.bind.annotation.XmlRootElement;

import org.onehippo.cms7.essentials.dashboard.rest.Restful;

/**
 * @version "$Id$"
 */
@XmlRootElement(name = "property")
public class PropertyRestful implements Restful {

    private static final long serialVersionUID = 1L;
    private String name;
    private int type = PropertyType.STRING;
    private String value;
    private boolean multivalue;
    private List<String> values;

    public PropertyRestful(final String name) {
        this.name = name;
    }

    public PropertyRestful(final String name, final String value) {
        this.name = name;
        this.value = value;
    }

    public PropertyRestful(final String name, final String value, final int type) {
        this.name = name;
        this.value = value;
        this.type = type;
    }

    public PropertyRestful() {
    }

    public int getType() {
        return type;
    }

    public void setType(final int type) {
        this.type = type;
    }

    public void addValue(final String value) {
        if (values == null) {
            values = new ArrayList<>();
        }
        values.add(value);

    }

    public boolean isMultivalue() {
        return multivalue;
    }

    public void setMultivalue(final boolean multivalue) {
        this.multivalue = multivalue;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(final List<String> values) {
        this.values = values;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }


    public void setValue(final String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PropertyRestful{");
        sb.append("name='").append(name).append('\'');
        sb.append(", type=").append(type);
        sb.append(", value='").append(value).append('\'');
        sb.append(", multivalue=").append(multivalue);
        sb.append(", values=").append(values);
        sb.append('}');
        return sb.toString();
    }
}

