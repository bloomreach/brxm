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

package org.onehippo.cms7.essentials.dashboard.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.onehippo.cms7.essentials.dashboard.rest.Restful;

/**
 * @version "$Id$"
 */
@XmlRootElement(name = "property")
public class PropertyRestful<T> implements Restful {

    private static final long serialVersionUID = 1L;
    private String name;
    private T value;
    private boolean multivalue;
    private List<T> values;

    public PropertyRestful(final String name, final T value) {
        this.name = name;
        this.value = value;
    }

    public PropertyRestful() {
    }

    public void addValue(final T value) {
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

    public List<T> getValues() {
        return values;
    }

    public void setValues(final List<T> values) {
        this.values = values;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public T getValue() {
        return value;
    }

    public void setValue(final T value) {
        this.value = value;
    }
}
