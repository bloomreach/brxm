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
package org.hippoecm.hst.pagecomposer.jaxrs.services.repositorytests.fullrequestcycle;

import java.io.IOException;
import java.util.Map;

import javax.jcr.SimpleCredentials;
import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.pagecomposer.jaxrs.AbstractFullRequestCycleTest;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RootResourceTest extends AbstractFullRequestCycleTest {

    @Test
    public void keepalive_root_resource_as_admin() throws Exception {
        final SimpleCredentials admin = new SimpleCredentials("admin", "admin".toCharArray());
        assertions(admin);
    }

    @Test
    public void keepalive_root_resource_as_webmaster() throws Exception {
        final SimpleCredentials admin = new SimpleCredentials("editor", "editor".toCharArray());
        assertions(admin);
    }

    private void assertions(final SimpleCredentials admin) throws IOException, ServletException {
        final RequestResponseMock requestResponse = mockGetRequestResponse(
                "http", "localhost", "/_rp/cafebabe-cafe-babe-cafe-babecafebabe./keepalive", null, "GET");

        final MockHttpServletResponse response = render(requestResponse, admin);

        final String restResponse = response.getContentAsString();
        assertTrue(StringUtils.isNotEmpty(restResponse));

        final Map<String, Object> responseMap = mapper.reader(Map.class).readValue(restResponse);
        assertEquals(Boolean.TRUE, responseMap.get("success"));
        assertEquals("OK", responseMap.get("message"));
        assertEquals(null, responseMap.get("errorCode"));
    }

}
