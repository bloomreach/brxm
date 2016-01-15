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

import java.io.IOException;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.servlet.Filter;
import javax.servlet.ServletException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.contentrestapi.AbstractContentRestApiIT;
import org.hippoecm.hst.site.HstServices;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ContentRestApiRequestsIT extends AbstractContentRestApiIT {

    private static ObjectMapper mapper = new ObjectMapper();
    @Test
    public void test_about_us_document() throws IOException, ServletException, RepositoryException {

        final Filter filter = HstServices.getComponentManager().getComponent("org.hippoecm.hst.container.HstFilter");

        // about-us  handle identifier is 'ebebebeb-5fa8-48a8-b03b-4524373d992b'
        final RequestResponseMock requestResponse = mockGetRequestResponse(filter, "http", "localhost", "/api/documents/ebebebeb-5fa8-48a8-b03b-4524373d992b", null);

        final MockHttpServletRequest request = requestResponse.getRequest();
        final MockHttpServletResponse response = requestResponse.getResponse();

        filter.doFilter(request, response, requestResponse.getFilterChain());

        final String restResponse = response.getContentAsString();
        assertTrue(StringUtils.isNotEmpty(restResponse));

        final Map<String, Object> deserializedAboutUs = mapper.reader(Map.class).readValue(restResponse);
        assertEquals("about-us", deserializedAboutUs.get("hipporest:name"));
        assertEquals("published", deserializedAboutUs.get("hippostd:state"));
        assertEquals("2010-01-21T12:34:11.055+02:00", deserializedAboutUs.get("hippostdpubwf:creationDate"));

        final ImmutableList<String> mixins = ImmutableList.of("hippotranslated:translated", "mix:versionable");
        assertEquals(mixins, deserializedAboutUs.get("jcr:mixinTypes"));
    }

}
