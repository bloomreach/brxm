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

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "node", namespace = XmlNode.NAMESPACE_JCR_SV)
public class XmlNode {

    static final String NAMESPACE_JCR_SV = "http://www.jcp.org/jcr/sv/1.0";

    @XmlAttribute(name = "name", namespace = NAMESPACE_JCR_SV)
    @SuppressWarnings("unused")
    private String name;

    @XmlElement(name = "property", namespace = NAMESPACE_JCR_SV)
    @SuppressWarnings("unused")
    private List<XmlProperty> properties;

    @XmlElement(name = "node", namespace = NAMESPACE_JCR_SV)
    @SuppressWarnings("unused")
    private List<XmlNode> children;


    public String getName() {
        return name;
    }

    public List<XmlProperty> getProperties() {
        return listOrEmptyList(properties);
    }

    public List<XmlNode> getChildren() {
        return listOrEmptyList(children);
    }

    private static <T> List<T> listOrEmptyList(List<T> list) {
        if (list == null) {
            return Collections.emptyList();
        }
        return list;
    }

}
