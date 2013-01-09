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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.jsp.tagext.VariableInfo;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.util.HstRequestUtils;

public class DefineObjectsTag extends TagSupport {
    
    private static final long serialVersionUID = 1L;

    /**
     * Helper method.
     * <p>
     * Sets an pageContext attribute with <CODE>PAGE_SCOPE</CODE>.
     * 
     * @param attribute - the attribute object to set
     * @param attributeName - the name of the attribute object
     * 
     * @return void
     */
    private void setAttribute(Object attribute, String attributeName){
        if (pageContext.getAttribute(attributeName) == null) {   //Set attributes only once
            pageContext.setAttribute(attributeName,
                                     attribute,
                                     PageContext.PAGE_SCOPE);
        }
    }
    
    /**
     * Processes the <CODE>defineObjects</CODE> tag.
     * @return <CODE>SKIP_BODY</CODE>
     */
    public int doStartTag() throws JspException {
        
        HttpServletRequest servletRequest = (HttpServletRequest) pageContext.getRequest();
        HttpServletResponse servletResponse = (HttpServletResponse) pageContext.getResponse();
        HstRequest hstRequest = HstRequestUtils.getHstRequest(servletRequest);
        HstResponse hstResponse = HstRequestUtils.getHstResponse(servletRequest, servletResponse);
        
        // set attribute hstRequest
        setAttribute(hstRequest, "hstRequest");
        // set attribute hstResponse
        setAttribute(hstResponse, "hstResponse");
        
        // needed to loop through child content nodes in freemarker templates
        setAttribute(hstResponse.getChildContentNames(), "hstResponseChildContentNames");
        
        return SKIP_BODY;
    }
    
    /**
     * TagExtraInfo class for DefineObjectsTag.
     *
     */
    public static class TEI extends TagExtraInfo {

        public VariableInfo[] getVariableInfo(TagData tagData) {
            VariableInfo[] info = new VariableInfo[]{
                new VariableInfo("hstRequest",
                                 "org.hippoecm.hst.core.component.HstRequest",
                                 true,
                                 VariableInfo.AT_BEGIN),
                new VariableInfo("renderResponse",
                                 "org.hippoecm.hst.core.component.HstResponse",
                                 true,
                                 VariableInfo.AT_BEGIN)
            };
            
            return info;
        }
    }
}
