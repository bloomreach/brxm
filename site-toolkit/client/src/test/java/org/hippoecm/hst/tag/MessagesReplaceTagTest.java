/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
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

import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.resourcebundle.SimpleListResourceBundle;
import org.hippoecm.hst.util.HstRequestUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockBodyContent;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockPageContext;
import org.springframework.mock.web.MockServletContext;

import static org.junit.Assert.assertEquals;

/**
 * MessagesReplaceTagTest.
 */
public class MessagesReplaceTagTest {

    private MockServletContext servletContext;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockPageContext pageContext;

    private ResourceBundle bundle;
    private MessagesReplaceTag messagesReplaceTag;

    @Before
    public void before() throws Exception {
        servletContext = new MockServletContext();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        pageContext = new MockPageContext(servletContext, request, response);

        final Map<String, String> bundleContent = new HashMap<>();
        bundleContent.put("example.title", "<script>alert('hi there')</script>");
        bundleContent.put("example.subtitle", "<b>howdy?</b>");
        bundleContent.put("example.introduction", "This is an example introduction.");
        bundleContent.put("example.body", "<h1>Hello, World!</h1><p>This is the body.</p>");

        bundle = new SimpleListResourceBundle(bundleContent);

        messagesReplaceTag = new MessagesReplaceTag();
        messagesReplaceTag.setPageContext(pageContext);
        messagesReplaceTag.setBundle(bundle);
    }

    @Test
    public void testMessagesReplaceTagOutputWithoutEscaping() throws Exception {
        messagesReplaceTag.setEscapeMessageXml(false);
        messagesReplaceTag.doStartTag();

        final MockBodyContent bodyContent =
                new MockBodyContent(
                        "<h2>${example.subtitle}</h2>\n"
                        + "<p>${example.introduction}</p>\n"
                        + "<div>${example.body}</div>",
                        response);

        messagesReplaceTag.setBodyContent(bodyContent);
        messagesReplaceTag.doEndTag();

        final String[] lines = StringUtils.split(response.getContentAsString(), "\n");
        assertEquals("<h2><b>howdy?</b></h2>", lines[0]);
        assertEquals("<p>This is an example introduction.</p>", lines[1]);
        assertEquals("<div><h1>Hello, World!</h1><p>This is the body.</p></div>", lines[2]);
    }

    @Test
    public void testMessagesReplaceTagOutputByDefault() throws Exception {
        messagesReplaceTag.doStartTag();

        final MockBodyContent bodyContent =
                new MockBodyContent(
                        "<h1>${example.title}</h1>\n"
                        + "<div>${example.introduction}</div>",
                        response);

        messagesReplaceTag.setBodyContent(bodyContent);
        messagesReplaceTag.doEndTag();

        final String[] lines = StringUtils.split(response.getContentAsString(), "\n");
        assertEquals("<h1>" + HstRequestUtils.escapeXml("<script>alert('hi there')</script>") + "</h1>", lines[0]);
        assertEquals("<div>" + HstRequestUtils.escapeXml("This is an example introduction.") + "</div>", lines[1]);
    }

}
