/*
 *  Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.hst.ga.tags;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockPageContext;
import org.springframework.mock.web.MockServletContext;

public class GoogleAnalyticsAccountIdTagTest {

    private static final String TEST_GA_ACCOUNT_ID = "1234567890";

    private static final String EXPECTED_SCRIPT = "<script type=\"text/javascript\">\n" +
            "  Hippo_Ga_AccountId='" + TEST_GA_ACCOUNT_ID + "';\n" +
            "</script>\n";

    private MockServletContext servletContext;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockPageContext pageContext;

    private GoogleAnalyticsAccountIdTag tag;

    @Before
    public void setUp() throws Exception {
        servletContext = new MockServletContext();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        pageContext = new MockPageContext(servletContext, request, response);

        tag = new GoogleAnalyticsAccountIdTag();
        tag.setPageContext(pageContext);
    }

    @Test
    public void testGoogleAnalyticsAccountIdTag() throws Exception {
        tag.setValue(TEST_GA_ACCOUNT_ID);
        assertEquals(TEST_GA_ACCOUNT_ID, tag.getValue());

        tag.doStartTag();
        tag.doEndTag();

        assertEquals(EXPECTED_SCRIPT, response.getContentAsString());

        assertNull(tag.getValue());
    }

    @Test
    public void testGoogleAnalyticsAccountIdTagWithoutAccountId() throws Exception {
        tag.setValue(null);
        assertNull(tag.getValue());

		tag.doStartTag();
		tag.doEndTag();

		assertEquals("", response.getContentAsString());

		assertNull(tag.getValue());
	}
}
