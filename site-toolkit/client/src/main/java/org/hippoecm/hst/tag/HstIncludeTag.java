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

import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.container.ContainerConstants;

/**
 * Abstract supporting class for Hst URL tags (action, redner and resource)
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

        // if hstResponse is retrieved, then this servlet has been dispatched by hst component.
        HstResponse hstResponse = (HstResponse) pageContext.getRequest().getAttribute(ContainerConstants.HST_RESPONSE);
        
        if (hstResponse != null) {
            try {
                this.pageContext.getOut().flush();
                hstResponse.flushChildContent(this.ref);
            } catch (IOException e) {
            }
        }
        
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
