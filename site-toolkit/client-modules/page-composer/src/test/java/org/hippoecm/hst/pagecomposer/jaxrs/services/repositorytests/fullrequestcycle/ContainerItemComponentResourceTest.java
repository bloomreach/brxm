/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
import java.util.List;
import java.util.Map;

import javax.jcr.SimpleCredentials;
import javax.servlet.ServletException;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.Assert.assertEquals;

public class ContainerItemComponentResourceTest extends AbstractComponentResourceTest {

    @Test
    public void get_container_item() throws Exception {

        //creates the preview
        startEdit(ADMIN_CREDENTIALS);

        final String mountId = getNodeId("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");
        final String componentItemId = getNodeId("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:pages/containertestpage/main/container/banner-new-style");


        getComponentItemAs(ADMIN_CREDENTIALS, mountId, componentItemId, true);
        getComponentItemAs(EDITOR_CREDENTIALS, mountId, componentItemId, true);
        // author is not allowed to do a GET on ContainerItemComponentResource.getVariant()
        getComponentItemAs(AUTHOR_CREDENTIALS, mountId, componentItemId, false);

        final String componentItemIdNewStyle = getNodeId("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:pages/containertestpage/main/container/banner-new-style");

        getComponentItemAs(ADMIN_CREDENTIALS, mountId, componentItemIdNewStyle, true);
    }

    private void getComponentItemAs(final SimpleCredentials creds, final String mountId, final String componentItemId,
                                    final boolean allowed) throws IOException, ServletException {
        final RequestResponseMock requestResponse = mockGetRequestResponse(
                "http", "localhost", "/_rp/" + componentItemId + "./hippo-default/en", null, "GET");


        final MockHttpServletResponse response = render(mountId, requestResponse, creds);
        final String restResponse = response.getContentAsString();

        final Map<String, Object> responseMap = mapper.readerFor(Map.class).readValue(restResponse);

        if (allowed) {
            // see default BannerComponentInfo
            List<Map<String, String>> properties = (List) responseMap.get("properties");
            assertEquals(1, properties.size());

            Map<String, String> propertyRepresentation = properties.get(0);

            assertEquals("path", propertyRepresentation.get("name"));
            assertEquals("/some/default", propertyRepresentation.get("defaultValue"));
            assertEquals("/content/document", propertyRepresentation.get("value"));
        } else {
            assertEquals("FORBIDDEN", responseMap.get("errorCode"));
        }
    }
}
