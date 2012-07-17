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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.core.component.HeadElement;
import org.hippoecm.hst.core.component.HeadElementImpl;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.util.HeadElementUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

public class HeadContributionsTag extends TagSupport {
    
    static Logger logger = LoggerFactory.getLogger(HeadContributionsTag.class);

    private static final long serialVersionUID = 1L;
    
    private boolean xhtml;
    
    /**
     * Comma separated category includes list
     */
    private Set<String> categoryIncludes;
    
    /**
     * Comma separated category excludes list
     */
    private Set<String> categoryExcludes;
    
    public HeadContributionsTag() {
    }
    
    public void setXhtml(boolean xhtml) {
        this.xhtml = xhtml;
    }
    
    public boolean getXhtml() {
        return xhtml;
    }
    
    public void setCategoryIncludes(String categoryIncludes) {
        this.categoryIncludes = new HashSet<String>(Arrays.asList(StringUtils.split(categoryIncludes, ", \t")));
    }
    
    public String getCategoryIncludes() {
        return categoryIncludes != null ? StringUtils.join(categoryIncludes, ", ") : null;
    }
    
    public void setCategoryExcludes(String categoryExcludes) {
        this.categoryExcludes = new HashSet<String>(Arrays.asList(StringUtils.split(categoryExcludes, ", \t")));
    }
    
    public String getCategoryExcludes() {
        return categoryExcludes != null ? StringUtils.join(categoryExcludes, ", ") : null;
    }
    
    public int doEndTag() throws JspException {
        // if hstRequest is retrieved, then this servlet has been dispatched by hst component.
        HstResponse hstResponse = (HstResponse) pageContext.getRequest().getAttribute(ContainerConstants.HST_RESPONSE);
        
        if (hstResponse == null && pageContext.getResponse() instanceof HstResponse) {
            hstResponse = (HstResponse) pageContext.getResponse();
        }
        
        if (hstResponse != null) {
            List<Element> headElements = hstResponse.getHeadElements();
            
            if (headElements != null && !headElements.isEmpty()) {
                try {
                    boolean categoryIncludesEmpty = (categoryIncludes == null || categoryIncludes.isEmpty());
                    boolean categoryExcludesEmpty = (categoryExcludes == null || categoryExcludes.isEmpty());
                    
                    if (!categoryIncludesEmpty || !categoryExcludesEmpty) {
                        for (Element headElement : headElements) {
                            boolean skip = true;
                            
                            String category = headElement.getAttribute(ContainerConstants.HEAD_ELEMENT_CONTRIBUTION_CATEGORY_HINT_ATTRIBUTE);
                            if (category != null && "".equals(category)) {
                                category = null;
                            }
                            
                            if (!categoryIncludesEmpty) {
                                if (category != null && categoryIncludes.contains(category)) {
                                    skip = false;
                                    if (!categoryExcludesEmpty) {
                                        skip = categoryExcludes.contains(category);
                                    }
                                }
                            } else if (!categoryExcludesEmpty) {
                                if (category == null) {
                                    skip = false;
                                } else {
                                    skip = categoryExcludes.contains(category);
                                }
                            }
                            
                            if (skip) {
                                continue;
                            }
                            
                            HeadElement outHeadElement = new HeadElementImpl(headElement);
                            if (outHeadElement.hasAttribute(ContainerConstants.HEAD_ELEMENT_CONTRIBUTION_CATEGORY_HINT_ATTRIBUTE)) {
                                outHeadElement.removeAttribute(ContainerConstants.HEAD_ELEMENT_CONTRIBUTION_CATEGORY_HINT_ATTRIBUTE);
                            }
                            
                            if (xhtml) {
                                pageContext.getOut().println(HeadElementUtils.toXhtmlString(outHeadElement, isResponseTextHtmlContent()));
                            } else {
                                pageContext.getOut().println(HeadElementUtils.toHtmlString(outHeadElement));
                            }
                            
                            pageContext.getOut().flush();
                        }
                    } else {
                        for (Element headElement : headElements) {
                            HeadElement outHeadElement = new HeadElementImpl(headElement);
                            if (outHeadElement.hasAttribute(ContainerConstants.HEAD_ELEMENT_CONTRIBUTION_CATEGORY_HINT_ATTRIBUTE)) {
                                outHeadElement.removeAttribute(ContainerConstants.HEAD_ELEMENT_CONTRIBUTION_CATEGORY_HINT_ATTRIBUTE);
                            }
                            
                            if (xhtml) {
                                pageContext.getOut().println(HeadElementUtils.toXhtmlString(outHeadElement, isResponseTextHtmlContent()));
                            } else {
                                pageContext.getOut().println(HeadElementUtils.toHtmlString(outHeadElement));
                            }
                            
                            pageContext.getOut().flush();
                        }
                    }
                } catch (IOException ioe) {
                    throw new JspException("HeadContributionsTag Exception: cannot write to the output writer.");
                }
            }
        }
        
        return SKIP_BODY;
    }
    
    private boolean isResponseTextHtmlContent() {
        String responseContentType = pageContext.getResponse().getContentType();
        return (responseContentType != null && responseContentType.startsWith("text/html"));
    }
}
