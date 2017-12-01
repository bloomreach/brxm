/*
 * Copyright 2014-2017 Hippo B.V. (http://www.onehippo.com)
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

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "web-app", namespace = WebXml.NS)
public class WebXml {
    static final String NS = "http://java.sun.com/xml/ns/javaee";

    private List<WebFilter> filters;
    private List<WebFilterMapping> filterMappings;

    @XmlElement(name = "filter", namespace = NS)
    public List<WebFilter> getFilters() {
        return filters;
    }

    public void setFilters(final List<WebFilter> filters) {
        this.filters = filters;
    }

    @XmlElement(name = "filter-mapping", namespace = NS)
    public List<WebFilterMapping> getFilterMappings() {
        return filterMappings;
    }

    public void setFilterMappings(final List<WebFilterMapping> filterMappings) {
        this.filterMappings = filterMappings;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("WebXml{");
        sb.append("filters=").append(filters);
        sb.append('}');
        return sb.toString();
    }
}
