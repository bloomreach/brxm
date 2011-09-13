/*
 *  Copyright 2009 Hippo.
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
import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.VariableInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @deprecated Use {@link HstCmsEditLinkTag} instead. 
 */

@Deprecated
public class HstSurfAndEditTag extends HstCmsEditLinkTag {

    private final static Logger log = LoggerFactory.getLogger(HstSurfAndEditTag.class);
    
    private static final long serialVersionUID = 1L;

    /* (non-Javadoc)
     * @see javax.servlet.jsp.tagext.TagSupport#doStartTag()
     */
    @Override
    public int doStartTag() throws JspException{
        log.warn("Using deprecated HstSurfAndEditTag (tld 'surfandeditlink' tag has been deprecated). Use 'cmseditlink' tag instead, " +
        		"and specify a 'var' attribute to store the link in to retain the same behaviour as for the 'surfandeditlink' tag");
        return super.doStartTag(); 
    }
   
    @Override
    protected void write(String link, String nodeId) throws IOException {
         JspWriter writer = pageContext.getOut();
         writer.print(link);
    }
    
    /**
     * TagExtraInfo class for HstSurfAndEditTag.
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
