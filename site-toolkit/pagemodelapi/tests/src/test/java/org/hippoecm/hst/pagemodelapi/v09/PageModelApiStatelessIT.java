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
package org.hippoecm.hst.pagemodelapi.v09;

import java.util.List;

import org.apache.logging.log4j.core.LogEvent;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.PageModelApiCleanupValve;
import org.hippoecm.hst.core.container.PageModelApiInitializationValve;
import org.hippoecm.hst.pagemodelapi.common.AbstractPageModelApiITCases;
import org.hippoecm.hst.pagemodelapi.v10.DeterministicJsonPointerFactory;
import org.hippoecm.hst.site.HstServices;
import org.junit.Test;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PageModelApi requests should by default never create an http session if at the beginning of the request there is not yet an
 * http session. PageModelApi requests should be and stay stateless. If an http session is created during a Page Model
 * API request, an error should be logged
 */
public class PageModelApiStatelessIT extends AbstractPageModelApiITCases {

    @Test
    public void assert_no_http_session_created_homepage() throws Exception {
        DeterministicJsonPointerFactory.reset();

        final RequestResponseMock requestResponse = mockGetRequestResponse(
                "http", "localhost", "/spa/resourceapi", null);

        requestResponse.getRequest().addHeader(ContainerConstants.PAGE_MODEL_ACCEPT_VERSION, "1.0");
        render(requestResponse);

        assertThat(requestResponse.getRequest().getSession(false))
                .as("Expected no http session was created")
                .isNull();
    }

    @Test
    public void assertions_http_session_creation_not_allowed_for_page_model_api_request() throws Exception {
        DeterministicJsonPointerFactory.reset();

        // since in 14.2 it is allowed by default, we need to switch the default to not allowed
        PageModelApiInitializationValve component = HstServices.getComponentManager().getComponent(PageModelApiInitializationValve.class.getName());
        component.setStatelessRequestValidation(true);

        final RequestResponseMock requestResponse = mockGetRequestResponse(
                "http", "localhost", "/spa/resourceapi/httpsessionpage", null);

        requestResponse.getRequest().addHeader(ContainerConstants.PAGE_MODEL_ACCEPT_VERSION, "1.0");

        try (Log4jInterceptor interceptor = Log4jInterceptor.onError().trap(PageModelApiCleanupValve.class).build()) {
            render(requestResponse);
            List<LogEvent> messages = interceptor.getEvents();
            assertThat(messages.size())
                    .as("Expected an error message for not allowed http session creation")
                    .isEqualTo(1);
            assertThat(interceptor.messages().findFirst().get())
                    .as("Expected error message wrt prohibited http session creation")
                    .contains("The http session will be invalidated again if still live to avoid congestion");

        }

        assertThat(requestResponse.getRequest().getSession(false))
                .as("Although an http session was created, expected PageModelApiCleanupValve to invalidate " +
                        "it again")
                .isNull();
    }


    @Test
    public void assertions_http_session_creation_allowed_for_page_model_api_request_if_configured_to_be_allowed() throws Exception {
        DeterministicJsonPointerFactory.reset();
        PageModelApiInitializationValve component = HstServices.getComponentManager().getComponent(PageModelApiInitializationValve.class.getName());

        // don't report on http session creation
        component.setStatelessRequestValidation(false);

        final RequestResponseMock requestResponse = mockGetRequestResponse(
                "http", "localhost", "/spa/resourceapi/httpsessionpage", null);

        requestResponse.getRequest().addHeader(ContainerConstants.PAGE_MODEL_ACCEPT_VERSION, "1.0");
        render(requestResponse);
        assertThat(requestResponse.getRequest().getSession(false))
                .as("Expected that a http session was created")
                .isNotNull();
    }
}
