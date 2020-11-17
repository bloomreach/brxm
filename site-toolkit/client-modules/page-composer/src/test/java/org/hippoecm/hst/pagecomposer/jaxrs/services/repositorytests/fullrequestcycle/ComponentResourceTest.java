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
import java.util.Collections;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Maps;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ActionsAndStatesRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.CategoryRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ExtResponseRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.component.Category;
import org.hippoecm.hst.pagecomposer.jaxrs.services.component.HstAction;
import org.hippoecm.hst.pagecomposer.jaxrs.services.component.HstState;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.util.DocumentUtils;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.junit.Test;
import org.onehippo.repository.branch.BranchConstants;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;

import static java.util.stream.Collectors.toMap;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.component.HstAction.CHANNEL_CLOSE;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.component.HstAction.CHANNEL_DELETE;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.component.HstAction.CHANNEL_DISCARD_CHANGES;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.component.HstAction.CHANNEL_MANAGE_CHANGES;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.component.HstAction.CHANNEL_PUBLISH;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.component.HstAction.CHANNEL_SETTINGS;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.component.HstAction.PAGE_COPY;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.component.HstAction.PAGE_DELETE;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.component.HstAction.PAGE_MOVE;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.component.HstAction.PAGE_NEW;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.component.HstAction.PAGE_PROPERTIES;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.component.HstAction.XPAGE_COPY;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.component.HstAction.XPAGE_DELETE;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.component.HstAction.XPAGE_MOVE;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.component.HstAction.XPAGE_PUBLISH;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.component.HstAction.XPAGE_RENAME;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.component.HstAction.XPAGE_SCHEDULE_PUBLICATION;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.component.HstAction.XPAGE_SCHEDULE_UNPUBLICATION;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.component.HstAction.XPAGE_UNPUBLISH;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.component.HstCategory.CHANNEL;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.component.HstCategory.PAGE;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.component.HstCategory.WORKFLOW;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.component.HstCategory.XPAGE;
import static org.hippoecm.repository.HippoStdNodeType.HIPPOSTD_STATE;
import static org.hippoecm.repository.HippoStdNodeType.UNPUBLISHED;

public class ComponentResourceTest extends AbstractComponentResourceTest {

    @Test
    public void test_actions_and_states_for_unlocked_page_component() throws RepositoryException, IOException, ServletException {

        final String containerTestPageId = getNodeId("/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages/containertestpage");

        final MockHttpServletResponse response = getActionsAndStatesRequest(containerTestPageId);
        Assertions.assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        final ActionsAndStatesRepresentation actionsAndStates = getActionsAndStates(response);

        final Map<String, Boolean> actions = flattenActions(actionsAndStates.getActions());
        final Map<String, Object> states = flattenStates(actionsAndStates.getStates());

        final Map<String, Boolean> expectedActionItems = ImmutableMap.<String, Boolean>builder()
                .put(key(CHANNEL, CHANNEL_CLOSE), true)
                .put(key(CHANNEL, CHANNEL_DELETE), false)
                .put(key(CHANNEL, CHANNEL_DISCARD_CHANGES), false)
                .put(key(CHANNEL, CHANNEL_MANAGE_CHANGES), false)
                .put(key(CHANNEL, CHANNEL_PUBLISH), false)
                .put(key(CHANNEL, CHANNEL_SETTINGS), true)
                .put(key(PAGE, PAGE_COPY), true)
                .put(key(PAGE, PAGE_DELETE), false)
                .put(key(PAGE, PAGE_MOVE), false)
                .put(key(PAGE, PAGE_NEW), true)
                .put(key(PAGE, PAGE_PROPERTIES), false)
                .build();
        Assertions.assertThat(actions)
                .describedAs("A page component request contains all channel and page actions")
                .isEqualTo(expectedActionItems);

        final Map<String, Object> expectedStates = ImmutableMap.<String, Object>builder()
                .put(key(CHANNEL, HstState.CHANNEL_XPAGE_LAYOUTS), Maps.newHashMap("hst:xpages/xpage1", "XPage 1"))
                .put(key(CHANNEL, HstState.CHANNEL_XPAGE_TEMPLATE_QUERIES), Collections.emptyMap())
                .build();
        Assertions.assertThat(states)
                .describedAs("A page component contains only channel states")
                .isEqualTo(expectedStates);
    }

    @Test
    public void test_actions_and_states_for_locked_page_component() throws RepositoryException, IOException, ServletException {

        final String containerTestPageId = getNodeId("/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages/containertestpage");
        final Session session = createSession(ADMIN_CREDENTIALS);
        session.getNodeByIdentifier(getNodeId("/hst:hst/hst:configurations/unittestproject")).setProperty(HstNodeTypes.CONFIGURATION_PROPERTY_LOCKED, true);
        session.save();
        session.logout();

        final MockHttpServletResponse response = getActionsAndStatesRequest(containerTestPageId);
        Assertions.assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        final ActionsAndStatesRepresentation actionsAndStates = getActionsAndStates(response);
        final Map<String, Boolean> actions = flattenActions(actionsAndStates.getActions());
        final Map<String, Object> states = flattenStates(actionsAndStates.getStates());

        final Map<String, Boolean> expectedActionItems = ImmutableMap.<String, Boolean>builder()
                .put(key(CHANNEL, CHANNEL_CLOSE), true)
                .put(key(CHANNEL, CHANNEL_DELETE), false)
                .put(key(CHANNEL, CHANNEL_DISCARD_CHANGES), false)
                .put(key(CHANNEL, CHANNEL_MANAGE_CHANGES), false)
                .put(key(CHANNEL, CHANNEL_PUBLISH), false)
                .put(key(CHANNEL, CHANNEL_SETTINGS), true)
                .build();
        Assertions.assertThat(actions)
                .describedAs("A page component request contains all channel and page actions")
                .isEqualTo(expectedActionItems);

        final Map<String, Object> expectedStates = ImmutableMap.<String, Object>builder()
                .put(key(CHANNEL, HstState.CHANNEL_XPAGE_LAYOUTS), Maps.newHashMap("hst:xpages/xpage1", "XPage 1"))
                .put(key(CHANNEL, HstState.CHANNEL_XPAGE_TEMPLATE_QUERIES), Collections.emptyMap())
                .build();
        Assertions.assertThat(states)
                .describedAs("A page component contains only channel states")
                .isEqualTo(expectedStates);
    }

    @Test
    public void test_actions_and_states_for_published_xpage() throws RepositoryException, IOException, ServletException, WorkflowException {

        final String xpagePath = "/unittestcontent/documents/unittestproject/experiences/expPage1";
        final String handleId = getNodeId(xpagePath);
        final HippoSession hippoSession = (HippoSession) createSession(ADMIN_CREDENTIALS);
        final WorkflowManager workflowManager = hippoSession.getWorkspace().getWorkflowManager();
        final DocumentWorkflow documentWorkflow = (DocumentWorkflow) workflowManager.getWorkflow("default", hippoSession.getNodeByIdentifier(handleId));
        documentWorkflow.depublish();
        documentWorkflow.publish();
        final String name = DocumentUtils.getDisplayName(documentWorkflow.getNode()).orElse("UNDEFINED");
        final String documentXPageId = getVariant(documentWorkflow.getNode(), UNPUBLISHED).getNode("hst:xpage").getIdentifier();
        hippoSession.save();
        hippoSession.logout();

        final MockHttpServletResponse response = getActionsAndStatesRequest(documentXPageId);
        Assertions.assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        final ActionsAndStatesRepresentation actionsAndStates = getActionsAndStates(response);
        final Map<String, Boolean> actions = flattenActions(actionsAndStates.getActions());

        final Map<String, Boolean> expectedActionItems = ImmutableMap.<String, Boolean>builder()
                .put(key(CHANNEL, CHANNEL_CLOSE), true)
                .put(key(CHANNEL, CHANNEL_DELETE), false)
                .put(key(CHANNEL, CHANNEL_DISCARD_CHANGES), false)
                .put(key(CHANNEL, CHANNEL_MANAGE_CHANGES), false)
                .put(key(CHANNEL, CHANNEL_PUBLISH), false)
                .put(key(CHANNEL, CHANNEL_SETTINGS), true)
                .put(key(XPAGE, XPAGE_COPY), true)
                .put(key(XPAGE, XPAGE_DELETE), false)
                .put(key(XPAGE, XPAGE_MOVE), false)
                .put(key(XPAGE, XPAGE_PUBLISH), false)
                .put(key(XPAGE, XPAGE_RENAME), false)
                .put(key(XPAGE, XPAGE_SCHEDULE_PUBLICATION), false)
                .put(key(XPAGE, XPAGE_SCHEDULE_UNPUBLICATION), true)
                .put(key(XPAGE, XPAGE_UNPUBLISH), true)
                .build();
        Assertions.assertThat(actions)
                .describedAs("An xpage request contains all channel and xpage actions")
                .isEqualTo(expectedActionItems);

        final Map<String, Object> states = flattenStates(actionsAndStates.getStates());

        final Map<String, Object> expectedStates = ImmutableMap.<String, Object>builder()
                .put(key(XPAGE, HstState.XPAGE_BRANCH_ID), BranchConstants.MASTER_BRANCH_ID)
                .put(key(XPAGE, HstState.XPAGE_ID), handleId)
                .put(key(XPAGE, HstState.XPAGE_NAME), name)
                .put(key(XPAGE, HstState.XPAGE_STATE), "live")
                .put(key(CHANNEL, HstState.CHANNEL_XPAGE_LAYOUTS), Maps.newHashMap("hst:xpages/xpage1", "XPage 1"))
                .put(key(CHANNEL, HstState.CHANNEL_XPAGE_TEMPLATE_QUERIES), Collections.emptyMap())
                .put(key(WORKFLOW, HstState.WORKFLOW_REQUESTS), Collections.emptyList())
                .build();
        Assertions.assertThat(states)
                .describedAs("A published xpage request contains xpage and channel states")
                .isEqualTo(expectedStates);
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

    private Map<String, Boolean> flattenActions(Map<String, CategoryRepresentation> actions) {
        return actions.entrySet().stream()
                .flatMap(ec -> ec.getValue().getItems().entrySet().stream().map(ea -> Pair.of(ec.getKey() + "." + ea.getKey(), ea.getValue().isEnabled())))
                .collect(toMap(Pair::getLeft, Pair::getRight));
    }

    private Map<String, Object> flattenStates(Map<String, Map<String, Object>> states) {
        return states.entrySet().stream()
                .flatMap(ec -> ec.getValue().entrySet().stream().map(ea -> Pair.of(ec.getKey() + "." + ea.getKey(), ea.getValue())))
                .collect(toMap(Pair::getLeft, Pair::getRight));
    }

    private static String key(Category category, HstAction action) {
        return category.getName() + "." + action.getName();
    }

    private static String key(Category category, HstState state) {
        return category.getName() + "." + state.getName();
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
