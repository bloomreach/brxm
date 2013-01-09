/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.hst.ga.tags;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

import org.hippoecm.hst.content.beans.standard.HippoDocumentBean;

public class GoogleAnalyticsTrackDocumentTag extends TagSupport {

    private static final long serialVersionUID = 1L;

    private HippoDocumentBean hippoDocumentBean;
    
    @Override
    public int doStartTag() throws JspException {
        if (hippoDocumentBean != null) {
            String documentPath = hippoDocumentBean.getCanonicalHandlePath();
            JspWriter writer = pageContext.getOut();
            try {
                writer.write("<script type=\"text/javascript\">\n");
                writer.write("  var Hippo_Ga_Documents = Hippo_Ga_Documents || [];\n");
                writer.write("  Hippo_Ga_Documents.push('" + documentPath + "');\n");
                writer.write("</script>\n");
            }
            catch (IOException e) {
                throw new JspException("IOException while trying to write script tag", e);
            }
        }
        return SKIP_BODY;
    }

    public void setHippoDocumentBean(HippoDocumentBean bean) {
        this.hippoDocumentBean = bean;
    }

}
