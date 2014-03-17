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
import javax.ws.rs.core.Response;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ExtResponseRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapPageRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapPagesRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.SiteMapResource;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PagesTest extends AbstractSiteMapResourceTest{

    private void initContext() throws Exception {
        // call below will init request context
        getSiteMapItemRepresentation(session, "home");
    }

    @Test
    public void test_pages_sorted_by_pathInfo() throws Exception {
        initContext();
        final SiteMapResource siteMapResource = createResource();
        final Response response = siteMapResource.getSiteMapPages();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        SiteMapPagesRepresentation siteMapPagesRepresentation =
                (SiteMapPagesRepresentation) ((ExtResponseRepresentation) response.getEntity()).getData();

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
    public void test_homepage_is_first_and_pathInfo_equals_slash() throws Exception {
        initContext();
        final SiteMapResource siteMapResource = createResource();
        final Response response = siteMapResource.getSiteMapPages();
        SiteMapPagesRepresentation siteMapPagesRepresentation =
                (SiteMapPagesRepresentation) ((ExtResponseRepresentation) response.getEntity()).getData();

        assertEquals("/", siteMapPagesRepresentation.getPages().get(0).getPathInfo());
        assertEquals("home", siteMapPagesRepresentation.getPages().get(0).getName());
    }


    @Test
    public void test_sitemap_item_page_title() throws Exception {
        final Node home = session.getNode("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:sitemap/home");
        home.setProperty(HstNodeTypes.SITEMAPITEM_PAGE_TITLE, "foo");
        session.save();
        initContext();
        final SiteMapResource siteMapResource = createResource();
        final Response response = siteMapResource.getSiteMapPages();
        SiteMapPagesRepresentation siteMapPagesRepresentation =
                (SiteMapPagesRepresentation) ((ExtResponseRepresentation) response.getEntity()).getData();

        assertEquals("/", siteMapPagesRepresentation.getPages().get(0).getPathInfo());
        assertEquals("home", siteMapPagesRepresentation.getPages().get(0).getName());
        assertEquals("foo", siteMapPagesRepresentation.getPages().get(0).getPageTitle());
    }

    @Test
    public void test_sitemap_contains_host_and_mountPath() throws Exception {
        initContext();
        final SiteMapResource siteMapResource = createResource();
        final Response response = siteMapResource.getSiteMapPages();
        SiteMapPagesRepresentation siteMapPagesRepresentation =
                (SiteMapPagesRepresentation) ((ExtResponseRepresentation) response.getEntity()).getData();
        assertEquals("localhost", siteMapPagesRepresentation.getHost());
        assertEquals("", siteMapPagesRepresentation.getMount());
    }

    @Test
    public void page_contains_container_item_in_page_definition() throws Exception {
        final Node container = session.getNode("/hst:hst/hst:configurations/unittestproject-preview/hst:pages/homepage")
                .addNode("container", HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT);
        container.setProperty(HstNodeTypes.COMPONENT_PROPERTY_XTYPE, "HST.vBox");
        final Node containerItem = container.addNode("item", HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT);
        containerItem.setProperty(HstNodeTypes.COMPONENT_PROPERTY_XTYPE, "HST.Item");
        session.save();

        final SiteMapPageRepresentation homePage = getHomePage();
        assertTrue(homePage.isHasContainerItemInPageDefinition());
    }

    @Test
    public void page_contains_container_item_in_referenced_container() throws Exception {
        final Node workspace = session.getNode("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace");
        final Node containers = workspace.addNode("hst:containers", "hst:containercomponentfolder");
        final Node containerNode = containers.addNode("testcontainer", "hst:containercomponent");
        containerNode.setProperty("hst:xtype", "HST.vBox");
        final Node containerItem = containerNode.addNode("item", HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT);
        containerItem.setProperty(HstNodeTypes.COMPONENT_PROPERTY_XTYPE, "HST.Item");
        final Node homePageContainer = session.getNode("/hst:hst/hst:configurations/unittestproject-preview/hst:pages/homepage")
                .addNode("container", "hst:containercomponentreference");
        homePageContainer.setProperty("hst:referencecomponent", "testcontainer");

        session.save();

        final SiteMapPageRepresentation homePage = getHomePage();
        // REFERENCED items do not count!
        assertFalse(homePage.isHasContainerItemInPageDefinition());

        final HstComponentConfiguration homePageConfig = mountResource.getPageComposerContextService().getEditingPreviewSite()
                .getComponentsConfiguration().getComponentConfiguration(homePage.getComponentConfigurationId());

        assertNotNull("Although SiteMapPageRepresentation returns isHasContainerItemInPageDefinition() as false, the " +
                "page instance (not definition) has a container item.", homePageConfig.getChildByName("container").getChildByName("item"));
    }


    @Test
    public void page_contains_container_item_in_referenced_page_definition()  throws Exception {
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
        final SiteMapPageRepresentation homePage = getHomePage();
        // REFERENCED items do not count!
        assertFalse(homePage.isHasContainerItemInPageDefinition());

        final HstComponentConfiguration homePageConfig = mountResource.getPageComposerContextService().getEditingPreviewSite()
                .getComponentsConfiguration().getComponentConfiguration(homePage.getComponentConfigurationId());

        assertNotNull("Although SiteMapPageRepresentation returns isHasContainerItemInPageDefinition() as false, the " +
                "page instance (not definition) has a container item.", homePageConfig.getChildByName("basecontainer").getChildByName("item"));

    }

    @Test
    public void page_contains_container_item_in_inherited_page_definition()  throws Exception {
        Node basepage = session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:abstractpages/basepage");
        final Node container = basepage.addNode("basecontainer", HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT);
        container.setProperty(HstNodeTypes.COMPONENT_PROPERTY_XTYPE, "HST.vBox");
        final Node containerItem = container.addNode("item", HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT);
        containerItem.setProperty(HstNodeTypes.COMPONENT_PROPERTY_XTYPE, "HST.Item");
        session.save();
        final SiteMapPageRepresentation homePage = getHomePage();
        // INHERITED CONFIGURATION items do not count!
        assertFalse(homePage.isHasContainerItemInPageDefinition());

        final HstComponentConfiguration homePageConfig = mountResource.getPageComposerContextService().getEditingPreviewSite()
                .getComponentsConfiguration().getComponentConfiguration(homePage.getComponentConfigurationId());

        assertNotNull("Although SiteMapPageRepresentation returns isHasContainerItemInPageDefinition() as false, the " +
                "page instance (not definition) has a container item.", homePageConfig.getChildByName("basecontainer").getChildByName("item"));

    }

    public SiteMapPageRepresentation getHomePage() throws Exception {
        initContext();
        final SiteMapResource siteMapResource = createResource();
        final Response response = siteMapResource.getSiteMapPages();
        SiteMapPagesRepresentation siteMapPagesRepresentation =
                (SiteMapPagesRepresentation) ((ExtResponseRepresentation) response.getEntity()).getData();
        return siteMapPagesRepresentation.getPages().get(0);
    }
}
