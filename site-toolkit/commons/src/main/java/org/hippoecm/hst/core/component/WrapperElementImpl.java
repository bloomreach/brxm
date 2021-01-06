/*
 *  Copyright 2008-2021 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

/**
 * WrapperElementImpl
 * 
 * @version $Id$
 */
public class WrapperElementImpl implements WrapperElement
{

    public static final String SKIP_ESCAPING_PREAMBLE_ELEMENT_ATTRIBUTES_KEY = WrapperElement.class.getName() + "skipEncodingPreamble";

    private String tagName;
    private Map<String, String> attributes;
    private Set<String> skipEscapingAttrs;
    private String textContent;

    public WrapperElementImpl()
    {
    }
    
    public WrapperElementImpl(String tagName)
    {
        this.tagName = tagName;
    }
    
    public WrapperElementImpl(final Element element)
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

        skipEscapingAttrs = (Set<String>) element.getUserData(SKIP_ESCAPING_PREAMBLE_ELEMENT_ATTRIBUTES_KEY);
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
    
    public String getTextContent()
    {
        return textContent;
    }
    
    public void setTextContent(String textContent)
    {
        this.textContent = textContent;
    }
    
    @Override
    public Object clone() throws CloneNotSupportedException {
        WrapperElementImpl cloned = (WrapperElementImpl) super.clone();
        
        cloned.tagName = tagName;
        cloned.attributes = new HashMap<String, String>(attributes);
        cloned.textContent = textContent;
        
        return cloned;
    }

    public Set<String> getSkipEscapingAttrs() {
        return skipEscapingAttrs == null ? Collections.emptySet() : skipEscapingAttrs;
    }
}
