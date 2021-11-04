/*
 *  Copyright 2017-2019 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.Session;

import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.hst.Channel;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.hippoecm.hst.configuration.HstNodeTypes.CONFIGURATION_PROPERTY_LOCKED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ConfigurationLockedTest extends MountResourceTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        final Session session = createSession("admin", "admin");
        // lock the unittestproject
        session.getNode("/hst:hst/hst:configurations/unittestproject").setProperty(CONFIGURATION_PROPERTY_LOCKED, true);
        session.save();
        session.logout();
        // give time for jcr events to evict model
        Thread.sleep(100);
    }

    @Test
    public void assert_unittestproject_channel_locked_setup() throws Exception {
        HstManager manager = siteComponentManager.getComponent(HstManager.class);
        Channel unittestproject = manager.getVirtualHosts().getChannels("dev-localhost").get("unittestproject");
        assertTrue(unittestproject.isConfigurationLocked());
    }

    @Test
    public void unittestproject_channel_unlocked() throws Exception {
        final Session session = createSession("admin", "admin");
        // lock the unittestproject
        session.getNode("/hst:hst/hst:configurations/unittestproject").setProperty(CONFIGURATION_PROPERTY_LOCKED, false);
        session.save();
        session.logout();
        // give time for jcr events to evict model
        Thread.sleep(100);
        HstManager manager = siteComponentManager.getComponent(HstManager.class);
        Channel unittestproject = manager.getVirtualHosts().getChannels("dev-localhost").get("unittestproject");
        assertFalse(unittestproject.isConfigurationLocked());
    }

    @Test
    @Override
    public void start_edit_creating_preview_config_as_admin() throws Exception {
        try {
            super.start_edit_creating_preview_config_as_admin();
        } catch (ForbiddenException e) {
            fail("Expected that 'start edit' is allowed *even* when live is locked because start edit only creates the " +
                    "(locked) preview");
        }
        Session session = createSession("admin", "admin");
        try {
            assertTrue(session.getNode("/hst:hst/hst:configurations/unittestproject-preview").getProperty(CONFIGURATION_PROPERTY_LOCKED).getBoolean());
        } finally {
            session.logout();
        }
    }

    private void forbiddenAssertions(final ForbiddenException e) throws java.io.IOException {
        final Map<String, Object> responseMap = getResponseMap(e);

        assertEquals(Boolean.FALSE, responseMap.get("success"));
        assertEquals(ClientError.FORBIDDEN.name(), responseMap.get("errorCode"));
        assertEquals("POST operation is forbidden when channel has its configuration locked.", responseMap.get("message"));
    }

    private Map<String, Object> getResponseMap(final ForbiddenException e) throws java.io.IOException {
        MockHttpServletResponse response = e.getResponse();
        final String restResponse = response.getContentAsString();
        return mapper.readerFor(Map.class).readValue(restResponse);
    }

    @Test
    @Override
    public void start_edit_creating_preview_config_as_webmaster() throws Exception {
        try {
            super.start_edit_creating_preview_config_as_webmaster();
        } catch (ForbiddenException e) {
            fail("Expected that 'start edit' is allowed *even* when live is locked because start edit only creates the " +
                    "(locked) preview");
        }
        Session session = createSession("admin", "admin");
        try {
            assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestproject-preview"));
        } finally {
            session.logout();
        }
    }

    @Test
    @Override
    public void liveuser_cannot_invoke_start_edit() throws Exception {
        try {
            super.liveuser_cannot_invoke_start_edit();
            fail("liveuser should not be allowed to start edit");
        } catch (ForbiddenException e) {
            // ignore
        }
        Session session = createSession("admin", "admin");
        try {
            assertFalse(session.nodeExists("/hst:hst/hst:configurations/unittestproject-preview"));
        } finally {
            session.logout();
        }
    }

    @Test
    @Override
    public void copy_a_page_as_admin() throws Exception {
        try {
            super.copy_a_page_as_admin();
            fail("Expected forbidden");
        } catch (ForbiddenException e) {
            forbiddenAssertions(e);
        }
    }

    @Test
    @Override
    public void copy_a_page_as_editor() throws Exception {
        try {
            super.copy_a_page_as_editor();
            fail("Expected forbidden");
        } catch (ForbiddenException e) {
            forbiddenAssertions(e);
        }
    }

    @Test
    @Override
    public void liveuser_cannot_copy_page() throws Exception {
        try {
            super.liveuser_cannot_copy_page();
            fail("Expected forbidden");
        } catch (ForbiddenException e) {
            final Map<String, Object> responseMap = getResponseMap(e);
            assertEquals(ClientError.FORBIDDEN.name(), responseMap.get("errorCode"));
            assertEquals("Method 'copy' is not allowed to be invoked since current user does not have the right privileges", responseMap.get("message"));
        }
    }

    @Test
    @Override
    public void publish_as_admin_changes_of_admin() throws Exception {
        try {
            super.publish_as_admin_changes_of_admin();
            fail("Expected forbidden");
        } catch (ForbiddenException e) {
            forbiddenAssertions(e);
        }
    }

    @Test
    @Override
    public void publish_as_editor_changes_of_editor() throws Exception {
        try {
            super.publish_as_editor_changes_of_editor();
            fail("Expected forbidden");
        } catch (ForbiddenException e) {
            forbiddenAssertions(e);
        }
    }

    @Test
    @Override
    public void discard_as_admin_changes_of_admin() throws Exception {
        try {
            super.discard_as_admin_changes_of_admin();
            fail("Expected forbidden");
        } catch (ForbiddenException e) {
            forbiddenAssertions(e);
        }
    }

    @Test
    @Override
    public void discard_as_editor_changes_of_editor() throws Exception {
        try {
            super.discard_as_editor_changes_of_editor();
            fail("Expected forbidden");
        } catch (ForbiddenException e) {
            forbiddenAssertions(e);
        }
    }

    @Test
    @Override
    public void publish_userswithchanges_as_admin_succeeds() throws Exception {
        try {
            super.publish_userswithchanges_as_admin_succeeds();
            fail("Expected forbidden");
        } catch (ForbiddenException e) {
            forbiddenAssertions(e);
        }
    }

    @Test
    @Override
    public void publish_userswithchanges_as_editor_fails() throws Exception {
        try {
            super.publish_userswithchanges_as_editor_fails();
            fail("Expected forbidden");
        } catch (ForbiddenException e) {
            forbiddenAssertions(e);
        }
    }

}
