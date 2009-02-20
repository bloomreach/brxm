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

import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.VariableInfo;

import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.component.HstURL;

public class HstURLTag extends BaseHstURLTag {

    private static final long serialVersionUID = 1L;
    
    protected HstURL url;

    @Override
    protected HstURL getUrl() {
        if (this.url == null) {
            HttpServletResponse servletResponse = (HttpServletResponse) this.pageContext.getResponse();
            
            if (servletResponse instanceof HstResponse) {
                this.url = ((HstResponse) servletResponse).createURL(getType());
            }
        }
        
        return this.url;
    }

    @Override
    protected void setUrl(HstURL url) {
        this.url = url;
    }

    /**
     * TagExtraInfo class for HstURLTag.
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
