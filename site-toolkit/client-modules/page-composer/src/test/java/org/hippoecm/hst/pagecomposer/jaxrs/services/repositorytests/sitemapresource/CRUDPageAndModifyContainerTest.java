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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.Response;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.internal.CanonicalInfo;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.cxf.CXFJaxrsHstConfigService;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ExtResponseRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.ContainerComponentResource;
import org.hippoecm.hst.pagecomposer.jaxrs.services.SiteMapResource;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.ContainerHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.LockHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.PagesHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.SiteMapHelper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * This test class is to confirm: <p> When a new page is created from a prototype, it is locked. Container items can be
 * added / removed by the lock holder, but not by others </p> <p> When an existing page has containers modified, then
 * the lock holder of this container can still delete the page. Others than the lock holder can only modify other
 * containers, but cannot further modify the page or remove it </p>
 */
public class CRUDPageAndModifyContainerTest extends AbstractSiteMapResourceTest {

    private final LockHelper helper = new LockHelper();

    private void initContext() throws Exception {
        // call below will init request context
        getSiteMapItemRepresentation(session, "home");
    }

    @Test
    public void test_create_page_and_modify_container() throws Exception {
        // add catalog item first
        Node catalogItem = addDefaultCatalogItem();
        initContext();
        final SiteMapItemRepresentation newFoo = createSiteMapItemRepresentation("foo", getPrototypePageUUID());
        final SiteMapResource siteMapResource = createResource();
        siteMapResource.create(newFoo);

        String newPageNodeName = "foo-" + session.getNodeByIdentifier(getPrototypePageUUID()).getName();
        final Node newPage = session.getNode(getPreviewConfigurationWorkspacePagesPath() + "/" + newPageNodeName);
        final Node container1 = newPage.getNode("main/container1");
        final Node container2 = newPage.getNode("main/container2");

        // assert the containers in a newly created page are locked and have a versionStamp
        assertEquals("admin", container1.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).getString());
        assertEquals("admin", container2.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).getString());

        // assert the containers have a version stamp equal to the version stamp of the newly created page
        long pageVersionStamp = newPage.getProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED).getDate().getTimeInMillis();
        assertTrue(pageVersionStamp > 0);
        long container1VersionStamp = newPage.getProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED).getDate().getTimeInMillis();
        long container2VersionStamp = newPage.getProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED).getDate().getTimeInMillis();

        assertEquals(pageVersionStamp, container1VersionStamp);
        assertEquals(pageVersionStamp, container2VersionStamp);

        // override the config identifier to now set the container from the prototype as REQUEST_CONFIG_NODE_IDENTIFIER
        final HstRequestContext ctx = mountResource.getPageComposerContextService().getRequestContext();
        ctx.setAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER, container1.getIdentifier());

        // add a container item
        final ContainerComponentResource containerResource = createContainerResource();
        final long versionStamp = container1.getProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED).getDate().getTimeInMillis();

        final Response response = containerResource.createContainerItem(catalogItem.getIdentifier(),
                versionStamp);
        assertEquals(((ExtResponseRepresentation) response.getEntity()).getMessage(),
                Response.Status.OK.getStatusCode(), response.getStatus());
        ContainerItemRepresentation containerItemRepresentation = (ContainerItemRepresentation) ((ExtResponseRepresentation) response.getEntity()).getData();
        assertEquals(getPreviewConfigurationWorkspacePagesPath() + "/" + newPageNodeName + "/main/container1/catalog-item",
                containerItemRepresentation.getPath());

        // assert there is now an explicit lock on the container of the new container item : possible because 'page' is
        // locked by same session
        Node containerNode = session.getNode(containerItemRepresentation.getPath()).getParent();
        assertTrue("Container should still be explicitly locked.",
                containerNode.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY));

        mountResource.publish();

        // assert all locks are gone
        assertFalse("Container should not locked any more after publication.",
                containerNode.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY));
        assertFalse("Page should not locked any more after publication.",
                newPage.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY));


        final Node newLivePage = session.getNode(getLiveConfigurationWorkspacePagesPath() + "/" + newPageNodeName);

        assertFalse("Live page should not locked.",
                newLivePage.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY));

        final Node newLiveContainerNode = session.getNode(containerNode.getPath().replace("-preview/", "/"));

        assertFalse("Live container should not locked.",
                newLiveContainerNode.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY));

    }

    @Test
    public void test_create_page_and_modify_container_succeeds_incorrect_versionStamp_when_user_contains_lock_already() throws Exception {
        // add catalog item first
        Node catalogItem = addDefaultCatalogItem();
        initContext();
        final SiteMapItemRepresentation newFoo = createSiteMapItemRepresentation("foo", getPrototypePageUUID());
        final SiteMapResource siteMapResource = createResource();
        siteMapResource.create(newFoo);

        String newPageNodeName = "foo-" + session.getNodeByIdentifier(getPrototypePageUUID()).getName();
        final Node newPage = session.getNode(getPreviewConfigurationWorkspacePagesPath() + "/" + newPageNodeName);
        final Node container = newPage.getNode("main/container1");
        // override the config identifier to now set the container from the prototype as REQUEST_CONFIG_NODE_IDENTIFIER
        final HstRequestContext ctx = mountResource.getPageComposerContextService().getRequestContext();
        ctx.setAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER, container.getIdentifier());

        // add a container item
        final ContainerComponentResource containerResource = createContainerResource();
        final long incorrectVersionStamp = 123;

        final Response response = containerResource.createContainerItem(catalogItem.getIdentifier(),
                incorrectVersionStamp);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void test_create_page_and_modify_container_fails_incorrect_versionStamp_when_user_no_lock_yet() throws Exception {
        // add catalog item first
        Node catalogItem = addDefaultCatalogItem();
        initContext();
        final SiteMapItemRepresentation newFoo = createSiteMapItemRepresentation("foo", getPrototypePageUUID());
        final SiteMapResource siteMapResource = createResource();
        siteMapResource.create(newFoo);

        String newPageNodeName = "foo-" + session.getNodeByIdentifier(getPrototypePageUUID()).getName();
        final Node newPage = session.getNode(getPreviewConfigurationWorkspacePagesPath() + "/" + newPageNodeName);

        // publish removes the locks
        mountResource.publish();

        final Node container = newPage.getNode("main/container1");
        // override the config identifier to now set the container from the prototype as REQUEST_CONFIG_NODE_IDENTIFIER
        final HstRequestContext ctx = mountResource.getPageComposerContextService().getRequestContext();
        ctx.setAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER, container.getIdentifier());

        // add a container item
        final ContainerComponentResource containerResource = createContainerResource();
        final long incorrectVersionStamp = 123;

        final Response response = containerResource.createContainerItem(catalogItem.getIdentifier(),
                incorrectVersionStamp);

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        assertTrue(((ExtResponseRepresentation) response.getEntity()).getMessage().contains("has been modified"));
    }

    @Test
    public void test_after_create_page_other_users_cannot_modify_container() throws Exception {
        // add catalog item first
        Node catalogItem = addDefaultCatalogItem();
        initContext();
        final SiteMapItemRepresentation newFoo = createSiteMapItemRepresentation("foo", getPrototypePageUUID());
        final SiteMapResource siteMapResource = createResource();
        siteMapResource.create(newFoo);

        String newPageNodeName = "foo-" + session.getNodeByIdentifier(getPrototypePageUUID()).getName();
        final Node newPage = session.getNode(getPreviewConfigurationWorkspacePagesPath() + "/" + newPageNodeName);
        String newPageID = newPage.getIdentifier();

        // ASSERT NEW PAGE LOCKED FOR BOB
        final Session bob = createSession("bob", "bob");
        // set context on bob's session
        getSiteMapItemRepresentation(bob, "foo");
        final Node newPageByBob = bob.getNodeByIdentifier(newPageID);
        final Node containerByBob = newPageByBob.getNode("main/container1");
        // override the config identifier to now set the container from the prototype as REQUEST_CONFIG_NODE_IDENTIFIER
        final HstRequestContext ctx = mountResource.getPageComposerContextService().getRequestContext();
        ctx.setAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER, containerByBob.getIdentifier());

        // add a container item
        final ContainerComponentResource containerResource = createContainerResource();
        final Response response = containerResource.createContainerItem(catalogItem.getIdentifier(), 0);
        assertEquals(((ExtResponseRepresentation) response.getEntity()).getMessage(),
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        assertTrue(((ExtResponseRepresentation) response.getEntity()).getMessage().contains("cannot be locked"));

        bob.logout();
    }


    private Node addDefaultCatalogItem() throws RepositoryException {
        Node defaultCatalog = session.getNode("/hst:hst/hst:configurations/hst:default/hst:catalog");
        final Node catalogPackage = defaultCatalog.addNode("package", "hst:containeritempackage");
        final Node catalogItem = catalogPackage.addNode("catalog-item", HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT);
        catalogItem.setProperty(HstNodeTypes.COMPONENT_PROPERTY_XTYPE, "HST.Item");
        session.save();
        return catalogItem;
    }

    protected ContainerComponentResource createContainerResource() {
        final ContainerComponentResource containerComponentResource = new ContainerComponentResource();
        containerComponentResource.setPageComposerContextService(mountResource.getPageComposerContextService());
        ContainerHelper helper = new ContainerHelper();
        helper.setPageComposerContextService(mountResource.getPageComposerContextService());
        containerComponentResource.setContainerHelper(helper);
        return containerComponentResource;
    }
}
