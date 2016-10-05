/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.repository.mock;

import javax.jcr.PropertyType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

public class XmlProperty {

    @XmlAttribute(name = "name", namespace = MockNode.NAMESPACE_JCR_SV)
    @SuppressWarnings("unused")
    private String name;

    @XmlAttribute(name = "type", namespace = MockNode.NAMESPACE_JCR_SV)
    @SuppressWarnings("unused")
    private String type;

    @XmlAttribute(name = "multiple", namespace = MockNode.NAMESPACE_JCR_SV)
    @SuppressWarnings("unused")
    private boolean multiple;

    @XmlElement(name = "value", namespace = MockNode.NAMESPACE_JCR_SV)
    @SuppressWarnings("unused")
    private String[] values;


    public String getName() {
        return name;
    }

    public int getType() {
        return PropertyType.valueFromName(type);
    }

    public boolean isMultiple() {
        return multiple;
    }

    public String[] getValues() {
        return values;
    }

    public String getValue() {
        return values.length > 0 ? values[0] : null;
    }

}
