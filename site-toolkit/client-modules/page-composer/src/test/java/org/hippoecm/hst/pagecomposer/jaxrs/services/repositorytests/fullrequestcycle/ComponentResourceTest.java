/*
 * Copyright 2020 Bloomreach
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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.Assertions;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ActionsAndStatesRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ExtResponseRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.action.Category;
import org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstAction;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.junit.Test;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;

import static java.util.stream.Collectors.toMap;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstAction.CHANNEL_CLOSE;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstAction.CHANNEL_DELETE;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstAction.CHANNEL_DISCARD_CHANGES;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstAction.CHANNEL_MANAGE_CHANGES;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstAction.CHANNEL_PUBLISH;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstAction.CHANNEL_SETTINGS;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstAction.PAGE_COPY;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstAction.PAGE_DELETE;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstAction.PAGE_MOVE;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstAction.PAGE_NEW;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstAction.PAGE_PROPERTIES;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstAction.XPAGE_DELETE;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstAction.XPAGE_MOVE;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstAction.XPAGE_NEW;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstCategories.channel;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstCategories.page;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstCategories.xpage;
import static org.hippoecm.repository.HippoStdNodeType.HIPPOSTD_STATE;
import static org.hippoecm.repository.HippoStdNodeType.UNPUBLISHED;

public class ComponentResourceTest extends AbstractComponentResourceTest {

    @Test
    public void test_actions_for_unlocked_page_component() throws RepositoryException, IOException, ServletException {

        final String containerTestPageId = getNodeId("/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages/containertestpage");

        final MockHttpServletResponse response = getActionsAndStatesRequest(containerTestPageId);
        Assertions.assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        final Map<String, Boolean> actions = flatten(getActionsAndStates(response));

        final Map<String, Boolean> expectedActionItems = ImmutableMap.<String, Boolean>builder()
                .put(key(channel(), CHANNEL_CLOSE), true)
                .put(key(channel(), CHANNEL_DELETE), false)
                .put(key(channel(), CHANNEL_DISCARD_CHANGES), false)
                .put(key(channel(), CHANNEL_MANAGE_CHANGES), false)
                .put(key(channel(), CHANNEL_PUBLISH), false)
                .put(key(channel(), CHANNEL_SETTINGS), true)
                .put(key(page(), PAGE_COPY), true)
                .put(key(page(), PAGE_DELETE), false)
                .put(key(page(), PAGE_MOVE), false)
                .put(key(page(), PAGE_NEW), true)
                .put(key(page(), PAGE_PROPERTIES), false)
                .build();
        Assertions.assertThat(actions)
                .describedAs("A page component request contains all channel and page components")
                .isEqualTo(expectedActionItems);
    }

    @Test
    public void test_actions_for_locked_page_component() throws RepositoryException, IOException, ServletException {

        final String containerTestPageId = getNodeId("/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages/containertestpage");
        final Session session = createSession(ADMIN_CREDENTIALS);
        session.getNodeByIdentifier(getNodeId("/hst:hst/hst:configurations/unittestproject")).setProperty(HstNodeTypes.CONFIGURATION_PROPERTY_LOCKED, true);
        session.save();
        session.logout();

        final MockHttpServletResponse response = getActionsAndStatesRequest(containerTestPageId);
        Assertions.assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        final Map<String, Boolean> actions = flatten(getActionsAndStates(response));

        final Map<String, Boolean> expectedActionItems = ImmutableMap.<String, Boolean>builder()
                .put(key(channel(), CHANNEL_CLOSE), true)
                .put(key(channel(), CHANNEL_DELETE), false)
                .put(key(channel(), CHANNEL_DISCARD_CHANGES), false)
                .put(key(channel(), CHANNEL_MANAGE_CHANGES), false)
                .put(key(channel(), CHANNEL_PUBLISH), false)
                .put(key(channel(), CHANNEL_SETTINGS), true)
                .build();
        Assertions.assertThat(actions)
                .describedAs("A page component request contains all channel and page components")
                .isEqualTo(expectedActionItems);
    }

    @Test
    public void test_actions_for_xpage_component() throws RepositoryException, IOException, ServletException, WorkflowException {

        final String xpagePath = "/unittestcontent/documents/unittestproject/experiences/expPage1";
        final String handleId = getNodeId(xpagePath);
        final HippoSession hippoSession = (HippoSession) createSession(ADMIN_CREDENTIALS);
        final WorkflowManager workflowManager = hippoSession.getWorkspace().getWorkflowManager();
        final DocumentWorkflow documentWorkflow = (DocumentWorkflow) workflowManager.getWorkflow("default", hippoSession.getNodeByIdentifier(handleId));
        documentWorkflow.depublish();
        documentWorkflow.publish();
        final String documentXPageId = getVariant(documentWorkflow.getNode(), UNPUBLISHED).getNode("hst:xpage").getIdentifier();
        hippoSession.save();
        hippoSession.logout();

        final MockHttpServletResponse response = getActionsAndStatesRequest(documentXPageId);
        Assertions.assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        final Map<String, Boolean> actions = flatten(getActionsAndStates(response));

        final Map<String, Boolean> expectedActionItems = ImmutableMap.<String, Boolean>builder()
                .put(key(channel(), CHANNEL_CLOSE), true)
                .put(key(channel(), CHANNEL_DELETE), false)
                .put(key(channel(), CHANNEL_DISCARD_CHANGES), false)
                .put(key(channel(), CHANNEL_MANAGE_CHANGES), false)
                .put(key(channel(), CHANNEL_PUBLISH), false)
                .put(key(channel(), CHANNEL_SETTINGS), true)
                .put(key(xpage(), XPAGE_DELETE), false)
                .put(key(xpage(), XPAGE_MOVE), false)
                .put(key(xpage(), XPAGE_NEW), false)
                .build();
        Assertions.assertThat(actions)
                .describedAs("A page component request contains all channel and page components")
                .isEqualTo(expectedActionItems);
    }

    private MockHttpServletResponse getActionsAndStatesRequest(String containerId) throws RepositoryException, IOException, ServletException {


        final String homeSiteMapItemUuid = getNodeId("/hst:hst/hst:configurations/unittestproject/hst:sitemap/home");

        final String pathInfo = "/_rp/" + containerId + "./item/" + homeSiteMapItemUuid;
        final String mountId = getNodeId("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

        final RequestResponseMock requestResponseMock = mockGetRequestResponse("http", "localhost", pathInfo, null, "GET");
        requestResponseMock.getRequest().setQueryString("unwrapped=true");

        return render(mountId, requestResponseMock, ADMIN_CREDENTIALS, null);
    }

    private ActionsAndStatesRepresentation getActionsAndStates(MockHttpServletResponse response) throws UnsupportedEncodingException, JsonProcessingException {
        final ExtResponseRepresentation extResponseRepresentation = mapper.readValue(response.getContentAsString(), ExtResponseRepresentation.class);
        // Jackson's representation for the data object is a map of maps.
        // However, for assertions we prefer the ActionRepresentation.
        // So 1st we map the data to a string
        final String dataAsString = mapper.writeValueAsString(extResponseRepresentation.getData());
        // and then back again to an ActionRepresentation
        return mapper.readValue(dataAsString, ActionsAndStatesRepresentation.class);
    }

    private Map<String, Boolean> flatten(ActionsAndStatesRepresentation actionsAndStatesRepresentation) {
        return actionsAndStatesRepresentation.getActions().entrySet().stream()
                .flatMap(ec -> ec.getValue().getItems().entrySet().stream().map(ea -> Pair.of(ec.getKey() + "." + ea.getKey(), ea.getValue().isEnabled())))
                .collect(toMap(Pair::getLeft, Pair::getRight));
    }

    private static String key(Category category, HstAction action) {
        return category.getName() + "." + action.getName();
    }

    private Node getVariant(final Node handle, final String state) throws RepositoryException {
        for (Node variant : new NodeIterable(handle.getNodes(handle.getName()))) {
            if (state.equals(JcrUtils.getStringProperty(variant, HIPPOSTD_STATE, null))) {
                return variant;
            }
        }
        return null;
    }

}
