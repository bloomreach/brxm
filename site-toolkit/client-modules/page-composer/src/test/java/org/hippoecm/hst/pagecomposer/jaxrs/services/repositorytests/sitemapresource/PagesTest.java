/*
 * Copyright 2014-2020 Hippo B.V. (http://www.onehippo.com)
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
import javax.ws.rs.core.Response;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ResponseRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.MountRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapPageRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapPagesRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.SiteMapResource;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class PagesTest extends AbstractSiteMapResourceTest {

    private void initContext() throws Exception {
        // call below will init request context
        getSiteMapItemRepresentation(session, "home");
    }

    @Test
    public void get_hostname() throws Exception {
        initContext();
        final SiteMapResource siteMapResource = createResource();
        final Response response = siteMapResource.getMountRepresentation();
        final ResponseRepresentation representation = (ResponseRepresentation) response.getEntity();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        final MountRepresentation data =  (MountRepresentation)representation.getData();
        assertThat(data.getHostName(), is("localhost"));
        assertThat(data.getMountPath(), is(""));
    }

    @Test
    public void pages_sorted_by_pathInfo() throws Exception {
        initContext();
        final SiteMapResource siteMapResource = createResource();
        final Response response = siteMapResource.getSiteMapPages(null);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        SiteMapPagesRepresentation siteMapPagesRepresentation =
                (SiteMapPagesRepresentation) ((ResponseRepresentation) response.getEntity()).getData();

        SiteMapPageRepresentation prev = null;
        for (SiteMapPageRepresentation siteMapPageRepresentation : siteMapPagesRepresentation.getPages()) {
            if (prev == null) {
                prev = siteMapPageRepresentation;
                continue;
            }
            assertTrue(siteMapPageRepresentation.getPathInfo().compareTo(prev.getPathInfo()) >= 0);
            prev = siteMapPageRepresentation;
        }
    }

    @Test
    public void homepage_is_first_and_pathInfo_equals_slash() throws Exception {
        initContext();
        final SiteMapResource siteMapResource = createResource();
        final Response response = siteMapResource.getSiteMapPages(null);
        SiteMapPagesRepresentation siteMapPagesRepresentation =
                (SiteMapPagesRepresentation) ((ResponseRepresentation) response.getEntity()).getData();

        assertEquals("/", siteMapPagesRepresentation.getPages().get(0).getPathInfo());
        assertEquals("home", siteMapPagesRepresentation.getPages().get(0).getName());
    }

    @Test
    public void skip_page_by_sitemap_item_property_hiddeninchannelmanager() throws Exception {
        // mark homepage to be hidden from pages in channel manager
        final Node home = session.getNode("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:sitemap/home");
        home.setProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_HIDDEN_IN_CHANNEL_MANAGER, true);
        session.save();
        initContext();
        final SiteMapResource siteMapResource = createResource();
        final Response response = siteMapResource.getSiteMapPages(null);
        SiteMapPagesRepresentation siteMapPagesRepresentation =
                (SiteMapPagesRepresentation) ((ResponseRepresentation) response.getEntity()).getData();
        assertFalse("home".equals(siteMapPagesRepresentation.getPages().get(0).getName()));
    }

    @Test
    public void skip_page_for_webfiles_because_container_resource_is_true() throws Exception {
        initContext();
        final SiteMapResource siteMapResource = createResource();
        final Response response = siteMapResource.getSiteMapPages(null);
        SiteMapPagesRepresentation siteMapPagesRepresentation =
                (SiteMapPagesRepresentation) ((ResponseRepresentation) response.getEntity()).getData();

        for (SiteMapPageRepresentation siteMapPageRepresentation : siteMapPagesRepresentation.getPages()) {
            // hst:default/hst:siteap/webfiles sitemap item has hst:containerresource = true hence not part of pages overview
            assertFalse("webfiles".equals(siteMapPageRepresentation.getPathInfo()));
        }
    }

    @Test
    public void dont_skip_page_for_webfiles_if_container_resource_set_to_false() throws Exception {
        session.getNode("/hst:hst/hst:configurations/hst:default/hst:sitemap/webfiles")
                .setProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_CONTAINER_RESOURCE, false);
        session.save();
        Thread.sleep(200);
        initContext();
        final SiteMapResource siteMapResource = createResource();
        final Response response = siteMapResource.getSiteMapPages(null);
        SiteMapPagesRepresentation siteMapPagesRepresentation =
                (SiteMapPagesRepresentation) ((ResponseRepresentation) response.getEntity()).getData();

        boolean found = false;
        for (SiteMapPageRepresentation siteMapPageRepresentation : siteMapPagesRepresentation.getPages()) {
            // hst:default/hst:sitemap/webfiles sitemap item has now hst:containerresource = false hence should be part of pages overview
            if ("webfiles".equals(siteMapPageRepresentation.getPathInfo())) {
                found = true;
                break;
            }
        }
        assertTrue("page with pathInfo 'webfiles' expected", found);
    }

    @Test
    public void skip_pages_for_sitemap_items_that_are_index_items() throws Exception {
        initContext();
        final SiteMapResource siteMapResource = createResource();
        final Response response = siteMapResource.getSiteMapPages(null);
        SiteMapPagesRepresentation siteMapPagesRepresentation =
                (SiteMapPagesRepresentation) ((ResponseRepresentation) response.getEntity()).getData();

        boolean found = false;
        for (SiteMapPageRepresentation siteMapPageRepresentation : siteMapPagesRepresentation.getPages()) {
            if ("_index_".equals(siteMapPageRepresentation.getName())) {
                found = true;
                break;
            }
        }
        assertFalse("page for sitemap item '_index_' should be skipped", found);
    }


    // If set to 'false' the /webfiles sitemap
    // item should become visible as channel mngr page

    @Test
    public void sitemap_item_page_title() throws Exception {
        final Node home = session.getNode("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:sitemap/home");
        home.setProperty(HstNodeTypes.SITEMAPITEM_PAGE_TITLE, "foo");
        session.save();
        Thread.sleep(200);
        initContext();
        final SiteMapResource siteMapResource = createResource();
        final Response response = siteMapResource.getSiteMapPages(null);
        SiteMapPagesRepresentation siteMapPagesRepresentation =
                (SiteMapPagesRepresentation) ((ResponseRepresentation) response.getEntity()).getData();

        assertEquals("/", siteMapPagesRepresentation.getPages().get(0).getPathInfo());
        assertEquals("home", siteMapPagesRepresentation.getPages().get(0).getName());
        assertEquals("foo", siteMapPagesRepresentation.getPages().get(0).getPageTitle());
    }

    @Test
    public void sitemap_contains_host_and_mountPath() throws Exception {
        initContext();
        final SiteMapResource siteMapResource = createResource();
        final Response response = siteMapResource.getSiteMapPages(null);
        SiteMapPagesRepresentation siteMapPagesRepresentation =
                (SiteMapPagesRepresentation) ((ResponseRepresentation) response.getEntity()).getData();
        assertEquals("localhost", siteMapPagesRepresentation.getHost());
        assertEquals("", siteMapPagesRepresentation.getMount());
    }

    @Test
    public void page_contains_container_item_in_page_definition() throws Exception {
        final Node container = session.getNode("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:pages/homepage")
                .addNode("container", HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT);
        container.setProperty(HstNodeTypes.COMPONENT_PROPERTY_XTYPE, "HST.vBox");
        final Node containerItem = container.addNode("item", HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT);
        containerItem.setProperty(HstNodeTypes.COMPONENT_PROPERTY_XTYPE, "HST.Item");
        session.save();
        Thread.sleep(200);

        final SiteMapItemRepresentation homePage = getHomePage();
        assertTrue(homePage.getHasContainerItemInPageDefinition());
    }

    @Test
    public void page_contains_container_item_in_referenced_container() throws Exception {
        final Node workspace = session.getNode("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace");
        final Node containers = workspace.addNode("hst:containers", "hst:containercomponentfolder");
        final Node containerNode = containers.addNode("testcontainer", "hst:containercomponent");
        containerNode.setProperty("hst:xtype", "HST.vBox");
        final Node containerItem = containerNode.addNode("item", HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT);
        containerItem.setProperty(HstNodeTypes.COMPONENT_PROPERTY_XTYPE, "HST.Item");
        final Node homePageContainer = session.getNode("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:pages/homepage")
                .addNode("container", "hst:containercomponentreference");
        homePageContainer.setProperty("hst:referencecomponent", "testcontainer");

        session.save();
        Thread.sleep(200);

        final SiteMapItemRepresentation homePage = getHomePage();
        // REFERENCED items do not count!
        assertFalse(homePage.getHasContainerItemInPageDefinition());

        final HstComponentConfiguration homePageConfig = mountResource.getPageComposerContextService().getEditingPreviewSite()
                .getComponentsConfiguration().getComponentConfiguration(homePage.getComponentConfigurationId());

        assertNotNull("Although SiteMapPageRepresentation returns isHasContainerItemInPageDefinition() as false, the " +
                "page instance (not definition) has a container item.", homePageConfig.getChildByName("container").getChildByName("item"));
    }


    @Test
    public void page_contains_container_item_in_referenced_page_definition() throws Exception {
        session.move("/hst:hst/hst:configurations/unittestcommon/hst:abstractpages",
                "/hst:hst/hst:configurations/unittestproject/hst:abstractpages");
        Node basepage = session.getNode("/hst:hst/hst:configurations/unittestproject/hst:abstractpages/basepage");
        final Node container = basepage.addNode("basecontainer", HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT);
        container.setProperty(HstNodeTypes.COMPONENT_PROPERTY_XTYPE, "HST.vBox");
        final Node containerItem = container.addNode("item", HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT);
        containerItem.setProperty(HstNodeTypes.COMPONENT_PROPERTY_XTYPE, "HST.Item");

        JcrUtils.copy(session, "/hst:hst/hst:configurations/unittestproject/hst:abstractpages",
                "/hst:hst/hst:configurations/unittestproject-preview/hst:abstractpages");
        session.save();
        Thread.sleep(200);
        final SiteMapItemRepresentation homePage = getHomePage();
        // REFERENCED items do not count!
        assertFalse(homePage.getHasContainerItemInPageDefinition());

        final HstComponentConfiguration homePageConfig = mountResource.getPageComposerContextService().getEditingPreviewSite()
                .getComponentsConfiguration().getComponentConfiguration(homePage.getComponentConfigurationId());

        assertNotNull("Although SiteMapPageRepresentation returns isHasContainerItemInPageDefinition() as false, the " +
                "page instance (not definition) has a container item.", homePageConfig.getChildByName("basecontainer").getChildByName("item"));

    }

    @Test
    public void page_contains_container_item_in_inherited_page_definition() throws Exception {
        Node basepage = session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:abstractpages/basepage");

        final Node container = basepage.addNode("basecontainer", HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT);
        container.setProperty(HstNodeTypes.COMPONENT_PROPERTY_XTYPE, "HST.vBox");
        final Node containerItem = container.addNode("item", HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT);
        containerItem.setProperty(HstNodeTypes.COMPONENT_PROPERTY_XTYPE, "HST.Item");
        session.save();

        Thread.sleep(200);
        final SiteMapItemRepresentation homePage = getHomePage();
        // INHERITED CONFIGURATION items do not count!
        assertFalse(homePage.getHasContainerItemInPageDefinition());

        final HstComponentConfiguration homePageConfig = mountResource.getPageComposerContextService().getEditingPreviewSite()
                .getComponentsConfiguration().getComponentConfiguration(homePage.getComponentConfigurationId());

        assertNotNull("Although SiteMapPageRepresentation returns isHasContainerItemInPageDefinition() as false, the " +
                "page instance (not definition) has a container item.", homePageConfig.getChildByName("basecontainer").getChildByName("item"));

    }

    public SiteMapItemRepresentation getHomePage() throws Exception {
        return getSiteMapItemRepresentation(session, "home");
    }
}
