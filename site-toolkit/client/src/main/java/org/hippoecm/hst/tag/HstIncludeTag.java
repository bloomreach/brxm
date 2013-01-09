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

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.util.HstRequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Supporting class for including the content of a child component window.
 * @version $Id$
 */
public class HstIncludeTag extends TagSupport {
    
    private static final long serialVersionUID = 1L;
    
    private final static Logger log = LoggerFactory.getLogger(HstIncludeTag.class);

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
        try {
            HttpServletRequest servletRequest = (HttpServletRequest) pageContext.getRequest();
            HttpServletResponse servletResponse = (HttpServletResponse) pageContext.getResponse();
            HstRequest hstRequest = HstRequestUtils.getHstRequest(servletRequest);
            HstResponse hstResponse = HstRequestUtils.getHstResponse(servletRequest, servletResponse);

            if (hstRequest == null || hstResponse == null) {
                return EVAL_PAGE;
            }

            try {
                JspWriter writer = pageContext.getOut();
                writer.flush();
                hstResponse.flushChildContent(ref);
            } catch (IOException e) {
                if (log.isDebugEnabled()) {
                    log.warn("Exception happened while including child content for '"+ref+"'.", e);
                } else {
                    log.warn("Exception happened while including child content for '{}' : {}",ref, e.toString());
                }
            }

            return EVAL_PAGE;
        } finally {
            cleanup();
        }
    }

    protected void cleanup() {
        ref = null;
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
