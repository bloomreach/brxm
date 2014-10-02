/*
 * Copyright 2014-2014 Hippo B.V. (http://www.onehippo.com)
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.component.HstURL;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.mock.core.component.MockHstRequest;
import org.hippoecm.hst.mock.core.component.MockHstResponse;
import org.hippoecm.hst.mock.core.component.MockHstURL;
import org.hippoecm.hst.util.HstRequestUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockPageContext;
import org.springframework.mock.web.MockServletContext;

/**
 * TestBaseHstURLTag
 */
public class TestBaseHstURLTag {

    private BaseHstURLTag urlTag;
    private MockPageContext pageContext;

    @Before
    public void before() throws Exception {
        MockServletContext servletContext = new MockServletContext();
        HstRequest hstRequest = new MockHstRequest();
        HstResponse hstResponse = new MockHstResponse();
        HttpServletRequest request = new MockHttpServletRequest();
        HttpServletResponse response = new MockHttpServletResponse();
        request.setAttribute(ContainerConstants.HST_REQUEST, hstRequest);
        request.setAttribute(ContainerConstants.HST_RESPONSE, hstResponse);
        pageContext = new MockPageContext(servletContext, request, response);

        urlTag = new TestHstURLTag();
        urlTag.setPageContext(pageContext);
        urlTag.setUrl(new MockHstURL());
    }

    @Test
    public void testSimpleParametersAddition() throws Exception {
        urlTag.doStartTag();

        // add 5 parameters
        for (int i = 0; i < 5; i++) {
            ParamTag paramTag = new ParamTag();
            paramTag.setParent(urlTag);
            paramTag.setName("param" + i);
            paramTag.setValue("value" + i);
            paramTag.doStartTag();
            paramTag.doEndTag();
        }

        urlTag.doEndTag();

        Map<String, String[]> params = urlTag.getUrl().getParameterMap();
        assertNotNull(params);

        // check if five parameters are good
        for (int i = 0; i < 5; i++) {
            assertEquals("value" + i, getFirstArrayStringItem(params.get("param" + i)));
        }
    }

    @Test
    public void testSimpleParametersRemoval() throws Exception {
        urlTag.doStartTag();

        // first add 5 parameters
        for (int i = 0; i < 5; i++) {
            ParamTag paramTag = new ParamTag();
            paramTag.setParent(urlTag);
            paramTag.setName("param" + i);
            paramTag.setValue("value" + i);
            paramTag.doStartTag();
            paramTag.doEndTag();
        }

        urlTag.doEndTag();

        urlTag.doStartTag();

        // removing the first three parameters
        for (int i = 0; i < 3; i++) {
            ParamTag paramTag = new ParamTag();
            paramTag.setParent(urlTag);
            paramTag.setName("param" + i);
            paramTag.setValue(null);
            paramTag.doStartTag();
            paramTag.doEndTag();
        }

        urlTag.doEndTag();

        Map<String, String[]> params = urlTag.getUrl().getParameterMap();
        assertNotNull(params);

        // check if the first three parameters are empty
        for (int i = 0; i < 3; i++) {
            assertTrue(StringUtils.isEmpty(getFirstArrayStringItem(params.get("param" + i))));
        }

        // check if the last two parameters are still there
        for (int i = 3; i < 5; i++) {
            assertEquals("value" + i, getFirstArrayStringItem(params.get("param" + i)));
        }
    }

    @Test
    public void testParametersRemovalAndAddition() throws Exception {
        urlTag.doStartTag();

        // first add 5 parameters
        for (int i = 0; i < 5; i++) {
            ParamTag paramTag = new ParamTag();
            paramTag.setParent(urlTag);
            paramTag.setName("param" + i);
            paramTag.setValue("value" + i);
            paramTag.doStartTag();
            paramTag.doEndTag();
        }

        urlTag.doEndTag();

        urlTag.doStartTag();

        // removing and adding for each parameter
        // to see if it tries to remove first and add later.
        for (int i = 0; i < 5; i++) {
            ParamTag paramTag1 = new ParamTag();
            paramTag1.setParent(urlTag);
            paramTag1.setName("param" + i);
            paramTag1.setValue(null);
            paramTag1.doStartTag();
            paramTag1.doEndTag();

            ParamTag paramTag2 = new ParamTag();
            paramTag2.setParent(urlTag);
            paramTag2.setName("param" + i);
            paramTag2.setValue("value" + i);
            paramTag2.doStartTag();
            paramTag2.doEndTag();
        }

        urlTag.doEndTag();

        Map<String, String[]> params = urlTag.getUrl().getParameterMap();
        assertNotNull(params);

        // check if five parameters are good
        for (int i = 0; i < 5; i++) {
            assertEquals("value" + i, getFirstArrayStringItem(params.get("param" + i)));
        }
    }

    @Test
    public void testUrlEscapedByDefault() throws Exception {
        final String testLink = "/site/news/a/b/c?p1=1&p2=2&p3=3";
        final String escapedTestLink = HstRequestUtils.escapeXml(testLink);

        urlTag.setVar("link");
        urlTag.setUrl(new MockHstURL() {
            @Override
            public String toString() {
                return testLink;
            }
        });

        urlTag.doStartTag();
        urlTag.doEndTag();

        String link = (String) pageContext.getAttribute("link");

        assertEquals(escapedTestLink, link);
    }

    @Test
    public void testUrlNotEscaped() throws Exception {
        final String testLink = "/site/news/a/b/c?p1=1&p2=2&p3=3";

        urlTag.setEscapeXml(false);
        urlTag.setVar("link");
        urlTag.setUrl(new MockHstURL() {
            @Override
            public String toString() {
                return testLink;
            }
        });

        urlTag.doStartTag();
        urlTag.doEndTag();

        String link = (String) pageContext.getAttribute("link");

        assertEquals(testLink, link);
    }

    private String getFirstArrayStringItem(String [] array) {
        if (array == null || array.length == 0) {
            return null;
        }

        return array[0];
    }

    private static class TestHstURLTag extends BaseHstURLTag {
        private static final long serialVersionUID = 1L;

        private HstURL url;

        @Override
        protected HstURL getUrl() {
            return url;
        }

        @Override
        protected void setUrl(HstURL url) {
            this.url = url;
        }
    }
}
