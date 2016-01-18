/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.contentrestapi.requests;

import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.contentrestapi.AbstractContentRestApiIT;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ContentRestApiRequestsIT extends AbstractContentRestApiIT {

    private static ObjectMapper mapper = new ObjectMapper();

    @Test
    public void test_about_us_document() throws Exception {

        // about-us  handle identifier is 'ebebebeb-5fa8-48a8-b03b-4524373d992b'
        final RequestResponseMock requestResponse = mockGetRequestResponse(filter, "http", "localhost", "/api/documents/30092f4e-2ef7-4c72-86a5-8ce895908937", null);

        final MockHttpServletRequest request = requestResponse.getRequest();
        final MockHttpServletResponse response = requestResponse.getResponse();

        filter.doFilter(request, response, requestResponse.getFilterChain());

        final String restResponse = response.getContentAsString();
        assertTrue(StringUtils.isNotEmpty(restResponse));

        final Map<String, Object> deserializedAboutUs = mapper.reader(Map.class).readValue(restResponse);
        assertEquals("about-us", deserializedAboutUs.get("name"));
        // TODO assertEquals("published", deserializedAboutUs.get("hippostd:state"));
        // TODO assertEquals("2010-01-21T12:34:11.055+02:00", deserializedAboutUs.get("hippostdpubwf:creationDate"));

        // TODO final ImmutableList<String> mixins = ImmutableList.of("hippotranslation:translated", "mix:versionable");
        // TODO assertEquals(mixins, deserializedAboutUs.get("jcr:mixinTypes"));
    }

    @Test
    public void non_handle_nodes_are_not_allowed() throws Exception {
        // ebebebeb-5fa8-48a8-b03b-4524373d992a is folder node
        final RequestResponseMock requestResponse = mockGetRequestResponse(filter, "http", "localhost", "/api/documents/ebebebeb-5fa8-48a8-b03b-4524373d992a", null);
        final MockHttpServletRequest request = requestResponse.getRequest();
        final MockHttpServletResponse response = requestResponse.getResponse();

        filter.doFilter(request, response, requestResponse.getFilterChain());
        assertEquals(SC_BAD_REQUEST, response.getStatus());
        assertTrue(response.getContentAsString().contains("should belong to a node of type"));
    }

    @Test
    public void handle_node_of_content_outside_api_mount_is_not_allowed() throws Exception {
        // a62a34ae-5f42-4482-a27a-7f39459ec8ee homepage of subsite
        final RequestResponseMock requestResponse = mockGetRequestResponse(filter, "http", "localhost", "/api/documents/a62a34ae-5f42-4482-a27a-7f39459ec8ee", null);
        final MockHttpServletRequest request = requestResponse.getRequest();
        final MockHttpServletResponse response = requestResponse.getResponse();

        filter.doFilter(request, response, requestResponse.getFilterChain());
        assertEquals(SC_NOT_FOUND, response.getStatus());
        System.out.println(response.getContentAsString());
        assertTrue(response.getContentAsString().contains("does not belong to the content of '/api'"));
    }

}
