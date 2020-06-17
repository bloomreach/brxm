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
 *
 */
package org.hippoecm.hst.pagecomposer.jaxrs.services.repositorytests.fullrequestcycle;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import javax.ws.rs.core.Response;

import org.assertj.core.api.Assertions;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ActionsRepresentation;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.core.JsonProcessingException;

import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstCategories.channel;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstCategories.page;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstCategories.xpage;

public class ComponentResourceTest extends AbstractComponentResourceTest {

    @Test
    public void test_actions_for_unlocked_page_component() throws RepositoryException, IOException, ServletException {
        final String containerTestPageId = getNodeId("/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages/containertestpage");

        final MockHttpServletResponse response = getActionsRequest(containerTestPageId);
        Assertions.assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        final ActionsRepresentation actions = getActions(response);
        Assertions.assertThat(actions.getActions())
                .isNotEmpty()
                .containsKeys(channel().getName(), page().getName())
                .doesNotContainKey(xpage().getName());
    }

    private MockHttpServletResponse getActionsRequest(String containerId) throws RepositoryException, IOException, ServletException {

        final String pathInfo = "/_rp/" + containerId;
        final String mountId = getNodeId("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

        final RequestResponseMock requestResponseMock = mockGetRequestResponse("http", "localhost", pathInfo, null, "GET");
        requestResponseMock.getRequest().setQueryString("unwrapped=true");

        return render(mountId, requestResponseMock, ADMIN_CREDENTIALS, null);
    }

    private ActionsRepresentation getActions(MockHttpServletResponse response) throws UnsupportedEncodingException, JsonProcessingException {
        return mapper.readValue(response.getContentAsString(), ActionsRepresentation.class);
    }

}
