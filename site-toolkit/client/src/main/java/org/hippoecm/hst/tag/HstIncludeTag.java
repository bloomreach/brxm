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

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagSupport;

import org.hippoecm.hst.core.component.HeadElement;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.util.HeadElementUtils;
import org.hippoecm.hst.util.HstRequestUtils;

/**
 * Supporting class for including the content of a child component window.
 * @version $Id$
 */
public class HstIncludeTag extends TagSupport {
    
    private static final long serialVersionUID = 1L;
    
    protected String ref = null;
    
    /* (non-Javadoc)
     * @see javax.servlet.jsp.tagext.TagSupport#doStartTag()
     */
    @Override
    public int doStartTag() throws JspException{
        return EVAL_BODY_INCLUDE;
    }
    
    
    /* (non-Javadoc)
     * @see javax.servlet.jsp.tagext.TagSupport#doEndTag()
     */
    @Override
    public int doEndTag() throws JspException{

        HttpServletRequest servletRequest = (HttpServletRequest) pageContext.getRequest();
        HttpServletResponse servletResponse = (HttpServletResponse) pageContext.getResponse();
        HstRequest hstRequest = HstRequestUtils.getHstRequest(servletRequest);
        HstResponse hstResponse = HstRequestUtils.getHstResponse(servletRequest, servletResponse);
        
        if (hstRequest == null || hstResponse == null) {
            return EVAL_PAGE;
        }
        
        ContentRenderingContext ctx = (ContentRenderingContext) pageContext.getAttribute(ContentRenderingContext.NAME, PageContext.PAGE_SCOPE);
        if (ctx == null) {
            ctx = (ContentRenderingContext) pageContext.getAttribute(ContentRenderingContext.NAME, PageContext.REQUEST_SCOPE);
            if (ctx == null) {
                ctx = (ContentRenderingContext) hstRequest.getRequestContext().getAttribute(ContentRenderingContext.NAME);
            }
        }
        HeadElement wrapperElem = null;
        if (ctx != null) {
            wrapperElem = ctx.getWrapperElement();
        }
        
        try {
            JspWriter writer = pageContext.getOut();
            
            if (wrapperElem != null) {
                wrapperElem.setAttribute(ctx.getContentNameAttribute(), ref);
                writer.write('<');
                writer.write(wrapperElem.getTagName());
                for (Map.Entry<String, String> entry : wrapperElem.getAttributeMap().entrySet()) {
                    writer.write(' ');
                    writer.write(entry.getKey());
                    writer.write("=\"");
                    writer.write(HeadElementUtils.encode(entry.getValue()));
                    writer.write("\"");
                }
                writer.write('>');
            }
            
            writer.flush();
            
            hstResponse.flushChildContent(ref);
            
            if (wrapperElem != null) {
                writer.write("</");
                writer.write(wrapperElem.getTagName());
                writer.write('>');
            }
        } catch (IOException e) {
        }
        
        ref = null;
        
        return EVAL_PAGE;
    }
    
    /**
     * Returns the referenced name of the child window content to include
     * @return String
     */
    public String getRef() {
        return this.ref;
    }
    
    /**
     * Sets the ref property.
     * @param ref The referenced name of the child window content to include
     * @return void
     */
    public void setRef(String ref) {
        this.ref = ref;
    }
    
}
