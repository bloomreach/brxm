/*
 *  Copyright 2008-2021 Hippo B.V. (http://www.onehippo.com)
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.core.component.HeadElementImpl;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.util.HeadElementUtils;
import org.hippoecm.hst.util.HstRequestUtils;
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

    //It is suppressed because try-catch-finally block is not replacable with try-with-resource statement
    @SuppressWarnings("squid:S2093")
    public int doEndTag() throws JspException {
        try {
            final HstRequest hstRequest = HstRequestUtils.getHstRequest((HttpServletRequest) pageContext.getRequest());

            if (hstRequest == null) {
                return SKIP_BODY;
            }

            if (!HstRequest.RENDER_PHASE.equals(hstRequest.getLifecyclePhase())) {
                return SKIP_BODY;
            }

            final HstResponse hstResponse = HstRequestUtils.getHstResponse(
                    (HttpServletRequest) pageContext.getRequest(), (HttpServletResponse) pageContext.getResponse());

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
                    dbfac.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                    dbfac.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
                    dbfac.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

                    DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
                    if (StringUtils.isNotBlank(xmlText)) {
                        reader = new StringReader(xmlText);
                        Document doc = docBuilder.parse(new InputSource(reader));
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
