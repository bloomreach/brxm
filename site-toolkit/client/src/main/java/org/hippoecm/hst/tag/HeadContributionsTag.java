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

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.component.HeadElement;
import org.hippoecm.hst.core.component.HeadElementImpl;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.util.HeadElementUtils;
import org.w3c.dom.Element;

public class HeadContributionsTag extends TagSupport {

    private static final long serialVersionUID = 1L;

    private static final String DEFAULT_PAGE_TITLE_DELIMITER = "-";
    private boolean xhtml;

    /**
     * Comma separated category includes list
     */
    private Set<String> categoryIncludes;

    /**
     * Comma separated category excludes list
     */
    private Set<String> categoryExcludes;

    private boolean includePageTitle;

    private String pageTitleDelimiter = DEFAULT_PAGE_TITLE_DELIMITER;

    public HeadContributionsTag() {
    }

    public void setXhtml(boolean xhtml) {
        this.xhtml = xhtml;
    }

    public boolean getXhtml() {
        return xhtml;
    }

    public void setCategoryIncludes(String categoryIncludes) {
        this.categoryIncludes = new LinkedHashSet<>(Arrays.asList(StringUtils.split(categoryIncludes, ", \t")));
    }

    public String getCategoryIncludes() {
        return categoryIncludes != null ? StringUtils.join(categoryIncludes, ", ") : null;
    }

    public void setCategoryExcludes(String categoryExcludes) {
        this.categoryExcludes = new LinkedHashSet<>(Arrays.asList(StringUtils.split(categoryExcludes, ", \t")));
    }

    public String getCategoryExcludes() {
        return categoryExcludes != null ? StringUtils.join(categoryExcludes, ", ") : null;
    }

    public boolean isIncludePageTitle() {
        return includePageTitle;
    }

    public void setIncludePageTitle(final boolean includePageTitle) {
        this.includePageTitle = includePageTitle;
    }

    public String getPageTitleDelimiter() {
        return pageTitleDelimiter;
    }

    public void setPageTitleDelimiter(final String pageTitleDelimiter) {
        this.pageTitleDelimiter = pageTitleDelimiter;
    }

    public int doEndTag() throws JspException {
        try {
            // if hstRequest is retrieved, then this servlet has been dispatched by hst component.
            HstResponse hstResponse = (HstResponse) pageContext.getRequest().getAttribute(ContainerConstants.HST_RESPONSE);

            if (hstResponse == null && pageContext.getResponse() instanceof HstResponse) {
                hstResponse = (HstResponse) pageContext.getResponse();
            }

            List<Element> headElements = hstResponse != null ? hstResponse.getHeadElements() : null;

            if (headElements == null) {
                return SKIP_BODY;
            }

            boolean isTitleElementWritten = false;
            for (Element headElement : headElements) {
                if (shouldBeIncludedInOutput(headElement)) {
                    if (includePageTitle && "title".equalsIgnoreCase(headElement.getTagName())) {
                        isTitleElementWritten = true;
                        final String pageTitle = getPageTitle();
                        if (StringUtils.isNotBlank(pageTitle)) {
                            StringBuilder titleBuilder = new StringBuilder(pageTitle);
                            if (StringUtils.isNotBlank(headElement.getTextContent())) {
                                titleBuilder.append(" ").append(pageTitleDelimiter).append(" ")
                                        .append(headElement.getTextContent());
                            }
                            headElement.setTextContent(titleBuilder.toString());
                        }
                    }
                    outputHeadElement(headElement);
                }
            }
            final String pageTitle;
            if (!isTitleElementWritten && includePageTitle && StringUtils.isNotBlank(pageTitle = getPageTitle())) {
                final Element titleElement = hstResponse.createElement("title");
                titleElement.appendChild(titleElement.getOwnerDocument().createTextNode(pageTitle));
                outputHeadElement(titleElement);
            }

            try {
                pageContext.getOut().flush();
            } catch (IOException e) {
                throw new JspException("Cannot flush the output", e);
            }

            return SKIP_BODY;
        } finally {
            cleanup();
        }
    }

    private String getPageTitle() {
        final ResolvedSiteMapItem resolvedSiteMapItem = RequestContextProvider.get().getResolvedSiteMapItem();
        if (resolvedSiteMapItem == null) {
            return null;
        }
        return resolvedSiteMapItem.getPageTitle();
    }

    protected void cleanup() {
        xhtml = false;
        categoryIncludes = null;
        categoryExcludes = null;
        includePageTitle = false;
        pageTitleDelimiter = DEFAULT_PAGE_TITLE_DELIMITER;
    }

    private boolean shouldBeIncludedInOutput(Element headElement) {
        boolean filterOnIncludes = categoryIncludes != null && !categoryIncludes.isEmpty();
        boolean filterOnExcludes = categoryExcludes != null && !categoryExcludes.isEmpty();

        if (!filterOnIncludes && !filterOnExcludes) {
            return true;
        }

        String category = headElement.getAttribute(ContainerConstants.HEAD_ELEMENT_CONTRIBUTION_CATEGORY_HINT_ATTRIBUTE);
        boolean shouldInclude = !filterOnIncludes || categoryIncludes.contains(category);
        boolean shouldExclude = filterOnExcludes && categoryExcludes.contains(category);

        return shouldInclude && !shouldExclude;
    }

    private void outputHeadElement(Element headElement) throws JspException {
        HeadElement outHeadElement = new HeadElementImpl(headElement);
        if (outHeadElement.hasAttribute(ContainerConstants.HEAD_ELEMENT_CONTRIBUTION_CATEGORY_HINT_ATTRIBUTE)) {
            outHeadElement.removeAttribute(ContainerConstants.HEAD_ELEMENT_CONTRIBUTION_CATEGORY_HINT_ATTRIBUTE);
        }

        String elementOutput;
        if (xhtml) {
            elementOutput = HeadElementUtils.toXhtmlString(outHeadElement, isResponseTextHtmlContent());
        } else {
            elementOutput = HeadElementUtils.toHtmlString(outHeadElement);
        }

        try {
            pageContext.getOut().println(elementOutput);
        } catch (IOException ioe) {
            throw new JspException("HeadContributionsTag Exception: cannot write to the output writer.");
        }
    }

    private boolean isResponseTextHtmlContent() {
        String responseContentType = pageContext.getResponse().getContentType();
        return (responseContentType != null && responseContentType.startsWith("text/html"));
    }

}
