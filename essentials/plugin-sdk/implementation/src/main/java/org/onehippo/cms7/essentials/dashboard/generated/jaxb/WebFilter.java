/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.generated.jaxb;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "filter", namespace = WebXml.NS)
public class WebFilter {
    private String name;
    private String filterClass;

    @XmlElement(name = "filter-name", namespace = WebXml.NS)
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @XmlElement(name = "filter-class", namespace = WebXml.NS)
    public String getFilterClass() {
        return filterClass;
    }

    public void setFilterClass(final String filterClass) {
        this.filterClass = filterClass;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("WebFilter{");
        sb.append("name='").append(name).append("'");
        sb.append(", filterClass='").append(filterClass).append("'");
        sb.append('}');
        return sb.toString();
    }
}
