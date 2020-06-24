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
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.Assertions;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ActionsRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.action.Category;
import org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstAction;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;

import static java.util.stream.Collectors.toMap;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstAction.CHANNEL_CLOSE;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstAction.CHANNEL_CONFIRM;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstAction.CHANNEL_DELETE;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstAction.CHANNEL_DISCARD_CHANGES;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstAction.CHANNEL_MANAGE_CHANGED;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstAction.CHANNEL_PUBLISH;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstAction.CHANNEL_SETTINGS;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstAction.PAGE_COPY;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstAction.PAGE_DELETE;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstAction.PAGE_MOVE;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstAction.PAGE_NEW;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstAction.PAGE_PROPERTIES;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstAction.PAGE_TOOLS;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstCategories.channel;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstCategories.page;

public class ComponentResourceTest extends AbstractComponentResourceTest {

    @Test
    public void test_actions_for_unlocked_page_component() throws RepositoryException, IOException, ServletException {

        final String containerTestPageId = getNodeId("/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages/containertestpage");

        final MockHttpServletResponse response = getActionsRequest(containerTestPageId);
        Assertions.assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        final Map<String, Boolean> actions = flatten(getActions(response));

        final Map<String, Boolean> expectedActionItems = ImmutableMap.<String, Boolean>builder()
                .put(key(channel(), CHANNEL_CLOSE), true)
                .put(key(channel(), CHANNEL_CONFIRM), true)
                .put(key(channel(), CHANNEL_DELETE), true)
                .put(key(channel(), CHANNEL_DISCARD_CHANGES), true)
                .put(key(channel(), CHANNEL_MANAGE_CHANGED), true)
                .put(key(channel(), CHANNEL_PUBLISH), true)
                .put(key(channel(), CHANNEL_SETTINGS), true)
                .put(key(page(), PAGE_COPY), true)
                .put(key(page(), PAGE_DELETE), true)
                .put(key(page(), PAGE_MOVE), true)
                .put(key(page(), PAGE_NEW), true)
                .put(key(page(), PAGE_PROPERTIES), true)
                .put(key(page(), PAGE_TOOLS), true)
                .build();
        Assertions.assertThat(actions)
                .describedAs("A page component request contains all channel and page components")
                .isEqualTo(expectedActionItems);
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

    private Map<String, Boolean> flatten(ActionsRepresentation actionsRepresentation) {
        return actionsRepresentation.getActions().entrySet().stream()
                .flatMap(ec -> ec.getValue().getItems().entrySet().stream().map(ea -> Pair.of(ec.getKey() + "." + ea.getKey(), ea.getValue().isEnabled())))
                .collect(toMap(Pair::getLeft, Pair::getRight));
    }

    private static String key(Category category, HstAction action) {
        return category.getName() + "." + action.getName();
    }

}
