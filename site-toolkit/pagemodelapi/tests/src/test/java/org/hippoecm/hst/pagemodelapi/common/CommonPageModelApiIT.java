/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagemodelapi.common;

import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.JsonNode;

import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.pagemodelapi.v10.DeterministicJsonPointerFactory;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hippoecm.hst.core.container.ContainerConstants.PAGE_MODEL_API_VERSION;

public class CommonPageModelApiIT extends AbstractPageModelApiITCases {

    @Test
    public void default_page_model_api_version_is_v10() throws Exception {
        DeterministicJsonPointerFactory.reset();
        final RequestResponseMock requestResponse = mockGetRequestResponse(
                "http", "localhost", "/spa/resourceapi/home", null);

        final MockHttpServletResponse response = render(requestResponse);

        JsonNode root = mapper.readTree(response.getContentAsString());

        assertThat(root.get("meta").get("version").asText())
                .as("Expected default version to be 1.0")
                .isEqualTo("1.0");

        assertThat(response.getHeader(PAGE_MODEL_API_VERSION))
                .as("Expected default header version to be 1.0")
                .isEqualTo("1.0");

    }

    @Test
    public void request_existing_version_api() throws Exception {
        DeterministicJsonPointerFactory.reset();
        final RequestResponseMock requestResponse = mockGetRequestResponse(
                "http", "localhost", "/spa/resourceapi/home", null);

        requestResponse.request.addHeader(ContainerConstants.PAGE_MODEL_ACCEPT_VERSION, "1.0");

        final MockHttpServletResponse response = render(requestResponse);

        assertThat(response.getHeader(PAGE_MODEL_API_VERSION))
                .as("Expected default header version to be 1.0")
                .isEqualTo("1.0");

    }

    @Test
    public void non_existing_requested_version_api_results_in_BAD_REQUEST() throws Exception {
        final RequestResponseMock requestResponse = mockGetRequestResponse(
                "http", "localhost", "/spa/resourceapi/home", null);

        requestResponse.request.addHeader(ContainerConstants.PAGE_MODEL_ACCEPT_VERSION, "0.1");

        final MockHttpServletResponse response = render(requestResponse);

        assertThat(response.getStatus())
                .as("Expected a BAD_REQUEST for non-supported version")
                .isEqualTo(HttpServletResponse.SC_BAD_REQUEST);

        assertThat(response.getErrorMessage())
                .isEqualTo("UnsupportedApiVersion: Header 'Accept-Version: 0.1' points to a non-supported version");

    }
}
