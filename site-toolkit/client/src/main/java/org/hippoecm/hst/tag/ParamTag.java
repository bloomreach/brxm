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
import javax.servlet.jsp.tagext.TagSupport;


/**
 * A tag handler for the <CODE>param</CODE> tag. Defines a parameter that
 * can be added to a <CODE>HstURL</CODE>
 * <BR>The following attributes are mandatory:
 *   <UL>
 *       <LI><CODE>name</CODE>
 *       <LI><CODE>value</CODE>
 *   </UL>
 */
public class ParamTag extends TagSupport {
    
    private static final long serialVersionUID = 1L;

    private String name = null;
    private String value = null;

    /* (non-Javadoc)
     * @see javax.servlet.jsp.tagext.TagSupport#doStartTag()
     */
    @Override
    public int doStartTag() throws JspException {
        BaseHstURLTag urlTag = (BaseHstURLTag)
                findAncestorWithClass(this, BaseHstURLTag.class);

        if (urlTag == null) {
            throw new JspException("the 'param' Tag must have a HST's url tag as a parent");
        }

        urlTag.addParameter(getName(), getValue());

        return SKIP_BODY;
    }

    /**
     * Returns the name.
     * @return String
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the value.
     * @return String
     */
    public String getValue() throws JspException {
        if (value == null) {
            value = "";
        }
        return value;
    }

    /**
     * Sets the name.
     * @param name The name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the value.
     * @param value The value to set
     */
    public void setValue(String value) {
        this.value = value;
    }

}
