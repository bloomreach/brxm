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

import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.VariableInfo;

import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.component.HstURL;
import org.hippoecm.hst.core.container.ContainerConstants;

public class HstRenderURLTag extends BaseHstURLTag {

    private static final long serialVersionUID = 1L;

    protected HstURL url;

    @Override
    protected void cleanup() {
        url = null;
        super.cleanup();
    }

    @Override
    protected HstURL getUrl() {
        if (this.url == null) {
            // if hstResponse is retrieved, then this servlet has been dispatched by hst component.
            HstResponse hstResponse = (HstResponse) pageContext.getRequest().getAttribute(ContainerConstants.HST_RESPONSE);
            
            if (hstResponse == null && pageContext.getResponse() instanceof HstResponse) {
                hstResponse = (HstResponse) pageContext.getResponse();
            }
            
            if (hstResponse != null) {
                this.url = hstResponse.createRenderURL();
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
