/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * HeadElementImpl
 * 
 * @version $Id$
 */
public class HeadElementImpl implements HeadElement
{
    
    private static final long serialVersionUID = 1L;
    
    private String tagName;
    private Map<String, String> attributes;
    private String textContent;
    private Collection<HeadElement> childHeadElements;
    
    public HeadElementImpl()
    {
    }
    
    public HeadElementImpl(String tagName)
    {
        this.tagName = tagName;
    }
    
    public HeadElementImpl(final Node element)
    {
        textContent = element.getTextContent();
        tagName = element.getNodeName();

        if (attributes == null)
        {
            attributes = new HashMap<String, String>();
        }
        else
        {
            attributes.clear();
        }
        
        NamedNodeMap attrs = element.getAttributes();
        if (attrs != null) {
            for (int i = 0; i < attrs.getLength(); i++)
            {
                Attr attr = (Attr) attrs.item(i);
                attributes.put(attr.getName(), attr.getValue());
            }
        }

        
        NodeList nodeList = element.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++)
        {
            Node node = nodeList.item(i);
            
            if ("SCRIPT".equals(element.getNodeName().toUpperCase()) || node.getNodeType() == Node.ELEMENT_NODE)
            {
                if (childHeadElements == null)
                {
                    childHeadElements = new ArrayList<HeadElement>();
                }
                
                childHeadElements.add(new HeadElementImpl(node));
            }
        }
    }
    
    public String getTagName()
    {
        return tagName;
    }
    
    public void setTagName(String tagName) {
        this.tagName = tagName;
    }
    
    public boolean hasAttribute(String name)
    {
        if (attributes == null) {
            return false;
        }
        
        return attributes.containsKey(name);
    }
    
    public String getAttribute(String name)
    {
        if (attributes == null) {
            return null;
        }
        
        return attributes.get(name);
    }
    
    public Map<String, String> getAttributeMap()
    {
        if (attributes == null) {
            return Collections.emptyMap();
        }
        
        return Collections.unmodifiableMap(attributes);
    }
    
    public void setAttribute(String name, String value) {
        if (attributes == null) {
            attributes = new LinkedHashMap<String, String>();
        }
        
        attributes.put(name, value);
    }
    
    public String removeAttribute(String name) {
        if (attributes == null) {
            return null;
        }
        
        return attributes.remove(name);
    }
    
    public String getTextContent()
    {
        return textContent;
    }
    
    public void setTextContent(String textContent)
    {
        this.textContent = textContent;
    }
    
    public boolean hasChildHeadElements()
    {
        return (childHeadElements != null && !childHeadElements.isEmpty());
    }
    
    public Collection<HeadElement> getChildHeadElements()
    {
        if (childHeadElements != null)
        {
            return childHeadElements;
        }
        else
        {
            return Collections.emptyList();
        }
    }
    
    @Override
    public Object clone() throws CloneNotSupportedException {
        HeadElementImpl cloned = (HeadElementImpl) super.clone();
        
        cloned.tagName = tagName;
        cloned.attributes = new LinkedHashMap<String, String>(attributes);
        cloned.textContent = textContent;
        
        if (childHeadElements != null) {
            cloned.childHeadElements = new ArrayList<HeadElement>();
            
            for (HeadElement child : childHeadElements) {
                cloned.childHeadElements.add((HeadElement) child.clone());
            }
        }
        
        return cloned;
    }
}
