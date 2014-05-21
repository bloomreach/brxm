/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import org.hippoecm.hst.core.component.HeadElementImpl;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.util.HeadElementUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

public class HeadContributionTag extends BodyTagSupport {

    static Logger logger = LoggerFactory.getLogger(HeadContributionTag.class);
    
    private static final long serialVersionUID = 1L;
    
    protected String keyHint;
    
    protected Element element;
    
    /**
     * Comma separated category list where this head element should be in.
     */
    protected String category;
    
    public int doEndTag() throws JspException {
        try {
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

                    DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
                    DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
                    reader = new StringReader(xmlText);
                    Document doc = docBuilder.parse(new InputSource(reader));
                    element = doc.getDocumentElement();
                } catch (Exception ex) {
                    throw new JspException(ex);
                } finally {
                    if (reader != null) try { reader.close(); } catch (Exception ce) { }
                }
            }

            if (element != null) {
                if (this.keyHint == null) {
                    this.keyHint = HeadElementUtils.toHtmlString(new HeadElementImpl(element));

                    if (hstResponse.containsHeadElement(this.keyHint)) {
                        return SKIP_BODY;
                    }
                }

                if (category != null) {
                    String existingCategoryHint = element.getAttribute(ContainerConstants.HEAD_ELEMENT_CONTRIBUTION_CATEGORY_HINT_ATTRIBUTE);
                    // if there already exists category hint in the element itself, ignore category property.
                    if (existingCategoryHint == null || "".equals(existingCategoryHint)) {
                        element.setAttribute(ContainerConstants.HEAD_ELEMENT_CONTRIBUTION_CATEGORY_HINT_ATTRIBUTE, category);
                    }
                }

                hstResponse.addHeadElement(element, this.keyHint);
            }

            return EVAL_PAGE;
        } finally {
            cleanup();
        }
    }

    protected void cleanup() {
        keyHint = null;
        element = null;
        category = null;
    }

    public void setKeyHint(String keyHint) {
        this.keyHint = keyHint;
    }
    
    public String getKeyHint() {
        return this.keyHint;
    }
    
    public void setElement(Element element) {
        this.element = element;
    }
    
    public Element getElement() {
        return this.element;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public String getCategory() {
        return category;
    }
    
}
