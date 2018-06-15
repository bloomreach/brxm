/*
 * Copyright 2015-2018 Hippo B.V. (http://www.onehippo.com)
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.easymock.EasyMock;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.HstContainerURLImpl;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.mock.core.component.MockHstRequest;
import org.hippoecm.hst.mock.core.component.MockHstResponse;
import org.hippoecm.hst.mock.core.request.MockHstRequestContext;
import org.hippoecm.hst.util.HstRequestUtils;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockPageContext;
import org.springframework.mock.web.MockServletContext;

import static org.easymock.EasyMock.anyBoolean;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isNull;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

public class TestHstLinkTag {

    private MockPageContext pageContext;
    private HstLinkTag linkTag;
    private HstLinkCreator linkCreator;

    private HstLink hstLink;
    private ResolvedMount resolvedMount;

    private void init(final String testPath) {
        MockServletContext servletContext = new MockServletContext();
        HstRequest hstRequest = new MockHstRequest();
        HstResponse hstResponse = new MockHstResponse();
        HttpServletRequest request = new MockHttpServletRequest();
        HttpServletResponse response = new MockHttpServletResponse();
        request.setAttribute(ContainerConstants.HST_REQUEST, hstRequest);
        request.setAttribute(ContainerConstants.HST_RESPONSE, hstResponse);
        final MockHstRequestContext mockHstRequestContext = new MockHstRequestContext();
        resolvedMount = EasyMock.createNiceMock(ResolvedMount.class);
        final HstContainerURLImpl baseURL = new HstContainerURLImpl();
        baseURL.setCharacterEncoding("utf-8");
        baseURL.setURIEncoding("utf-8");
        mockHstRequestContext.setBaseURL(baseURL);
        mockHstRequestContext.setResolvedMount(resolvedMount);
        request.setAttribute(ContainerConstants.HST_REQUEST_CONTEXT, mockHstRequestContext);

        linkCreator = EasyMock.createNiceMock(HstLinkCreator.class);
        mockHstRequestContext.setLinkCreator(linkCreator);

        hstLink = EasyMock.createNiceMock(HstLink.class);

        expect(hstLink.toUrlForm(anyObject(), anyBoolean())).andStubReturn(StringUtils.substringBefore(testPath, "?"));
        replay(hstLink);

        pageContext = new MockPageContext(servletContext, request, response);
        linkTag = new HstLinkTag();
        linkTag.setVar("result");
        linkTag.setPath(testPath);
        linkTag.setPageContext(pageContext);

    }

    @Test
    public void test_hst_link_by_path_with_queryString_but_without_params_returns_with_queryString_xml_escaped() throws Exception {
        final String testPath = "/css/style.css?foo=bar&page=3";
        init(testPath);

        expect(linkCreator.create(eq("/css/style.css"), isNull())).andReturn(hstLink).once();
        replay(resolvedMount, linkCreator);

        linkTag.doEndTag();
        String link = (String) pageContext.getAttribute("result");
        final String escapedExpectedResult = HstRequestUtils.escapeXml(testPath);
        assertEquals("query string should be XML escaped in the result", escapedExpectedResult, link);
        verify(linkCreator);
    }


    @Test
    public void test_hst_link_by_path_with_queryString_and_with_params_returns_merged() throws Exception {
        final String testPath = "/css/style.css?foo=bar&page=3";
        init(testPath);

        expect(linkCreator.create(eq("/css/style.css"), isNull())).andReturn(hstLink).once();
        replay(resolvedMount, linkCreator);

        for (int i = 0; i < 2; i++) {
            ParamTag paramTag = new ParamTag();
            paramTag.setParent(linkTag);
            paramTag.setName("param" + i);
            paramTag.setValue("value" + i);
            paramTag.doStartTag();
            paramTag.doEndTag();
        }
        linkTag.doEndTag();
        final String escapedExpectedResult = HstRequestUtils.escapeXml("/css/style.css?param0=value0&param1=value1&foo=bar&page=3");
        String link = (String) pageContext.getAttribute("result");
        assertEquals("query string and params should be escaped in URL and params are first", escapedExpectedResult, link);
        verify(linkCreator);
    }

    @Test
    public void test_hst_link_by_path_with_queryString_and_with_overlapping_params_returns_merged_and_queryString_precedence() throws Exception {
        final String testPath = "/css/style.css?foo=bar&page=3";
        init(testPath);

        expect(linkCreator.create(eq("/css/style.css"), isNull())).andReturn(hstLink).once();
        replay(resolvedMount, linkCreator);

        {
            ParamTag paramTag = new ParamTag();
            paramTag.setParent(linkTag);
            paramTag.setName("foo");
            paramTag.setValue("lux");
            paramTag.doStartTag();
            paramTag.doEndTag();
        }
        {
            ParamTag paramTag = new ParamTag();
            paramTag.setParent(linkTag);
            paramTag.setName("param1");
            paramTag.setValue("value1");
            paramTag.doStartTag();
            paramTag.doEndTag();
        }

        linkTag.doEndTag();
        final String escapedExpectedResult = HstRequestUtils.escapeXml("/css/style.css?foo=bar&param1=value1&page=3");
        String link = (String) pageContext.getAttribute("result");
        assertEquals("query string and params should be escaped in URL and querystring has precedence for overlapping params",
                escapedExpectedResult, link);
        verify(linkCreator);
    }

    @Test
    public void test_hst_link_by_path_with_queryString_and_with_overlapping_param_that_has_null_value_has_queryString_precedence() throws Exception {
        final String testPath = "/css/style.css?foo=bar&page=3";
        init(testPath);

        expect(linkCreator.create(eq("/css/style.css"), isNull())).andReturn(hstLink).once();
        replay(resolvedMount, linkCreator);

        {
            ParamTag paramTag = new ParamTag();
            paramTag.setParent(linkTag);
            paramTag.setName("foo");
            paramTag.setValue(null);
            paramTag.doStartTag();
            paramTag.doEndTag();
        }

        linkTag.doEndTag();
        final String escapedExpectedResult = HstRequestUtils.escapeXml("/css/style.css?foo=bar&page=3");
        String link = (String) pageContext.getAttribute("result");
        assertEquals("query string and params should be escaped in URL and querystring has precedence for overlapping params",
                escapedExpectedResult, link);
        verify(linkCreator);
    }

    @Test
    public void test_hst_link_by_path_with_queryString_with_null_value_get_is_sign_added() throws Exception {
        final String testPath = "/css/style.css?foo&bar";
        init(testPath);

        expect(linkCreator.create(eq("/css/style.css"), isNull())).andReturn(hstLink).once();
        replay(resolvedMount, linkCreator);

        linkTag.doEndTag();
        final String escapedExpectedResult = HstRequestUtils.escapeXml("/css/style.css?foo=&bar=");
        String link = (String) pageContext.getAttribute("result");
        assertEquals("query string with empty values get '=' appended",
                escapedExpectedResult, link);
        verify(linkCreator);
    }

    @Test
    public void test_hst_link_by_path_with_queryString_with_null_value_and_with_overlapping_param_that_has_non_null_value_has_queryString_precedence() throws Exception {
        final String testPath = "/css/style.css?foo&page=3";
        init(testPath);

        expect(linkCreator.create(eq("/css/style.css"), isNull())).andReturn(hstLink).once();
        replay(resolvedMount, linkCreator);

        {
            ParamTag paramTag = new ParamTag();
            paramTag.setParent(linkTag);
            paramTag.setName("foo");
            paramTag.setValue("lux");
            paramTag.doStartTag();
            paramTag.doEndTag();
        }

        linkTag.doEndTag();
        final String escapedExpectedResult = HstRequestUtils.escapeXml("/css/style.css?foo=&page=3");
        String link = (String) pageContext.getAttribute("result");
        assertEquals("query string and params should be escaped in URL and querystring has precedence for overlapping params",
                escapedExpectedResult, link);
        verify(linkCreator);
    }

    @Test
    public void test_hst_link_by_path_with_queryString_unescaped() throws Exception {
        final String testPath = "/css/style.css?foo=bar&page=3";
        init(testPath);

        expect(linkCreator.create(eq("/css/style.css"), isNull())).andReturn(hstLink).once();
        replay(resolvedMount, linkCreator);

        linkTag.setEscapeXml(false);
        linkTag.doEndTag();
        String link = (String) pageContext.getAttribute("result");
        assertEquals("query string should be unescaped in the result", testPath, link);
        verify(linkCreator);
    }

    @Test
    public void hst_link_by_path_that_starts_with_https() throws Exception {
        final String testPath = "http://www.onehippo.org/css/style.css?foo=bar&page=3";
        init(testPath);
        expect(linkCreator.create(eq("http://www.onehippo.org/css/style.css"), isNull())).andReturn(hstLink).once();
        replay(resolvedMount, linkCreator);

        linkTag.setEscapeXml(false);
        linkTag.doEndTag();
        String link = (String) pageContext.getAttribute("result");
        assertEquals("http://www.onehippo.org/css/style.css?foo=bar&page=3", link);
        verify(linkCreator);
    }

    @Test
    public void hst_link_by_path_that_starts_with_multiple_https_http_prefixes() throws Exception {
        final String testPath = "http:http://https://www.onehippo.org/css/style.css?foo=bar&page=3";
        init(testPath);

        expect(linkCreator.create(eq("http:http://https://www.onehippo.org/css/style.css"), isNull())).andReturn(hstLink).once();
        replay(resolvedMount, linkCreator);

        linkTag.setEscapeXml(false);
        linkTag.doEndTag();
        String link = (String) pageContext.getAttribute("result");
        assertEquals("http:http://https://www.onehippo.org/css/style.css?foo=bar&page=3", link);
        verify(linkCreator);
    }

    @Test
    public void test_hst_link_with_param_with_unicode_name_and_value() throws Exception {
        final String testPath = "/css/style.css";
        init(testPath);

        expect(linkCreator.create(eq("/css/style.css"), isNull())).andReturn(hstLink).once();
        replay(resolvedMount, linkCreator);

        ParamTag paramTag = new ParamTag();
        paramTag.setParent(linkTag);
        paramTag.setName("name-人");
        paramTag.setValue("value-人");
        paramTag.doStartTag();
        paramTag.doEndTag();

        linkTag.doEndTag();
        String link = (String) pageContext.getAttribute("result");
        assertEquals("/css/style.css?name-%E4%BA%BA=value-%E4%BA%BA", link);
        verify(linkCreator);
    }

}
