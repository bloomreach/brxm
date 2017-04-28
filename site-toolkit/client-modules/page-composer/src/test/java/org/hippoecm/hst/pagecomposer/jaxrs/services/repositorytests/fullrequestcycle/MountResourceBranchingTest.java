/*
 *  Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.pagecomposer.jaxrs.AbstractFullRequestCycleTest;
import org.hippoecm.hst.pagecomposer.jaxrs.AbstractPageComposerTest;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ExtIdsRepresentation;
import org.hippoecm.hst.site.HstServices;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static java.util.Collections.singletonList;
import static org.hippoecm.hst.configuration.HstNodeTypes.BRANCH_PROPERTY_BRANCHOF;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_INHERITS_FROM;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY;
import static org.hippoecm.hst.configuration.HstNodeTypes.MIXINTYPE_HST_BRANCH;
import static org.hippoecm.repository.util.JcrUtils.getMultipleStringProperty;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MountResourceBranchingTest extends MountResourceTest {

    private static final SimpleCredentials ADMIN_CREDENTIALS = new SimpleCredentials("admin", "admin".toCharArray());
    private final SimpleCredentials EDITOR_CREDENTIALS = new SimpleCredentials("editor", "editor".toCharArray());

    @Test
    public void create_branch_as_admin() throws Exception {
        createBranchAssertions(ADMIN_CREDENTIALS, "foo");
    }

    @Test
    public void create_branch_as_webmaster() throws Exception {
        createBranchAssertions(EDITOR_CREDENTIALS, "foo");
    }

    @Test
    public void branch_is_always_created_from_live_and_not_from_preview() throws Exception {
        // make sure to trigger some changes in preview of unittestproject before creating a branch
        copyPage(ADMIN_CREDENTIALS);
        Session session = createSession("admin", "admin");
        assertFalse(session.nodeExists("/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:sitemap/home-copy"));
        assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:sitemap/home-copy"));

        createBranch(ADMIN_CREDENTIALS, "foo");

        // now we expect the branch *not* to contain 'home-copy' because that page was not yet in live
        assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestproject-foo/hst:workspace/hst:sitemap"));
        assertFalse(session.nodeExists("/hst:hst/hst:configurations/unittestproject-foo/hst:workspace/hst:sitemap/home-copy"));

        // now remove the created branch, publish the preview page, and create the branch again: Then we expect the
        // home-copy to be there
        session.getNode("/hst:hst/hst:configurations/unittestproject-foo").remove();
        session.save();
        publish(ADMIN_CREDENTIALS);
        assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:sitemap/home-copy"));
        assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:sitemap/home-copy"));

        createBranch(ADMIN_CREDENTIALS, "foo");
        // now we expect the branch to contain 'home-copy' because that page was in live
        assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestproject-foo/hst:workspace/hst:sitemap/home-copy"));

        session.logout();
    }

    @Test
    public void creating_an_existing_branch_not_allowed() throws Exception {
        createBranch(ADMIN_CREDENTIALS, "foo");
        final Map<String, Object> responseMap = createBranch(ADMIN_CREDENTIALS, "foo");

        assertEquals(Boolean.FALSE, responseMap.get("success"));
        assertEquals("ITEM_EXISTS", responseMap.get("errorCode"));
    }

    private Map<String, Object> createBranch(final Credentials creds, final String branchName) throws RepositoryException, IOException, ServletException {
        final String mountId = getNodeId("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");
        final RequestResponseMock requestResponse = mockGetRequestResponse(
                "http", "localhost", "/_rp/"+ mountId + "./createbranch/"+ branchName, null, "PUT");

        final MockHttpServletResponse response = render(mountId, requestResponse, creds);
        final String restResponse = response.getContentAsString();
        return mapper.reader(Map.class).readValue(restResponse);
    }

    private void createBranchAssertions(final Credentials creds, final String branchName) throws RepositoryException, IOException, ServletException {
        final Map<String, Object> responseMap = createBranch(creds, branchName);
        assertEquals(Boolean.TRUE, responseMap.get("success"));
        assertEquals("Branch created successfully", responseMap.get("message"));

        Session session = createSession("admin", "admin");
        assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestproject/hst:workspace"));
        assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:sitemap"));
        assertFalse(session.nodeExists("/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages"));

        // the created branch *does* have pages as well in the workspace because always automatically added to a branch

        assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestproject-"+branchName+"/hst:workspace"));
        assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestproject-"+branchName+"/hst:workspace/hst:sitemap"));
        assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestproject-"+branchName+"/hst:workspace/hst:pages"));

        Node branchHstConfigNode = session.getNode("/hst:hst/hst:configurations/unittestproject-" + branchName);
        assertArrayEquals(new String[]{"../unittestproject"}, getMultipleStringProperty(branchHstConfigNode, GENERAL_PROPERTY_INHERITS_FROM, null));
        assertTrue(branchHstConfigNode.isNodeType(MIXINTYPE_HST_BRANCH));


        assertEquals("unittestproject", branchHstConfigNode.getProperty(BRANCH_PROPERTY_BRANCHOF).getString());

        session.logout();
    }

    @Test
    public void branch_channels_are_present_in_channels_overview() throws Exception {
        createBranch(ADMIN_CREDENTIALS, "foo");
        Thread.sleep(100);
        final HstManager hstManager = HstServices.getComponentManager().getComponent(HstManager.class.getName());
        VirtualHosts virtualHosts = hstManager.getVirtualHosts();

        Map<String, Channel> channels = virtualHosts.getChannels("dev-localhost");
        assertTrue("Channel for branch 'foo' was expected to be there",  channels.containsKey("unittestproject-foo"));
        Channel branch = channels.get("unittestproject-foo");
        assertFalse("Since 'unittestproject' does not have channel node in workspace, it should not be editable in " +
                "the branch either",branch.isChannelSettingsEditable());

        assertEquals("Channel node should be inherited and not cloned since not in workspace",
                "/hst:hst/hst:configurations/unittestproject/hst:channel", branch.getChannelPath());
        assertEquals("/hst:hst/hst:configurations/unittestproject-foo", branch.getHstConfigPath());
    }

    @Test
    public void branch_when_channel_is_in_workspace() throws Exception {
        Session session = createSession("admin", "admin");
        session.move("/hst:hst/hst:configurations/unittestproject/hst:channel",
                "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:channel");
        session.save();
        session.logout();
        createBranch(ADMIN_CREDENTIALS, "foo");
        Thread.sleep(100);
        final HstManager hstManager = HstServices.getComponentManager().getComponent(HstManager.class.getName());
        VirtualHosts virtualHosts = hstManager.getVirtualHosts();Map<String, Channel> channels = virtualHosts.getChannels("dev-localhost");
        Channel branch = channels.get("unittestproject-foo");
        assertTrue("Since 'unittestproject' does have channel node in workspace, it should be editable in " +
                "the branch as well", branch.isChannelSettingsEditable());
    }

    // TODO assertions when creating a preview from the branch (and when creating preview from master)

//    @Test
//    public void branch_when_channel_is_in_workspace() throws Exception {
//        Session session = createSession("admin", "admin");
//        session.move("/hst:hst/hst:configurations/unittestproject/hst:channel",
//                "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:channel");
//        session.save();
//        session.logout();
//        createBranch(ADMIN_CREDENTIALS, "foo");
//        Thread.sleep(100);
//        final HstManager hstManager = HstServices.getComponentManager().getComponent(HstManager.class.getName());
//        VirtualHosts virtualHosts = hstManager.getVirtualHosts();Map<String, Channel> channels = virtualHosts.getChannels("dev-localhost");
//        Channel branch = channels.get("unittestproject-foo");
//        assertTrue("Since 'unittestproject' does have channel node in workspace, it should be editable in " +
//                "the branch as well", branch.isChannelSettingsEditable());
//    }
}
