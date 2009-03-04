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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.jsp.tagext.VariableInfo;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.container.HstComponentWindow;

/**
 * Abstract supporting class for Hst URL tags (action, redner and resource)
 */

public class HstContentTag extends TagSupport {
    
    private static final long serialVersionUID = 1L;

    protected String name = null;
    
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

        HttpServletRequest request = (HttpServletRequest) this.pageContext.getRequest();
        
        if (request instanceof HstRequest) {
            HstComponentWindow myWindow = ((HstRequest) request).getComponentWindow();
            HstComponentWindow childWindow = myWindow.getChildWindow(this.name);
            
            if (childWindow == null) {
                childWindow = myWindow.getChildWindowByReferenceName(this.name);
            }
            
            if (childWindow != null) {
                try {
                    this.pageContext.getOut().flush();
                    childWindow.flushContent();
                } catch (IOException e) {
                }
            }
        }
        
        return EVAL_PAGE;
    }
    
    /**
     * Returns the name of the child window content to include
     * @return String
     */
    public String getName() {
        return this.name;
    }
    
    /**
     * Sets the name property.
     * @param name The name of the child window content to include
     * @return void
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /* -------------------------------------------------------------------*/
        
    /**
     * TagExtraInfo class for HstContentTag.
     */
    public static class TEI extends TagExtraInfo {
        
        public VariableInfo[] getVariableInfo(TagData tagData) {
            VariableInfo vi[] = null;
            String var = tagData.getAttributeString("name");
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
