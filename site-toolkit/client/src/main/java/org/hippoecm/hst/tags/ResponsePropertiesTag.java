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
package org.hippoecm.hst.tags;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.jsp.tagext.VariableInfo;

import org.hippoecm.hst.core.component.HstResponse;

public class ResponsePropertiesTag extends TagSupport {

    private static final long serialVersionUID = 1L;
    
    protected String var;
    
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
        if (pageContext.getAttribute(attributeName) == null){   //Set attributes only once
 
            pageContext.setAttribute(attributeName,
                                     attribute,
                                     PageContext.PAGE_SCOPE);
        }
    }
      
    public int doEndTag() throws JspException {
        HstResponse hstResponse = null;
        
        if (pageContext.getResponse() instanceof HstResponse) {
            hstResponse = (HstResponse) pageContext.getResponse();
        }

        if (hstResponse != null) {
            setAttribute(hstResponse.getProperties(), getVar());
        }
        
        return SKIP_BODY;
    }
    
    public void setVar(String var) {
        this.var = var;
    }
    
    public String getVar() {
        return this.var;
    }

    /* -------------------------------------------------------------------*/
    
    /**
     * TagExtraInfo class for HstContentTag.
     */
    public static class TEI extends TagExtraInfo {
        
        public VariableInfo[] getVariableInfo(TagData tagData) {
            VariableInfo vi[] = null;
            String var = tagData.getAttributeString("var");
            if (var != null) {
                vi = new VariableInfo[1];
                vi[0] =
                    new VariableInfo(var, "java.lang.String", true,
                                 VariableInfo.AT_BEGIN);
            }
            return vi;
        }

    }

}
