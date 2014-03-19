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


import java.util.ArrayList;
import java.util.List;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.ws.rs.core.Response;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.cxf.CXFJaxrsHstConfigService;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ExtResponseRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.ContainerComponentResource;
import org.hippoecm.hst.pagecomposer.jaxrs.services.SiteMapResource;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.LockHelper;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import junit.framework.Assert;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ReApplyPrototypesTest  extends AbstractSiteMapResourceTest {

    private class TestContext {
        Node catalogItem;
        String prototypeUUID;
        String secondPrototypeUUID;
        SiteMapResource siteMapResource;
        String initFooPageNodeLocation;
        List<ContainerItemRepresentation> addedPageContainerItems = new ArrayList<>();
    }

    private TestContext initContextAndFixture(final String[] secondPrototype, final String[] relContainerPathsForItem) throws Exception {
        TestContext testContext = new TestContext();
        testContext.catalogItem = addDefaultCatalogItem();
        final Node secondPrototypeNode;
        if (secondPrototype.length == 0) {
            secondPrototypeNode = JcrUtils.copy(session, "/hst:hst/hst:configurations/unittestproject-preview/hst:prototypepages/prototype-page",
                    "/hst:hst/hst:configurations/unittestproject-preview/hst:prototypepages/protoxxx");
        } else {
            // build it
            RepositoryTestCase.build(secondPrototype, session);
            secondPrototypeNode = session.getNode(secondPrototype[0]);
        }

        testContext.secondPrototypeUUID = secondPrototypeNode.getIdentifier();
        session.save();
        Thread.sleep(200);

        getSiteMapItemRepresentation(session, "home");
        testContext.prototypeUUID = getPrototypePageUUID();

        final SiteMapItemRepresentation newFoo = createSiteMapItemRepresentation("foo", testContext.prototypeUUID);
        testContext.siteMapResource = createResource();
        final Response response = testContext.siteMapResource.create(newFoo);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final SiteMapItemRepresentation createdFoo = getSiteMapItemRepresentation(session, "foo");
        assertEquals("foo", createdFoo.getName());

        final String fooPageId = createdFoo.getComponentConfigurationId();
        assertEquals("hst:pages/foo-prototype-page", fooPageId);
        final HstComponentConfiguration fooPage = mountResource.getPageComposerContextService().getEditingPreviewSite()
                .getComponentsConfiguration().getComponentConfiguration(fooPageId);

        assertEquals("foo-prototype-page", fooPage.getName());
        Node fooPageNode = session.getNodeByIdentifier(fooPage.getCanonicalIdentifier());
        testContext.initFooPageNodeLocation = fooPageNode.getPath();


        final HstRequestContext ctx = mountResource.getPageComposerContextService().getRequestContext();

        // add container items
        final ContainerComponentResource containerResource = createContainerResource();
        for (String relContainerPath : relContainerPathsForItem) {
            String elems[] = relContainerPath.split("/");
            HstComponentConfiguration container = fooPage;
            for (String elem : elems) {
                assertNotNull(container);
                container = container.getChildByName(elem);
            }
            assertEquals(HstComponentConfiguration.Type.CONTAINER_COMPONENT, container.getComponentType());
            // override the config identifier to now set the container from the prototype as REQUEST_CONFIG_NODE_IDENTIFIER
            ctx.setAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER, container.getCanonicalIdentifier());
            final long versionStamp = 0;
            final Response addedItem = containerResource.createContainerItem(testContext.catalogItem.getIdentifier(),
                    versionStamp);
            assertEquals(Response.Status.OK.getStatusCode(), addedItem.getStatus());
            ContainerItemRepresentation cir = (ContainerItemRepresentation)((ExtResponseRepresentation) addedItem.getEntity()).getData();
            assertTrue(
                    cir.getPath().startsWith("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:pages/foo-prototype-page/" + relContainerPath));
            testContext.addedPageContainerItems.add(cir);
        }
        return testContext;
    }


    private void reapplyPrototype(final TestContext testContext, final String uuid) throws Exception {
        final SiteMapItemRepresentation fooToUpdate = getSiteMapItemRepresentation(session, "foo");
        fooToUpdate.setComponentConfigurationId(uuid);
        final Response reappliedSamePrototype = testContext.siteMapResource.update(fooToUpdate);
        assertEquals(Response.Status.OK.getStatusCode(), reappliedSamePrototype.getStatus());
    }


    @Test
    public void test_reapply_prototype_with_same_containers() throws Exception {
        // empty means copy of existing one
        final String[] secondPrototype = {};
        final String[] relContainerPathsForItem = {"main/container1", "main/container1", "main/container2"};

        final TestContext testContext = initContextAndFixture(secondPrototype, relContainerPathsForItem);
        reapplyPrototype(testContext, testContext.secondPrototypeUUID);

        final SiteMapItemRepresentation updatedFoo = getSiteMapItemRepresentation(session, "foo");
        assertEquals("foo", updatedFoo.getName());
        final String newFooPageId = updatedFoo.getComponentConfigurationId();
        Node secondPrototypeNode = session.getNodeByIdentifier(testContext.secondPrototypeUUID);
        assertEquals("hst:pages/foo-"+secondPrototypeNode.getName(), newFooPageId);

        final HstComponentConfiguration updatedFooPage = mountResource.getPageComposerContextService().getEditingPreviewSite()
                .getComponentsConfiguration().getComponentConfiguration(newFooPageId);

        assertEquals("foo-"+secondPrototypeNode.getName(), updatedFooPage.getName());

        Node updatedFooPageNode = session.getNodeByIdentifier(updatedFooPage.getCanonicalIdentifier());
        // assert old page node does not exist any more because no live version
        assertFalse(session.nodeExists(testContext.initFooPageNodeLocation));
        // assert page is locked
        assertTrue(updatedFooPageNode.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY));
        // assert page containers are locked
        final Node newContainer1 = updatedFooPageNode.getNode("main/container1");
        final Node newContainer2 = updatedFooPageNode.getNode("main/container2");
        assertTrue(newContainer1.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY));
        assertTrue(newContainer2.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY));
        assertTrue(newContainer1.hasNode("catalog-item"));
        assertTrue(newContainer1.hasNode("catalog-item1"));
        assertTrue(newContainer2.hasNode("catalog-item"));
        assertFalse("second container should not have second catalog item", newContainer2.hasNode("catalog-item1"));
    }

    @Test
    public void test_reapply_to_same_prototype() throws Exception {
        // empty means copy of existing one
        final String[] secondPrototype = {};
        final String[] relContainerPathsForItem = {"main/container1", "main/container1", "main/container2"};

        final TestContext testContext = initContextAndFixture(secondPrototype, relContainerPathsForItem);

        reapplyPrototype(testContext, testContext.prototypeUUID);

        final SiteMapItemRepresentation updatedFoo = getSiteMapItemRepresentation(session, "foo");
        assertEquals("foo", updatedFoo.getName());
        final String newFooPageId = updatedFoo.getComponentConfigurationId();
        Node prototypeNode = session.getNodeByIdentifier(testContext.prototypeUUID);
        // since re-apply prototype is done with same prototype page, the new page will get a -1 postfix
        assertEquals("hst:pages/foo-"+prototypeNode.getName() + "-1", newFooPageId);

        final HstComponentConfiguration updatedFooPage = mountResource.getPageComposerContextService().getEditingPreviewSite()
                .getComponentsConfiguration().getComponentConfiguration(newFooPageId);

        assertEquals("foo-"+prototypeNode.getName() + "-1", updatedFooPage.getName());

        Node updatedFooPageNode = session.getNodeByIdentifier(updatedFooPage.getCanonicalIdentifier());
        // assert old page node does not exist any more because no live version
        assertFalse(session.nodeExists(testContext.initFooPageNodeLocation));
        // assert page is locked
        assertTrue(updatedFooPageNode.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY));
        // assert page containers are locked
        final Node newContainer1 = updatedFooPageNode.getNode("main/container1");
        final Node newContainer2 = updatedFooPageNode.getNode("main/container2");
        assertTrue(newContainer1.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY));
        assertTrue(newContainer2.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY));
        assertTrue(newContainer1.hasNode("catalog-item"));
        assertTrue(newContainer1.hasNode("catalog-item1"));
        assertTrue(newContainer2.hasNode("catalog-item"));
    }

    @Test
    public void test_reapply_prototype_with_same_containers_after_publication() throws Exception {
        // empty means copy of existing one
        final String[] secondPrototype = {};
        final String[] relContainerPathsForItem = {"main/container1", "main/container1", "main/container2"};

        final TestContext testContext = initContextAndFixture(secondPrototype, relContainerPathsForItem);
        mountResource.publish();

        reapplyPrototype(testContext, testContext.secondPrototypeUUID);

        final String newFooPageId = getSiteMapItemRepresentation(session, "foo").getComponentConfigurationId();

        final HstComponentConfiguration updatedFooPage = mountResource.getPageComposerContextService().getEditingPreviewSite()
                .getComponentsConfiguration().getComponentConfiguration(newFooPageId);

        // assert old page node does still exists and is locked because of live version!!!
        assertTrue(session.nodeExists(testContext.initFooPageNodeLocation));
        assertTrue(session.getNode(testContext.initFooPageNodeLocation).hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY));

        // assert page is locked
        Node updatedFooPageNode = session.getNodeByIdentifier(updatedFooPage.getCanonicalIdentifier());
        assertTrue(updatedFooPageNode.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY));
    }

    private void reapplyPrototypeAndRenameSiteMapItem(final TestContext testContext,
                                                      final String reapplyUUID,
                                                      final String newName) throws Exception {
        final SiteMapItemRepresentation fooToUpdate = getSiteMapItemRepresentation(session, "foo");
        fooToUpdate.setName(newName);
        fooToUpdate.setComponentConfigurationId(reapplyUUID);
        final Response reapplied = testContext.siteMapResource.update(fooToUpdate);
        assertEquals(Response.Status.OK.getStatusCode(), reapplied.getStatus());
    }


    @Test
    public void test_reapply_prototype_with_same_containers_and_rename_sitemap() throws Exception {
        // empty means copy of existing one
        final String[] secondPrototype = {};
        final String[] relContainerPathsForItem = {"main/container1", "main/container1", "main/container2"};
        final TestContext testContext = initContextAndFixture(secondPrototype, relContainerPathsForItem);
        reapplyPrototypeAndRenameSiteMapItem(testContext, testContext.secondPrototypeUUID, "bar");

        assertNull(getSiteMapItemRepresentation(session, "foo"));

        final SiteMapItemRepresentation updatedItem = getSiteMapItemRepresentation(session, "bar");
        assertEquals("bar", updatedItem.getName());
        final String newBarPageId = updatedItem.getComponentConfigurationId();
        Node prototypeNode = session.getNodeByIdentifier(testContext.secondPrototypeUUID);
        // also pages should get the new sitemap item name as prefix
        assertEquals("hst:pages/bar-"+prototypeNode.getName(), newBarPageId);

        final HstComponentConfiguration updatedPage = mountResource.getPageComposerContextService().getEditingPreviewSite()
                .getComponentsConfiguration().getComponentConfiguration(newBarPageId);

        assertEquals("bar-" + prototypeNode.getName(), updatedPage.getName());

    }

    @Test
    public void test_reapply_prototype_with_same_containers_after_rename_sitemap() throws Exception {
        // empty means copy of existing one
        final String[] secondPrototype = {};
        final String[] relContainerPathsForItem = {"main/container1", "main/container1", "main/container2"};
        final TestContext testContext = initContextAndFixture(secondPrototype, relContainerPathsForItem);

        final SiteMapItemRepresentation toUpdate = getSiteMapItemRepresentation(session, "foo");
        toUpdate.setName("bar");
        testContext.siteMapResource.update(toUpdate);

        final SiteMapItemRepresentation toUpdateAgain = getSiteMapItemRepresentation(session, "bar");
        toUpdateAgain.setComponentConfigurationId(testContext.secondPrototypeUUID);
        final Response reapplied = testContext.siteMapResource.update(toUpdateAgain);
        assertEquals(Response.Status.OK.getStatusCode(), reapplied.getStatus());
        // also pages should get the new sitemap item name as prefix

        final SiteMapItemRepresentation updatedAgain = getSiteMapItemRepresentation(session, "bar");
        final String newBarPageId = updatedAgain.getComponentConfigurationId();
        Node prototypeNode = session.getNodeByIdentifier(testContext.secondPrototypeUUID);
        assertEquals("hst:pages/bar-"+prototypeNode.getName(), newBarPageId);

        final HstComponentConfiguration updatedPage = mountResource.getPageComposerContextService().getEditingPreviewSite()
                .getComponentsConfiguration().getComponentConfiguration(newBarPageId);

        assertEquals("bar-" + prototypeNode.getName(), updatedPage.getName());
    }


    @Test
    public void test_reapply_prototype_with_same_and_other_container() throws Exception {
        final String[] secondPrototype = new String[] {
                "/hst:hst/hst:configurations/unittestproject/hst:prototypepages/protoxxx", HstNodeTypes.NODETYPE_HST_COMPONENT,
                    HstNodeTypes.COMPONENT_PROPERTY_REFERECENCECOMPONENT, "hst:abstractpages/basepage",
                    "/hst:hst/hst:configurations/unittestproject/hst:prototypepages/protoxxx/main", HstNodeTypes.NODETYPE_HST_COMPONENT,
                        "hst:template", "prototype",
                "/hst:hst/hst:configurations/unittestproject/hst:prototypepages/protoxxx/main/container1", HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT,
                        HstNodeTypes.COMPONENT_PROPERTY_XTYPE, "HST.vBox",
                "/hst:hst/hst:configurations/unittestproject/hst:prototypepages/protoxxx/main/holder2", HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT,
                HstNodeTypes.COMPONENT_PROPERTY_XTYPE, "HST.vBox"
        };
        final String[] relContainerPathsForItem = {"main/container1", "main/container1", "main/container2"};

        final TestContext testContext = initContextAndFixture(secondPrototype, relContainerPathsForItem);

        reapplyPrototype(testContext, testContext.secondPrototypeUUID);

        final SiteMapItemRepresentation updatedFoo = getSiteMapItemRepresentation(session, "foo");
        assertEquals("foo", updatedFoo.getName());
        final String newFooPageId = updatedFoo.getComponentConfigurationId();
        Node secondPrototypeNode = session.getNodeByIdentifier(testContext.secondPrototypeUUID);
        assertEquals("hst:pages/foo-"+secondPrototypeNode.getName(), newFooPageId);

        final HstComponentConfiguration updatedFooPage = mountResource.getPageComposerContextService().getEditingPreviewSite()
                .getComponentsConfiguration().getComponentConfiguration(newFooPageId);
        assertEquals("foo-"+secondPrototypeNode.getName(), updatedFooPage.getName());

        Node updatedFooPageNode = session.getNodeByIdentifier(updatedFooPage.getCanonicalIdentifier());

        final Node newContainer1 = updatedFooPageNode.getNode("main/container1");
        final Node newContainer2 = updatedFooPageNode.getNode("main/holder2");

        /*
         *
         * Since the page before re-applying prototype container
         * + main
         *    + container1
         *        + catalog-item
         *        + catalog-item1
         *    + container2
         *        + catalog-item
         *
         * AND it got mapped to secondPrototype:
         *
         * + protoxxx
         *   + main
         *      + container1
         *      + holder2
         *
         * We expect all catalog items to be moved to 'container1' (the first container, since there is no primary container
         * specified on secondPrototype). The container2/catalog-item should be relocated to container2/catalog-item2
         */

        assertTrue(newContainer1.hasNode("catalog-item"));
        assertTrue(newContainer1.hasNode("catalog-item1"));
        assertTrue("Expected the catalog item from container2 to be moved to container1 and " +
                "added a postfix '2' since there are already two with same name.", newContainer1.hasNode("catalog-item2"));
        Assert.assertEquals(0, newContainer2.getNodes().getSize());
    }


    @Test
    public void test_reapply_prototype_with_only_other_containers_gets_items_moved_to_first_container() throws Exception {
        final String[] secondPrototype = new String[] {
                "/hst:hst/hst:configurations/unittestproject/hst:prototypepages/protoxxx", HstNodeTypes.NODETYPE_HST_COMPONENT,
                    HstNodeTypes.COMPONENT_PROPERTY_REFERECENCECOMPONENT, "hst:abstractpages/basepage",
                    "/hst:hst/hst:configurations/unittestproject/hst:prototypepages/protoxxx/main", HstNodeTypes.NODETYPE_HST_COMPONENT,
                        "hst:template", "prototype",
                        "/hst:hst/hst:configurations/unittestproject/hst:prototypepages/protoxxx/main/holder1", HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT,
                            HstNodeTypes.COMPONENT_PROPERTY_XTYPE, "HST.vBox",
                        "/hst:hst/hst:configurations/unittestproject/hst:prototypepages/protoxxx/main/holder2", HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT,
                            HstNodeTypes.COMPONENT_PROPERTY_XTYPE, "HST.vBox"
        };
        final String[] relContainerPathsForItem = {"main/container1", "main/container1", "main/container2"};

        final TestContext testContext = initContextAndFixture(secondPrototype, relContainerPathsForItem);

        reapplyPrototype(testContext, testContext.secondPrototypeUUID);

        final SiteMapItemRepresentation updatedFoo = getSiteMapItemRepresentation(session, "foo");
        final String newFooPageId = updatedFoo.getComponentConfigurationId();

        final HstComponentConfiguration updatedFooPage = mountResource.getPageComposerContextService().getEditingPreviewSite()
                .getComponentsConfiguration().getComponentConfiguration(newFooPageId);

        Node updatedFooPageNode = session.getNodeByIdentifier(updatedFooPage.getCanonicalIdentifier());

        final Node newContainer1 = updatedFooPageNode.getNode("main/holder1");
        final Node newContainer2 = updatedFooPageNode.getNode("main/holder2");

        /*
         *
         * Since the page before re-applying prototype container
         * + main
         *    + container1
         *        + catalog-item
         *        + catalog-item1
         *    + container2
         *        + catalog-item
         *
         * AND it got mapped to secondPrototype:
         *
         * + protoxxx
         *   + main
         *      + holder1
         *      + holder2
         *
         * We expect all catalog items to be moved to 'holder1' (the first container, since there is no primary container
         * specified on secondPrototype). The container2/catalog-item should be relocated to container2/catalog-item2
         */

        assertTrue(newContainer1.hasNode("catalog-item"));
        assertTrue(newContainer1.hasNode("catalog-item1"));
        assertTrue("Expected the catalog item from container2 to be moved to container1 and " +
                "added a postfix '2' since there are already two with same name.",
                newContainer1.hasNode("catalog-item2"));
        assertEquals(0, newContainer2.getNodes().getSize());
    }

    @Test
    public void test_reapply_prototype_with_other_containers_gets_items_moved_to_marked_container() throws Exception {
        final String[] secondPrototype = new String[] {
                "/hst:hst/hst:configurations/unittestproject/hst:prototypepages/protoxxx", HstNodeTypes.NODETYPE_HST_COMPONENT,
                    HstNodeTypes.COMPONENT_PROPERTY_REFERECENCECOMPONENT, "hst:abstractpages/basepage",
                    "jcr:mixinTypes", HstNodeTypes.MIXINTYPE_HST_PROTOTYPE_META,
                     HstNodeTypes.PROTOTYPE_META_PRIMARY_CONTAINER, "main/holder2",
                    "/hst:hst/hst:configurations/unittestproject/hst:prototypepages/protoxxx/main", HstNodeTypes.NODETYPE_HST_COMPONENT,
                        "hst:template", "prototype",
                         "/hst:hst/hst:configurations/unittestproject/hst:prototypepages/protoxxx/main/holder1", HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT,
                             HstNodeTypes.COMPONENT_PROPERTY_XTYPE, "HST.vBox",
                        "/hst:hst/hst:configurations/unittestproject/hst:prototypepages/protoxxx/main/holder2", HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT,
                             HstNodeTypes.COMPONENT_PROPERTY_XTYPE, "HST.vBox"
        };
        final String[] relContainerPathsForItem = {"main/container1", "main/container1", "main/container2"};

        final TestContext testContext = initContextAndFixture(secondPrototype, relContainerPathsForItem);

        reapplyPrototype(testContext, testContext.secondPrototypeUUID);

        final SiteMapItemRepresentation updatedFoo = getSiteMapItemRepresentation(session, "foo");
        final String newFooPageId = updatedFoo.getComponentConfigurationId();

        final HstComponentConfiguration updatedFooPage = mountResource.getPageComposerContextService().getEditingPreviewSite()
                .getComponentsConfiguration().getComponentConfiguration(newFooPageId);

        Node updatedFooPageNode = session.getNodeByIdentifier(updatedFooPage.getCanonicalIdentifier());

        final Node newContainer1 = updatedFooPageNode.getNode("main/holder1");
        final Node newContainer2 = updatedFooPageNode.getNode("main/holder2");

        /*
         *
         * Since the page before re-applying prototype container
         * + main
         *    + container1
         *        + catalog-item
         *        + catalog-item1
         *    + container2
         *        + catalog-item
         *
         * AND it got mapped to secondPrototype:
         *
         * + protoxxx
         *   - hst:primarycontainer = main/holder2
         *   + main
         *      + holder1
         *      + holder2
         *
         * We expect all catalog items to be moved to 'holder2' (the second container, since there is a primary container
         * specified on secondPrototype).
         */

        assertTrue(newContainer2.hasNode("catalog-item"));
        assertTrue(newContainer2.hasNode("catalog-item1"));
        assertTrue("Expected the catalog item from container1 to be moved to container2 and " +
                "added a postfix '2' since there are already two with same name.",newContainer2.hasNode("catalog-item2"));
        assertEquals(0, newContainer1.getNodes().getSize());

    }

    @Test
    public void test_reapply_prototype_with_no_container_gets_existing_items_deleted() throws Exception {
        final String[] secondPrototype = new String[] {
                "/hst:hst/hst:configurations/unittestproject/hst:prototypepages/protoxxx", HstNodeTypes.NODETYPE_HST_COMPONENT,
                    HstNodeTypes.COMPONENT_PROPERTY_REFERECENCECOMPONENT, "hst:abstractpages/basepage",
                    "/hst:hst/hst:configurations/unittestproject/hst:prototypepages/protoxxx/main", HstNodeTypes.NODETYPE_HST_COMPONENT,
                        "hst:template", "prototype"
        };
        final String[] relContainerPathsForItem = {"main/container1", "main/container1", "main/container2"};

        final TestContext testContext = initContextAndFixture(secondPrototype, relContainerPathsForItem);

        final SiteMapItemRepresentation beforeReapply = getSiteMapItemRepresentation(session, "foo");
        final String beforeReapplyId = beforeReapply.getComponentConfigurationId();

        final HstComponentConfiguration beforeReapplyPage = mountResource.getPageComposerContextService().getEditingPreviewSite()
                .getComponentsConfiguration().getComponentConfiguration(beforeReapplyId);

        final Node pageNodeBeforeReapply = session.getNodeByIdentifier(beforeReapplyPage.getCanonicalIdentifier());
        String catalogUUID1 = pageNodeBeforeReapply.getNode("main/container1/catalog-item").getIdentifier();
        String catalogUUID2 = pageNodeBeforeReapply.getNode("main/container1/catalog-item1").getIdentifier();
        String catalogUUID3 = pageNodeBeforeReapply.getNode("main/container2/catalog-item").getIdentifier();


        reapplyPrototype(testContext, testContext.secondPrototypeUUID);

        final SiteMapItemRepresentation updatedFoo = getSiteMapItemRepresentation(session, "foo");
        assertEquals("foo", updatedFoo.getName());
        final String newFooPageId = updatedFoo.getComponentConfigurationId();
        Node secondPrototypeNode = session.getNodeByIdentifier(testContext.secondPrototypeUUID);
        assertEquals("hst:pages/foo-"+secondPrototypeNode.getName(), newFooPageId);

        final HstComponentConfiguration updatedFooPage = mountResource.getPageComposerContextService().getEditingPreviewSite()
                .getComponentsConfiguration().getComponentConfiguration(newFooPageId);
        assertEquals("foo-"+secondPrototypeNode.getName(), updatedFooPage.getName());

        /*
         *
         * Since the page before re-applying prototype container
         * + main
         *    + container1
         *        + catalog-item
         *        + catalog-item1
         *    + container2
         *        + catalog-item
         *
         * AND it got mapped to secondPrototype:
         *
         * + protoxxx
         *    + main
         *
         * We expect all catalog items to be deleted
         */
        try {
            session.getNodeByIdentifier(catalogUUID1);
            fail("Expected catalog item to be deleted");
        } catch (ItemNotFoundException e) {

        }
        try {
            session.getNodeByIdentifier(catalogUUID2);
            fail("Expected catalog item to be deleted");
        } catch (ItemNotFoundException e) {

        }
        try {
            session.getNodeByIdentifier(catalogUUID3);
            fail("Expected catalog item to be deleted");
        } catch (ItemNotFoundException e) {

        }
    }
}
