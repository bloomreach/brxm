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
package org.hippoecm.hst.tag;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.w3c.dom.Element;

/**
 * Creating DOM Element Attribute Supporting Tag
 */
public class HstAttributeTag extends BodyTagSupport {
    
    private static final long serialVersionUID = 1L;

    protected String name = null;
    
    protected String value = null;
    
    protected Element element = null;
    
    /* (non-Javadoc)
     * @see javax.servlet.jsp.tagext.TagSupport#doStartTag()
     */
    @Override
    public int doStartTag() throws JspException{
        
        HstElementTag elementTag = (HstElementTag) findAncestorWithClass(this, HstElementTag.class);

        if (elementTag == null) {
            throw new JspException("the 'attribute' Tag must have a HST's element tag as a parent");
        } else {
            element = elementTag.getElement();
        }
        
        if (element == null) {
            throw new JspException("Cannot find the element tag for this attribute tag.");
        }

        return EVAL_BODY_BUFFERED;
    }
    
    
    /* (non-Javadoc)
     * @see javax.servlet.jsp.tagext.TagSupport#doEndTag()
     */
    @Override
    public int doEndTag() throws JspException{
        try {
            if (value == null) {
                if (bodyContent != null && bodyContent.getString() != null) {
                    value = bodyContent.getString().trim();
                }
            }

            if (name != null && !"".equals(name)) {
                if (value != null) {
                    element.setAttribute(name, value);
                } else {
                    element.removeAttribute(name);
                }
            } else {
                throw new JspException("Invalid attribute name: " + name);
            }

            return EVAL_PAGE;
        } finally {
            cleanup();
        }
    }

    protected void cleanup() {
        name = null;
        value = null;
        element = null;
    }

    /* (non-Javadoc)
     * @see javax.servlet.jsp.tagext.TagSupport#release()
     */
    @Override
    public void release(){
        super.release();        
    }
    
    /**
     * Returns the name property.
     * @return String
     */
    public String getName() {
        return name;
    }
    
    /**
     * Sets the name property.
     * @param name The name to set
     * @return void
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Returns the value property.
     * @return String
     */
    public String getValue() {
        return value;
    }
    
    /**
     * Sets the value property.
     * @param value The value to set
     * @return void
     */
    public void setValue(String value) {
        this.value = value;
    }
    
}
