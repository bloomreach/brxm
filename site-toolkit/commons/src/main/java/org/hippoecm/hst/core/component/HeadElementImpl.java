/*
 *  Copyright 2008 Hippo.
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
    
    public HeadElementImpl(final Element element)
    {
        tagName = element.getTagName();
        
        if (attributes == null)
        {
            attributes = new HashMap<String, String>();
        }
        else
        {
            attributes.clear();
        }
        
        NamedNodeMap attrs = element.getAttributes();
        int length = attrs.getLength();
        
        for (int i = 0; i < length; i++)
        {
            Attr attr = (Attr) attrs.item(i);
            attributes.put(attr.getName(), attr.getValue());
        }
        
        textContent = element.getTextContent();
        
        NodeList nodeList = element.getChildNodes();
        length = nodeList.getLength();
        
        for (int i = 0; i < length; i++)
        {
            Node node = nodeList.item(i);
            
            if (node.getNodeType() == Node.ELEMENT_NODE)
            {
                if (childHeadElements == null)
                {
                    childHeadElements = new ArrayList<HeadElement>();
                }
                
                childHeadElements.add(new HeadElementImpl((Element) node));
            }
        }
    }
    
    public String getTagName()
    {
        return tagName;
    }
    
    public boolean hasAttribute(String name)
    {
        return attributes.containsKey(name);
    }
    
    public String getAttribute(String name)
    {
        return attributes.get(name);
    }
    
    public Map<String, String> getAttributeMap()
    {
        return Collections.unmodifiableMap(attributes);
    }
    
    public void setAttribute(String name, String value) {
        attributes.put(name, value);
    }
    
    public String removeAttribute(String name) {
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
    
}
