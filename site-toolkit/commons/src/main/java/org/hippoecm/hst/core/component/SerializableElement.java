/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * SerializableElement
 * <P>
 * Serializable representation for a DOM element and its descendants.
 * </P>
 */
public class SerializableElement implements Serializable {

    private static final long serialVersionUID = 1L;

    private String tagName;
    private Map<String, String> attributes;
    private String textContent;
    private Collection<SerializableElement> childElements;

    public SerializableElement() {
    }

    public SerializableElement(String tagName) {
        this.tagName = tagName;
    }

    public SerializableElement(final Element element) {
        tagName = element.getTagName();

        if (attributes == null) {
            attributes = new HashMap<String, String>();
        } else {
            attributes.clear();
        }

        NamedNodeMap attrs = element.getAttributes();
        int length = attrs.getLength();

        for (int i = 0; i < length; i++) {
            Attr attr = (Attr) attrs.item(i);
            attributes.put(attr.getName(), attr.getValue());
        }

        textContent = element.getTextContent();

        NodeList nodeList = element.getChildNodes();
        length = nodeList.getLength();

        for (int i = 0; i < length; i++) {
            Node node = nodeList.item(i);

            if (node.getNodeType() == Node.ELEMENT_NODE) {
                if (childElements == null) {
                    childElements = new ArrayList<SerializableElement>();
                }

                childElements.add(new SerializableElement((Element) node));
            }
        }
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public boolean hasAttribute(String name) {
        if (attributes == null) {
            return false;
        }

        return attributes.containsKey(name);
    }

    public String getAttribute(String name) {
        if (attributes == null) {
            return null;
        }

        return attributes.get(name);
    }

    public Map<String, String> getAttributeMap() {
        if (attributes == null) {
            attributes = new HashMap<String, String>();
        }

        return Collections.unmodifiableMap(attributes);
    }

    public void setAttribute(String name, String value) {
        if (attributes == null) {
            attributes = new HashMap<String, String>();
        }

        attributes.put(name, value);
    }

    public String removeAttribute(String name) {
        if (attributes == null) {
            return null;
        }

        return attributes.remove(name);
    }

    public String getTextContent() {
        return textContent;
    }

    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }

    public boolean hasChildElements() {
        return (childElements != null && !childElements.isEmpty());
    }

    public Collection<? extends SerializableElement> getChildElements() {
        if (childElements != null) {
            return childElements;
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        SerializableElement cloned = (SerializableElement) super.clone();

        cloned.tagName = tagName;
        cloned.attributes = new HashMap<String, String>(attributes);
        cloned.textContent = textContent;

        if (childElements != null) {
            cloned.childElements = new ArrayList<SerializableElement>();

            for (SerializableElement child : childElements) {
                cloned.childElements.add((SerializableElement) child.clone());
            }
        }

        return cloned;
    }
}
