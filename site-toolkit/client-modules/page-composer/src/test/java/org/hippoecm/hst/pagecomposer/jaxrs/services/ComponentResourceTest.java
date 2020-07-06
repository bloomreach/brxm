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

package org.hippoecm.hst.pagecomposer.jaxrs.services;


import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.ws.rs.Path;

import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.hippoecm.hst.pagecomposer.jaxrs.services.action.Action;
import org.hippoecm.hst.pagecomposer.jaxrs.services.action.ActionService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.restassured.http.ContentType;
import static java.util.Collections.singleton;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstAction.CHANNEL_DISCARD_CHANGES;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstAction.PAGE_COPY;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstAction.XPAGE_DELETE;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstCategories.channel;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstCategories.page;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstCategories.xpage;

@RunWith(EasyMockRunner.class)
public class ComponentResourceTest extends AbstractResourceTest {

    static final String MOCK_PATH = "/test";

    @Mock
    private ActionService actionService;

    @Path(MOCK_PATH)
    private static final class ComponentResourceMock extends ComponentResource {

    }

    @Before
    public void setUp() {
        final ComponentResourceMock resourceUnderTest = new ComponentResourceMock();
        resourceUnderTest.setActionService(actionService);
        setup(createDefaultConfig()
                .addServerSingleton(resourceUnderTest));
    }

    @Test
    public void test_get_menu() throws RepositoryException {
        final Map<String, Set<Action>> actions = new HashMap<>();
        actions.put(channel().getName(), singleton(CHANNEL_DISCARD_CHANGES.toAction(true)));
        actions.put(page().getName(), singleton(PAGE_COPY.toAction(true)));
        actions.put(xpage().getName(), singleton(XPAGE_DELETE.toAction(true)));
        expect(actionService.getActionsByCategory(anyObject(), anyString())).andReturn(actions);
        replay(actionService);
        // If you want to see the JSON use prettyPeek
        given().when()
                .get(MOCK_PATH + "/item/foo").prettyPeek()
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("success", equalTo(true))
                .body("reloadRequired", equalTo(false))
                .body("data.actions.channel.items.discard-changes.enabled", equalTo(true))
                .body("data.actions.page.items.copy.enabled", equalTo(true))
                .body("data.actions.xpage.items.delete.enabled", equalTo(true))
        ;
    }

    @Test
    public void test_get_menu_without_page_and_xpage() throws RepositoryException {
        final Map<String, Set<Action>> actions = new HashMap<>();
        expect(actionService.getActionsByCategory(anyObject(), anyString())).andReturn(actions);
        actions.put(channel().getName(), singleton(CHANNEL_DISCARD_CHANGES.toAction(true)));
        replay(actionService);
        // If you want to see the JSON use prettyPeek
        given().when()
                .get(MOCK_PATH + "/item/foo").prettyPeek()
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("success", equalTo(true))
                .body("reloadRequired", equalTo(false))
                .body("data.actions.channel.items.discard-changes.enabled", equalTo(true))
                .body("data.actions.page", nullValue())
                .body("data.actions.xpage", nullValue())
        ;
    }

}
