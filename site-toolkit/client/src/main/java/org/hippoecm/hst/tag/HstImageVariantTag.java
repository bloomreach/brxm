/*
 *  Copyright 2012 Hippo.
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
public class HstImageVariantTag extends BodyTagSupport {
    
    private static final long serialVersionUID = 1L;

    /**
     * the name of the image variant to use
     */
    protected String name = null;

    /**
     * the name of variants to replace. If <code>null</code> all variants will be replaced
     */
    protected String replace = null;

    /**
     * whether to fallback to original variant when the <code>name</code> variant does not exist. Default false
     */
    protected boolean fallback = false;

    /**
     * The html tag this image variant tag is part of
     */
    HstHtmlTag htmlTag;

    @Override
    public int doStartTag() throws JspException{

        htmlTag = (HstHtmlTag) findAncestorWithClass(this, HstHtmlTag.class);

        if (htmlTag == null) {
            throw new JspException("the 'imagevariant' Tag must have a HST's 'html' tag as a parent");
        }
        return SKIP_BODY;
    }
    

    @Override
    public int doEndTag() throws JspException{

        htmlTag.setImageVariant(name, replace, fallback);
        name = null;
        replace = null;
        htmlTag = null;
        fallback = false;
        return EVAL_PAGE;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setReplace(final String replace) {
        this.replace = replace;
    }

    public void setFallback(final boolean fallback) {
        this.fallback = fallback;
    }
}
