/**
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.tag;

import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.mock.core.component.MockHstRequest;
import org.hippoecm.hst.mock.core.component.MockHstResponse;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockPageContext;
import org.springframework.mock.web.MockServletContext;
import org.w3c.dom.Element;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * TestHeadContributionTag
 */
public class TestHeadContributionsTag {

    private HeadContributionsTag tag;
    private MockHstResponse hstResponse;

    @Before
    public void setUp() throws Exception {
        MockServletContext servletContext = new MockServletContext();

        MockHstRequest hstRequest = new MockHstRequest();
        hstResponse = new MockHstResponse();

        MockPageContext pageContext = new MockPageContext(servletContext, hstRequest, hstResponse);

        tag = new HeadContributionsTag();
        tag.setPageContext(pageContext);

        Element headElem = hstResponse.createElement("script");
        headElem.setAttribute("src", "cat1.js");
        headElem.setAttribute(ContainerConstants.HEAD_ELEMENT_CONTRIBUTION_CATEGORY_HINT_ATTRIBUTE, "cat1");
        hstResponse.addHeadElement(headElem, "cat1.js");

        headElem = hstResponse.createElement("script");
        headElem.setAttribute("src", "cat2.js");
        headElem.setAttribute(ContainerConstants.HEAD_ELEMENT_CONTRIBUTION_CATEGORY_HINT_ATTRIBUTE, "cat2");
        hstResponse.addHeadElement(headElem, "cat2.js");

        headElem = hstResponse.createElement("script");
        headElem.setAttribute("src", "cat3.js");
        headElem.setAttribute(ContainerConstants.HEAD_ELEMENT_CONTRIBUTION_CATEGORY_HINT_ATTRIBUTE, "cat3");
        hstResponse.addHeadElement(headElem, "cat3.js");

        headElem = hstResponse.createElement("script");
        headElem.setAttribute("src", "cat4.js");
        headElem.setAttribute(ContainerConstants.HEAD_ELEMENT_CONTRIBUTION_CATEGORY_HINT_ATTRIBUTE, "cat4");
        hstResponse.addHeadElement(headElem, "cat4.js");
    }

    @Test
    public void testCategoryWithNoIncludesAndNoExcludes() throws Exception {
        assertNull(tag.getCategoryIncludes());
        assertNull(tag.getCategoryExcludes());

        tag.doStartTag();
        tag.doEndTag();
        tag.release();

        String content = hstResponse.getContentAsString();

        assertNotNull(content);
        assertTrue(content.contains("src=\"cat1.js\""));
        assertTrue(content.contains("src=\"cat2.js\""));
        assertTrue(content.contains("src=\"cat3.js\""));
        assertTrue(content.contains("src=\"cat4.js\""));
    }

    @Test
    public void testCategoryIncludes() throws Exception {
        tag.setCategoryIncludes("cat1, cat2");

        assertEquals("cat1, cat2", tag.getCategoryIncludes());
        assertNull(tag.getCategoryExcludes());

        tag.doStartTag();
        tag.doEndTag();
        tag.release();

        String content = hstResponse.getContentAsString();

        assertNotNull(content);
        assertTrue(content.contains("src=\"cat1.js\""));
        assertTrue(content.contains("src=\"cat2.js\""));
        assertFalse(content.contains("src=\"cat3.js\""));
        assertFalse(content.contains("src=\"cat4.js\""));

        tag.setCategoryIncludes("cat3, cat4");
        assertEquals("cat3, cat4", tag.getCategoryIncludes());
        assertNull(tag.getCategoryExcludes());

        tag.doStartTag();
        tag.doEndTag();
        tag.release();

        content = hstResponse.getContentAsString();

        assertTrue(content.contains("src=\"cat1.js\""));
        assertTrue(content.contains("src=\"cat2.js\""));
        assertTrue(content.contains("src=\"cat3.js\""));
        assertTrue(content.contains("src=\"cat4.js\""));
    }

    @Test
    public void testCategoryExcludes() throws Exception {
        tag.setCategoryExcludes("cat1, cat2");

        assertNull(tag.getCategoryIncludes());
        assertEquals("cat1, cat2", tag.getCategoryExcludes());

        tag.doStartTag();
        tag.doEndTag();
        tag.release();

        String content = hstResponse.getContentAsString();

        assertNotNull(content);
        assertFalse(content.contains("src=\"cat1.js\""));
        assertFalse(content.contains("src=\"cat2.js\""));
        assertTrue(content.contains("src=\"cat3.js\""));
        assertTrue(content.contains("src=\"cat4.js\""));

        tag.setCategoryExcludes("cat3, cat4");
        assertNull(tag.getCategoryIncludes());
        assertEquals("cat3, cat4", tag.getCategoryExcludes());

        tag.doStartTag();
        tag.doEndTag();
        tag.release();

        content = hstResponse.getContentAsString();

        assertTrue(content.contains("src=\"cat1.js\""));
        assertTrue(content.contains("src=\"cat2.js\""));
        assertTrue(content.contains("src=\"cat3.js\""));
        assertTrue(content.contains("src=\"cat4.js\""));
    }

    @Test
    public void testCategoryIncludesAndExcludes() throws Exception {
        tag.setCategoryIncludes("cat1");
        tag.setCategoryExcludes("cat3");

        assertEquals("cat1", tag.getCategoryIncludes());
        assertEquals("cat3", tag.getCategoryExcludes());

        tag.doStartTag();
        tag.doEndTag();
        tag.release();

        String content = hstResponse.getContentAsString();

        assertNotNull(content);
        assertTrue(content.contains("src=\"cat1.js\""));
        assertFalse(content.contains("src=\"cat2.js\""));
        assertFalse(content.contains("src=\"cat3.js\""));
        assertFalse(content.contains("src=\"cat4.js\""));

        // if a category is included in both includes and excludes, it should be excluded.
        tag.setCategoryIncludes("cat2, cat3");
        tag.setCategoryExcludes("cat3, cat4");
        assertEquals("cat2, cat3", tag.getCategoryIncludes());
        assertEquals("cat3, cat4", tag.getCategoryExcludes());

        tag.doStartTag();
        tag.doEndTag();
        tag.release();

        content = hstResponse.getContentAsString();

        assertTrue(content.contains("src=\"cat1.js\""));
        assertTrue(content.contains("src=\"cat2.js\""));
        assertFalse(content.contains("src=\"cat3.js\""));
        assertFalse(content.contains("src=\"cat4.js\""));
    }

    @Test
    public void test_response_head_elements_are_cloned_on_access() {
        final int before = hstResponse.getHeadElements().size();
        assertTrue(before > 0);
        hstResponse.getHeadElements().clear();
        final int after = hstResponse.getHeadElements().size();
        assertEquals(before, after);
    }

    @Test
    public void testNoAutomaticIncludePageTitle() throws Exception {

        tag.setCategoryExcludes("cat1, cat2, cat3, cat4");

        final Element titleElement = hstResponse.createElement("title");
        titleElement.appendChild(titleElement.getOwnerDocument().createTextNode("Some title"));

        hstResponse.addHeadElement(titleElement, "title");

        tag.doStartTag();
        tag.doEndTag();
        tag.release();
        String content = hstResponse.getContentAsString();
        assertEquals("<title>Some title</title>\n", content);
    }

    @Test
    public void testWithAutomaticIncludePageTitle() throws Exception {
        HstRequestContext context = createMock(HstRequestContext.class);
        ResolvedSiteMapItem resolvedSiteMapItem = createMock(ResolvedSiteMapItem.class);
        expect(resolvedSiteMapItem.getPageTitle()).andReturn("sitemapitem page title").anyTimes();
        expect(context.getResolvedSiteMapItem()).andReturn(resolvedSiteMapItem).anyTimes();
        replay(context, resolvedSiteMapItem);

        ModifiableRequestContextProvider.set(context);
        tag.setCategoryExcludes("cat1, cat2, cat3, cat4");

        tag.setIncludePageTitle(true);
        final Element titleElement = hstResponse.createElement("title");
        titleElement.appendChild(titleElement.getOwnerDocument().createTextNode("Some title"));

        hstResponse.addHeadElement(titleElement, "title");

        tag.doStartTag();
        tag.doEndTag();
        tag.release();
        String content = hstResponse.getContentAsString();
        assertEquals("<title>sitemapitem page title - Some title</title>\n", content);

        ModifiableRequestContextProvider.clear();
    }

    @Test
    public void testWithAutomaticIncludePageTitleWithDifferentDelim() throws Exception {
        HstRequestContext context = createMock(HstRequestContext.class);
        ResolvedSiteMapItem resolvedSiteMapItem = createMock(ResolvedSiteMapItem.class);
        expect(resolvedSiteMapItem.getPageTitle()).andReturn("sitemapitem page title").anyTimes();
        expect(context.getResolvedSiteMapItem()).andReturn(resolvedSiteMapItem).anyTimes();
        replay(context, resolvedSiteMapItem);

        ModifiableRequestContextProvider.set(context);
        tag.setCategoryExcludes("cat1, cat2, cat3, cat4");

        tag.setIncludePageTitle(true);
        tag.setPageTitleDelimiter("|");

        final Element titleElement = hstResponse.createElement("title");
        titleElement.appendChild(titleElement.getOwnerDocument().createTextNode("Some title"));

        hstResponse.addHeadElement(titleElement, "title");

        tag.doStartTag();
        tag.doEndTag();
        tag.release();
        String content = hstResponse.getContentAsString();
        assertEquals("<title>sitemapitem page title | Some title</title>\n", content);

        ModifiableRequestContextProvider.clear();
    }

    @Test
    public void testDelimiterGetsEncoded() throws Exception {
        HstRequestContext context = createMock(HstRequestContext.class);
        ResolvedSiteMapItem resolvedSiteMapItem = createMock(ResolvedSiteMapItem.class);
        expect(resolvedSiteMapItem.getPageTitle()).andReturn("sitemapitem page title").anyTimes();
        expect(context.getResolvedSiteMapItem()).andReturn(resolvedSiteMapItem).anyTimes();
        replay(context, resolvedSiteMapItem);

        ModifiableRequestContextProvider.set(context);
        tag.setCategoryExcludes("cat1, cat2, cat3, cat4");

        tag.setIncludePageTitle(true);
        tag.setPageTitleDelimiter("<>");

        final Element titleElement = hstResponse.createElement("title");
        titleElement.appendChild(titleElement.getOwnerDocument().createTextNode("Some title"));

        hstResponse.addHeadElement(titleElement, "title");

        tag.doStartTag();
        tag.doEndTag();
        tag.release();
        String content = hstResponse.getContentAsString();
        assertEquals("<title>sitemapitem page title &lt;&gt; Some title</title>\n", content);

        ModifiableRequestContextProvider.clear();
    }
}
