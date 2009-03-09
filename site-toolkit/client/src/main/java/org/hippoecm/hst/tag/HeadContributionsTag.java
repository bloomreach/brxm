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

import org.hippoecm.hst.core.component.HstResponse;
import org.jdom.input.DOMBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.w3c.dom.Element;

public class HeadContributionsTag extends TagSupport {

    private static final long serialVersionUID = 1L;
    
    public int doEndTag() throws JspException {
        HstResponse hstResponse = null;
        
        if (pageContext.getResponse() instanceof HstResponse) {
            hstResponse = (HstResponse) pageContext.getResponse();
        }

        if (hstResponse != null) {
            List<Element> headElements = hstResponse.getHeadElements();
            
            if (headElements != null && !headElements.isEmpty()) {
                XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
                DOMBuilder domBuilder = null;
                JspWriter writer = pageContext.getOut();
                
                try {
                    org.jdom.Element jdomHeadElement = null;
                    
                    for (Element headElement : headElements) {
                        if (headElement instanceof org.jdom.Element) {
                            jdomHeadElement = (org.jdom.Element) headElement;
                        } else {
                            if (domBuilder == null) {
                                domBuilder = new DOMBuilder();
                            }
                            
                            jdomHeadElement = domBuilder.build(headElement);
                        }
                        
                        writer.print(outputter.outputString(jdomHeadElement));
                    }
                } catch (IOException ioe) {
                    throw new JspException("HeadContributionsTag Exception: cannot write to the output writer.");
                }
            }
        }
        
        return SKIP_BODY;
    }

}
