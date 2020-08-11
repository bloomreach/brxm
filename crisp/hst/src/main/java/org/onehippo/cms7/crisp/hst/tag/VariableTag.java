/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.crisp.hst.tag;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * A tag handler for the <CODE>variable</CODE> tag.
 */
public class VariableTag extends TagSupport {

    private static final long serialVersionUID = 1L;

    private String name = null;
    private String value = null;

    /**
     * {@inheritDoc}
     */
    @Override
    public int doStartTag() throws JspException {
        try {
            VariableContainerTag paramContainerTag = (VariableContainerTag) findAncestorWithClass(this,
                    VariableContainerTag.class);

            if (paramContainerTag == null) {
                throw new JspException("the 'param' Tag must have a ParamContainerTag as a parent");
            }

            paramContainerTag.addVariable(getName(), getValue());
            return SKIP_BODY;
        } finally {
            cleanup();
        }
    }

    protected void cleanup() {
        name = null;
        value = null;
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
