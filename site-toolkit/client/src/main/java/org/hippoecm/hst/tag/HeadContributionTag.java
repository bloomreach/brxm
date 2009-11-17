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

import java.io.Reader;
import java.io.StringReader;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.utils.PageContextPropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class HeadContributionTag extends BodyTagSupport {

    static Logger logger = LoggerFactory.getLogger(HeadContributionTag.class);
    
    private static final long serialVersionUID = 1L;
    
    protected String keyHint;
    
    protected org.w3c.dom.Element element;
    
    public int doEndTag() throws JspException {
        // if hstResponse is retrieved, then this servlet has been dispatched by hst component.
        HstResponse hstResponse = (HstResponse) pageContext.getRequest().getAttribute(ContainerConstants.HST_RESPONSE);

        if (hstResponse == null && pageContext.getResponse() instanceof HstResponse) {
            hstResponse = (HstResponse) pageContext.getResponse();
        }
        
        if (hstResponse == null) {
            return SKIP_BODY;
        }
        
        if (this.keyHint != null && hstResponse.containsHeadElement(this.keyHint)) {
            return SKIP_BODY;
        }
        
        if (this.element == null) {
            Reader reader = null;
            
            try {
                String xmlText = "";
    
                if (bodyContent != null && bodyContent.getString() != null) {
                    xmlText = bodyContent.getString().trim();
                }
                
                if (this.keyHint == null) {
                    this.keyHint = xmlText;
    
                    if (hstResponse.containsHeadElement(this.keyHint)) {
                        return SKIP_BODY;
                    }
                }
                
                if (!"".equals(xmlText)) {
                    DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
                    DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
                    Document doc = docBuilder.parse(new InputSource(new StringReader(xmlText)));
                    element = doc.getDocumentElement();
                }
            } catch (Exception ex) {
                throw new JspException(ex);
            } finally {
                if (reader != null) try { reader.close(); } catch (Exception ce) { }
            }
        }
        
        if (element != null) {
            if (this.keyHint == null) {
                this.keyHint = new StringBuilder().append(element.getTextContent()).toString();
            }
            
            hstResponse.addHeadElement(element, this.keyHint);
        }
        
        keyHint = null;
        element = null;
        
        return EVAL_PAGE;
    }
    
    public void setKeyHint(String keyHint) {
        this.keyHint = keyHint;
    }
    
    public String getKeyHint() {
        return this.keyHint;
    }
    
    public void setElement(org.w3c.dom.Element element) {
        this.element = element;
    }
    
    public void setElementByBeanPath(String beanPath) {
        this.element = (org.w3c.dom.Element) PageContextPropertyUtils.getProperty(pageContext, beanPath);
    }
    
    public org.w3c.dom.Element getElement() {
        return this.element;
    }

}
