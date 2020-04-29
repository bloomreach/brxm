/*
 *  Copyright 2016-2020 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.pagecomposer.jaxrs.AbstractFullRequestCycleTest;
import org.hippoecm.hst.pagecomposer.jaxrs.AbstractPageComposerTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.hippoecm.hst.configuration.HstNodeTypes.CHANNEL_PROPERTY_DELETABLE;
import static org.hippoecm.hst.configuration.HstNodeTypes.CONFIGURATION_PROPERTY_LOCKED;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_INHERITS_FROM;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_CONFIGURATION;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError.CHILD_MOUNT_EXISTS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class RootResourceTest extends AbstractFullRequestCycleTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        final Session session = createSession("admin", "admin");
        AbstractPageComposerTest.createHstConfigBackup(session);
        session.save();
        session.logout();
    }

    @After
    public void tearDown() throws Exception {
        final Session session = createSession("admin", "admin");
        AbstractPageComposerTest.restoreHstConfigBackup(session);
        session.logout();
        super.tearDown();
    }

    @Test
    public void remove_channel_not_allowed_if_not_hippo_admin_role_fails() throws Exception {
        final RequestResponseMock requestResponse = mockGetRequestResponse(
                "http", "localhost", "/_rp/cafebabe-cafe-babe-cafe-babecafebabe./channels/unittestproject", null, "DELETE");

        Session session = createSession("admin", "admin");
        try {
            final String mountId = session.getNode("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root").getIdentifier();

            final SimpleCredentials editor = new SimpleCredentials("editor", "editor".toCharArray());
            final MockHttpServletResponse response = render(mountId, requestResponse, editor);
            assertEquals(SC_FORBIDDEN, response.getStatus());
        } finally {
            session.logout();
        }
    }

    @Test
    public void remove_channel_with_right_role_but_channel_not_existing_results_in_404() throws Exception {

        final RequestResponseMock requestResponse = mockGetRequestResponse(
                "http", "localhost", "/_rp/cafebabe-cafe-babe-cafe-babecafebabe./channels/nonexisting", null, "DELETE");

        SimpleCredentials admin = new SimpleCredentials("admin", "admin".toCharArray());

        final MockHttpServletResponse response = render(null, requestResponse, admin);
        assertEquals(SC_NOT_FOUND, response.getStatus());
    }

    @Test
    public void remove_channel_with_right_role_but_channel_not_deletable_fails() throws Exception {

        final RequestResponseMock requestResponse = mockGetRequestResponse(
                "http", "localhost", "/_rp/cafebabe-cafe-babe-cafe-babecafebabe./channels/unittestproject", null, "DELETE");

        SimpleCredentials admin = new SimpleCredentials("admin", "admin".toCharArray());


        Session session = createSession("admin", "admin");
        try {
            final String mountId = session.getNode("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root").getIdentifier();

            final MockHttpServletResponse response = render(mountId, requestResponse, admin);
            assertEquals(SC_INTERNAL_SERVER_ERROR, response.getStatus());
        } finally {
            session.logout();
        }
    }

    @Test
    public void remove_channel_with_right_role_and_channel_deletable_but_mount_has_child_mounts_fails() throws Exception {

        Session session = createSession("admin", "admin");
        session.getNode("/hst:hst/hst:configurations/unittestproject/hst:channel").setProperty(CHANNEL_PROPERTY_DELETABLE, true);
        session.save();

        try {
            final RequestResponseMock requestResponse = mockGetRequestResponse(
                    "http", "localhost", "/_rp/cafebabe-cafe-babe-cafe-babecafebabe./channels/unittestproject", null, "DELETE");

            SimpleCredentials admin = new SimpleCredentials("admin", "admin".toCharArray());

            final String mountId = session.getNode("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root").getIdentifier();
            final MockHttpServletResponse response = render(mountId, requestResponse, admin);
            assertEquals(SC_BAD_REQUEST, response.getStatus());

            final String restResponse = response.getContentAsString();
            Map<String, Object> result = mapper.readerFor(Map.class).readValue(restResponse);
            assertEquals(CHILD_MOUNT_EXISTS.toString(), result.get("error"));
        } finally {
            session.logout();
        }
    }

    @Test
    public void remove_channel_with_right_role_and_channel_deletable_and_mount_has_no_child_mounts_succeeds() throws Exception {

        Session session = createSession("admin", "admin");
        session.getNode("/hst:hst/hst:configurations/unittestsubproject/hst:channel").setProperty(CHANNEL_PROPERTY_DELETABLE, true);
        session.save();

        try {
            final RequestResponseMock requestResponse = mockGetRequestResponse(
                    "http", "localhost", "/_rp/cafebabe-cafe-babe-cafe-babecafebabe./channels/unittestsubproject", null, "DELETE");

            SimpleCredentials admin = new SimpleCredentials("admin", "admin".toCharArray());

            // pre assertions
            assertTrue(session.nodeExists("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root/subsite"));
            assertTrue(session.nodeExists("/hst:hst/hst:hosts/testgroup/test/unit/sub/hst:root"));
            assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestsubproject"));
            assertTrue(session.nodeExists("/hst:hst/hst:sites/unittestsubproject"));

            final String mountId = session.getNode("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root/subsite").getIdentifier();

            final MockHttpServletResponse response = render(mountId, requestResponse, admin);
            assertEquals(SC_OK, response.getStatus());

            // assert 'unittestsubproject' configurations are gone

            assertFalse(session.nodeExists("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root/subsite"));
            assertFalse(session.nodeExists("/hst:hst/hst:hosts/testgroup/test/unit/sub/hst:root"));
            assertFalse(session.nodeExists("/hst:hst/hst:configurations/unittestsubproject"));
            assertFalse(session.nodeExists("/hst:hst/hst:sites/unittestsubproject"));
        } finally {
            session.logout();
        }
    }

    @Test
    public void successful_remove_channel_keeps_hst_configuration_in_case_it_is_inherited() throws Exception {

        Session session = createSession("admin", "admin");
        session.getNode("/hst:hst/hst:configurations/unittestsubproject/hst:channel").setProperty(CHANNEL_PROPERTY_DELETABLE, true);

        // add a new configuration node that inherits from unittestsubproject : That makes the unittestsubproject not possible
        // to be deleted
        Node subInheritingConfig = session.getNode("/hst:hst/hst:configurations").addNode("subinheritingconfig", NODETYPE_HST_CONFIGURATION);
        subInheritingConfig.setProperty(GENERAL_PROPERTY_INHERITS_FROM, new String[]{"../unittestsubproject"});
        session.save();

        try {
            final RequestResponseMock requestResponse = mockGetRequestResponse(
                    "http", "localhost", "/_rp/cafebabe-cafe-babe-cafe-babecafebabe./channels/unittestsubproject", null, "DELETE");

            SimpleCredentials admin = new SimpleCredentials("admin", "admin".toCharArray());

            // pre assertions
            assertTrue(session.nodeExists("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root/subsite"));
            assertTrue(session.nodeExists("/hst:hst/hst:hosts/testgroup/test/unit/sub/hst:root"));
            assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestsubproject"));
            assertTrue(session.nodeExists("/hst:hst/hst:configurations/subinheritingconfig"));
            assertTrue(session.nodeExists("/hst:hst/hst:sites/unittestsubproject"));

            final String mountId = session.getNode("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root/subsite").getIdentifier();

            final MockHttpServletResponse response = render(mountId, requestResponse, admin);
            assertEquals(SC_OK, response.getStatus());

            assertFalse(session.nodeExists("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root/subsite"));
            assertFalse(session.nodeExists("/hst:hst/hst:hosts/testgroup/test/unit/sub/hst:root"));
            assertFalse(session.nodeExists("/hst:hst/hst:sites/unittestsubproject"));

            assertTrue("unittestsubproject config should not had been removed because of inheritance by subinheritingconfig", session.nodeExists("/hst:hst/hst:configurations/unittestsubproject"));
            assertTrue(session.nodeExists("/hst:hst/hst:configurations/subinheritingconfig"));
        } finally {
            session.logout();
        }
    }

    @Test
    public void remove_channel_with_right_role_and_channel_deletable_but_configuration_locked_wont_succeed_even_if_rendering_mount_is_not_yet_set() throws Exception {

        // org.hippoecm.hst.pagecomposer.jaxrs.cxf.HstConfigLockedCheckInvokerPreprocessor.preprocoess() will directly
        // return null because pageComposerContextService.isRenderingMountSet() returns false. But still a locked config
        // should not be possible to be removed

        Session session = createSession("admin", "admin");
        Node configuration = session.getNode("/hst:hst/hst:configurations/unittestsubproject");
        configuration.getNode("hst:channel").setProperty(CHANNEL_PROPERTY_DELETABLE, true);
        configuration.setProperty(CONFIGURATION_PROPERTY_LOCKED, true);
        session.save();

        try {
            final RequestResponseMock requestResponse = mockGetRequestResponse(
                    "http", "localhost", "/_rp/cafebabe-cafe-babe-cafe-babecafebabe./channels/unittestsubproject", null, "DELETE");

            SimpleCredentials admin = new SimpleCredentials("admin", "admin".toCharArray());

            // pre assertions
            assertTrue(session.nodeExists("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root/subsite"));
            assertTrue(session.nodeExists("/hst:hst/hst:hosts/testgroup/test/unit/sub/hst:root"));
            assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestsubproject"));
            assertTrue(session.nodeExists("/hst:hst/hst:sites/unittestsubproject"));


            final MockHttpServletResponse response = render(null, requestResponse, admin);
            assertEquals(SC_INTERNAL_SERVER_ERROR, response.getStatus());

            final String mountId = session.getNode("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root/subsite").getIdentifier();
            final RequestResponseMock requestResponse2 = mockGetRequestResponse(
                    "http", "localhost", "/_rp/cafebabe-cafe-babe-cafe-babecafebabe./channels/unittestsubproject", null, "DELETE");

            final MockHttpServletResponse response2 = render(mountId, requestResponse2, admin);
            assertEquals(SC_FORBIDDEN, response2.getStatus());

        } finally {
            session.logout();
        }
    }

    @Test
    public void set_or_clear_variant_succeeds_for_locked_configuration() throws Exception {

        Session session = createSession("admin", "admin");
        Node configuration = session.getNode("/hst:hst/hst:configurations/unittestsubproject");
        configuration.setProperty(CONFIGURATION_PROPERTY_LOCKED, true);
        session.save();

        try {
            {
                final RequestResponseMock requestResponse = mockGetRequestResponse(
                        "http", "localhost", "/_rp/cafebabe-cafe-babe-cafe-babecafebabe./setvariant/foo", null, "POST");

                SimpleCredentials admin = new SimpleCredentials("admin", "admin".toCharArray());
                final MockHttpServletResponse response = render(null, requestResponse, admin);
                assertEquals("Even for locked configuration setvariant POST should be allowed", SC_OK, response.getStatus());


                Object renderVariant = requestResponse.getCmsSessionContext().getContextPayload().get(ContainerConstants.RENDER_VARIANT);
                assertNotNull(renderVariant);
                assertEquals("foo", renderVariant);
            }
            {
                final RequestResponseMock requestResponse = mockGetRequestResponse(
                        "http", "localhost", "/_rp/cafebabe-cafe-babe-cafe-babecafebabe./clearvariant", null, "POST");

                SimpleCredentials admin = new SimpleCredentials("admin", "admin".toCharArray());

                final String mountId = session.getNode("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root").getIdentifier();

                final MockHttpServletResponse response = render(mountId, requestResponse, admin);
                assertEquals("Even for locked configuration clearvariant POST should be allowed", SC_OK, response.getStatus());
                assertNull(requestResponse.getRequest().getSession().getAttribute(ContainerConstants.RENDER_VARIANT));
            }
        } finally {
            session.logout();
        }
    }

    @Test
    public void do_not_get_channel_as_admin_for_undefined_runtime_host() throws Exception {
        final RequestResponseMock requestResponse = mockGetRequestResponse("http", "site-eng.example.org",
                "/_rp/cafebabe-cafe-babe-cafe-babecafebabe./channels/unittestproject", null, "GET");

        SimpleCredentials admin = new SimpleCredentials("admin", "admin".toCharArray());
        final MockHttpServletResponse response = render(null, requestResponse, admin);
        assertEquals(response.getContentAsString(), "");
    }

}