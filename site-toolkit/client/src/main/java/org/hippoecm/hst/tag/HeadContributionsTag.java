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
import java.util.List;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.dom4j.io.DOMReader;
import org.dom4j.io.HTMLWriter;
import org.dom4j.io.OutputFormat;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.container.ContainerConstants;

public class HeadContributionsTag extends TagSupport {

    private static final long serialVersionUID = 1L;
    private OutputFormat outputFormat = OutputFormat.createPrettyPrint();
    
    public HeadContributionsTag() {
        this.outputFormat.setExpandEmptyElements(true);
    }
    
    public void setXhtml(boolean xhtml) {
        this.outputFormat.setXHTML(xhtml);
    }
    
    public boolean getXhtml() {
        return this.outputFormat.isXHTML();
    }
    
    public int doEndTag() throws JspException {
        // if hstRequest is retrieved, then this servlet has been dispatched by hst component.
        HstResponse hstResponse = (HstResponse) pageContext.getRequest().getAttribute(ContainerConstants.HST_RESPONSE);
        
        if (hstResponse == null && pageContext.getResponse() instanceof HstResponse) {
            hstResponse = (HstResponse) pageContext.getResponse();
        }
        
        if (hstResponse != null) {
            List<org.w3c.dom.Element> headElements = hstResponse.getHeadElements();
            
            if (headElements != null && !headElements.isEmpty()) {
                JspWriter writer = pageContext.getOut();
                HTMLWriter htmlWriter = new HTMLWriter(writer, this.outputFormat);
                
                try {
                    org.dom4j.Element dom4jHeadElement = null;
                    DocumentBuilder documentBuilder = null;
                    
                    for (org.w3c.dom.Element headElement : headElements) {
                        if (headElement instanceof org.dom4j.Element) {
                            dom4jHeadElement = (org.dom4j.Element) headElement;
                        } else {
                            if (documentBuilder == null) {
                                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                                documentBuilder = factory.newDocumentBuilder();
                            }
                            
                            org.w3c.dom.Document w3cDoc = documentBuilder.newDocument();
                            w3cDoc.appendChild(headElement);
                            DOMReader reader = new DOMReader();
                            org.dom4j.Document dom4jDoc = reader.read(w3cDoc);
                            dom4jHeadElement = dom4jDoc.getRootElement();
                        }
                        
                        htmlWriter.write(dom4jHeadElement);
                    }
                } catch (ParserConfigurationException e) {
                    throw new JspException("HeadContributionsTag Exception: cannot write to the output writer.");
                } catch (IOException ioe) {
                    throw new JspException("HeadContributionsTag Exception: cannot write to the output writer.");
                }
            }
        }
        
        return SKIP_BODY;
    }

}
