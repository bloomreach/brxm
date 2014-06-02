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

package org.onehippo.cms7.essentials.dashboard.generated.jaxb;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * @version "$Id$"
 */

@XmlType(name = "context-param",
        namespace = WebXml.NS,
        propOrder = {"description", "paramName", "paramValue"})
public class WebContextParam {


    private String description;
    private String paramName;
    private String paramValue;

    @XmlElement(name = "description", namespace = WebXml.NS)
    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    @XmlElement(name = "param-name", namespace = WebXml.NS)
    public String getParamName() {
        return paramName;
    }

    public void setParamName(final String paramName) {
        this.paramName = paramName;
    }

    @XmlElement(name = "param-value", namespace = WebXml.NS)
    public String getParamValue() {
        return paramValue;
    }

    public void setParamValue(final String paramValue) {
        this.paramValue = paramValue;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("WebContextParam{");
        sb.append("description='").append(description).append('\'');
        sb.append(", paramName='").append(paramName).append('\'');
        sb.append(", paramValue='").append(paramValue).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
