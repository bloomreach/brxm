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

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.container.ContainerConstants;

/**
 * This tag produces a unique value for the current HST component.
 * <p/>
 * <p/>
 * A tag handler for the <CODE>namespace</CODE> tag. writes a unique value
 * for the current HstComponent <BR>This tag has no attributes
 */
public class NamespaceTag extends TagSupport {
    
    private static final long serialVersionUID = 1L;

    /* (non-Javadoc)
     * @see javax.servlet.jsp.tagext.Tag#doStartTag()
     */
    public int doStartTag() throws JspException {
        
        String namespace = "";
        
        // if hstResponse is retrieved, then this servlet has been dispatched by hst component.
        HstResponse hstResponse = (HstResponse) pageContext.getRequest().getAttribute(ContainerConstants.HST_RESPONSE);
        
        if (hstResponse == null && pageContext.getResponse() instanceof HstResponse) {
            hstResponse = (HstResponse) pageContext.getResponse();
        }
        
        if (hstResponse != null) {
            namespace = hstResponse.getNamespace();
        }
        
        JspWriter writer = pageContext.getOut();
        
        try {
            writer.print(namespace);
        } catch (IOException ioe) {
            throw new JspException("Unable to write namespace", ioe);
        }
        
        return SKIP_BODY;
    }
}
