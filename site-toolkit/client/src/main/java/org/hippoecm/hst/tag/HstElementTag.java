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
package org.hippoecm.hst.tag;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.VariableInfo;

import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.w3c.dom.Element;

/**
 * Creating DOM Element Supporting Tag
 */
public class HstElementTag extends BodyTagSupport {
    
    private static final long serialVersionUID = 1L;

    protected String var = null;
    
    protected String name = null;
    
    protected Element element = null;
    
    protected Element parentElement = null;
    
    /* (non-Javadoc)
     * @see javax.servlet.jsp.tagext.TagSupport#doStartTag()
     */
    @Override
    public int doStartTag() throws JspException{
        
        if (var != null) {
            pageContext.removeAttribute(var, PageContext.PAGE_SCOPE);
        }
        
        HstElementTag parentElementTag = (HstElementTag) findAncestorWithClass(this, HstElementTag.class);

        if (parentElementTag != null) {
            parentElement = parentElementTag.getElement();
        }

        if (name != null && !"".equals(name)) {
            // if hstResponse is retrieved, then this servlet has been dispatched by hst component.
            HstResponse hstResponse = (HstResponse) pageContext.getRequest().getAttribute(ContainerConstants.HST_RESPONSE);

            if (hstResponse == null && pageContext.getResponse() instanceof HstResponse) {
                hstResponse = (HstResponse) pageContext.getResponse();
            }

            if (hstResponse != null) {
                element = hstResponse.createElement(name);
            }
        } else {
            throw new JspException("Invalid element name: " + name);
        }
        
        if (element != null) {
            if (parentElement != null) {
                parentElement.appendChild(element);
            }

            if (var != null && !"".equals(var)) {
                pageContext.setAttribute(var, element, PageContext.PAGE_SCOPE);
            } else {
                throw new JspException("Invalid variable name: " + var);
            }
        }
        
        return EVAL_BODY_BUFFERED;
    }
    
    
    /* (non-Javadoc)
     * @see javax.servlet.jsp.tagext.TagSupport#doEndTag()
     */
    @Override
    public int doEndTag() throws JspException{
        try {
            if (bodyContent != null && bodyContent.getString() != null) {
                String textContent = bodyContent.getString();

                if (element != null) {
                    element.appendChild(element.getOwnerDocument().createTextNode(textContent));
                }
            }
            return EVAL_PAGE;
        } finally {
            cleanup();
        }
    }

    protected void cleanup() {
        var = null;
        name = null;
        element = null;
        parentElement = null;
        bodyContent = null;
    }


    /* (non-Javadoc)
    * @see javax.servlet.jsp.tagext.TagSupport#release()
    */
    @Override
    public void release(){
        super.release();        
    }
    
    /**
     * Returns the var property.
     * @return String
     */
    public String getVar() {
        return var;
    }
    
    /**
     * Sets the var property.
     * @param var The var to set
     * @return void
     */
    public void setVar(String var) {
        this.var = var;
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
     * Returns the created element
     * @return
     */
    public Element getElement() {
        return this.element;
    }
    
    /* -------------------------------------------------------------------*/
        
    /**
     * TagExtraInfo class for HstURLTag.
     */
    public static class TEI extends TagExtraInfo {
        
        public VariableInfo[] getVariableInfo(TagData tagData) {
            VariableInfo vi[] = null;
            String var = tagData.getAttributeString("var");
            if (var != null) {
                vi = new VariableInfo[1];
                vi[0] =
                    new VariableInfo(var, "org.w3c.dom.Element", true,
                                 VariableInfo.AT_BEGIN);
            }
            return vi;
        }

    }
}
