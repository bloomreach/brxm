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
import java.io.Reader;
import java.io.StringReader;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.hippoecm.hst.core.component.HstResponse;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.DOMOutputter;

public class HeadContributionTag extends BodyTagSupport {

    private static final long serialVersionUID = 1L;
    
    protected String keyHint;
    
    public int doEndTag() throws JspException {
        HstResponse hstResponse = null;

        if (pageContext.getResponse() instanceof HstResponse) {
            hstResponse = (HstResponse) pageContext.getResponse();
        }

        if (hstResponse == null) {
            return SKIP_BODY;
        }
        
        if (this.keyHint != null && hstResponse.containsHeadElement(this.keyHint)) {
            return SKIP_BODY;
        }
        
        Reader reader = null;
        
        try {
            String xmlText = "";

            if (bodyContent != null && bodyContent.getString() != null) {
                xmlText = bodyContent.getString().trim();
            }
            
            if (this.keyHint == null) {
                this.keyHint = xmlText;
            }

            reader = new StringReader(xmlText);
            
            SAXBuilder builder = new SAXBuilder(false);
            Document doc = builder.build(reader);
            DOMOutputter outputter = new DOMOutputter();
            
            hstResponse.addHeadElement(outputter.output(doc).getDocumentElement(), this.keyHint);

            return EVAL_PAGE;
        } catch (JDOMException ex) {
            throw new JspException(ex);
        } catch (IOException ex) {
            throw new JspException(ex);
        } finally {
            if (reader != null) try { reader.close(); } catch (Exception ce) { }
        }
    }
    
    public void setKeyHint(String keyHint) {
        this.keyHint = keyHint;
    }
    
    public String getKeyHint() {
        return this.keyHint;
    }

}
