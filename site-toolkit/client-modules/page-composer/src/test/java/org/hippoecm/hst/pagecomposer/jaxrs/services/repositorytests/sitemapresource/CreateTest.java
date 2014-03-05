/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.services.repositorytests.sitemapresource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.Response;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ExtResponseRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.SiteMapResource;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.LockHelper;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CreateTest extends AbstractSiteMapResourceTest {

    private final LockHelper helper = new LockHelper();

    private void initContext() throws Exception {
        // call below will init request context
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");
    }

    @Test
    public void test_create() throws Exception {
        initContext();
        final SiteMapItemRepresentation newFoo = new SiteMapItemRepresentation();
        newFoo.setName("foo");
        final String singleRowPrototypePageUUID = getSingleRowPrototypePageUUID();
        newFoo.setComponentConfigurationId(singleRowPrototypePageUUID);
        newFoo.setRelativeContentPath("relFoo");
        Map<String, String> params = new HashMap<>();
        params.put("lux", "qux");
        newFoo.setLocalParameters(params);
        final SiteMapResource siteMapResource = createResource();
        final Response response = siteMapResource.create(newFoo);
        assertEquals(((ExtResponseRepresentation) response.getEntity()).getMessage(),
                Response.Status.OK.getStatusCode(), response.getStatus());

        String newId = (String) ((ExtResponseRepresentation) response.getEntity()).getData();

        final Node newSitemapItemNode = session.getNodeByIdentifier(newId);
        assertEquals("foo", newSitemapItemNode.getName());

        String newPageNodeName = "foo-" + session.getNodeByIdentifier(singleRowPrototypePageUUID).getName();

        assertEquals("hst:pages/"+newPageNodeName,
                newSitemapItemNode.getProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID).getString());

        assertTrue(session.nodeExists(getPreviewConfigurationWorkspacePagesPath() + "/" + newPageNodeName));

        // ASSERT NEW NODES ARE NOT LOCKED FOR CURRENT SESSION
        try {
            helper.acquireLock(newSitemapItemNode);
        } catch (ClientException e) {
            fail("Session '"+session.getUserID()+"' should contain the lock");
        }
        try {
            helper.acquireLock(session.getNode(getPreviewConfigurationWorkspacePagesPath() + "/" + newPageNodeName));
        } catch (ClientException e) {
            fail("Session '"+session.getUserID()+"' should contain the lock");
        }

        // ASSERT NEW NODES ARE LOCKED FOR BOB
        final Session bob = createSession("bob", "bob");
        Node newNodeByBob = bob.getNodeByIdentifier(newId);
        //  acquiring should fail now
        try {
            helper.acquireLock(newNodeByBob);
            fail("Expected an ClientException when trying to acquire lock");
        } catch (ClientException e) {
            assertTrue(e.getMessage().contains("cannot be locked"));
        }

        Node newPageNodeByBob = bob.getNode(getPreviewConfigurationWorkspacePagesPath() + "/" + newPageNodeName);
        //  acquiring should fail now
        try {
            helper.acquireLock(newPageNodeByBob);
            fail("Expected an ClientException when trying to acquire lock");
        } catch (ClientException e) {
            assertTrue(e.getMessage().contains("cannot be locked"));
        }

        bob.logout();

    }

    @Test
    public void test_create_succeeds_when_workspace_missing() throws Exception {
        // TODO
    }

    @Test
    public void test_create_succeeds_when_workspace_pages_missing() throws Exception {
        // TODO
    }

    @Test
    public void test_create_succeeds_when_workspace_sitemap_missing() throws Exception {
        // TODO
    }

    @Test
    public void test_create_fails_no_name() throws Exception {
        initContext();
        final SiteMapItemRepresentation newFoo = new SiteMapItemRepresentation();
        final SiteMapResource siteMapResource = createResource();
        final Response response = siteMapResource.create(newFoo);
        assertEquals(((ExtResponseRepresentation) response.getEntity()).getMessage(),
                Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void test_create_fails_non_existing_prototype_id() throws Exception {
        initContext();
        final SiteMapItemRepresentation newFoo = new SiteMapItemRepresentation();
        newFoo.setName("foo");
        newFoo.setComponentConfigurationId(UUID.randomUUID().toString());

        final SiteMapResource siteMapResource = createResource();
        final Response response = siteMapResource.create(newFoo);

        assertEquals(((ExtResponseRepresentation) response.getEntity()).getMessage(),
                Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }


    @Test
    public void test_create_fails_if_component_not_valid_id() throws Exception {
        initContext();
        final SiteMapItemRepresentation newFoo = new SiteMapItemRepresentation();
        newFoo.setName("foo");
        newFoo.setComponentConfigurationId("invalid-uuid");
        final SiteMapResource siteMapResource = createResource();
        final Response response = siteMapResource.create(newFoo);
        assertEquals(((ExtResponseRepresentation) response.getEntity()).getMessage(),
                Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }


    @Test
    public void test_create_fails_if_component_id_not_of_prototype_page() throws Exception {
        initContext();
        final SiteMapItemRepresentation newFoo = new SiteMapItemRepresentation();
        newFoo.setName("foo");
        // home page is not allowed as prototype
        newFoo.setComponentConfigurationId(getHomePageUUID());
        final SiteMapResource siteMapResource = createResource();
        final Response response = siteMapResource.create(newFoo);
        assertEquals(((ExtResponseRepresentation) response.getEntity()).getMessage(),
                Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }


    @Test
    public void test_create_fails_if_component_id_is_prototype_page_of_inherited_config() throws Exception {
        String prototypePath = "/hst:hst/hst:configurations/unittestproject/hst:prototypepages/singlerow-page";
        String commonConfigPrototypePath = "/hst:hst/hst:configurations/unittestcommon/hst:prototypepages/singlerow-page";
        session.move(prototypePath, commonConfigPrototypePath);
        initContext();
        final SiteMapItemRepresentation newFoo = new SiteMapItemRepresentation();
        newFoo.setName("foo");
        newFoo.setComponentConfigurationId(session.getNode(commonConfigPrototypePath).getIdentifier());
        final SiteMapResource siteMapResource = createResource();
        final Response response = siteMapResource.create(newFoo);
        assertEquals(((ExtResponseRepresentation) response.getEntity()).getMessage(),
                Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }



    @Test
    public void test_create_two_sitemap_items_from_same_prototype_page() throws Exception {
        initContext();
        final SiteMapItemRepresentation newFoo = new SiteMapItemRepresentation();
        final SiteMapItemRepresentation newBar = new SiteMapItemRepresentation();
        newFoo.setName("foo");
        newBar.setName("bar");
        final String singleRowPrototypePageUUID = getSingleRowPrototypePageUUID();
        newFoo.setComponentConfigurationId(singleRowPrototypePageUUID);
        newBar.setComponentConfigurationId(singleRowPrototypePageUUID);

        final SiteMapResource siteMapResource = createResource();
        siteMapResource.create(newFoo);
        siteMapResource.create(newBar);

        String newFooPageNodeName = "foo-" + session.getNodeByIdentifier(singleRowPrototypePageUUID).getName();
        String newBarPageNodeName = "bar-" + session.getNodeByIdentifier(singleRowPrototypePageUUID).getName();
        assertTrue(session.nodeExists(getPreviewConfigurationWorkspacePagesPath() + "/" + newFooPageNodeName));
        assertTrue(session.nodeExists(getPreviewConfigurationWorkspacePagesPath() + "/" + newBarPageNodeName));
    }

    @Test
    public void test_create_sitemap_creates_page_name_with_postfix() throws Exception {
        // the 'foo' sitemap item below with in the end try to create a new page
        // called 'foo-'+prototypePageNodeName . If this node already exists, we expect that a node
        // 'foo-'+prototypePageNodeName + '-1' is created
        initContext();
        final String singleRowPrototypePageUUID = getSingleRowPrototypePageUUID();
        String prototypePageNodeName = session.getNodeByIdentifier(singleRowPrototypePageUUID).getName();
        final Node node = session.getNode("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:pages")
                .addNode("foo-" + prototypePageNodeName, HstNodeTypes.NODETYPE_HST_COMPONENT);
        session.save();

        final SiteMapItemRepresentation newFoo = new SiteMapItemRepresentation();
        newFoo.setName("foo");
        newFoo.setComponentConfigurationId(singleRowPrototypePageUUID);

        final SiteMapResource siteMapResource = createResource();
        final Response response = siteMapResource.create(newFoo);
        String expectedPageName = "foo-"+prototypePageNodeName +"-1";
        assertTrue(session.nodeExists(getPreviewConfigurationWorkspacePagesPath() + "/" + expectedPageName));
        String newSitemapItemId = (String) ((ExtResponseRepresentation) response.getEntity()).getData();
        final Node newSitemapItemNode = session.getNodeByIdentifier(newSitemapItemId);
        assertEquals("hst:pages/"+expectedPageName,
                newSitemapItemNode.getProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID).getString());
    }

    @Test
    public void test_create_sitemap_creates_page_name_until_finds_valid_one() throws Exception {
        // Same as above but now we already have
        // 'foo-'+prototypePageNodeName
        // and
        // 'foo-'+prototypePageNodeName + '-1'
        initContext();
        final String singleRowPrototypePageUUID = getSingleRowPrototypePageUUID();
        String prototypePageNodeName = session.getNodeByIdentifier(singleRowPrototypePageUUID).getName();
        session.getNode("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:pages")
                .addNode("foo-"+prototypePageNodeName, HstNodeTypes.NODETYPE_HST_COMPONENT);

        session.getNode("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:pages")
                .addNode("foo-"+prototypePageNodeName + "-1", HstNodeTypes.NODETYPE_HST_COMPONENT);

        session.save();

        final SiteMapItemRepresentation newFoo = new SiteMapItemRepresentation();
        newFoo.setName("foo");
        newFoo.setComponentConfigurationId(singleRowPrototypePageUUID);

        final SiteMapResource siteMapResource = createResource();
        final Response response = siteMapResource.create(newFoo);
        String expectedPageName = "foo-"+prototypePageNodeName +"-2";
        assertTrue(session.nodeExists(getPreviewConfigurationWorkspacePagesPath() + "/" + expectedPageName));
        String newSitemapItemId = (String) ((ExtResponseRepresentation) response.getEntity()).getData();
        final Node newSitemapItemNode = session.getNodeByIdentifier(newSitemapItemId);
        assertEquals("hst:pages/"+expectedPageName,
                newSitemapItemNode.getProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID).getString());
    }

    @Test
    public void test_delete_new_created() throws Exception {
        initContext();
        final SiteMapItemRepresentation newFoo = new SiteMapItemRepresentation();
        newFoo.setName("foo");
        newFoo.setComponentConfigurationId(getSingleRowPrototypePageUUID());
        final SiteMapResource siteMapResource = createResource();
        siteMapResource.create(newFoo);
        {
            // assert bob cannot move/delete new one
            final Session bob = createSession("bob", "bob");
            final SiteMapItemRepresentation fooByBob = getSiteMapItemRepresentation(bob, "foo");
            fooByBob.setName("bar");
            final Response bobResponse = siteMapResource.update(fooByBob);
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), bobResponse.getStatus());
            assertThat(((ExtResponseRepresentation) bobResponse.getEntity()).getMessage(), is(ClientError.ITEM_ALREADY_LOCKED.name()));

            final Response bobResponseDelete = siteMapResource.delete(fooByBob.getId());
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), bobResponseDelete.getStatus());
            assertThat(((ExtResponseRepresentation) bobResponseDelete.getEntity()).getMessage(), is(ClientError.ITEM_ALREADY_LOCKED.name()));

            bob.logout();
        }
        // assert admin can delete new one and that there is *no* 'deleted' marker left behind
        final SiteMapItemRepresentation fooByAdmin = getSiteMapItemRepresentation(session, "foo");
        String uuid = fooByAdmin.getId();
        final Response delete = siteMapResource.delete(uuid);
        assertEquals(Response.Status.OK.getStatusCode(), delete.getStatus());
        try {
            session.getNodeByIdentifier(uuid);
            fail("Node should be deleted");
        } catch (ItemNotFoundException e) {
            // correct
        }

    }


    @Test
    public void test_rename_created_and_then_delete() throws Exception {
        initContext();
        final SiteMapItemRepresentation newFoo = new SiteMapItemRepresentation();
        newFoo.setName("foo");
        newFoo.setComponentConfigurationId(getSingleRowPrototypePageUUID());
        final SiteMapResource siteMapResource = createResource();
        siteMapResource.create(newFoo);

        final SiteMapItemRepresentation foo = getSiteMapItemRepresentation(session, "foo");

        String pathNewCreated = session.getNodeByIdentifier(foo.getId()).getPath();

        foo.setName("bar");
        final Response renamed = siteMapResource.update(foo);
        assertEquals(Response.Status.OK.getStatusCode(), renamed.getStatus());

        // no deleted marker should be created for new nodes
        assertFalse(session.nodeExists(pathNewCreated));

        final Node fooBar = session.getNodeByIdentifier(foo.getId());
        assertEquals("bar", fooBar.getName());

        final Response delete = siteMapResource.delete(foo.getId());
        assertEquals(Response.Status.OK.getStatusCode(), delete.getStatus());
        try {
            session.getNodeByIdentifier(foo.getId());
            fail("Node should be deleted");
        } catch (ItemNotFoundException e) {
            // correct
        }
    }

    @Test
    public void test_rename_created_to_existing_live_and_then_delete_should_keep_deleted_marker() throws Exception {

        final SiteMapResource siteMapResource = createResource();
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");
        siteMapResource.delete(home.getId());


        final SiteMapItemRepresentation newFoo = new SiteMapItemRepresentation();
        newFoo.setName("foo");
        newFoo.setComponentConfigurationId(getSingleRowPrototypePageUUID());
        siteMapResource.create(newFoo);

        // because of Jackrabbit issue we need a fresh session for tests with this move because
        // JR sometimes throws a incorrect repository exception during SessionMoveOperation
        final Session admin = createSession("admin", "admin");
        final SiteMapItemRepresentation foo = getSiteMapItemRepresentation(admin, "foo");
        foo.setName("home");
        newFoo.setComponentConfigurationId(getSingleRowPrototypePageUUID());
        final Response renamed = siteMapResource.update(foo);
        assertEquals(Response.Status.OK.getStatusCode(), renamed.getStatus());

        final Node fooToHome = admin.getNodeByIdentifier(foo.getId());
        assertEquals("home", fooToHome.getName());

        final Response delete = siteMapResource.delete(foo.getId());
        assertEquals(Response.Status.OK.getStatusCode(), delete.getStatus());
        try {
            final Node fooToHomeNode = admin.getNodeByIdentifier(foo.getId());
            assertEquals("deleted", fooToHomeNode.getProperty(HstNodeTypes.EDITABLE_PROPERTY_STATE).getString());

            final Session bob = createSession("bob", "bob");
            assertNull(getSiteMapItemRepresentation(bob, "home"));
            Node homeNodeByBob = bob.getNodeByIdentifier(foo.getId());
            try {
                helper.acquireLock(homeNodeByBob);
                fail("Expected an IllegalStateException when trying to acquire lock");
            } catch (ClientException e) {
                assertTrue(e.getMessage().contains("cannot be locked"));
            }
        } catch (ItemNotFoundException e) {
            fail("Node should be marked as deleted as exists in live!");
        }

        admin.logout();
    }

    @Test
    public void test_rename_created_and_then_back_again() throws Exception {
        initContext();
        final SiteMapItemRepresentation newFoo = new SiteMapItemRepresentation();
        newFoo.setName("foo");
        newFoo.setComponentConfigurationId(getSingleRowPrototypePageUUID());
        final SiteMapResource siteMapResource = createResource();
        siteMapResource.create(newFoo);

        final SiteMapItemRepresentation foo = getSiteMapItemRepresentation(session, "foo");
        foo.setName("bar");
        final Response renamed = siteMapResource.update(foo);
        assertEquals(Response.Status.OK.getStatusCode(), renamed.getStatus());

        final Node fooBar = session.getNodeByIdentifier(foo.getId());
        assertEquals("bar", fooBar.getName());


        // because of Jackrabbit issue we need a fresh session for tests with this move because
        // JR sometimes throws a incorrect repository exception during SessionMoveOperation
        {
            final Session admin = createSession("admin", "admin");
            final SiteMapItemRepresentation bar = getSiteMapItemRepresentation(admin, "bar");
            bar.setName("foo");
            final Response renamedAgain = siteMapResource.update(bar);
            assertEquals(Response.Status.OK.getStatusCode(), renamedAgain.getStatus());
            final Node barFoo = admin.getNodeByIdentifier(foo.getId());
            assertEquals("foo", barFoo.getName());
            admin.logout();
        }
    }


    @Test
    public void test_create_fails_when_existing_in_workspace() throws Exception {
        initContext();
        final SiteMapItemRepresentation newItem = new SiteMapItemRepresentation();
        newItem.setName("home");
        newItem.setComponentConfigurationId(getSingleRowPrototypePageUUID());
        final SiteMapResource siteMapResource = createResource();
        final Response response = siteMapResource.create(newItem);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertThat(((ExtResponseRepresentation) response.getEntity()).getMessage(), is(ClientError.ITEM_NAME_NOT_UNIQUE.name()));
    }

    @Test
    public void test_create_fails_for_existing_sitemap_item_name() throws Exception {
        initContext();
        final SiteMapItemRepresentation newItem = new SiteMapItemRepresentation();
        newItem.setName("foo");
        newItem.setComponentConfigurationId(getSingleRowPrototypePageUUID());
        final SiteMapResource siteMapResource = createResource();
        final Response response = siteMapResource.create(newItem);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        final SiteMapItemRepresentation next = new SiteMapItemRepresentation();
        next.setName("foo");
        next.setComponentConfigurationId(getSingleRowPrototypePageUUID());
        final Response fail = siteMapResource.create(next);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), fail.getStatus());
        assertThat(((ExtResponseRepresentation) fail.getEntity()).getMessage(), is(ClientError.ITEM_NAME_NOT_UNIQUE.name()));
    }


    @Test
    public void test_create_below_parent() throws Exception {
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");
        final SiteMapItemRepresentation homeChild = new SiteMapItemRepresentation();
        homeChild.setName("homeChild");
        homeChild.setComponentConfigurationId(getSingleRowPrototypePageUUID());
        final SiteMapResource siteMapResource = createResource();
        {
            final Response response = siteMapResource.create(homeChild, home.getId());
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        }
        // assert parent *not* locked and bob can add sibling child.
        // after this, neither bob nor admin are allowed to rename 'home' any more

        {
            // assert bob cannot move/delete new one
            final Session bob = createSession("bob", "bob");
            final SiteMapItemRepresentation homeByBob = getSiteMapItemRepresentation(bob, "home");
            final SiteMapItemRepresentation childByBob = new SiteMapItemRepresentation();
            childByBob.setName("childByBob");
            childByBob.setComponentConfigurationId(getSingleRowPrototypePageUUID());
            final Response bobResponse = siteMapResource.create(childByBob, homeByBob.getId());
            assertEquals(Response.Status.OK.getStatusCode(), bobResponse.getStatus());

            // now try to  rename home. Should not be possible because of locked child by admin
            homeByBob.setName("newName");
            final Response fail = siteMapResource.update(homeByBob);
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), fail.getStatus());
            assertThat(((ExtResponseRepresentation) fail.getEntity()).getMessage(), is(ClientError.ITEM_ALREADY_LOCKED.name()));
            bob.logout();
        }

        {
            // refetch for admin because getSiteMapItemRepresentation sets hst request context also
            final SiteMapItemRepresentation homeAgain = getSiteMapItemRepresentation(session, "home");
            homeAgain.setName("newName");
            final Response fail = siteMapResource.update(homeAgain);
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), fail.getStatus());
            assertThat(((ExtResponseRepresentation) fail.getEntity()).getMessage(), is(ClientError.ITEM_ALREADY_LOCKED.name()));
        }

        // delete the childByBob : after that, admin should be able to rename home
        {
            final Session bob = createSession("bob", "bob");
            final SiteMapItemRepresentation childByBob = getSiteMapItemRepresentation(bob, "home/childByBob");
            siteMapResource.delete(childByBob.getId());
            bob.logout();
        }
        {
            // refetch for admin because getSiteMapItemRepresentation sets hst request context also
            final SiteMapItemRepresentation renameHome = getSiteMapItemRepresentation(session, "home");
            renameHome.setName("newName");
            final Response success = siteMapResource.update(renameHome);
            assertEquals(Response.Status.OK.getStatusCode(), success.getStatus());
        }
    }

    @Test
    public void test_create_below_non_existing_parent() throws Exception {
        initContext();
        final SiteMapItemRepresentation homeChild = new SiteMapItemRepresentation();
        homeChild.setName("homeChild");
        final SiteMapResource siteMapResource = createResource();
        {
            final Response response = siteMapResource.create(homeChild, UUID.randomUUID().toString());
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }
    }


    @Test
    public void test_create_below_invalid_uuid_parent() throws Exception {
        initContext();
        final SiteMapItemRepresentation homeChild = new SiteMapItemRepresentation();
        homeChild.setName("homeChild");
        final SiteMapResource siteMapResource = createResource();
        {
            final Response response = siteMapResource.create(homeChild, "non-uuid");
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void test_create_below_invalid_parent() throws Exception {
        initContext();
        final Node menuNode = session.getNode("/hst:hst/hst:configurations/unittestproject-preview/hst:sitemenus/main");
        String uuidInvalidLocation = menuNode.getIdentifier();
        final SiteMapItemRepresentation homeChild = new SiteMapItemRepresentation();
        homeChild.setName("homeChild");
        final SiteMapResource siteMapResource = createResource();
        {
            final Response response = siteMapResource.create(homeChild, uuidInvalidLocation);
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void test_create_fails_when_parent_locked() throws Exception {
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");
        home.setComponentConfigurationId("foo");
        final SiteMapResource siteMapResource = createResource();
        siteMapResource.update(home);

        {
            // assert bob cannot add below home
            final Session bob = createSession("bob", "bob");
            final SiteMapItemRepresentation homeByBob = getSiteMapItemRepresentation(bob, "home");
            final SiteMapItemRepresentation childByBob = new SiteMapItemRepresentation();
            childByBob.setName("childByBob");
            final Response bobResponse = siteMapResource.create(childByBob, homeByBob.getId());
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), bobResponse.getStatus());
            bob.logout();
        }
    }

    @Test
    public void test_create_fails_when_exists_in_non_workspace_sitemap() throws Exception {
        initContext();
        final SiteMapItemRepresentation newItem = new SiteMapItemRepresentation();
        newItem.setName("about-us");
        newItem.setComponentConfigurationId(getSingleRowPrototypePageUUID());
        final SiteMapResource siteMapResource = createResource();
        final Response response = siteMapResource.create(newItem);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertThat(((ExtResponseRepresentation) response.getEntity()).getMessage(), is(ClientError.ITEM_EXISTS_OUTSIDE_WORKSPACE.name()));
    }

    @Test
    public void test_create_succeeds_when_no_non_workspace_sitemap() throws Exception {
        session.getNode("/hst:hst/hst:configurations/unittestproject/hst:sitemap").remove();
        session.getNode("/hst:hst/hst:configurations/unittestproject-preview/hst:sitemap").remove();
        session.save();
        initContext();
        final SiteMapItemRepresentation newItem = new SiteMapItemRepresentation();
        newItem.setName("about-us");
        newItem.setComponentConfigurationId(getSingleRowPrototypePageUUID());
        final SiteMapResource siteMapResource = createResource();
        final Response response = siteMapResource.create(newItem);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

}
