/**
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

import java.io.UnsupportedEncodingException;

import javax.servlet.jsp.JspException;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.mock.core.component.MockHstRequest;
import org.hippoecm.hst.mock.core.component.MockHstResponse;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockBodyContent;
import org.springframework.mock.web.MockPageContext;
import org.springframework.mock.web.MockServletContext;
import org.w3c.dom.Element;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestHeadContributionTag {

    private HeadContributionTag tag;
    private MockHstRequest hstRequest;
    private MockHstResponse hstResponse;

    @Before
    public void setUp() {
        MockServletContext servletContext = new MockServletContext();

        hstRequest = new MockHstRequest();
        hstRequest.setLifecyclePhase(HstRequest.RENDER_PHASE);
        hstResponse = new MockHstResponse();

        MockPageContext pageContext = new MockPageContext(servletContext, hstRequest, hstResponse);

        tag = new HeadContributionTag();
        tag.setPageContext(pageContext);
    }

    @Test
    public void emptyTag() throws UnsupportedEncodingException, JspException {
        tag.doStartTag();
        tag.doEndTag();
        tag.release();

        assertEquals(0, hstResponse.getHeadElements().size());

        String content = hstResponse.getContentAsString();
        assertTrue(StringUtils.isEmpty(content));
    }

    @Test
    public void simpleContribution() throws UnsupportedEncodingException, JspException {
        tag.doStartTag();
        tag.setBodyContent(new MockBodyContent("<script>'foo';</script>", hstResponse));
        tag.doEndTag();
        tag.release();

        assertEquals(1, hstResponse.getHeadElements().size());
        final Element elem = hstResponse.getHeadElements().get(0);
        assertEquals("script", elem.getTagName());
        assertEquals("'foo';", elem.getTextContent());
    }

    @Test
    public void noContributionIfNotRenderPhase() throws UnsupportedEncodingException, JspException {
        hstRequest.setLifecyclePhase(HstRequest.ACTION_PHASE);

        tag.doStartTag();
        tag.setBodyContent(new MockBodyContent("<script>'foo';</script>", hstResponse));
        tag.doEndTag();
        tag.release();

        assertEquals(0, hstResponse.getHeadElements().size());

        hstRequest.setLifecyclePhase(HstRequest.RESOURCE_PHASE);

        tag.doStartTag();
        tag.setBodyContent(new MockBodyContent("<script>'foo';</script>", hstResponse));
        tag.doEndTag();
        tag.release();

        assertEquals(0, hstResponse.getHeadElements().size());
    }

}